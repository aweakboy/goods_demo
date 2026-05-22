package com.trading.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.trading.common.BusinessException;
import com.trading.config.AlipayConfig;
import com.trading.entity.Order;
import com.trading.enums.OrderStatus;
import com.trading.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock AlipayConfig alipayConfig;
    @Mock OrderRepository orderRepository;
    @Mock AlipayClientFactory alipayClientFactory;
    @Mock AlipaySignatureVerifier alipaySignatureVerifier;
    @Mock AlipayClient alipayClient;
    @Mock MembershipService membershipService;

    @InjectMocks PaymentService paymentService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(alipayConfig.getAlipayPublicKey()).thenReturn("public-key");
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void handleNotify_success_marksOrderPaid() throws Exception {
        Order order = pendingOrder();
        when(alipaySignatureVerifier.verify(anyMap(), anyString())).thenReturn(true);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        boolean result = paymentService.handleNotify(Map.of(
                "trade_status", "TRADE_SUCCESS",
                "out_trade_no", "100",
                "trade_no", "trade-100"
        ));

        assertTrue(result);
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals("trade-100", order.getAlipayTradeNo());
        verify(orderRepository).save(order);
    }

    @Test
    void handleNotify_paidOrder_isIdempotent() throws Exception {
        Order order = paidOrder();
        when(alipaySignatureVerifier.verify(anyMap(), anyString())).thenReturn(true);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        boolean result = paymentService.handleNotify(Map.of(
                "trade_status", "TRADE_SUCCESS",
                "out_trade_no", "100",
                "trade_no", "trade-100"
        ));

        assertTrue(result);
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleNotify_membershipTrade_routesToMembershipService() throws Exception {
        when(alipaySignatureVerifier.verify(anyMap(), anyString())).thenReturn(true);

        boolean result = paymentService.handleNotify(Map.of(
                "trade_status", "TRADE_SUCCESS",
                "out_trade_no", "MEM-20",
                "trade_no", "trade-20"
        ));

        assertTrue(result);
        verify(membershipService).handlePaymentSuccess(20L, "trade-20");
        verify(orderRepository, never()).findById(anyLong());
    }

    @Test
    void handleNotify_invalidSignature_doesNotUpdateOrder() throws Exception {
        when(alipaySignatureVerifier.verify(anyMap(), anyString())).thenReturn(false);

        boolean result = paymentService.handleNotify(Map.of(
                "trade_status", "TRADE_SUCCESS",
                "out_trade_no", "100",
                "trade_no", "trade-100"
        ));

        assertFalse(result);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void reconcileOrderPayment_successQuery_marksOrderPaid() throws Exception {
        Order order = pendingOrder();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(alipayClientFactory.create()).thenReturn(alipayClient);
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(successQuery("20.00"));

        Order result = paymentService.reconcileOrderPayment(100L, 1L);

        assertSame(order, result);
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals("trade-100", order.getAlipayTradeNo());
        verify(orderRepository).save(order);
    }

    @Test
    void reconcileOrderPayment_amountMismatch_doesNotMarkPaid() throws Exception {
        Order order = pendingOrder();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(alipayClientFactory.create()).thenReturn(alipayClient);
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(successQuery("19.99"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.reconcileOrderPayment(100L, 1L));

        assertEquals(400, ex.getStatus());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void reconcileOrderPayment_nonSuccessTrade_doesNotMarkPaid() throws Exception {
        Order order = pendingOrder();
        AlipayTradeQueryResponse response = successQuery("20.00");
        response.setTradeStatus("WAIT_BUYER_PAY");
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(alipayClientFactory.create()).thenReturn(alipayClient);
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(response);

        Order result = paymentService.reconcileOrderPayment(100L, 1L);

        assertSame(order, result);
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void reconcileOrderPayment_otherBuyer_throwsForbidden() {
        Order order = pendingOrder();
        order.setBuyerId(2L);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.reconcileOrderPayment(100L, 1L));

        assertEquals(403, ex.getStatus());
        verify(alipayClientFactory, never()).create();
    }

    private Order pendingOrder() {
        return Order.builder()
                .id(100L)
                .buyerId(1L)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("20.00"))
                .build();
    }

    private Order paidOrder() {
        return Order.builder()
                .id(100L)
                .buyerId(1L)
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("20.00"))
                .alipayTradeNo("trade-100")
                .build();
    }

    private AlipayTradeQueryResponse successQuery(String amount) {
        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setCode("10000");
        response.setOutTradeNo("100");
        response.setTradeNo("trade-100");
        response.setTradeStatus("TRADE_SUCCESS");
        response.setTotalAmount(amount);
        return response;
    }
}
