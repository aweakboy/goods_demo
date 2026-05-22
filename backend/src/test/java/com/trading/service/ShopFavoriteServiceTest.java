package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.ShopFavoriteResponse;
import com.trading.dto.ShopFavoriteStateResponse;
import com.trading.entity.Shop;
import com.trading.entity.ShopFavorite;
import com.trading.enums.ProductStatus;
import com.trading.repository.ShopFavoriteRepository;
import com.trading.repository.ShopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopFavoriteServiceTest {

    @Mock ShopFavoriteRepository shopFavoriteRepository;
    @Mock ShopRepository shopRepository;

    @InjectMocks ShopFavoriteService shopFavoriteService;

    @Test
    void favoriteShop_activeShop_createsFavorite() {
        when(shopRepository.findById(10L)).thenReturn(Optional.of(activeShop()));
        when(shopFavoriteRepository.findByBuyerIdAndShopId(2L, 10L)).thenReturn(Optional.empty());
        when(shopFavoriteRepository.save(any(ShopFavorite.class))).thenAnswer(i -> i.getArgument(0));
        when(shopFavoriteRepository.countByShopId(10L)).thenReturn(1L);

        ShopFavoriteStateResponse response = shopFavoriteService.favoriteShop(2L, 10L);

        assertTrue(response.getFavorited());
        assertEquals(1L, response.getFavoriteCount());
        verify(shopFavoriteRepository).save(any(ShopFavorite.class));
    }

    @Test
    void favoriteShop_duplicateDoesNotCreateAgain() {
        when(shopRepository.findById(10L)).thenReturn(Optional.of(activeShop()));
        when(shopFavoriteRepository.findByBuyerIdAndShopId(2L, 10L))
                .thenReturn(Optional.of(favorite(2L, 10L)));
        when(shopFavoriteRepository.countByShopId(10L)).thenReturn(1L);

        ShopFavoriteStateResponse response = shopFavoriteService.favoriteShop(2L, 10L);

        assertTrue(response.getFavorited());
        verify(shopFavoriteRepository, never()).save(any());
    }

    @Test
    void unfavoriteShop_isIdempotent() {
        when(shopFavoriteRepository.findByBuyerIdAndShopId(2L, 10L))
                .thenReturn(Optional.of(favorite(2L, 10L)), Optional.empty());
        when(shopFavoriteRepository.countByShopId(10L)).thenReturn(0L);

        ShopFavoriteStateResponse first = shopFavoriteService.unfavoriteShop(2L, 10L);
        ShopFavoriteStateResponse second = shopFavoriteService.unfavoriteShop(2L, 10L);

        assertFalse(first.getFavorited());
        assertFalse(second.getFavorited());
        verify(shopFavoriteRepository).delete(any(ShopFavorite.class));
    }

    @Test
    void favoriteShop_missingOrInactiveShop_throws() {
        when(shopRepository.findById(10L)).thenReturn(Optional.empty());
        BusinessException missing = assertThrows(BusinessException.class,
                () -> shopFavoriteService.favoriteShop(2L, 10L));
        assertEquals(404, missing.getStatus());

        Shop inactive = activeShop();
        inactive.setStatus(ProductStatus.INACTIVE);
        when(shopRepository.findById(10L)).thenReturn(Optional.of(inactive));
        BusinessException inactiveEx = assertThrows(BusinessException.class,
                () -> shopFavoriteService.favoriteShop(2L, 10L));
        assertEquals(404, inactiveEx.getStatus());
        verify(shopFavoriteRepository, never()).save(any());
    }

    @Test
    void listFavorites_returnsCurrentBuyerFavoritesOnly() {
        ShopFavorite favorite = favorite(2L, 10L);
        favorite.setShop(activeShop());
        when(shopFavoriteRepository.findByBuyerIdOrderByCreatedAtDesc(eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of(favorite)));

        Page<ShopFavoriteResponse> result = shopFavoriteService.listFavorites(2L, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).getShopId());
        verify(shopFavoriteRepository).findByBuyerIdOrderByCreatedAtDesc(eq(2L), any());
    }

    private Shop activeShop() {
        return Shop.builder()
                .id(10L)
                .sellerId(20L)
                .name("测试店铺")
                .status(ProductStatus.ACTIVE)
                .build();
    }

    private ShopFavorite favorite(Long buyerId, Long shopId) {
        return ShopFavorite.builder()
                .id(1L)
                .buyerId(buyerId)
                .shopId(shopId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
