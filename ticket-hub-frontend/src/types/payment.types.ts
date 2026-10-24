export const PaymentStatus = {
  INIT: 0,
  IN_PROGRESS: 1,
  SUCCESS: 2,
  FAILED: 3,
};

export interface PaymentResponse {
  id: string;
  paymentId: string;
  orderNumber: string;
  userId: string;
  amount: number;
  paymentMethod: string;
  paymentStatus: (typeof PaymentStatus)[keyof typeof PaymentStatus];
  gatewayTransactionId: string;
  paymentUrl: string;
  createdAt: string;
  updatedAt: string;
}
