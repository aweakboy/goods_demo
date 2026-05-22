package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.ShopFavoriteResponse;
import com.trading.dto.ShopFavoriteStateResponse;
import com.trading.entity.Shop;
import com.trading.entity.ShopFavorite;
import com.trading.enums.ProductStatus;
import com.trading.repository.ShopFavoriteRepository;
import com.trading.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShopFavoriteService {

    private final ShopFavoriteRepository shopFavoriteRepository;
    private final ShopRepository shopRepository;

    @Transactional
    public ShopFavoriteStateResponse favoriteShop(Long buyerId, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> BusinessException.notFound("店铺不存在"));
        if (shop.getStatus() != ProductStatus.ACTIVE) {
            throw BusinessException.notFound("店铺已关闭，无法收藏");
        }
        shopFavoriteRepository.findByBuyerIdAndShopId(buyerId, shopId)
                .orElseGet(() -> shopFavoriteRepository.save(ShopFavorite.builder()
                        .buyerId(buyerId)
                        .shopId(shopId)
                        .build()));
        return state(shopId, true);
    }

    @Transactional
    public ShopFavoriteStateResponse unfavoriteShop(Long buyerId, Long shopId) {
        shopFavoriteRepository.findByBuyerIdAndShopId(buyerId, shopId)
                .ifPresent(shopFavoriteRepository::delete);
        return state(shopId, false);
    }

    @Transactional(readOnly = true)
    public Page<ShopFavoriteResponse> listFavorites(Long buyerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return shopFavoriteRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable)
                .map(ShopFavoriteResponse::from);
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Long buyerId, Long shopId) {
        return buyerId != null && shopFavoriteRepository.existsByBuyerIdAndShopId(buyerId, shopId);
    }

    @Transactional(readOnly = true)
    public long countFavorites(Long shopId) {
        return shopFavoriteRepository.countByShopId(shopId);
    }

    private ShopFavoriteStateResponse state(Long shopId, boolean favorited) {
        return ShopFavoriteStateResponse.builder()
                .shopId(shopId)
                .favorited(favorited)
                .favoriteCount(shopFavoriteRepository.countByShopId(shopId))
                .build();
    }
}
