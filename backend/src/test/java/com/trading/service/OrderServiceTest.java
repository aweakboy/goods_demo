package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.OrderRequest;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;

    @InjectMocks OrderService orderService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void createOrder_emptyCart_throwsBadRequest() {
        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of());
        OrderRequest req = new OrderRequest();
        req.setAddress("测试地址");
        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void createOrder_insufficientStock_throwsBadRequest() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(5).build();
        Product product = Product.builder().id(10L).name("商品A").price(BigDecimal.TEN).stock(3).version(0).build();
        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        OrderRequest req = new OrderRequest();
        req.setAddress("测试地址");
        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("库存不足"));
    }

    @Test
    void pay_nonPendingOrder_throwsBadRequest() {
        Order order = Order.builder().id(1L).buyerId(1L).status(OrderStatus.PAID).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.pay(1L, 1L));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void cancel_restoresStock() {
        OrderItem item = OrderItem.builder().productId(10L).quantity(2).build();
        Order order = Order.builder().id(1L).buyerId(1L).status(OrderStatus.PENDING_PAYMENT)
                .items(List.of(item)).build();
        Product product = Product.builder().id(10L).stock(5).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        orderService.cancel(1L, 1L);
        assertEquals(7, product.getStock());
    }
}
