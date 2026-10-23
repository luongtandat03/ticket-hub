/*
 * @ (#) PaymentAppServiceImpl.java       1.0     10/8/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.payment.impl;
/*
 * @author: Luong Tan Dat
 * @date: 10/8/2026
 */

import com.alibaba.fastjson.JSON;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tphcm.ddd.application.dto.response.PaymentResponse;
import vn.tphcm.ddd.application.dto.response.TicketDetailResponse;
import vn.tphcm.ddd.application.dto.response.TicketOrderResponse;
import vn.tphcm.ddd.application.dto.response.TicketResponse;
import vn.tphcm.ddd.application.exception.PaymentException;
import vn.tphcm.ddd.application.mapper.PaymentMapper;
import vn.tphcm.ddd.application.service.order.TicketOrderAppService;
import vn.tphcm.ddd.application.service.payment.PaymentAppService;
import vn.tphcm.ddd.application.service.ticket.TicketAppService;
import vn.tphcm.ddd.application.service.ticket.TicketDetailAppService;
import vn.tphcm.ddd.domain.model.OutboxEvent;
import vn.tphcm.ddd.domain.model.PaymentTransaction;
import vn.tphcm.ddd.domain.model.User;
import vn.tphcm.ddd.domain.repository.OutboxEventRepository;
import vn.tphcm.ddd.domain.service.PaymentTransactionDomainService;
import vn.tphcm.ddd.domain.service.UserDomainService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedService;
import vn.tphcm.ddd.infrastructure.gateway.VnPayGatewayService;
import vn.tphcm.ddd.infrastructure.mq.email.EmailMQMessage;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-APP-SERVICE")
public class PaymentAppServiceImpl implements PaymentAppService {
    private final PaymentTransactionDomainService paymentTransactionDomainService;
    private final TicketOrderAppService ticketOrderAppService;
    private final TicketAppService ticketAppService;
    private final TicketDetailAppService ticketDetailAppService;
    private final RedisDistributedService redisDistributedService;
    private final VnPayGatewayService vnPayGatewayService;
    private final TransactionTemplate transactionTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final UserDomainService userDomainService;
    private final PaymentMapper paymentMapper;

    @Override
    public String paymentOrder(String orderNumber, String method) throws UnsupportedEncodingException {
        log.info("paymentOrder | {}, {}", orderNumber, method);

        RedisDistributedLocker lock = redisDistributedService.getRedisDistributedLock(genPaymentLock(orderNumber));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = (User) authentication.getPrincipal();

        try {
            boolean isLock = lock.tryLock(1, 5, TimeUnit.SECONDS);

            if (!isLock) {
                throw new PaymentException("Transaction is handling, please try again later");
            }

            TicketOrderResponse ticketOrder = ticketOrderAppService.findByOrderNumber(orderNumber);

            if (ticketOrder == null || !Integer.valueOf(0).equals(ticketOrder.getOrderStatus())) {
                throw new PaymentException("Payment Order Not Found");
            }

            PaymentTransaction existing = paymentTransactionDomainService.findByOrderNumber(orderNumber);

            if (existing != null && Integer.valueOf(1).equals(existing.getPaymentStatus())) {
                return existing.getPaymentUrl();
            }

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .paymentId(UUID.randomUUID().toString())
                    .orderNumber(orderNumber)
                    .userId(user.getId())
                    .amount(ticketOrder.getTotalAmount())
                    .paymentMethod(method)
                    .createDate(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                            .format(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))))
                    .build();

            paymentTransactionDomainService.createPaymentTransaction(transaction);

            String paymentUrl = vnPayGatewayService.createPaymentUrl(transaction);

            paymentTransactionDomainService.updateTransactionInProgress(
                    transaction.getPaymentId(),
                    paymentUrl,
                    transaction.getGatewayTransactionId(),
                    1);

            return paymentUrl;

        } catch (PaymentException e) {
            log.error("Payment was error {}", e.getMessage());
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Payment was interrupted, please try again later " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<String, String> handleVnPayIPN(Map<String, String> request) {
        if (!vnPayGatewayService.verifySignature(request)) {
            return rsp("97", "Invalid Signature");
        }

        String orderNumber = extractOrderNumber(request.get("vnp_OrderInfo"));

        if (orderNumber == null) {
            return rsp("01", "Payment not found");
        }

        RedisDistributedLocker locker = redisDistributedService.getRedisDistributedLock(genPaymentLock(orderNumber));
        try {
            boolean isLock = locker.tryLock(1, 5, TimeUnit.SECONDS);

            if (!isLock) {
                throw new PaymentException("Transaction is handling, please try again later");
            }

            return transactionTemplate.execute(tx -> processIpn(orderNumber, request));
        } catch (InterruptedException e) {
            log.error("Payment was interrupted, please try again later {}", e.getMessage());
            Thread.currentThread().interrupt();
            return rsp("99", "Unknown Error");
        } catch (Exception e) {
            log.error("VNPay IPN error", e);
            return rsp("99", "Unknow error");
        } finally {
            locker.unlock();
        }
    }

    @Override
    public List<PaymentResponse> findAll() {
        List<PaymentTransaction> paymentResponseList = paymentTransactionDomainService.findAll();

        return paymentResponseList
                .stream()
                .map(paymentMapper::toDto)
                .toList();
    }

    @Override
    public int reconcileStuckPayment(long expiredTime, int limit) {
        LocalDateTime createdBefore = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(expiredTime);

        List<PaymentTransaction> stuck = paymentTransactionDomainService.findStuckInProgress(createdBefore, limit);

        int resolved = 0;

        for (PaymentTransaction transaction : stuck) {
            try {
                if (reconcile(transaction.getOrderNumber())) {
                    resolved++;
                }
            } catch (Exception e) {
                log.error("[RECONCILE] failed for order {}", transaction.getOrderNumber(), e);
            }
        }

        return resolved;
    }

    private boolean reconcile(String orderNumber) {
        RedisDistributedLocker locker = redisDistributedService.getRedisDistributedLock(genPaymentLock(orderNumber));

        try {
            boolean isLock = locker.tryLock(1, 5, TimeUnit.SECONDS);

            if (!isLock) {
                return false;
            }

            PaymentTransaction paymentTransaction = paymentTransactionDomainService.findByOrderNumber(orderNumber);
            if (paymentTransaction == null || !Integer.valueOf(1).equals(paymentTransaction.getPaymentStatus())) {
                log.warn("Payment Transaction not pending");
                return false;
            }

            Map<String, String> response = vnPayGatewayService.queryTransaction(paymentTransaction);

            if (response == null) {
                log.warn("Payment Transaction not found");
                return false;
            }

            String responseCode = response.get("vnp_ResponseCode");

            if ("91".equals(responseCode)) {
                transactionTemplate.executeWithoutResult(status -> failPayment(paymentTransaction));
                return true;
            }

            if (!"00".equals(responseCode)) {
                log.warn("[RECONCILE] querydr order={} responseCode={}", orderNumber, responseCode);
                return false;
            }

            String status = response.get("vnp_TransactionStatus");
            if ("01".equals(status)) {
                return false;
            }

            if ("00".equals(status)) {
                long expected = paymentTransaction.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();

                if (!String.valueOf(expected).equals(response.get("vnp_Amount"))) {
                    log.error("[RECONCILE] amount mismatch order={} expected={} vnpay={}",
                            orderNumber, expected, response.get("vnp_Amount"));
                    return false;
                }

                paymentTransaction.setGatewayTransactionId(response.get("vnp_TransactionNo"));
                transactionTemplate.executeWithoutResult(paymentStatus -> successPayment(paymentTransaction));
                return true;
            }
            transactionTemplate.executeWithoutResult(paymentStatus -> failPayment(paymentTransaction));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentException("Reconcile was interrupted, please try again later " + e.getMessage());
        } finally {
            locker.unlock();
        }
    }

    private void saveTicketEmailOutBox(User buyer, TicketOrderResponse ticketOrderResponse) {
        TicketDetailResponse ticketDetail = ticketDetailAppService.getTicketDetail(ticketOrderResponse.getTicketId(), null);
        TicketResponse ticket = ticketAppService.getTicketById(ticketDetail.getActivityId());

        Map<String, Object> data = new HashMap<>();
        data.put("orderNumber", ticketOrderResponse.getOrderNumber());
        data.put("quantity", ticketOrderResponse.getQuantity());
        data.put("eventName", ticket.getDescription());
        data.put("paidAt", LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        data.put("ticketType", ticketDetail.getName());
        data.put("seat", "Tự do");
        data.put("totalAmount", ticketOrderResponse.getTotalAmount().toPlainString());
        data.put("eventDate", ticket.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        String token = "MQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        String messageId = UuidCreator.getTimeOrderedEpoch().toString();

        EmailMQMessage message = new EmailMQMessage(
                token, messageId, "PURCHASE", buyer.getEmail(), null, data, System.currentTimeMillis()
        );

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(token)
                .eventType("SEND_EMAIL")
                .payload(JSON.toJSONString(message))
                .status(0)
                .createdAt(LocalDateTime.now(ZoneId.systemDefault()))
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    private String extractOrderNumber(String orderInfo) {
        if (orderInfo == null) {
            return null;
        }

        int index = orderInfo.indexOf("OKS");

        if (index < 0) {
            return null;
        }

        return orderInfo.substring(index);
    }

    private Map<String, String> processIpn(String orderNumber, Map<String, String> request) {
        PaymentTransaction paymentTransaction = paymentTransactionDomainService
                .findByOrderNumber(orderNumber);

        if (paymentTransaction == null) {
            return rsp("01", "Payment Not Found");
        }

        Map<String, String> error = validateIpn(paymentTransaction, request);
        if (!error.isEmpty()) {
            return error;
        }

        if (isPaymentSuccess(request)) {
            successPayment(paymentTransaction);
        } else {
            failPayment(paymentTransaction);
        }

        return rsp("00", "Confirm success");

    }

    private Map<String, String> validateIpn(PaymentTransaction paymentTransaction, Map<String, String> request) {
        long paid;

        try {
            paid = Long.parseLong(request.get("vnp_Amount"));
        } catch (NumberFormatException e) {
            log.error("Payment Amount Not Found, {}", e.getMessage());
            return rsp("04", "Invalid Amount");
        }
        long expired = paymentTransaction.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();

        if (paid != expired) {
            return rsp("04", "Invalid Amount");
        }

        if (!Integer.valueOf(1).equals(paymentTransaction.getPaymentStatus())) {
            return rsp("02", "Order already confirmed");
        }

        return Collections.emptyMap();
    }

    private boolean isPaymentSuccess(Map<String, String> request) {
        return "00".equals(request.get("vnp_ResponseCode"))
                && "00".equals(request.get("vnp_TransactionStatus"));
    }

    private void successPayment(PaymentTransaction paymentTransaction) {
        boolean isPaid = ticketOrderAppService.markOrderPaid(paymentTransaction.getOrderNumber());

        if (!isPaid) {
            log.error("Payment received for non-pending order {}, refund required", paymentTransaction.getOrderNumber());

            paymentTransactionDomainService.updateTransactionInProgress(paymentTransaction.getPaymentId(),
                    paymentTransaction.getPaymentUrl(),
                    paymentTransaction.getGatewayTransactionId(),
                    4);

            return;
        }

        TicketOrderResponse order = ticketOrderAppService.findByOrderNumber(paymentTransaction.getOrderNumber());
        User buyer = userDomainService.findById(paymentTransaction.getUserId());

        paymentTransactionDomainService.updateTransactionInProgress(
                paymentTransaction.getPaymentId(),
                paymentTransaction.getPaymentUrl(),
                paymentTransaction.getGatewayTransactionId(), 2);

        saveTicketEmailOutBox(buyer, order);
    }

    private void failPayment(PaymentTransaction paymentTransaction) {
        paymentTransactionDomainService.updateTransactionInProgress(
                paymentTransaction.getPaymentId(),
                paymentTransaction.getPaymentUrl(),
                paymentTransaction.getGatewayTransactionId(), 3);
        ticketOrderAppService.cancelOrderAfterPayment(paymentTransaction.getOrderNumber());
    }

    private String genPaymentLock(String orderNumber) {
        return "LOCK:PAYMENT:" + orderNumber;
    }

    private Map<String, String> rsp(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
