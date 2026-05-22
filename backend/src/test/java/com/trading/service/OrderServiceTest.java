package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.AddressValidationResult;
import com.trading.dto.OrderRequest;
import com.trading.entity.*;
import com.trading.enums.AddressValidationStatus;
import com.trading.enums.BuyerCouponStatus;
import com.trading.enums.CouponAudience;
import com.trading.enums.CouponStatus;
import com.trading.enums.OrderStatus;
import com.trading.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @Mock ShopRepository shopRepository;
    @Mock PaymentService paymentService;
    @Mock AddressValidationService addressValidationService;
    @Mock BuyerAddressService buyerAddressService;
    @Mock BuyerCouponService buyerCouponService;
    @Mock MembershipService membershipService;
    @Mock ShipmentService shipmentService;
    @Mock OrderCouponUsageRepository orderCouponUsageRepository;

    @InjectMocks OrderService orderService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void createOrder_emptyCart_throwsBadRequest() {
        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of());
        OrderRequest req = structuredAddressRequest();
        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void createOrder_insufficientStock_throwsBadRequest() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(5).build();
        Product product = Product.builder().id(10L).name("商品A").price(BigDecimal.TEN).stock(3).version(0).build();
        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        OrderRequest req = structuredAddressRequest();
        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("库存不足"));
    }

    @Test
    void validatePayable_nonPendingOrder_throwsBadRequest() {
        Order order = Order.builder().id(1L).buyerId(1L).status(OrderStatus.PAID).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.validatePayable(1L, 1L));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void createOrder_missingStructuredAddress_throwsBadRequest() {
        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, new OrderRequest()));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("收货信息不完整"));
    }

    @Test
    void createOrder_legacyAddressOnly_throwsBadRequest() {
        OrderRequest req = new OrderRequest();
        req.setAddress("浙江省杭州市西湖区文三路100号");

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("收货信息不完整"));
    }

    @Test
    void createOrder_legacyAndNewCouponFields_throwsBadRequest() {
        OrderRequest req = structuredAddressRequest();
        req.setBuyerCouponId(300L);
        req.setBuyerCouponIds(List.of(301L));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("不能同时提交"));
        verify(cartItemRepository, never()).findByBuyerId(anyLong());
    }

    @Test
    void createOrder_success_savesReceiverAddressSnapshot() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(2).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("商品A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        Shop shop = Shop.builder().id(30L).sellerId(20L).name("测试店铺").build();
        OrderRequest req = structuredAddressRequest();

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.of(shop));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order order = i.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(100L)).thenAnswer(i -> {
            Order saved = Order.builder()
                    .id(100L)
                    .buyerId(1L)
                    .status(OrderStatus.PENDING_PAYMENT)
                    .totalAmount(BigDecimal.valueOf(20))
                    .address("浙江省杭州市西湖区文三路100号")
                    .receiverName(req.getReceiverName())
                    .receiverPhone(req.getReceiverPhone())
                    .receiverProvince(req.getReceiverProvince())
                    .receiverCity(req.getReceiverCity())
                    .receiverDistrict(req.getReceiverDistrict())
                    .receiverDetailAddress(req.getReceiverDetailAddress())
                    .receiverFullAddress("浙江省杭州市西湖区文三路100号")
                    .receiverLongitude(new BigDecimal("120.1234567"))
                    .receiverLatitude(new BigDecimal("30.1234567"))
                    .receiverAddressValidationStatus(AddressValidationStatus.VALID)
                    .build();
            return Optional.of(saved);
        });

        Order order = orderService.createOrder(1L, req);

        assertEquals("张三", order.getReceiverName());
        assertEquals("13800138000", order.getReceiverPhone());
        assertEquals("浙江省杭州市西湖区文三路100号", order.getReceiverFullAddress());
        assertEquals(new BigDecimal("120.1234567"), order.getReceiverLongitude());
        assertEquals(AddressValidationStatus.VALID, order.getReceiverAddressValidationStatus());
        assertEquals(3, product.getStock());
        verify(cartItemRepository).deleteByBuyerIdAndProductIdIn(1L, List.of(10L));
    }

    @Test
    void createOrder_withAddressId_savesAddressBookSnapshot() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(2).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("商品A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        Shop shop = Shop.builder().id(30L).sellerId(20L).name("测试店铺").build();
        OrderRequest req = new OrderRequest();
        req.setAddressId(88L);
        Order[] saved = new Order[1];

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(buyerAddressService.getValidAddressForOrder(1L, 88L)).thenReturn(validBuyerAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.of(shop));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            saved[0] = i.getArgument(0);
            saved[0].setId(200L);
            return saved[0];
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(200L)).thenAnswer(i -> Optional.of(saved[0]));

        Order order = orderService.createOrder(1L, req);

        assertEquals("李四", order.getReceiverName());
        assertEquals("上海市上海市浦东新区世纪大道1号", order.getReceiverFullAddress());
        assertEquals(new BigDecimal("121.5000000"), order.getReceiverLongitude());
        assertEquals(AddressValidationStatus.VALID, order.getReceiverAddressValidationStatus());
        assertEquals(3, product.getStock());
        verify(addressValidationService, never()).validateOrThrow(any());
        verify(cartItemRepository).deleteByBuyerIdAndProductIdIn(1L, List.of(10L));
    }

    @Test
    void createOrder_withCoupon_savesAmountAndCouponSnapshotAndMarksUsed() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(3).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("商品A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        req.setBuyerCouponId(300L);
        Order[] saved = new Order[1];
        BuyerCouponService.CouponUsage usage = couponUsage();

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.empty());
        when(buyerCouponService.prepareForOrder(1L, 300L, BigDecimal.valueOf(30))).thenReturn(usage);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            saved[0] = i.getArgument(0);
            saved[0].setId(3000L);
            return saved[0];
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(3000L)).thenAnswer(i -> Optional.of(saved[0]));

        Order order = orderService.createOrder(1L, req);

        assertEquals(BigDecimal.valueOf(30), order.getOriginalAmount());
        assertEquals(BigDecimal.valueOf(5), order.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(25), order.getTotalAmount());
        assertEquals(200L, order.getCouponId());
        assertEquals(300L, order.getBuyerCouponId());
        assertEquals("满30减5", order.getCouponName());
        assertEquals(2, product.getStock());
        verify(buyerCouponService).markUsed(usage, 3000L);
        verify(cartItemRepository).deleteByBuyerIdAndProductIdIn(1L, List.of(10L));
    }

    @Test
    void createOrder_withTwoStackableCoupons_savesUsageDetailsAndTotalDiscount() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(5).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("商品A")
                .price(BigDecimal.TEN)
                .stock(6)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        req.setBuyerCouponIds(List.of(300L, 301L));
        Order[] saved = new Order[1];
        BuyerCouponService.CouponUsagePlan plan = multiCouponPlan(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(7)
        );

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.empty());
        when(buyerCouponService.prepareForOrder(1L, List.of(300L, 301L), BigDecimal.valueOf(50))).thenReturn(plan);
        when(orderCouponUsageRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            saved[0] = i.getArgument(0);
            saved[0].setId(3600L);
            return saved[0];
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(3600L)).thenAnswer(i -> Optional.of(saved[0]));

        Order order = orderService.createOrder(1L, req);

        assertEquals(BigDecimal.valueOf(50), order.getOriginalAmount());
        assertEquals(BigDecimal.valueOf(17), order.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(33), order.getTotalAmount());
        assertEquals(2, order.getCouponUsages().size());
        assertEquals(CouponAudience.PUBLIC, order.getCouponUsages().get(0).getAudience());
        assertEquals(CouponAudience.MEMBER, order.getCouponUsages().get(1).getAudience());
        assertEquals(BigDecimal.valueOf(7), order.getCouponUsages().get(1).getAppliedDiscountAmount());
        verify(buyerCouponService).markUsed(plan, 3600L);
        verify(buyerCouponService, never()).markUsed(any(BuyerCouponService.CouponUsage.class), anyLong());
    }

    @Test
    void createOrder_withTwoCoupons_appliesMembershipDiscountAfterCoupons() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(5).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("商品A")
                .price(BigDecimal.TEN)
                .stock(6)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        req.setBuyerCouponIds(List.of(300L, 301L));
        Order[] saved = new Order[1];
        BuyerCouponService.CouponUsagePlan plan = multiCouponPlan(
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(7)
        );

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.empty());
        when(buyerCouponService.prepareForOrder(1L, List.of(300L, 301L), BigDecimal.valueOf(50))).thenReturn(plan);
        when(membershipService.prepareOrderDiscount(1L, BigDecimal.valueOf(33)))
                .thenReturn(new MembershipService.OrderMembershipDiscount(
                        7L,
                        "Gold",
                        BigDecimal.valueOf(0.90),
                        new BigDecimal("3.30")
                ));
        when(orderCouponUsageRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            saved[0] = i.getArgument(0);
            saved[0].setId(3700L);
            return saved[0];
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(3700L)).thenAnswer(i -> Optional.of(saved[0]));

        Order order = orderService.createOrder(1L, req);

        assertEquals(BigDecimal.valueOf(17), order.getDiscountAmount());
        assertEquals(new BigDecimal("3.30"), order.getMembershipDiscountAmount());
        assertEquals(new BigDecimal("29.70"), order.getTotalAmount());
        verify(membershipService).prepareOrderDiscount(1L, BigDecimal.valueOf(33));
    }

    @Test
    void createOrder_withTwoCoupons_clampsPayableAmountAtZero() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(3).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("商品A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        req.setBuyerCouponIds(List.of(300L, 301L));
        Order[] saved = new Order[1];
        BuyerCouponService.CouponUsagePlan plan = multiCouponPlan(
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(20)
        );

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.empty());
        when(buyerCouponService.prepareForOrder(1L, List.of(300L, 301L), BigDecimal.valueOf(30))).thenReturn(plan);
        when(orderCouponUsageRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            saved[0] = i.getArgument(0);
            saved[0].setId(3800L);
            return saved[0];
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(3800L)).thenAnswer(i -> Optional.of(saved[0]));

        Order order = orderService.createOrder(1L, req);

        assertEquals(BigDecimal.valueOf(40), order.getDiscountAmount());
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        verify(membershipService).prepareOrderDiscount(1L, BigDecimal.ZERO);
    }

    @Test
    void createOrder_nonMember_doesNotApplyMembershipDiscount() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(2).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("Product A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        Order[] saved = new Order[1];

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.empty());
        when(membershipService.prepareOrderDiscount(1L, BigDecimal.valueOf(20))).thenReturn(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            saved[0] = i.getArgument(0);
            saved[0].setId(3100L);
            return saved[0];
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(3100L)).thenAnswer(i -> Optional.of(saved[0]));

        Order order = orderService.createOrder(1L, req);

        assertEquals(BigDecimal.valueOf(20), order.getTotalAmount());
        assertNull(order.getMembershipPlanId());
        assertNull(order.getMembershipPlanName());
        assertNull(order.getMembershipDiscountRate());
        assertEquals(BigDecimal.ZERO, order.getMembershipDiscountAmount());
        verify(membershipService).prepareOrderDiscount(1L, BigDecimal.valueOf(20));
    }

    @Test
    void createOrder_activeMember_appliesDiscountAndSavesSnapshot() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(2).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("Product A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        Order[] saved = new Order[1];

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.empty());
        when(membershipService.prepareOrderDiscount(1L, BigDecimal.valueOf(20)))
                .thenReturn(new MembershipService.OrderMembershipDiscount(
                        7L,
                        "Gold",
                        BigDecimal.valueOf(0.90),
                        BigDecimal.valueOf(2)
                ));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            saved[0] = i.getArgument(0);
            saved[0].setId(3200L);
            return saved[0];
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(3200L)).thenAnswer(i -> Optional.of(saved[0]));

        Order order = orderService.createOrder(1L, req);

        assertEquals(BigDecimal.valueOf(18), order.getTotalAmount());
        assertEquals(7L, order.getMembershipPlanId());
        assertEquals("Gold", order.getMembershipPlanName());
        assertEquals(BigDecimal.valueOf(0.90), order.getMembershipDiscountRate());
        assertEquals(BigDecimal.valueOf(2), order.getMembershipDiscountAmount());
    }

    @Test
    void createOrder_couponAndMembershipDiscount_stacksAfterCoupon() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(3).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("Product A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        req.setBuyerCouponId(300L);
        Order[] saved = new Order[1];
        BuyerCouponService.CouponUsage usage = couponUsage();

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.empty());
        when(buyerCouponService.prepareForOrder(1L, 300L, BigDecimal.valueOf(30))).thenReturn(usage);
        when(membershipService.prepareOrderDiscount(1L, BigDecimal.valueOf(25)))
                .thenReturn(new MembershipService.OrderMembershipDiscount(
                        7L,
                        "Gold",
                        BigDecimal.valueOf(0.95),
                        new BigDecimal("1.25")
                ));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            saved[0] = i.getArgument(0);
            saved[0].setId(3300L);
            return saved[0];
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(3300L)).thenAnswer(i -> Optional.of(saved[0]));

        Order order = orderService.createOrder(1L, req);

        assertEquals(BigDecimal.valueOf(30), order.getOriginalAmount());
        assertEquals(BigDecimal.valueOf(5), order.getDiscountAmount());
        assertEquals(new BigDecimal("1.25"), order.getMembershipDiscountAmount());
        assertEquals(new BigDecimal("23.75"), order.getTotalAmount());
        verify(buyerCouponService).markUsed(usage, 3300L);
    }

    @Test
    void createOrder_expiredMember_doesNotApplyMembershipDiscount() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(2).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("Product A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        Order[] saved = new Order[1];

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.empty());
        when(membershipService.prepareOrderDiscount(1L, BigDecimal.valueOf(20))).thenReturn(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            saved[0] = i.getArgument(0);
            saved[0].setId(3400L);
            return saved[0];
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(3400L)).thenAnswer(i -> Optional.of(saved[0]));

        Order order = orderService.createOrder(1L, req);

        assertEquals(BigDecimal.valueOf(20), order.getTotalAmount());
        assertNull(order.getMembershipPlanId());
        assertEquals(BigDecimal.ZERO, order.getMembershipDiscountAmount());
    }

    @Test
    void createOrder_membershipSnapshotKeepsAppliedValues() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(2).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("Product A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        Order[] saved = new Order[1];

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.empty());
        when(membershipService.prepareOrderDiscount(1L, BigDecimal.valueOf(20)))
                .thenReturn(new MembershipService.OrderMembershipDiscount(
                        7L,
                        "Gold",
                        BigDecimal.valueOf(0.90),
                        BigDecimal.valueOf(2)
                ));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            saved[0] = i.getArgument(0);
            saved[0].setId(3500L);
            return saved[0];
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(3500L)).thenAnswer(i -> Optional.of(saved[0]));

        Order order = orderService.createOrder(1L, req);

        assertEquals(7L, order.getMembershipPlanId());
        assertEquals("Gold", order.getMembershipPlanName());
        assertEquals(BigDecimal.valueOf(0.90), order.getMembershipDiscountRate());
        assertEquals(BigDecimal.valueOf(2), order.getMembershipDiscountAmount());
    }

    @Test
    void createOrder_invalidCoupon_doesNotCreateOrderOrDeductStockOrClearCart() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(3).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("商品A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        req.setBuyerCouponId(300L);

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.empty());
        when(buyerCouponService.prepareForOrder(1L, 300L, BigDecimal.valueOf(30)))
                .thenThrow(BusinessException.badRequest("订单金额未达到优惠券使用门槛"));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));

        assertEquals(400, ex.getStatus());
        assertEquals(5, product.getStock());
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(membershipService, never()).prepareOrderDiscount(anyLong(), any());
        verify(buyerCouponService, never()).markUsed(any(BuyerCouponService.CouponUsage.class), anyLong());
        verify(buyerCouponService, never()).markUsed(any(BuyerCouponService.CouponUsagePlan.class), anyLong());
        verify(cartItemRepository, never()).deleteByBuyerIdAndProductIdIn(anyLong(), anyList());
    }

    @Test
    void createOrder_insufficientStockWithCoupon_doesNotMarkCouponUsed() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(6).build();
        Product product = Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("商品A")
                .price(BigDecimal.TEN)
                .stock(5)
                .version(0)
                .build();
        OrderRequest req = structuredAddressRequest();
        req.setBuyerCouponId(300L);

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));

        assertEquals(400, ex.getStatus());
        verify(buyerCouponService, never()).prepareForOrder(anyLong(), anyLong(), any());
        verify(buyerCouponService, never()).markUsed(any(BuyerCouponService.CouponUsage.class), anyLong());
        verify(buyerCouponService, never()).markUsed(any(BuyerCouponService.CouponUsagePlan.class), anyLong());
    }

    @Test
    void createOrder_addressIdNotFound_doesNotCreateOrderOrDeductStock() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(2).build();
        OrderRequest req = new OrderRequest();
        req.setAddressId(404L);

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(buyerAddressService.getValidAddressForOrder(1L, 404L))
                .thenThrow(BusinessException.notFound("地址不存在"));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));

        assertEquals(404, ex.getStatus());
        verify(productRepository, never()).findById(anyLong());
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByBuyerIdAndProductIdIn(anyLong(), anyList());
    }

    @Test
    void createOrder_foreignAddressId_doesNotCreateOrderOrDeductStock() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(2).build();
        OrderRequest req = new OrderRequest();
        req.setAddressId(99L);

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(buyerAddressService.getValidAddressForOrder(1L, 99L))
                .thenThrow(BusinessException.forbidden("无权访问该地址"));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));

        assertEquals(403, ex.getStatus());
        verify(productRepository, never()).findById(anyLong());
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByBuyerIdAndProductIdIn(anyLong(), anyList());
    }

    @Test
    void createOrder_addressValidationFails_doesNotCreateOrderOrDeductStock() {
        CartItem cartItem = CartItem.builder().id(1L).buyerId(1L).productId(10L).quantity(2).build();
        OrderRequest req = structuredAddressRequest();

        when(cartItemRepository.findByBuyerId(1L)).thenReturn(List.of(cartItem));
        when(addressValidationService.validateOrThrow(any()))
                .thenThrow(BusinessException.badRequest("地址无法定位，请检查后重新填写"));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createOrder(1L, req));

        assertEquals(400, ex.getStatus());
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByBuyerIdAndProductIdIn(anyLong(), anyList());
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

    @Test
    void cancel_withCoupon_restoresStockReleasesCouponAndKeepsSnapshot() {
        OrderItem item = OrderItem.builder().productId(10L).quantity(2).build();
        Order order = couponOrder(OrderStatus.PENDING_PAYMENT);
        order.setItems(List.of(item));
        Product product = Product.builder().id(10L).stock(5).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.cancel(1L, 1L);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals(7, product.getStock());
        assertCouponSnapshotUnchanged(result);
        verify(buyerCouponService).releaseForOrder(300L, 1L);
    }

    @Test
    void doCancelOrder_withCoupon_releasesCouponForSystemCancelPath() {
        OrderItem item = OrderItem.builder().productId(10L).quantity(1).build();
        Order order = couponOrder(OrderStatus.PENDING_PAYMENT);
        order.setItems(List.of(item));
        Product product = Product.builder().id(10L).stock(5).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.doCancelOrder(1L);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertEquals(6, product.getStock());
        verify(buyerCouponService).releaseForOrder(300L, 1L);
    }

    @Test
    void cancel_withCouponUsageDetails_releasesAllCoupons() {
        OrderItem item = OrderItem.builder().productId(10L).quantity(1).build();
        Order order = couponOrder(OrderStatus.PENDING_PAYMENT);
        order.setItems(List.of(item));
        Product product = Product.builder().id(10L).stock(5).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderCouponUsageRepository.findByOrderIdOrderByIdAsc(1L))
                .thenReturn(List.of(orderCouponUsage(300L), orderCouponUsage(301L)));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.cancel(1L, 1L);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(buyerCouponService).releaseForOrder(List.of(300L, 301L), 1L);
        verify(buyerCouponService, never()).releaseForOrder(300L, 1L);
    }

    @Test
    void cancel_withoutCoupon_doesNotReleaseCoupon() {
        OrderItem item = OrderItem.builder().productId(10L).quantity(2).build();
        Order order = Order.builder().id(1L).buyerId(1L).status(OrderStatus.PENDING_PAYMENT)
                .items(List.of(item)).build();
        Product product = Product.builder().id(10L).stock(5).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.cancel(1L, 1L);

        verify(buyerCouponService, never()).releaseForOrder(anyLong(), anyLong());
        verify(buyerCouponService, never()).releaseForOrder(anyList(), anyLong());
    }

    @Test
    void requestRefund_withCoupon_keepsCouponUsed() {
        Order order = couponOrder(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.requestRefund(1L, 1L, "商品问题");

        assertEquals(OrderStatus.REFUND_REQUESTED, result.getStatus());
        verify(buyerCouponService, never()).releaseForOrder(anyLong(), anyLong());
        verify(buyerCouponService, never()).releaseForOrder(anyList(), anyLong());
    }

    @Test
    void approveRefund_withCoupon_releasesCouponAfterPaymentRefundSuccess() {
        Order order = couponOrder(OrderStatus.REFUND_REQUESTED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.approveRefund(1L);

        assertEquals(OrderStatus.REFUNDED, result.getStatus());
        assertCouponSnapshotUnchanged(result);
        verify(paymentService).refund("trade-1", BigDecimal.valueOf(25), "1-refund");
        verify(buyerCouponService).releaseForOrder(300L, 1L);
    }

    @Test
    void approveRefund_withCouponUsageDetails_releasesAllCouponsAfterPaymentRefundSuccess() {
        Order order = couponOrder(OrderStatus.REFUND_REQUESTED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderCouponUsageRepository.findByOrderIdOrderByIdAsc(1L))
                .thenReturn(List.of(orderCouponUsage(300L), orderCouponUsage(301L)));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.approveRefund(1L);

        assertEquals(OrderStatus.REFUNDED, result.getStatus());
        verify(paymentService).refund("trade-1", BigDecimal.valueOf(25), "1-refund");
        verify(buyerCouponService).releaseForOrder(List.of(300L, 301L), 1L);
        verify(buyerCouponService, never()).releaseForOrder(300L, 1L);
    }

    @Test
    void rejectRefund_withCoupon_keepsCouponUsed() {
        Order order = couponOrder(OrderStatus.REFUND_REQUESTED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.rejectRefund(1L, "不符合退款条件");

        assertEquals(OrderStatus.REFUND_REJECTED, result.getStatus());
        verify(buyerCouponService, never()).releaseForOrder(anyLong(), anyLong());
        verify(buyerCouponService, never()).releaseForOrder(anyList(), anyLong());
    }

    @Test
    void approveRefund_paymentFailure_keepsOrderRefundRequestedAndCouponUsed() {
        Order order = couponOrder(OrderStatus.REFUND_REQUESTED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("refund failed"))
                .when(paymentService).refund(anyString(), any(BigDecimal.class), anyString());

        assertThrows(RuntimeException.class, () -> orderService.approveRefund(1L));

        assertEquals(OrderStatus.REFUND_REQUESTED, order.getStatus());
        verify(buyerCouponService, never()).releaseForOrder(anyLong(), anyLong());
        verify(buyerCouponService, never()).releaseForOrder(anyList(), anyLong());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirm_shippedOrder_completesAndKeepsShipmentState() {
        Order order = Order.builder().id(1L).buyerId(1L).status(OrderStatus.SHIPPED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(shipmentService.attachShipment(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.confirm(1L, 1L);

        assertEquals(OrderStatus.COMPLETED, result.getStatus());
        verify(shipmentService).attachShipment(order);
    }

    @Test
    void ship_delegatesToShipmentService() {
        com.trading.dto.ShipRequest request = new com.trading.dto.ShipRequest();
        request.setCarrierCode("SF");
        request.setCarrierName("顺丰速运");
        request.setTrackingNumber("SF100");
        Order shipped = Order.builder().id(1L).status(OrderStatus.SHIPPED).trackingNumber("SF100").build();
        when(shipmentService.createShipment(1L, 2L, request)).thenReturn(shipped);

        Order result = orderService.ship(1L, 2L, request);

        assertSame(shipped, result);
        verify(shipmentService).createShipment(1L, 2L, request);
    }

    private OrderRequest structuredAddressRequest() {
        OrderRequest req = new OrderRequest();
        req.setReceiverName("张三");
        req.setReceiverPhone("13800138000");
        req.setReceiverProvince("浙江省");
        req.setReceiverCity("杭州市");
        req.setReceiverDistrict("西湖区");
        req.setReceiverDetailAddress("文三路100号");
        return req;
    }

    private AddressValidationResult validAddress() {
        return AddressValidationResult.builder()
                .status(AddressValidationStatus.VALID)
                .longitude(new BigDecimal("120.1234567"))
                .latitude(new BigDecimal("30.1234567"))
                .formattedAddress("浙江省杭州市西湖区文三路100号")
                .province("浙江省")
                .city("杭州市")
                .district("西湖区")
                .build();
    }

    private BuyerAddress validBuyerAddress() {
        return BuyerAddress.builder()
                .id(88L)
                .buyerId(1L)
                .receiverName("李四")
                .receiverPhone("13900139000")
                .province("上海市")
                .city("上海市")
                .district("浦东新区")
                .detailAddress("世纪大道1号")
                .fullAddress("上海市上海市浦东新区世纪大道1号")
                .longitude(new BigDecimal("121.5000000"))
                .latitude(new BigDecimal("31.2000000"))
                .validationStatus(AddressValidationStatus.VALID)
                .defaultAddress(true)
                .build();
    }

    private BuyerCouponService.CouponUsage couponUsage() {
        Coupon coupon = Coupon.builder()
                .id(200L)
                .name("满30减5")
                .thresholdAmount(BigDecimal.valueOf(30))
                .discountAmount(BigDecimal.valueOf(5))
                .status(CouponStatus.ACTIVE)
                .build();
        BuyerCoupon buyerCoupon = BuyerCoupon.builder()
                .id(300L)
                .buyerId(1L)
                .couponId(200L)
                .status(BuyerCouponStatus.UNUSED)
                .build();
        buyerCoupon.setCoupon(coupon);
        return new BuyerCouponService.CouponUsage(
                buyerCoupon,
                200L,
                300L,
                "满30减5",
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(5)
        );
    }

    private BuyerCouponService.CouponUsagePlan multiCouponPlan(BigDecimal publicDiscount, BigDecimal memberDiscount) {
        return new BuyerCouponService.CouponUsagePlan(List.of(
                couponUsage(200L, 300L, "普通券", publicDiscount, CouponAudience.PUBLIC),
                couponUsage(201L, 301L, "会员券", memberDiscount, CouponAudience.MEMBER)
        ));
    }

    private BuyerCouponService.CouponUsage couponUsage(Long couponId, Long buyerCouponId, String name,
                                                       BigDecimal appliedDiscount, CouponAudience audience) {
        BuyerCoupon buyerCoupon = BuyerCoupon.builder()
                .id(buyerCouponId)
                .buyerId(1L)
                .couponId(couponId)
                .status(BuyerCouponStatus.UNUSED)
                .build();
        return new BuyerCouponService.CouponUsage(
                buyerCoupon,
                couponId,
                buyerCouponId,
                name,
                BigDecimal.valueOf(30),
                appliedDiscount,
                appliedDiscount,
                audience,
                true
        );
    }

    private OrderCouponUsage orderCouponUsage(Long buyerCouponId) {
        return OrderCouponUsage.builder()
                .orderId(1L)
                .couponId(buyerCouponId + 1000)
                .buyerCouponId(buyerCouponId)
                .couponName("券" + buyerCouponId)
                .audience(buyerCouponId == 301L ? CouponAudience.MEMBER : CouponAudience.PUBLIC)
                .stackable(true)
                .thresholdAmount(BigDecimal.valueOf(30))
                .couponDiscountAmount(BigDecimal.valueOf(5))
                .appliedDiscountAmount(BigDecimal.valueOf(5))
                .build();
    }

    private Order couponOrder(OrderStatus status) {
        return Order.builder()
                .id(1L)
                .buyerId(1L)
                .status(status)
                .totalAmount(BigDecimal.valueOf(25))
                .originalAmount(BigDecimal.valueOf(30))
                .discountAmount(BigDecimal.valueOf(5))
                .couponId(200L)
                .buyerCouponId(300L)
                .couponName("满30减5")
                .couponThresholdAmount(BigDecimal.valueOf(30))
                .couponDiscountAmount(BigDecimal.valueOf(5))
                .address("浙江省杭州市西湖区文三路100号")
                .alipayTradeNo("trade-1")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void assertCouponSnapshotUnchanged(Order order) {
        assertEquals(BigDecimal.valueOf(30), order.getOriginalAmount());
        assertEquals(BigDecimal.valueOf(5), order.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(25), order.getTotalAmount());
        assertEquals(200L, order.getCouponId());
        assertEquals(300L, order.getBuyerCouponId());
        assertEquals("满30减5", order.getCouponName());
        assertEquals(BigDecimal.valueOf(30), order.getCouponThresholdAmount());
        assertEquals(BigDecimal.valueOf(5), order.getCouponDiscountAmount());
    }
}
