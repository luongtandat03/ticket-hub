package vn.tphcm.ddd.controller.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.tphcm.ddd.application.dto.*;
import vn.tphcm.ddd.application.dto.response.*;
import vn.tphcm.ddd.application.exception.GlobalExceptionHandling;
import vn.tphcm.ddd.application.exception.PaymentException;
import vn.tphcm.ddd.application.service.authentication.AuthenticationAppService;
import vn.tphcm.ddd.application.service.excel.ExportExcelService;
import vn.tphcm.ddd.application.service.order.OrderMQAppService;
import vn.tphcm.ddd.application.service.order.TicketOrderAppService;
import vn.tphcm.ddd.application.service.payment.PaymentAppService;
import vn.tphcm.ddd.application.service.ticket.TicketAppService;
import vn.tphcm.ddd.application.service.ticket.TicketDetailAppService;
import vn.tphcm.ddd.domain.model.OrderQueue;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test controller bằng MockMvc standalone (không khởi động Spring, không cần Redis/Kafka/DB).
 * Bộ lọc bảo mật và @PreAuthorize nằm ngoài phạm vi: chỉ kiểm tra mapping, tham số và định dạng phản hồi.
 */
class ControllersTest {
    private final AuthenticationAppService authService = mock(AuthenticationAppService.class);
    private final PaymentAppService paymentService = mock(PaymentAppService.class);
    private final TicketOrderAppService orderService = mock(TicketOrderAppService.class);
    private final OrderMQAppService orderMQService = mock(OrderMQAppService.class);
    private final TicketAppService ticketService = mock(TicketAppService.class);
    private final TicketDetailAppService ticketDetailService = mock(TicketDetailAppService.class);
    private final ExportExcelService excelService = mock(ExportExcelService.class);

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new AuthenticationController(authService),
                        new PaymentController(paymentService),
                        new TicketOrderController(orderService),
                        new OrderQueueController(orderMQService),
                        new TicketController(ticketService),
                        new TicketDetailController(ticketDetailService),
                        new ExcelController(excelService))
                .setControllerAdvice(new GlobalExceptionHandling())
                .build();
    }

    // ---------------- AuthenticationController ----------------

    @Test
    void login_returnsTokens() throws Exception {
        when(authService.login("alice", "pw"))
                .thenReturn(SignInResponse.builder().accessToken("acc").refreshToken("ref").build());

        mvc.perform(post("/identity/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmailOrPhoneNumber\":\"alice\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("acc"))
                .andExpect(jsonPath("$.data.refreshToken").value("ref"));
    }

    @Test
    void register_callsServiceWithAllFields() throws Exception {
        mvc.perform(post("/identity/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"a@x.com\",\"password\":\"pw\",\"phoneNumber\":\"090\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(authService).register("alice", "a@x.com", "pw", "090");
    }

    @Test
    void confirm_callsServiceWithEmailAndCode() throws Exception {
        when(authService.confirmCode("a@x.com", "ABC123"))
                .thenReturn(SignInResponse.builder().accessToken("acc").build());

        mvc.perform(post("/identity/auth/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@x.com\",\"code\":\"ABC123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("acc"));
    }

    @Test
    void logout_stripsBearerPrefix() throws Exception {
        mvc.perform(post("/identity/logout").header("Authorization", "Bearer my.jwt.token"))
                .andExpect(status().isOk());

        verify(authService).logout("my.jwt.token");
    }

    @Test
    void logout_withoutHeader_isBadRequest() throws Exception {
        mvc.perform(post("/identity/logout")).andExpect(status().isBadRequest());
    }

    // ---------------- PaymentController ----------------

    @Test
    void createPayment_returnsGatewayUrl() throws Exception {
        when(paymentService.paymentOrder("OKS-1", "VNPAY")).thenReturn("https://pay/url");

        mvc.perform(post("/payment/create").param("orderNumber", "OKS-1").param("method", "VNPAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("https://pay/url"));
    }

    @Test
    void createPayment_missingParam_isBadRequest() throws Exception {
        mvc.perform(post("/payment/create").param("orderNumber", "OKS-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_paymentException_isMappedTo402() throws Exception {
        when(paymentService.paymentOrder(any(), any())).thenThrow(new PaymentException("Payment Order Not Found"));

        mvc.perform(post("/payment/create").param("orderNumber", "OKS-1").param("method", "VNPAY"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message").value("Payment Order Not Found"));
    }

    @Test
    void callback_returnsServiceResponseVerbatim() throws Exception {
        when(paymentService.handleVnPayIPN(anyMap())).thenReturn(Map.of("RspCode", "00", "Message", "Confirm Success"));

        mvc.perform(get("/payment/callback").param("vnp_TxnRef", "123").param("vnp_Amount", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));

        verify(paymentService).handleVnPayIPN(Map.of("vnp_TxnRef", "123", "vnp_Amount", "1000"));
    }

    @Test
    void getPayments_returnsList() throws Exception {
        when(paymentService.findAll()).thenReturn(List.of(new PaymentResponse()));

        mvc.perform(get("/payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ---------------- TicketOrderController ----------------

    @Test
    void orderByCas_returnsServiceResult() throws Exception {
        when(orderService.decreaseStockCAS("t1", 2)).thenReturn(true);

        mvc.perform(put("/order/t1/2/cas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void orderByCas_nonNumericQuantity_isBadRequest() throws Exception {
        mvc.perform(put("/order/t1/abc/cas")).andExpect(status().isBadRequest());
    }

    @Test
    void placeOrderCas_success() throws Exception {
        when(orderService.placeOrderCAS("t1", 2)).thenReturn(PlaceOrderResponse.success("OKS-1"));

        mvc.perform(post("/order/cas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketId\":\"t1\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.placeOrderTaskId").value("OKS-1"));
    }

    @Test
    void placeOrderCas_businessFailure_isStill200WithFailPayload() throws Exception {
        when(orderService.placeOrderCAS("t1", 2)).thenReturn(PlaceOrderResponse.fail("OUT_OF_STOCK", "sold out"));

        mvc.perform(post("/order/cas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketId\":\"t1\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.code").value("OUT_OF_STOCK"));
    }

    @Test
    void getOrderByNumber_returnsOrder() throws Exception {
        TicketOrderResponse order = new TicketOrderResponse();
        order.setOrderNumber("OKS-1");
        when(orderService.findByOrderNumber("OKS-1")).thenReturn(order);

        mvc.perform(get("/order/OKS-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("OKS-1"));
    }

    @Test
    void listOrders_requiresNTableParam() throws Exception {
        mvc.perform(get("/order/list")).andExpect(status().isBadRequest());

        when(orderService.findAll("202610")).thenReturn(List.of());
        mvc.perform(get("/order/list").param("nTable", "202610")).andExpect(status().isOk());
    }

    @Test
    void listOrdersPaged_usesDefaultLimit50AndEmptyCursor() throws Exception {
        when(orderService.findPage(anyString(), anyString(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), null, false));

        mvc.perform(get("/order/list/page").param("nTable", "202610"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasMore").value(false));

        verify(orderService).findPage("202610", "", 50);
    }

    @Test
    void cancelOrder_returnsServiceResult() throws Exception {
        when(orderService.cancelOrder("OKS-1")).thenReturn(true);

        mvc.perform(delete("/order/OKS-1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    // ---------------- OrderQueueController ----------------

    @Test
    void placeOrderMq_queued_returnsToken() throws Exception {
        when(orderMQService.placeOrderMQ("t1", 1))
                .thenReturn(OrderQueue.builder().token("MQ-1").status(0).build());

        mvc.perform(post("/order/mq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketId\":\"t1\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.placeOrderTaskId").value("MQ-1"));
    }

    @Test
    void placeOrderMq_failed_extractsCodeFromMessage() throws Exception {
        when(orderMQService.placeOrderMQ("t1", 1))
                .thenReturn(OrderQueue.builder().status(2).message("OUT_OF_STOCK: Sold out").build());

        mvc.perform(post("/order/mq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketId\":\"t1\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.code").value("OUT_OF_STOCK"));
    }

    @Test
    void placeOrderMq_failedWithoutColon_usesGenericErrorCode() throws Exception {
        when(orderMQService.placeOrderMQ("t1", 1))
                .thenReturn(OrderQueue.builder().status(2).message("something broke").build());

        mvc.perform(post("/order/mq")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketId\":\"t1\",\"quantity\":1}"))
                .andExpect(jsonPath("$.data.code").value("ERROR"));
    }

    @Test
    void orderStatus_returnsQueueRow() throws Exception {
        when(orderMQService.getOrderStatus("MQ-1"))
                .thenReturn(OrderQueue.builder().token("MQ-1").status(1).build());

        mvc.perform(get("/order/status/MQ-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("MQ-1"))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    // ---------------- TicketController / TicketDetailController ----------------

    @Test
    void getTicket_returnsTicket() throws Exception {
        TicketResponse ticket = new TicketResponse();
        ticket.setId("t1");
        ticket.setName("Concert");
        when(ticketService.getTicketById("t1")).thenReturn(ticket);

        mvc.perform(get("/ticket/t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Concert"));
    }

    @Test
    void getTicketsByStatus_alwaysQueriesStatusOne() throws Exception {
        when(ticketService.getTicketsByStatus(1, 5L)).thenReturn(List.of(new TicketResponse()));

        mvc.perform(get("/ticket").param("version", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void getTicketDetail_passesOptionalVersion() throws Exception {
        TicketDetailResponse detail = new TicketDetailResponse();
        detail.setId("d1");
        when(ticketDetailService.getTicketDetail("d1", 5L)).thenReturn(detail);

        mvc.perform(get("/ticket/t1/detail/d1").param("version", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("d1"));
    }

    @Test
    void getTicketDetail_withoutVersion_passesNull() throws Exception {
        mvc.perform(get("/ticket/t1/detail/d1")).andExpect(status().isOk());

        verify(ticketDetailService).getTicketDetail("d1", null);
    }

    // ---------------- ExcelController ----------------

    @Test
    void export_delegatesToService() throws Exception {
        mvc.perform(post("/export").param("nTable", "202610")).andExpect(status().isOk());

        verify(excelService).exportData(any(), eq("202610"));
    }

    @Test
    void exportWithThread_delegatesToService() throws Exception {
        mvc.perform(post("/exportWithThread").param("nTable", "202610")).andExpect(status().isOk());

        verify(excelService).exportDataWithThread(any(), eq("202610"));
    }
}
