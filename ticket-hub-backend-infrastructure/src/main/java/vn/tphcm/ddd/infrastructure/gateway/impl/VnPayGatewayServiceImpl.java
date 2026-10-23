/*
 * @ (#) VnPayGatewayService.java       1.0     10/8/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.gateway.impl;
/*
 * @author: Luong Tan Dat
 * @date: 10/8/2026
 */

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import vn.tphcm.ddd.domain.model.PaymentTransaction;
import vn.tphcm.ddd.infrastructure.gateway.VnPayGatewayService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VnPayGatewayServiceImpl implements VnPayGatewayService {
    @Value("${vnp.terminalId}")
    private String terminalId;

    @Value("${vnp.secretKey}")
    private String secretKey;

    @Value("${vnp.url}")
    private String vnpUrl;

    @Value("${vnp.ip}")
    private String ip;

    @Value("${vnp.returnUrl}")
    private String returnUrl;

    @Value("${vnp.apiUrl}")
    private String apiUrl;

    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final List<String> QUERY_RESPONSE_HASH_FIELDS = List.of(
            "vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message", "vnp_TmnCode",
            "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate", "vnp_TransactionNo",
            "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo",
            "vnp_PromotionCode", "vnp_PromotionAmount");

    private final RestClient restClient = RestClient.create();

    @Override
    public String createPaymentUrl(PaymentTransaction transaction) throws UnsupportedEncodingException {
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "querydr");
        vnpParams.put("vnp_TmnCode", terminalId);
        vnpParams.put("vnp_Amount", transaction.getAmount().multiply(new BigDecimal(100)).toBigInteger().toString());
        vnpParams.put("vnp_CurrCode", "VND");

        String txnRef = transaction.getPaymentId().substring(transaction.getPaymentId().length() - 12);
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Order Payment:" + transaction.getOrderNumber());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", ip);

        LocalDateTime createdDate = LocalDateTime.parse(transaction.getCreateDate(), VNP_DATE);

        vnpParams.put("vnp_CreateDate", transaction.getCreateDate());
        vnpParams.put("vnp_ExpireDate", VNP_DATE.format(createdDate.plusMinutes(15)));
        vnpParams.put("vnp_OrderType", "other");

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            String fieldName = entry.getKey();

            String fieldValue = entry.getValue();

            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName)
                        .append("=")
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII))
                        .append("&");

                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append("=")
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII))
                        .append("&");
            }
        }

        hashData.deleteCharAt(hashData.length() - 1);
        query.deleteCharAt(query.length() - 1);

        String vnpSecureHash = hmacSHA512(secretKey, hashData.toString());

        return vnpUrl + "?" + query + "&vnp_SecureHash=" + vnpSecureHash;
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");

        if (received == null || received.isEmpty()) {
            return false;
        }

        Map<String, String> sorted = new TreeMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && v != null && !v.isEmpty()) {
                sorted.put(k, v);
            }
        });

        String hashData = sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        String calculated = hmacSHA512(secretKey, hashData);

        log.info("hashData={}", hashData);

        return MessageDigest.isEqual(
                calculated.getBytes(StandardCharsets.UTF_8),
                received.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public Map<String, String> queryTransaction(PaymentTransaction transaction) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String createdDate = VNP_DATE.format(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        String txnRef = transaction.getPaymentId().substring(transaction.getPaymentId().length() - 12);

        String orderInfo = "Order Payment:" + transaction.getOrderNumber();

        String hashData = String.join("|", requestId, "2.1.0", "querydr",
                terminalId, txnRef, transaction.getCreateDate(),
                createdDate, ip, orderInfo);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", "2.1.0");
        body.put("vnp_Command", "querydr");
        body.put("vnp_TmnCode", terminalId);
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionDate", transaction.getCreateDate());
        body.put("vnp_CreateDate", createdDate);
        body.put("vnp_IpAddr", ip);
        body.put("vnp_SecureHash", hmacSHA512(secretKey, hashData));

        try {
            Map<String, Object> raw = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (raw == null) {
                return Collections.emptyMap();
            }

            Map<String, String> response = new HashMap<>();

            raw.forEach((k, v) -> response.put(k, v == null ? "" : String.valueOf(v)));

            if (!response.containsKey("vnp_SecureHash")) {
                log.warn("VNPay querydr rejected, paymentId={}, code={}, message={}",
                        transaction.getPaymentId(), response.get("vnp_ResponseCode"), response.get("vnp_Message"));
                return Collections.emptyMap();
            }

            if (!verifyQueryResponse(response)) {
                log.error("Vnpay querydr: invalid response signature, paymentId={}, response={}",
                        transaction.getPaymentId(), response);
                return Collections.emptyMap();
            }

            return response;

        } catch (Exception e) {
            log.error("VNPay querydr failed, paymentId={}", transaction.getPaymentId(), e);
            return Collections.emptyMap();
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");

            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKeySpec);

            byte[] rawHmac = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();

            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b & 0xff));
            }

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error creating Signature ", e);
        }
    }

    private boolean verifyQueryResponse(Map<String, String> queryResponse) {
        String reveived = queryResponse.get("vnp_SecureHash");

        if (reveived == null || reveived.isEmpty()) {
            return false;
        }

        String data = QUERY_RESPONSE_HASH_FIELDS.stream()
                .map(field -> queryResponse.getOrDefault(field, ""))
                .collect(Collectors.joining("|"));

        return MessageDigest.isEqual(
                hmacSHA512(secretKey, data).getBytes(StandardCharsets.UTF_8),
                reveived.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }
}
