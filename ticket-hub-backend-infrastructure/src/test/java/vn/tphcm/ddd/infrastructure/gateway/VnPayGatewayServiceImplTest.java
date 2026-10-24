package vn.tphcm.ddd.infrastructure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import vn.tphcm.ddd.domain.model.PaymentTransaction;
import vn.tphcm.ddd.infrastructure.gateway.impl.VnPayGatewayServiceImpl;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class VnPayGatewayServiceImplTest {
    private static final String SECRET = "test-secret";
    private static final String API_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private static final String CREATE_DATE = "20261015103000";

    private VnPayGatewayServiceImpl gateway;
    private MockRestServiceServer server;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        gateway = new VnPayGatewayServiceImpl();
        ReflectionTestUtils.setField(gateway, "terminalId", "TESTTMN");
        ReflectionTestUtils.setField(gateway, "secretKey", SECRET);
        ReflectionTestUtils.setField(gateway, "vnpUrl", "https://sandbox.vnpayment.vn/pay");
        ReflectionTestUtils.setField(gateway, "ip", "127.0.0.1");
        ReflectionTestUtils.setField(gateway, "returnUrl", "http://127.0.0.1:1122/payment/callback");
        ReflectionTestUtils.setField(gateway, "apiUrl", API_URL);

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(gateway, "restClient", builder.build());
    }

    private PaymentTransaction transaction(String amount) {
        return PaymentTransaction.builder()
                .paymentId("11111111-2222-3333-4444-555555555555")
                .orderNumber("OKS-SGN-1-abc")
                .amount(new BigDecimal(amount))
                .createDate(CREATE_DATE)
                .build();
    }

    private Map<String, String> signedParams() throws Exception {
        String url = gateway.createPaymentUrl(transaction("150000"));
        Map<String, String> params = new HashMap<>();
        UriComponentsBuilder.fromUriString(url).build().getQueryParams()
                .forEach((k, v) -> params.put(k, URLDecoder.decode(v.get(0), StandardCharsets.UTF_8)));
        return params;
    }

    private static String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        StringBuilder sb = new StringBuilder();
        for (byte b : mac.doFinal(data.getBytes(StandardCharsets.UTF_8))) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    // ---------------- createPaymentUrl ----------------

    @Test
    void createPaymentUrl_startsWithConfiguredGatewayUrl() throws Exception {
        assertTrue(gateway.createPaymentUrl(transaction("1")).startsWith("https://sandbox.vnpayment.vn/pay?"));
    }

    @Test
    void createPaymentUrl_containsAmountInSmallestUnitAndOrderInfo() throws Exception {
        Map<String, String> params = signedParams();

        assertEquals("15000000", params.get("vnp_Amount"));
        assertEquals("TESTTMN", params.get("vnp_TmnCode"));
        assertEquals("VND", params.get("vnp_CurrCode"));
        assertEquals("Order Payment:OKS-SGN-1-abc", params.get("vnp_OrderInfo"));
        assertEquals("555555555555", params.get("vnp_TxnRef"));
        assertNotNull(params.get("vnp_SecureHash"));
    }

    @Test
    void createPaymentUrl_usesStoredCreateDateAndExpiresFifteenMinutesLater() throws Exception {
        // querydr phải gửi lại đúng vnp_CreateDate này, nên không được lấy giờ hiện tại
        Map<String, String> params = signedParams();

        assertEquals(CREATE_DATE, params.get("vnp_CreateDate"));
        assertEquals("20261015104500", params.get("vnp_ExpireDate"));
    }

    // ---------------- verifySignature (IPN) ----------------

    @Test
    void verifySignature_acceptsOwnSignedParams() throws Exception {
        assertTrue(gateway.verifySignature(signedParams()));
    }

    @Test
    void verifySignature_acceptsUpperCaseHash() throws Exception {
        Map<String, String> params = signedParams();
        params.put("vnp_SecureHash", params.get("vnp_SecureHash").toUpperCase());

        assertTrue(gateway.verifySignature(params));
    }

    @Test
    void verifySignature_rejectsTamperedAmount() throws Exception {
        Map<String, String> params = signedParams();
        params.put("vnp_Amount", "100");

        assertFalse(gateway.verifySignature(params));
    }

    @Test
    void verifySignature_rejectsMissingOrEmptyHash() throws Exception {
        Map<String, String> params = signedParams();
        params.remove("vnp_SecureHash");
        assertFalse(gateway.verifySignature(params));

        params.put("vnp_SecureHash", "");
        assertFalse(gateway.verifySignature(params));
    }

    @Test
    void verifySignature_rejectsWrongSecret() throws Exception {
        Map<String, String> params = signedParams();
        ReflectionTestUtils.setField(gateway, "secretKey", "other-secret");

        assertFalse(gateway.verifySignature(params));
    }

    // ---------------- queryTransaction (querydr) ----------------

    private static final List<String> RESPONSE_HASH_FIELDS = List.of(
            "vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message", "vnp_TmnCode",
            "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate", "vnp_TransactionNo",
            "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo",
            "vnp_PromotionCode", "vnp_PromotionAmount");

    /** Phản hồi querydr được ký đúng như VNPay. */
    private Map<String, String> signedQueryResponse(String transactionStatus) throws Exception {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("vnp_ResponseId", "resp-1");
        r.put("vnp_Command", "querydr");
        r.put("vnp_ResponseCode", "00");
        r.put("vnp_Message", "QueryDR Success");
        r.put("vnp_TmnCode", "TESTTMN");
        r.put("vnp_TxnRef", "555555555555");
        r.put("vnp_Amount", "15000000");
        r.put("vnp_BankCode", "NCB");
        r.put("vnp_PayDate", "20261015103200");
        r.put("vnp_TransactionNo", "14123456");
        r.put("vnp_TransactionType", "01");
        r.put("vnp_TransactionStatus", transactionStatus);
        r.put("vnp_OrderInfo", "Order Payment:OKS-SGN-1-abc");
        r.put("vnp_PromotionCode", "");
        r.put("vnp_PromotionAmount", "");

        StringJoiner data = new StringJoiner("|");
        RESPONSE_HASH_FIELDS.forEach(f -> data.add(r.getOrDefault(f, "")));
        r.put("vnp_SecureHash", hmac(data.toString()));
        return r;
    }

    @Test
    void query_sendsSignedQuerydrRequestWithOriginalCreateDate() throws Exception {
        AtomicReference<String> sentBody = new AtomicReference<>();
        server.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(req -> sentBody.set(req.getBody().toString()))
                .andRespond(withSuccess(json.writeValueAsString(signedQueryResponse("00")), MediaType.APPLICATION_JSON));

        gateway.queryTransaction(transaction("150000"));

        server.verify();
        Map<String, String> body = json.readValue(sentBody.get(),
                json.getTypeFactory().constructMapType(Map.class, String.class, String.class));

        assertEquals("querydr", body.get("vnp_Command"));
        assertEquals("2.1.0", body.get("vnp_Version"));
        assertEquals("TESTTMN", body.get("vnp_TmnCode"));
        assertEquals("555555555555", body.get("vnp_TxnRef"));
        assertEquals(CREATE_DATE, body.get("vnp_TransactionDate"));
        assertEquals("127.0.0.1", body.get("vnp_IpAddr"));

        String expectedHash = hmac(String.join("|",
                body.get("vnp_RequestId"), "2.1.0", "querydr", "TESTTMN", "555555555555",
                CREATE_DATE, body.get("vnp_CreateDate"), "127.0.0.1", body.get("vnp_OrderInfo")));
        assertEquals(expectedHash, body.get("vnp_SecureHash"));
    }

    @Test
    void query_validSignedResponse_isReturned() throws Exception {
        server.expect(requestTo(API_URL))
                .andRespond(withSuccess(json.writeValueAsString(signedQueryResponse("00")), MediaType.APPLICATION_JSON));

        Map<String, String> response = gateway.queryTransaction(transaction("150000"));

        assertEquals("00", response.get("vnp_ResponseCode"));
        assertEquals("00", response.get("vnp_TransactionStatus"));
        assertEquals("14123456", response.get("vnp_TransactionNo"));
        assertEquals("15000000", response.get("vnp_Amount"));
    }

    @Test
    void query_tamperedResponse_isRejected() throws Exception {
        // Ai đó sửa trạng thái từ thất bại thành thành công trên đường truyền
        Map<String, String> forged = signedQueryResponse("02");
        forged.put("vnp_TransactionStatus", "00");
        server.expect(requestTo(API_URL))
                .andRespond(withSuccess(json.writeValueAsString(forged), MediaType.APPLICATION_JSON));

        Map<String, String> response = gateway.queryTransaction(transaction("150000"));

        assertTrue(response == null || response.isEmpty(), "Phản hồi bị sửa không được tin");
    }

    @Test
    void query_responseWithoutSignature_isRejected() throws Exception {
        Map<String, String> unsigned = signedQueryResponse("00");
        unsigned.remove("vnp_SecureHash");
        server.expect(requestTo(API_URL))
                .andRespond(withSuccess(json.writeValueAsString(unsigned), MediaType.APPLICATION_JSON));

        Map<String, String> response = gateway.queryTransaction(transaction("150000"));

        assertTrue(response == null || response.isEmpty());
    }

    @Test
    void query_vnPayServerError_returnsNoResult() {
        server.expect(requestTo(API_URL)).andRespond(withServerError());

        Map<String, String> response = gateway.queryTransaction(transaction("150000"));

        assertTrue(response == null || response.isEmpty());
    }

    @Test
    void query_unsignedErrorResponse_isNotTrustedButDoesNotThrow() throws Exception {
        // 94: tra cứu cùng giao dịch quá dày; 03: sai định dạng (vd vnp_IpAddr có kèm cổng). VNPay không ký các phản hồi này.
        for (String[] error : new String[][]{{"94", "Request is duplicated"}, {"03", "Invalid data format"}}) {
            server.reset();
            server.expect(requestTo(API_URL))
                    .andRespond(withSuccess(json.writeValueAsString(
                            Map.of("vnp_ResponseCode", error[0], "vnp_Message", error[1])), MediaType.APPLICATION_JSON));

            Map<String, String> response = gateway.queryTransaction(transaction("150000"));

            assertTrue(response == null || response.isEmpty(),
                    "Phản hồi lỗi không có chữ ký không được dùng để đổi trạng thái: " + error[0]);
        }
    }

    @Test
    void query_sendsPureIpAddress() throws Exception {
        // VNPay trả 03 nếu vnp_IpAddr không phải IP thuần (vd "127.0.0.1:1122")
        AtomicReference<String> sentBody = new AtomicReference<>();
        server.expect(requestTo(API_URL))
                .andExpect(req -> sentBody.set(req.getBody().toString()))
                .andRespond(withSuccess(json.writeValueAsString(signedQueryResponse("00")), MediaType.APPLICATION_JSON));

        gateway.queryTransaction(transaction("150000"));

        Map<String, String> body = json.readValue(sentBody.get(),
                json.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        assertTrue(body.get("vnp_IpAddr").matches("\\d{1,3}(\\.\\d{1,3}){3}"), body.get("vnp_IpAddr"));
    }
}
