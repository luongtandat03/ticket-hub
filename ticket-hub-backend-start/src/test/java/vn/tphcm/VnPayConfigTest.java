package vn.tphcm;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Đọc application.yml thô (chưa thay biến môi trường) để chặn các lỗi cấu hình VNPay đã từng gặp. */
class VnPayConfigTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> vnp() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/application.yml")) {
            assertNotNull(in, "không tìm thấy application.yml");
            Map<String, Object> yaml = new Yaml().load(in);
            return (Map<String, Object>) yaml.get("vnp");
        }
    }

    @Test
    void returnUrl_hasHttpSchemeSoVnPayCanRedirectBack() throws Exception {
        String returnUrl = String.valueOf(vnp().get("returnUrl"));

        assertTrue(returnUrl.startsWith("http://") || returnUrl.startsWith("https://"),
                "Thiếu http:// thì VNPay hiện trang lỗi sau khi thanh toán: " + returnUrl);
        assertTrue(returnUrl.endsWith("/payment/callback"), returnUrl);
    }

    @Test
    void ip_isNotCombinedWithPortOrScheme() throws Exception {
        String ip = String.valueOf(vnp().get("ip"));

        // vnp_IpAddr phải là IP thuần, nếu không querydr trả 03 (Invalid data format)
        assertFalse(ip.contains("http"), ip);
        assertFalse(ip.replace("${", "").replace("}", "").contains(":"), ip);
    }

    @Test
    void apiUrl_isConfiguredForQuerydr() throws Exception {
        assertNotNull(vnp().get("apiUrl"), "thiếu vnp.apiUrl: job đối soát không gọi được VNPay");
    }
}
