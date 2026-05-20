package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.OrderRequest;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    @Transactional
    public Order createOrder(Long buyerId, OrderRequest req) {
        List<CartItem> cartItems = cartItemRepository.findByBuyerId(buyerId);
        if (req.getCartItemIds() != null && !req.getCartItemIds().isEmpty()) {
            cartItems = cartItems.stream()
                    .filter(c -> req.getCartItemIds().contains(c.getId()))
                    .toList();
        }
        if (cartItems.isEmpty()) {
            throw BusinessException.badRequest("请先添加商品到购物车");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> BusinessException.notFound("商品不存在"));
            if (product.getStock() < cartItem.getQuantity()) {
                throw BusinessException.badRequest("商品 [" + product.getName() + "] 库存不足");
            }
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            String shopName = shopRepository.findBySellerId(product.getSellerId())
                    .map(Shop::getName).orElse(null);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .shopName(shopName)
                    .price(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build());
        }

        Order order = Order.builder()
                .buyerId(buyerId)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(total)
                .address(req.getAddress())
                .build();
        order = orderRepository.save(order);

        Long orderId = order.getId();
        orderItems.forEach(i -> i.setOrderId(orderId));
        orderItemRepository.saveAll(orderItems);

        List<Long> purchasedProductIds = cartItems.stream().map(CartItem::getProductId).toList();
        cartItemRepository.deleteByBuyerIdAndProductIdIn(buyerId, purchasedProductIds);

        return orderRepository.findById(orderId).orElseThrow();
    }

    public List<Order> getBuyerOrders(Long buyerId, OrderStatus status) {
        if (status != null) {
            return orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(buyerId, status);
        }
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public Order getOrderDetail(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (!order.getBuyerId().equals(buyerId)) {
            throw BusinessException.forbidden("无权查看该订单");
        }
        return order;
    }

    @Transactional
    public Order pay(Long orderId, Long buyerId) {
        Order order = getOrderDetail(orderId, buyerId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessException.badRequest("订单状态不允许支付");
        }
        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirm(Long orderId, Long buyerId) {
        Order order = getOrderDetail(orderId, buyerId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw BusinessException.badRequest("订单尚未发货，无法确认收货");
        }
        order.setStatus(OrderStatus.COMPLETED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancel(Long orderId, Long buyerId) {
        Order order = getOrderDetail(orderId, buyerId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessException.badRequest("只能取消待付款订单");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.getItems().forEach(item -> {
            productRepository.findById(item.getProductId()).ifPresent(p -> {
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            });
        });
        return orderRepository.save(order);
    }

    public List<Order> getSellerOrders(Long sellerId, OrderStatus status) {
        if (status != null) {
            return orderRepository.findSellerOrdersByStatus(sellerId, status);
        }
        return orderRepository.findSellerOrders(sellerId);
    }

    @Transactional
    public Order ship(Long orderId, Long sellerId, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getStatus() != OrderStatus.PAID) {
            throw BusinessException.badRequest("只能对已付款订单发货");
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        return orderRepository.save(order);
    }
}
