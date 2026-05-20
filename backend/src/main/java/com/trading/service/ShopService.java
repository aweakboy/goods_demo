package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.*;
import com.trading.entity.*;
import com.trading.enums.ProductStatus;
import com.trading.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // --- Seller: manage own shop ---

    public ShopResponse getMyShop(Long sellerId) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> BusinessException.notFound("尚未注册店铺"));
        return ShopResponse.from(shop);
    }

    @Transactional
    public ShopResponse register(Long sellerId, ShopRequest req) {
        if (shopRepository.findBySellerId(sellerId).isPresent()) {
            throw BusinessException.badRequest("您已注册店铺");
        }
        if (shopRepository.existsByName(req.getName())) {
            throw BusinessException.badRequest("店铺名称已存在");
        }
        Shop shop = Shop.builder()
                .sellerId(sellerId)
                .name(req.getName())
                .description(req.getDescription())
                .status(ProductStatus.ACTIVE)
                .build();
        return ShopResponse.from(shopRepository.save(shop));
    }

    @Transactional
    public ShopResponse update(Long sellerId, ShopRequest req) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> BusinessException.notFound("尚未注册店铺"));
        if (shopRepository.existsByNameAndIdNot(req.getName(), shop.getId())) {
            throw BusinessException.badRequest("店铺名称已存在");
        }
        shop.setName(req.getName());
        if (req.getDescription() != null) {
            shop.setDescription(req.getDescription());
        }
        return ShopResponse.from(shopRepository.save(shop));
    }

    // --- Public: storefront ---

    public ShopStorefrontResponse getStorefront(Long shopId, int page, int size) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> BusinessException.notFound("店铺不存在"));
        if (shop.getStatus() == ProductStatus.INACTIVE) {
            throw BusinessException.notFound("店铺已关闭");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findBySellerIdAndStatus(shop.getSellerId(), ProductStatus.ACTIVE, pageable);
        Page<ProductWithShopResponse> productPage = products.map(p -> ProductWithShopResponse.from(p, shop.getId(), shop.getName()));

        ShopStorefrontResponse resp = new ShopStorefrontResponse();
        resp.setId(shop.getId());
        resp.setName(shop.getName());
        resp.setDescription(shop.getDescription());
        resp.setStatus(shop.getStatus().name());
        resp.setCreatedAt(shop.getCreatedAt());
        resp.setProducts(productPage);
        return resp;
    }

    public List<ShopResponse> searchShops(String name) {
        return shopRepository.searchActive(name).stream()
                .map(ShopResponse::from)
                .collect(Collectors.toList());
    }

    // --- Admin ---

    public Page<AdminShopResponse> getShopsForAdmin(String status, int page, int size) {
        ProductStatus statusEnum = status != null ? ProductStatus.valueOf(status) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<Shop> shops = shopRepository.findAllByFilter(statusEnum, pageable);

        List<Long> sellerIds = shops.getContent().stream().map(Shop::getSellerId).distinct().collect(Collectors.toList());
        Map<Long, String> sellerNames = userRepository.findAllById(sellerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return shops.map(s -> AdminShopResponse.from(
                s,
                sellerNames.getOrDefault(s.getSellerId(), ""),
                productRepository.countBySellerId(s.getSellerId())
        ));
    }

    @Transactional
    public AdminShopResponse updateShopStatus(Long shopId, String newStatus) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> BusinessException.notFound("店铺不存在"));
        shop.setStatus(ProductStatus.valueOf(newStatus));
        shopRepository.save(shop);
        long count = productRepository.countBySellerId(shop.getSellerId());
        return AdminShopResponse.from(shop, "", count);
    }

    // --- Utility for product creation check ---

    public Shop requireActiveShop(Long sellerId) {
        return shopRepository.findBySellerId(sellerId)
                .filter(s -> s.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.badRequest("请先注册店铺"));
    }
}
