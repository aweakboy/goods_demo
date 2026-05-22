package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.ProductRequest;
import com.trading.dto.ProductWithShopResponse;
import com.trading.entity.*;
import com.trading.enums.ProductStatus;
import com.trading.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final PriceAlertService priceAlertService;

    public Page<ProductWithShopResponse> searchProducts(Long categoryId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Product> products = productRepository.searchActive(categoryId, kw, pageable);
        return enrichWithShop(products);
    }

    public ProductWithShopResponse getProduct(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("商品不存在"));
        if (p.getStatus() == ProductStatus.INACTIVE) {
            throw BusinessException.notFound("商品已下架");
        }
        Shop shop = shopRepository.findBySellerId(p.getSellerId()).orElse(null);
        Long shopId = shop != null ? shop.getId() : null;
        String shopName = shop != null ? shop.getName() : null;
        return ProductWithShopResponse.from(p, shopId, shopName);
    }

    private Page<ProductWithShopResponse> enrichWithShop(Page<Product> products) {
        List<Long> sellerIds = products.getContent().stream().map(Product::getSellerId).distinct().toList();
        Map<Long, Shop> shopBySellerMap = new HashMap<>();
        for (Long sellerId : sellerIds) {
            shopRepository.findBySellerId(sellerId).ifPresent(s -> shopBySellerMap.put(sellerId, s));
        }
        return products.map(p -> {
            Shop s = shopBySellerMap.get(p.getSellerId());
            return ProductWithShopResponse.from(p, s != null ? s.getId() : null, s != null ? s.getName() : null);
        });
    }

    public List<Category> getActiveCategories() {
        return categoryRepository.findByStatus(ProductStatus.ACTIVE);
    }

    public Product createProduct(ProductRequest req, Long sellerId) {
        shopRepository.findBySellerId(sellerId)
                .filter(s -> s.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.badRequest("请先注册店铺"));
        Product p = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stock(req.getStock())
                .imageUrl(req.getImageUrl())
                .categoryId(req.getCategoryId())
                .sellerId(sellerId)
                .status(ProductStatus.ACTIVE)
                .build();
        return productRepository.save(p);
    }

    @Transactional
    public Product updateProduct(Long productId, ProductRequest req, Long sellerId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound("商品不存在"));
        if (!p.getSellerId().equals(sellerId)) {
            throw BusinessException.forbidden("无权修改他人商品");
        }
        BigDecimal oldPrice = p.getPrice();
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setStock(req.getStock());
        p.setImageUrl(req.getImageUrl());
        p.setCategoryId(req.getCategoryId());
        if (req.getStatus() != null) {
            p.setStatus(ProductStatus.valueOf(req.getStatus()));
        }
        Product saved = productRepository.save(p);
        if (oldPrice != null && req.getPrice() != null && req.getPrice().compareTo(oldPrice) < 0) {
            priceAlertService.processPriceChange(saved.getId(), oldPrice, saved.getPrice());
        }
        return saved;
    }

    public List<Product> getSellerProducts(Long sellerId) {
        return productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }
}
