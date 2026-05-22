package com.trading.service;

import com.trading.annotation.OperationLog;
import com.trading.common.BusinessException;
import com.trading.dto.*;
import com.trading.entity.*;
import com.trading.enums.*;
import com.trading.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final ShipmentService shipmentService;

    // --- Overview ---

    public AdminOverviewResponse getOverview() {
        long totalUsers = userRepository.count();
        long activeProducts = productRepository.countByStatus(ProductStatus.ACTIVE);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayOrders = orderRepository.countByCreatedAtBetween(todayStart, LocalDateTime.now());
        var totalRevenue = orderRepository.sumCompletedAmount();
        return new AdminOverviewResponse(totalUsers, activeProducts, todayOrders, totalRevenue);
    }

    // --- User Management ---

    public Page<AdminUserResponse> getUsers(String role, String status, int page, int size) {
        Role roleEnum = role != null ? Role.valueOf(role) : null;
        UserStatus statusEnum = status != null ? UserStatus.valueOf(status) : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findByFilters(roleEnum, statusEnum, pageable)
                .map(AdminUserResponse::from);
    }

    @OperationLog(module = "管理", action = "修改用户状态")
    @Transactional
    public AdminUserResponse updateUserStatus(Long targetId, String newStatus, Long currentAdminId) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if (target.getRole() == Role.ADMIN) {
            throw BusinessException.badRequest("不允许禁用管理员账户");
        }
        if (targetId.equals(currentAdminId)) {
            throw BusinessException.badRequest("不允许禁用当前登录账户");
        }
        target.setStatus(UserStatus.valueOf(newStatus));
        return AdminUserResponse.from(userRepository.save(target));
    }

    // --- Product Moderation ---

    public Page<AdminProductResponse> getProducts(String status, String sellerName, String shopName, int page, int size) {
        ProductStatus statusEnum = status != null ? ProductStatus.valueOf(status) : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findByAdminFilters(statusEnum, sellerName, shopName, pageable);

        List<Long> sellerIds = products.getContent().stream().map(Product::getSellerId).distinct().collect(Collectors.toList());
        Map<Long, String> sellerNameMap = userRepository.findAllById(sellerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        Map<Long, String> shopNameMap = new java.util.HashMap<>();
        for (Long sid : sellerIds) {
            shopRepository.findBySellerId(sid).ifPresent(s -> shopNameMap.put(sid, s.getName()));
        }

        return products.map(p -> AdminProductResponse.from(
                p,
                sellerNameMap.getOrDefault(p.getSellerId(), ""),
                shopNameMap.get(p.getSellerId())
        ));
    }

    @Transactional
    public void updateProductStatus(Long productId, String newStatus) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound("商品不存在"));
        product.setStatus(ProductStatus.valueOf(newStatus));
        productRepository.save(product);
    }

    // --- Category Management ---

    public List<AdminCategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by("id")).stream()
                .map(c -> AdminCategoryResponse.from(c, productRepository.countByCategoryId(c.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminCategoryResponse createCategory(String name) {
        if (categoryRepository.existsByName(name)) {
            throw BusinessException.badRequest("分类名称已存在");
        }
        Category category = Category.builder().name(name).status(ProductStatus.ACTIVE).build();
        category = categoryRepository.save(category);
        return AdminCategoryResponse.from(category, 0);
    }

    @Transactional
    public AdminCategoryResponse updateCategory(Long id, String name) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("分类不存在"));
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw BusinessException.badRequest("分类名称已存在");
        }
        category.setName(name);
        category = categoryRepository.save(category);
        long count = productRepository.countByCategoryId(id);
        return AdminCategoryResponse.from(category, count);
    }

    @Transactional
    public AdminCategoryResponse updateCategoryStatus(Long id, String newStatus) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("分类不存在"));
        category.setStatus(ProductStatus.valueOf(newStatus));
        category = categoryRepository.save(category);
        long count = productRepository.countByCategoryId(id);
        return AdminCategoryResponse.from(category, count);
    }

    // --- Order Oversight ---

    public Page<AdminOrderResponse> getOrders(String status, int page, int size) {
        OrderStatus statusEnum = status != null ? OrderStatus.valueOf(status) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findAllByAdminFilter(statusEnum, pageable);

        List<Long> buyerIds = orders.getContent().stream().map(Order::getBuyerId).distinct().collect(Collectors.toList());
        Map<Long, String> buyerNames = userRepository.findAllById(buyerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return orders.map(o -> AdminOrderResponse.from(
                o,
                buyerNames.getOrDefault(o.getBuyerId(), ""),
                false,
                shipmentService != null ? shipmentService.getShipmentResponse(o) : null
        ));
    }

    public AdminOrderResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        String buyerName = userRepository.findById(order.getBuyerId())
                .map(User::getUsername).orElse("");
        return AdminOrderResponse.from(
                order,
                buyerName,
                true,
                shipmentService != null ? shipmentService.getShipmentResponse(order) : null
        );
    }
}
