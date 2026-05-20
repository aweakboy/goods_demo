package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.entity.User;
import com.trading.enums.Role;
import com.trading.enums.UserStatus;
import com.trading.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock OrderRepository orderRepository;

    @InjectMocks AdminService adminService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void updateUserStatus_targetIsAdmin_throwsBadRequest() {
        User admin = User.builder().id(2L).role(Role.ADMIN).status(UserStatus.ACTIVE).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.updateUserStatus(2L, "DISABLED", 1L));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("不允许禁用管理员"));
    }

    @Test
    void updateUserStatus_targetIsSelf_throwsBadRequest() {
        User self = User.builder().id(1L).role(Role.BUYER).status(UserStatus.ACTIVE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.updateUserStatus(1L, "DISABLED", 1L));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("不允许禁用当前登录账户"));
    }

    @Test
    void updateUserStatus_validTarget_updatesStatus() {
        User buyer = User.builder().id(3L).role(Role.BUYER).status(UserStatus.ACTIVE).username("buyer1").build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(buyer));
        when(userRepository.save(any())).thenReturn(buyer);
        adminService.updateUserStatus(3L, "DISABLED", 1L);
        assertEquals(UserStatus.DISABLED, buyer.getStatus());
        verify(userRepository).save(buyer);
    }
}
