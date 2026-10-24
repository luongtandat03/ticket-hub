package vn.tphcm.ddd.domain.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import vn.tphcm.ddd.domain.model.*;
import vn.tphcm.ddd.domain.repository.*;
import vn.tphcm.ddd.domain.service.impl.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DomainServicesTest {

    // ---------------- UserDomainServiceImpl ----------------

    @Test
    void user_delegatesToRepository() {
        UserRepository repo = mock(UserRepository.class);
        UserDomainServiceImpl service = new UserDomainServiceImpl(repo);
        User u = new User();
        when(repo.save(u)).thenReturn(u);
        when(repo.findById("1")).thenReturn(u);
        when(repo.findByEmail("a@x.com")).thenReturn(u);
        when(repo.existsByUsername("alice")).thenReturn(true);
        when(repo.existsByEmail("a@x.com")).thenReturn(true);
        when(repo.existsByPhonenumber("090")).thenReturn(true);

        assertSame(u, service.save(u));
        assertSame(u, service.findById("1"));
        assertSame(u, service.findByEmail("a@x.com"));
        assertTrue(service.existsByUsername("alice"));
        assertTrue(service.existsByEmail("a@x.com"));
        assertTrue(service.existsByPhoneNumber("090"));
        assertFalse(service.existsByUsername("bob"));
    }

    // ---------------- PaymentTransactionDomainImpl ----------------

    @Test
    void payment_create_defaultsMissingStatusToZero() {
        PaymentTransactionRepository repo = mock(PaymentTransactionRepository.class);
        when(repo.save(any())).thenReturn(true);
        PaymentTransactionDomainImpl service = new PaymentTransactionDomainImpl(repo);
        PaymentTransaction tx = PaymentTransaction.builder().paymentId("p1").build();

        assertTrue(service.createPaymentTransaction(tx));
        assertEquals(0, tx.getPaymentStatus());
    }

    @Test
    void payment_create_keepsExistingStatus() {
        PaymentTransactionRepository repo = mock(PaymentTransactionRepository.class);
        PaymentTransactionDomainImpl service = new PaymentTransactionDomainImpl(repo);
        PaymentTransaction tx = PaymentTransaction.builder().paymentId("p1").paymentStatus(2).build();

        service.createPaymentTransaction(tx);

        assertEquals(2, tx.getPaymentStatus());
    }

    @Test
    void payment_updateStatus_passesArgumentsInRepositoryOrder() {
        PaymentTransactionRepository repo = mock(PaymentTransactionRepository.class);
        when(repo.updateStatus("p1", 2, "gw", "url")).thenReturn(true);
        PaymentTransactionDomainImpl service = new PaymentTransactionDomainImpl(repo);

        assertTrue(service.updateTransactionInProgress("p1", "url", "gw", 2));
        verify(repo).updateStatus("p1", 2, "gw", "url");
    }

    @Test
    void payment_findByOrderNumber_returnsNullWhenAbsent() {
        PaymentTransactionRepository repo = mock(PaymentTransactionRepository.class);
        PaymentTransaction tx = PaymentTransaction.builder().orderNumber("o1").build();
        when(repo.getPaymentTransactionByOrderNumber("o1")).thenReturn(Optional.of(tx));
        when(repo.getPaymentTransactionByOrderNumber("none")).thenReturn(Optional.empty());
        PaymentTransactionDomainImpl service = new PaymentTransactionDomainImpl(repo);

        assertSame(tx, service.findByOrderNumber("o1"));
        assertNull(service.findByOrderNumber("none"));
    }

    @Test
    void payment_findAll_delegates() {
        PaymentTransactionRepository repo = mock(PaymentTransactionRepository.class);
        List<PaymentTransaction> all = List.of(new PaymentTransaction());
        when(repo.findAll()).thenReturn(all);

        assertSame(all, new PaymentTransactionDomainImpl(repo).findAll());
    }

    // ---------------- Ticket / TicketDetail ----------------

    @Test
    void ticket_delegatesToRepository() {
        TicketRepository repo = mock(TicketRepository.class);
        Ticket t = new Ticket();
        when(repo.findById("t1")).thenReturn(t);
        when(repo.findByStatus(1)).thenReturn(List.of(t));
        TicketDomainServiceImpl service = new TicketDomainServiceImpl(repo);

        assertSame(t, service.getTicketById("t1"));
        assertEquals(List.of(t), service.findByStatus(1));
    }

    @Test
    void ticketDetail_returnsNullWhenAbsent() {
        TicketDetailRepository repo = mock(TicketDetailRepository.class);
        TicketDetail d = new TicketDetail();
        when(repo.findById("d1")).thenReturn(Optional.of(d));
        when(repo.findById("none")).thenReturn(Optional.empty());
        TicketDetailDomainServiceImpl service = new TicketDetailDomainServiceImpl(repo);

        assertSame(d, service.getTicketDefaultCacheVip("d1"));
        assertNull(service.getTicketDefaultCacheVip("none"));
    }

    // ---------------- TicketOrderDomainServiceImpl ----------------

    @Test
    void ticketOrder_stockOperationsDelegate() {
        TicketOrderRepository repo = mock(TicketOrderRepository.class);
        when(repo.decreaseStock("t1", 2)).thenReturn(true);
        when(repo.decreaseStockCAS("t1", 10, 2)).thenReturn(true);
        when(repo.increaseStock("t1", 2)).thenReturn(false);
        TicketOrderDomainServiceImpl service = new TicketOrderDomainServiceImpl(repo);

        assertTrue(service.decreaseStock("t1", 2));
        assertTrue(service.decreaseStockCAS("t1", 10, 2));
        assertFalse(service.increaseStock("t1", 2));
    }

    // ---------------- OrderDeductionDomainServiceImpl ----------------

    @Test
    void orderDeduction_queriesDelegate() {
        OrderDeductionRepository repo = mock(OrderDeductionRepository.class);
        OrderDeductionDomainServiceImpl service = new OrderDeductionDomainServiceImpl(repo);
        Object[] first = new Object[]{"a"};
        List<Object[]> rows = List.<Object[]>of(first);
        Object[] row = new Object[]{"a"};
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        when(repo.findAll("202610")).thenReturn(rows);
        when(repo.findByOrderNumber("202610", "o1")).thenReturn(row);
        when(repo.findByDateRange("202610", from, to)).thenReturn(rows);
        when(repo.findPage("202610", "", 10)).thenReturn(rows);
        when(repo.updateOrderStatus("202610", "o1", 2)).thenReturn(true);

        assertSame(rows, service.findAll("202610"));
        assertSame(row, service.findByOrderNumber("202610", "o1"));
        assertSame(rows, service.findByDateRange("202610", from, to));
        assertSame(rows, service.findPage("202610", "", 10));
        assertTrue(service.updateOrderStatus("202610", "o1", 2));
    }

    @Test
    void orderDeduction_insertDelegates() {
        OrderDeductionRepository repo = mock(OrderDeductionRepository.class);
        TicketOrder order = new TicketOrder();

        new OrderDeductionDomainServiceImpl(repo).insertOrder("202610", order);

        verify(repo).insertOrder("202610", order);
    }

    // ---------------- UserDetailsServiceImpl ----------------

    @Test
    void userDetails_loadByUsername_attachesRolesAndPermissions() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(users, roles, permissions);

        User user = new User();
        user.setId("u1");
        Role role = new Role();
        role.setId("r1");
        role.setKey("MEMBER");
        Permission permission = new Permission();
        permission.setPermissionKey("TICKET_READ");
        when(users.findByUsernameOrEmailOrPhone("alice", "alice", "alice")).thenReturn(user);
        when(roles.selectRolesByUserId("u1")).thenReturn(List.of(role));
        when(permissions.selectPermissionsByRoleId("r1")).thenReturn(List.of(permission));

        User loaded = (User) service.loadUserByUsername("alice");

        assertEquals(1, loaded.getRoles().size());
        assertTrue(loaded.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEMBER")));
        assertTrue(loaded.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("TICKET_READ")));
    }

    @Test
    void userDetails_loadByUsername_unknownUserThrows() {
        UserRepository users = mock(UserRepository.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(users, mock(RoleRepository.class), mock(PermissionRepository.class));
        when(users.findByUsernameOrEmailOrPhone(any(), any(), any())).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ghost"));
    }

    @Test
    void userDetails_loadById_attachesRoles() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(users, roles, permissions);
        User user = new User();
        user.setId("u1");
        Role role = new Role();
        role.setId("r1");
        role.setKey("ADMIN");
        when(users.findById("u1")).thenReturn(user);
        when(roles.selectRolesByUserId("u1")).thenReturn(List.of(role));
        when(permissions.selectPermissionsByRoleId("r1")).thenReturn(List.of());

        User loaded = (User) service.loadUserById("u1");

        assertEquals("ADMIN", loaded.getRoles().get(0).getKey());
    }

    @Test
    void userDetails_loadById_unknownUserThrows() {
        UserRepository users = mock(UserRepository.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(users, mock(RoleRepository.class), mock(PermissionRepository.class));
        when(users.findById("none")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserById("none"));
    }

    @Test
    void userDetails_loadById_secondCallIsServedFromCache() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(users, roles, mock(PermissionRepository.class));
        User user = new User();
        user.setId("u1");
        when(users.findById("u1")).thenReturn(user);
        when(roles.selectRolesByUserId("u1")).thenReturn(List.of());

        UserDetails first = service.loadUserById("u1");
        UserDetails second = service.loadUserById("u1");

        assertSame(first, second);
        verify(users, times(1)).findById("u1");
        verify(roles, times(1)).selectRolesByUserId("u1");
    }

    @Test
    void userDetails_loginWarmsCacheForLaterRequests() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(users, roles, mock(PermissionRepository.class));
        User user = new User();
        user.setId("u1");
        when(users.findByUsernameOrEmailOrPhone("alice", "alice", "alice")).thenReturn(user);
        when(roles.selectRolesByUserId("u1")).thenReturn(List.of());

        service.loadUserByUsername("alice");
        UserDetails byId = service.loadUserById("u1");

        assertSame(user, byId);
        verify(users, never()).findById(any());
    }

    @Test
    void userDetails_unknownUserIsNotCached() {
        UserRepository users = mock(UserRepository.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(users, mock(RoleRepository.class), mock(PermissionRepository.class));
        when(users.findById("u2")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserById("u2"));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserById("u2"));
        verify(users, times(2)).findById("u2");
    }

    @Test
    void userDetails_loginAlwaysReadsDatabase() {
        // Đăng nhập kiểm tra mật khẩu nên phải luôn đọc DB; cache lưu theo userId, không theo username
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(users, roles, mock(PermissionRepository.class));
        User user = new User();
        user.setId("u1");
        when(users.findById("u1")).thenReturn(user);
        when(roles.selectRolesByUserId("u1")).thenReturn(List.of());
        service.loadUserById("u1"); // u1 đã nằm trong cache

        // Không có tài khoản nào đăng nhập bằng chuỗi "u1" → phải báo không tìm thấy, không lấy user từ cache
        when(users.findByUsernameOrEmailOrPhone("u1", "u1", "u1")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("u1"));
        verify(users).findByUsernameOrEmailOrPhone("u1", "u1", "u1");
    }

    @Test
    void payment_findStuckInProgress_delegates() {
        PaymentTransactionRepository repo = mock(PaymentTransactionRepository.class);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(16);
        List<PaymentTransaction> stuck = List.of(new PaymentTransaction());
        when(repo.findStuckInProgress(cutoff, 50)).thenReturn(stuck);

        assertSame(stuck, new PaymentTransactionDomainImpl(repo).findStuckInProgress(cutoff, 50));
    }
}
