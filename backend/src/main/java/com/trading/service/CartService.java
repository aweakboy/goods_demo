package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.CartItemRequest;
import com.trading.entity.*;
import com.trading.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public List<CartItem> getCart(Long buyerId) {
        return cartItemRepository.findByBuyerId(buyerId);
    }

    @Transactional
    public CartItem addItem(Long buyerId, CartItemRequest req) {
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> BusinessException.notFound("商品不存在"));

        var existing = cartItemRepository.findByBuyerIdAndProductId(buyerId, req.getProductId());
        int newQty = existing.map(CartItem::getQuantity).orElse(0) + req.getQuantity();

        if (newQty > product.getStock()) {
            throw BusinessException.badRequest("库存不足，当前库存：" + product.getStock());
        }

        CartItem item = existing.orElse(CartItem.builder()
                .buyerId(buyerId)
                .productId(req.getProductId())
                .build());
        item.setQuantity(newQty);
        return cartItemRepository.save(item);
    }

    @Transactional
    public CartItem updateItem(Long buyerId, Long itemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> BusinessException.notFound("购物车项不存在"));
        if (!item.getBuyerId().equals(buyerId)) {
            throw BusinessException.forbidden("无权操作");
        }
        if (quantity == 0) {
            cartItemRepository.delete(item);
            return null;
        }
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> BusinessException.notFound("商品不存在"));
        if (quantity > product.getStock()) {
            throw BusinessException.badRequest("库存不足");
        }
        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    @Transactional
    public void removeItem(Long buyerId, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> BusinessException.notFound("购物车项不存在"));
        if (!item.getBuyerId().equals(buyerId)) {
            throw BusinessException.forbidden("无权操作");
        }
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long buyerId) {
        cartItemRepository.deleteByBuyerId(buyerId);
    }
}
