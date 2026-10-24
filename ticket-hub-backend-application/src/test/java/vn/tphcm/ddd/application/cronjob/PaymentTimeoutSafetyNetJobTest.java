package vn.tphcm.ddd.application.cronjob;

import org.junit.jupiter.api.Test;
import vn.tphcm.ddd.application.service.payment.PaymentAppService;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class PaymentTimeoutSafetyNetJobTest {

    @Test
    void job_reconcilesOnlyAfterPaymentLinkExpiredInBatchesOf50() {
        // Link VNPay sống 15 phút: tra cứu sớm hơn chỉ nhận "chưa hoàn tất" và dễ dính lỗi 94 (tra cứu quá dày)
        PaymentAppService service = mock(PaymentAppService.class);
        when(service.reconcileStuckPayment(anyLong(), anyInt())).thenReturn(3);

        new PaymentTimeoutSafetyNetJob(service).reconcile();

        verify(service).reconcileStuckPayment(16L, 50);
    }
}
