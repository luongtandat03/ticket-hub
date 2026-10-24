package vn.tphcm.ddd.application.service.ticket;

import org.junit.jupiter.api.Test;
import vn.tphcm.ddd.application.dto.response.TicketDetailResponse;
import vn.tphcm.ddd.application.dto.response.TicketResponse;
import vn.tphcm.ddd.application.mapper.TicketMapper;
import vn.tphcm.ddd.application.service.ticket.cache.TicketAppCacheService;
import vn.tphcm.ddd.application.service.ticket.cache.TicketDetailCacheService;
import vn.tphcm.ddd.application.service.ticket.impl.TicketAppServiceImpl;
import vn.tphcm.ddd.application.service.ticket.impl.TicketDetailAppServiceImpl;
import vn.tphcm.ddd.domain.model.Ticket;
import vn.tphcm.ddd.domain.service.TicketDomainService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketAppServicesTest {

    // ---------------- TicketAppServiceImpl ----------------

    @Test
    void getTicketById_mapsDomainTicketToResponse() {
        TicketAppCacheService cache = mock(TicketAppCacheService.class);
        TicketDomainService domain = mock(TicketDomainService.class);
        TicketMapper mapper = mock(TicketMapper.class);
        TicketAppServiceImpl service = new TicketAppServiceImpl(cache, domain, mapper);

        Ticket ticket = new Ticket();
        TicketResponse response = new TicketResponse();
        when(domain.getTicketById("t1")).thenReturn(ticket);
        when(mapper.toResponse(ticket)).thenReturn(response);

        assertSame(response, service.getTicketById("t1"));
    }

    @Test
    void getTicketById_unknownTicket_mapperReceivesNull() {
        TicketAppCacheService cache = mock(TicketAppCacheService.class);
        TicketDomainService domain = mock(TicketDomainService.class);
        TicketMapper mapper = mock(TicketMapper.class);
        TicketAppServiceImpl service = new TicketAppServiceImpl(cache, domain, mapper);

        when(domain.getTicketById("none")).thenReturn(null);
        when(mapper.toResponse(null)).thenReturn(null);

        assertNull(service.getTicketById("none"));
    }

    @Test
    void getTicketsByStatus_delegatesToCacheService() {
        TicketAppCacheService cache = mock(TicketAppCacheService.class);
        TicketAppServiceImpl service = new TicketAppServiceImpl(cache, mock(TicketDomainService.class), mock(TicketMapper.class));
        List<TicketResponse> list = List.of(new TicketResponse());
        when(cache.getTicketsByStatus(1, 1231231313L)).thenReturn(list);

        assertSame(list, service.getTicketsByStatus(1, 1231231313L));
    }

    // ---------------- TicketDetailAppServiceImpl ----------------

    @Test
    void getTicketDetail_delegatesToCacheService() {
        TicketDetailCacheService cache = mock(TicketDetailCacheService.class);
        TicketDetailAppServiceImpl service = new TicketDetailAppServiceImpl(cache);
        TicketDetailResponse detail = new TicketDetailResponse();
        when(cache.getTicketDetail("t1", 3L)).thenReturn(detail);

        assertSame(detail, service.getTicketDetail("t1", 3L));
    }

    @Test
    void orderTicketByUser_returnsCacheDecision() {
        TicketDetailCacheService cache = mock(TicketDetailCacheService.class);
        TicketDetailAppServiceImpl service = new TicketDetailAppServiceImpl(cache);
        when(cache.orderTicketByUser("t1")).thenReturn(true);
        when(cache.orderTicketByUser("t2")).thenReturn(false);

        assertTrue(service.orderTicketByUser("t1"));
        assertFalse(service.orderTicketByUser("t2"));
    }
}
