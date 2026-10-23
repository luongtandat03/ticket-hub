/*
 * @ (#) AiChatTools.java       1.0     10/20/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.ai;
/*
 * @author: Luong Tan Dat
 * @date: 10/20/2026
 */

import com.anthropic.models.beta.messages.BetaTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.application.service.ticket.TicketAppService;
import vn.tphcm.ddd.application.service.ticket.cache.TicketDetailCacheService;
import vn.tphcm.ddd.domain.service.TicketDetailDomainService;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "AI-CHAT-TOOLS")
public class AiChatTools {
    public static final String LIST_EVENTS = "list_events";
    public static final String GET_TICKET_OPTIONS = "get_ticket_options";
    public static final String GET_MY_RECENT_ORDERS = "get_my_recent_orders";
    public static final String GET_ORDER_DETAIL = "get_order_detail";
    public static final String GET_BOOKING_REQUEST_STATUS = "get_booking_request_status";

    private static final int ACTIVE = 1;
    private static final int DEFAULT_ORDER_LIMIT = 5;
    private static final int MAX_ORDER_LIMIT = 10;

    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F-]{36}");
    private static final Pattern ORDER_NUMBER = Pattern.compile("[A-Z]{3}-[A-Z]{3}-\\d{13}-([0-9a-fA-F]{32})");
    private static final Pattern QUEUE_TOKEN = Pattern.compile("MQ-[0-9a-fA-F]{16}");

    private static final Map<String, String> LABELS = Map.of(
            LIST_EVENTS, "Đang xem các sự kiện đang mở bán",
            GET_TICKET_OPTIONS, "Đang kiểm tra giá và số vé còn lại",
            GET_MY_RECENT_ORDERS, "Đang xem các đơn hàng của bạn",
            GET_ORDER_DETAIL, "Đang tra cứu đơn hàng",
            GET_BOOKING_REQUEST_STATUS, "Đang kiểm tra yêu cầu đặt vé");

    private static final List<BetaTool> DEFINITIONS = List.of(

    );


    private final TicketAppService ticketAppService;
    private final TicketDetailDomainService ticketDetailDomainService;
    private final TicketDetailCacheService ticketDetailCacheService;


    public record Result(String content, boolean isError) {
    }

//    private static BetaTool tool(String name, String description, Map<String, Object> properties, List<String> )
}
