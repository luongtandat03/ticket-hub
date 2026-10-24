export interface TicketDetailResponse {
  id: string;
  name: string;
  description: string;
  stockInitial: number;
  stockAvailable: number;
  stockPrepared: boolean;
  priceOriginal: number;
  priceFlash: number;
  saleStartTime: string;
  saleEndTime: string;
  status: number;
  activityId: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface TicketListResponse {
  ticketList: TicketResponse[];
  version: number;
}

export interface TicketResponse {
  id: string;
  name: string;
  description: string;
  startSaleTime: string;
  endSaleTime: string;
  status: number;
  createdAt: string;
  updatedAt: string;
}

export interface TicketOrderResponse {
  id: string;
  userId: string;
  ticketId: string;
  orderNumber: string;
  quantity: number;
  orderStatus: number;
  totalAmount: number;
  terminalId: string;
  orderDate: string;
  createdAt: string;
  updatedAt: string;
}
