package com.trading.service;

import com.trading.annotation.OperationLog;
import com.trading.common.BusinessException;
import com.trading.dto.AddressValidationRequest;
import com.trading.dto.OrderRequest;
import com.trading.dto.ShipRequest;
import com.trading.entity.*;
import com.trading.enums.AddressValidationStatus;
import com.trading.enums.CouponAudience;
import com.trading.enums.OrderStatus;
import com.trading.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final AddressValidationService addressValidationService;
    private final BuyerAddressService buyerAddressService;
    private final BuyerCouponService buyerCouponService;
    private final MembershipService membershipService;
    private final ShipmentService shipmentService;
    private final OrderCouponUsageRepository orderCouponUsageRepository;

    @Value("${app.order.payment-timeout-minutes:30}")
    private int paymentTimeoutMinutes;

    @Value("${app.order.refund-window-days:7}")
    private int refundWindowDays;

    private final PaymentService paymentService;

    @OperationLog(module = "订单", action = "创建订单")
    @Transactional
    public Order createOrder(Long buyerId, OrderRequest req) {
        if (req.getBuyerCouponId() != null && req.getBuyerCouponIds() != null && !req.getBuyerCouponIds().isEmpty()) {
            throw BusinessException.badRequest("不能同时提交旧优惠券字段和多券字段");
        }
        if (req.getAddressId() == null && !hasStructuredAddress(req)) {
            throw BusinessException.badRequest("收货信息不完整");
        }

        List<CartItem> cartItems = cartItemRepository.findByBuyerId(buyerId);
        if (req.getCartItemIds() != null && !req.getCartItemIds().isEmpty()) {
            cartItems = cartItems.stream()
                    .filter(c -> req.getCartItemIds().contains(c.getId()))
                    .toList();
        }
        if (cartItems.isEmpty()) {
            throw BusinessException.badRequest("请先添加商品到购物车");
        }

        AddressSnapshot receiver = resolveReceiverAddress(buyerId, req);

        BigDecimal originalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<StockUpdate> stockUpdates = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> BusinessException.notFound("商品不存在"));
            if (product.getStock() < cartItem.getQuantity()) {
                throw BusinessException.badRequest("商品 [" + product.getName() + "] 库存不足");
            }
            stockUpdates.add(new StockUpdate(product, cartItem.getQuantity()));

            String shopName = shopRepository.findBySellerId(product.getSellerId())
                    .map(Shop::getName).orElse(null);
            originalAmount = originalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .shopName(shopName)
                    .price(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build());
        }

        BuyerCouponService.CouponUsage couponUsage = null;
        BuyerCouponService.CouponUsagePlan couponUsagePlan = BuyerCouponService.CouponUsagePlan.empty();
        boolean legacyCouponField = false;
        if (req.getBuyerCouponId() != null) {
            legacyCouponField = true;
            couponUsage = buyerCouponService.prepareForOrder(buyerId, req.getBuyerCouponId(), originalAmount);
            couponUsagePlan = new BuyerCouponService.CouponUsagePlan(List.of(couponUsage));
        } else if (req.getBuyerCouponIds() != null && !req.getBuyerCouponIds().isEmpty()) {
            couponUsagePlan = buyerCouponService.prepareForOrder(buyerId, req.getBuyerCouponIds(), originalAmount);
            couponUsage = couponUsagePlan.primaryUsage();
        }
        BigDecimal discountAmount = couponUsagePlan.totalDiscount();
        BigDecimal amountAfterCoupon = originalAmount.subtract(discountAmount);
        if (amountAfterCoupon.compareTo(BigDecimal.ZERO) < 0) {
            amountAfterCoupon = BigDecimal.ZERO;
        }
        MembershipService.OrderMembershipDiscount membershipDiscount =
                membershipService != null ? membershipService.prepareOrderDiscount(buyerId, amountAfterCoupon) : null;
        BigDecimal membershipDiscountAmount = membershipDiscount != null
                ? membershipDiscount.discountAmount()
                : BigDecimal.ZERO;
        BigDecimal total = amountAfterCoupon.subtract(membershipDiscountAmount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        for (StockUpdate update : stockUpdates) {
            Product product = update.product();
            product.setStock(product.getStock() - update.quantity());
            productRepository.save(product);
        }

        Order order = Order.builder()
                .buyerId(buyerId)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(total)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .couponId(couponUsage != null ? couponUsage.couponId() : null)
                .buyerCouponId(couponUsage != null ? couponUsage.buyerCouponId() : null)
                .couponName(couponUsage != null ? couponUsage.couponName() : null)
                .couponThresholdAmount(couponUsage != null ? couponUsage.thresholdAmount() : null)
                .couponDiscountAmount(couponUsage != null ? couponUsage.couponDiscountAmount() : null)
                .membershipPlanId(membershipDiscount != null ? membershipDiscount.planId() : null)
                .membershipPlanName(membershipDiscount != null ? membershipDiscount.planName() : null)
                .membershipDiscountRate(membershipDiscount != null ? membershipDiscount.discountRate() : null)
                .membershipDiscountAmount(membershipDiscountAmount)
                .address(receiver.fullAddress())
                .receiverName(receiver.receiverName())
                .receiverPhone(receiver.receiverPhone())
                .receiverProvince(receiver.province())
                .receiverCity(receiver.city())
                .receiverDistrict(receiver.district())
                .receiverDetailAddress(receiver.detailAddress())
                .receiverFullAddress(receiver.fullAddress())
                .receiverLongitude(receiver.longitude())
                .receiverLatitude(receiver.latitude())
                .receiverAddressValidationStatus(receiver.validationStatus())
                .expiredAt(LocalDateTime.now().plusMinutes(paymentTimeoutMinutes))
                .build();
        order = orderRepository.save(order);

        Long orderId = order.getId();
        List<OrderCouponUsage> couponUsages = saveCouponUsages(orderId, couponUsagePlan);
        if (legacyCouponField) {
            buyerCouponService.markUsed(couponUsage, orderId);
        } else {
            buyerCouponService.markUsed(couponUsagePlan, orderId);
        }
        orderItems.forEach(i -> i.setOrderId(orderId));
        orderItemRepository.saveAll(orderItems);

        List<Long> purchasedProductIds = cartItems.stream().map(CartItem::getProductId).toList();
        cartItemRepository.deleteByBuyerIdAndProductIdIn(buyerId, purchasedProductIds);

        Order result = orderRepository.findById(orderId).orElseThrow();
        result.setCouponUsages(couponUsages);
        return result;
    }

    public List<Order> getBuyerOrders(Long buyerId, OrderStatus status) {
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(buyerId, status);
        } else {
            orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
        }
        return enrichOrders(orders);
    }

    public Order getOrderDetail(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (!order.getBuyerId().equals(buyerId)) {
            throw BusinessException.forbidden("无权查看该订单");
        }
        return enrichOrder(order);
    }

    public Order validatePayable(Long orderId, Long buyerId) {
        Order order = getOrderDetail(orderId, buyerId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessException.badRequest("订单状态不允许支付");
        }
        return order;
    }

    @Transactional
    public Order confirm(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (!order.getBuyerId().equals(buyerId)) {
            throw BusinessException.forbidden("无权查看该订单");
        }
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw BusinessException.badRequest("订单尚未发货，无法确认收货");
        }
        order.setStatus(OrderStatus.COMPLETED);
        Order saved = orderRepository.save(order);
        return enrichOrder(saved);
    }

    @Transactional
    public Order cancel(Long orderId, Long buyerId) {
        Order order = getOrderDetail(orderId, buyerId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessException.badRequest("只能取消待付款订单");
        }
        return doCancelOrder(orderId);
    }

    @Transactional
    public Order doCancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return order; // 幂等：已被其他路径取消则直接返回
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.getItems().forEach(item ->
            productRepository.findById(item.getProductId()).ifPresent(p -> {
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            })
        );
        releaseCouponsForOrder(order);
        return orderRepository.save(order);
    }

    public List<Order> getSellerOrders(Long sellerId, OrderStatus status) {
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findSellerOrdersByStatus(sellerId, status);
        } else {
            orders = orderRepository.findSellerOrders(sellerId);
        }
        return enrichOrders(orders);
    }

    @Transactional
    public Order ship(Long orderId, Long sellerId, ShipRequest request) {
        return shipmentService.createShipment(orderId, sellerId, request);
    }

    @Transactional
    public Order ship(Long orderId, Long sellerId, String trackingNumber) {
        ShipRequest request = new ShipRequest();
        request.setCarrierCode("OTHER");
        request.setCarrierName("其他物流");
        request.setTrackingNumber(trackingNumber);
        return ship(orderId, sellerId, request);
    }

    @OperationLog(module = "退款", action = "申请退款")
    @Transactional
    public Order requestRefund(Long orderId, Long buyerId, String reason) {
        Order order = getOrderDetail(orderId, buyerId);
        OrderStatus s = order.getStatus();
        if (s == OrderStatus.COMPLETED) {
            if (order.getCreatedAt().plusDays(refundWindowDays).isBefore(LocalDateTime.now())) {
                throw BusinessException.badRequest("订单已超过退款期限");
            }
        } else if (s != OrderStatus.PAID && s != OrderStatus.SHIPPED) {
            throw BusinessException.badRequest("当前订单状态不支持退款");
        }
        order.setStatus(OrderStatus.REFUND_REQUESTED);
        order.setRefundReason(reason);
        return orderRepository.save(order);
    }

    @OperationLog(module = "退款", action = "批准退款")
    @Transactional
    public Order approveRefund(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getStatus() != OrderStatus.REFUND_REQUESTED) {
            throw BusinessException.badRequest("订单不在退款申请中");
        }
        paymentService.refund(
                order.getAlipayTradeNo(),
                order.getTotalAmount(),
                orderId + "-refund"
        );
        order.setStatus(OrderStatus.REFUNDED);
        releaseCouponsForOrder(order);
        return orderRepository.save(order);
    }

    @OperationLog(module = "退款", action = "拒绝退款")
    @Transactional
    public Order rejectRefund(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getStatus() != OrderStatus.REFUND_REQUESTED) {
            throw BusinessException.badRequest("订单不在退款申请中");
        }
        order.setStatus(OrderStatus.REFUND_REJECTED);
        order.setRefundRejectReason(reason);
        return orderRepository.save(order);
    }

    public List<Order> getRefundRequests() {
        return orderRepository.findByStatus(OrderStatus.REFUND_REQUESTED);
    }

    private boolean hasStructuredAddress(OrderRequest req) {
        return !isBlank(req.getReceiverName())
                && !isBlank(req.getReceiverPhone())
                && !isBlank(req.getReceiverProvince())
                && !isBlank(req.getReceiverCity())
                && !isBlank(req.getReceiverDistrict())
                && !isBlank(req.getReceiverDetailAddress());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String buildFullAddress(String province, String city, String district, String detailAddress) {
        return String.join("", province.trim(), city.trim(), district.trim(), detailAddress.trim());
    }

    private AddressSnapshot resolveReceiverAddress(Long buyerId, OrderRequest req) {
        if (req.getAddressId() != null) {
            BuyerAddress address = buyerAddressService.getValidAddressForOrder(buyerId, req.getAddressId());
            return new AddressSnapshot(
                    address.getReceiverName(),
                    address.getReceiverPhone(),
                    address.getProvince(),
                    address.getCity(),
                    address.getDistrict(),
                    address.getDetailAddress(),
                    address.getFullAddress(),
                    address.getLongitude(),
                    address.getLatitude(),
                    address.getValidationStatus()
            );
        }

        String receiverFullAddress = buildFullAddress(
                req.getReceiverProvince(),
                req.getReceiverCity(),
                req.getReceiverDistrict(),
                req.getReceiverDetailAddress()
        );
        var validation = addressValidationService.validateOrThrow(new AddressValidationRequest(
                req.getReceiverProvince(),
                req.getReceiverCity(),
                req.getReceiverDistrict(),
                req.getReceiverDetailAddress()
        ));
        return new AddressSnapshot(
                req.getReceiverName(),
                req.getReceiverPhone(),
                req.getReceiverProvince(),
                req.getReceiverCity(),
                req.getReceiverDistrict(),
                req.getReceiverDetailAddress(),
                receiverFullAddress,
                validation.getLongitude(),
                validation.getLatitude(),
                AddressValidationStatus.VALID
        );
    }

    private record AddressSnapshot(
            String receiverName,
            String receiverPhone,
            String province,
            String city,
            String district,
            String detailAddress,
            String fullAddress,
            BigDecimal longitude,
            BigDecimal latitude,
            AddressValidationStatus validationStatus
    ) {}

    private record StockUpdate(Product product, int quantity) {}

    private List<OrderCouponUsage> saveCouponUsages(Long orderId, BuyerCouponService.CouponUsagePlan couponUsagePlan) {
        if (couponUsagePlan == null || couponUsagePlan.usages().isEmpty()) {
            return List.of();
        }
        List<OrderCouponUsage> usages = couponUsagePlan.usages().stream()
                .map(usage -> OrderCouponUsage.builder()
                        .orderId(orderId)
                        .couponId(usage.couponId())
                        .buyerCouponId(usage.buyerCouponId())
                        .couponName(usage.couponName())
                        .audience(usage.audience() != null ? usage.audience() : CouponAudience.PUBLIC)
                        .stackable(usage.stackable())
                        .thresholdAmount(usage.thresholdAmount())
                        .couponDiscountAmount(usage.couponDiscountAmount())
                        .appliedDiscountAmount(usage.appliedDiscount())
                        .build())
                .toList();
        List<OrderCouponUsage> saved = orderCouponUsageRepository.saveAll(usages);
        return saved == null ? usages : saved;
    }

    private void releaseCouponsForOrder(Order order) {
        List<OrderCouponUsage> usages = orderCouponUsageRepository.findByOrderIdOrderByIdAsc(order.getId());
        if (usages != null && !usages.isEmpty()) {
            buyerCouponService.releaseForOrder(
                    usages.stream().map(OrderCouponUsage::getBuyerCouponId).toList(),
                    order.getId()
            );
            return;
        }
        if (order.getBuyerCouponId() != null) {
            buyerCouponService.releaseForOrder(order.getBuyerCouponId(), order.getId());
        }
    }

    private Order enrichOrder(Order order) {
        return enrichCouponUsages(enrichShipment(order));
    }

    private List<Order> enrichOrders(List<Order> orders) {
        return enrichCouponUsages(enrichShipments(orders));
    }

    private Order enrichCouponUsages(Order order) {
        if (order == null || order.getId() == null) {
            return order;
        }
        List<OrderCouponUsage> usages = orderCouponUsageRepository.findByOrderIdOrderByIdAsc(order.getId());
        order.setCouponUsages(usages == null ? List.of() : usages);
        return order;
    }

    private List<Order> enrichCouponUsages(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return orders;
        }
        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .filter(id -> id != null)
                .toList();
        if (orderIds.isEmpty()) {
            return orders;
        }
        List<OrderCouponUsage> usages = orderCouponUsageRepository.findByOrderIdInOrderByOrderIdAscIdAsc(orderIds);
        Map<Long, List<OrderCouponUsage>> usageMap = usages == null ? Map.of() : usages.stream()
                .collect(Collectors.groupingBy(OrderCouponUsage::getOrderId));
        orders.forEach(order -> order.setCouponUsages(usageMap.getOrDefault(order.getId(), List.of())));
        return orders;
    }

    private Order enrichShipment(Order order) {
        if (shipmentService == null) {
            return order;
        }
        Order enriched = shipmentService.attachShipment(order);
        return enriched != null ? enriched : order;
    }

    private List<Order> enrichShipments(List<Order> orders) {
        if (shipmentService == null) {
            return orders;
        }
        List<Order> enriched = shipmentService.attachShipments(orders);
        return enriched != null ? enriched : orders;
    }
}
