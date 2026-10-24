export const OrderStatus = { PENDING: 0, PAID: 1, CANCELLED: 2 } as const;

export interface PlaceOrderMQRequest {
  ticketId: string;
  quantity: number;
}

export interface CreateBookingRequest {
  ticketId: string;
  quantity: number;
}

export interface PlaceOrderResponse {
  success: boolean;
  code: string;
  message: string;
  placeOrderTaskId: string;
  orderId: string;
}

export interface TicketOrderResponse {
  id: string;
  userId: string;
  ticketId: string;
  orderNumber: string;
  quantity: number;
  orderStatus: typeof OrderStatus[keyof typeof OrderStatus];
  totalAmount: number;
  terminalId: string;
  orderDate: string;
  orderNotes: string;
  createdAt: string;
  updatedAt: string;
}
