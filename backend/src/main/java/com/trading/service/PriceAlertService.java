package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.PriceAlertResponse;
import com.trading.entity.BuyerNotification;
import com.trading.entity.PriceAlert;
import com.trading.entity.Product;
import com.trading.entity.Shop;
import com.trading.enums.BuyerNotificationType;
import com.trading.enums.PriceAlertStatus;
import com.trading.enums.ProductStatus;
import com.trading.repository.BuyerNotificationRepository;
import com.trading.repository.PriceAlertRepository;
import com.trading.repository.ProductRepository;
import com.trading.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final BuyerNotificationRepository buyerNotificationRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    @Transactional
    public PriceAlert createOrUpdateAlert(Long buyerId, Long productId, BigDecimal targetPrice) {
        Product product = activeProduct(productId);
        if (targetPrice == null || targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.badRequest("目标价必须大于0");
        }
        if (targetPrice.compareTo(product.getPrice()) >= 0) {
            throw BusinessException.badRequest("目标价必须低于当前价格");
        }
        PriceAlert alert = priceAlertRepository.findByBuyerIdAndProductId(buyerId, productId)
                .orElseGet(() -> PriceAlert.builder()
                        .buyerId(buyerId)
                        .productId(productId)
                        .build());
        alert.setTargetPrice(targetPrice);
        alert.setStatus(PriceAlertStatus.ACTIVE);
        alert.setLastNotifiedPrice(null);
        alert.setTriggeredAt(null);
        alert = priceAlertRepository.save(alert);
        alert.setProduct(product);
        return alert;
    }

    @Transactional
    public void cancelAlert(Long buyerId, Long productId) {
        priceAlertRepository.findByBuyerIdAndProductId(buyerId, productId).ifPresent(alert -> {
            alert.setStatus(PriceAlertStatus.CANCELLED);
            priceAlertRepository.save(alert);
        });
    }

    @Transactional(readOnly = true)
    public PriceAlertResponse getAlert(Long buyerId, Long productId) {
        return priceAlertRepository.findByBuyerIdAndProductId(buyerId, productId)
                .map(PriceAlertResponse::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<PriceAlertResponse> listAlerts(Long buyerId, int page, int size) {
        return priceAlertRepository.findByBuyerIdOrderByUpdatedAtDesc(buyerId, PageRequest.of(page, size))
                .map(PriceAlertResponse::from);
    }

    @Transactional
    public int processPriceChange(Long productId, BigDecimal oldPrice, BigDecimal newPrice) {
        if (productId == null || oldPrice == null || newPrice == null || newPrice.compareTo(oldPrice) >= 0) {
            return 0;
        }
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || product.getStatus() != ProductStatus.ACTIVE) {
            return 0;
        }
        Shop shop = shopRepository.findBySellerId(product.getSellerId()).orElse(null);
        List<PriceAlert> alerts = priceAlertRepository.findByProductIdAndStatusAndTargetPriceGreaterThanEqual(
                productId,
                PriceAlertStatus.ACTIVE,
                newPrice
        );
        LocalDateTime now = LocalDateTime.now();
        for (PriceAlert alert : alerts) {
            BuyerNotification notification = BuyerNotification.builder()
                    .buyerId(alert.getBuyerId())
                    .type(BuyerNotificationType.PRICE_DROP)
                    .title("商品降价提醒")
                    .content(product.getName() + " 已降至 ¥" + newPrice.toPlainString())
                    .productId(product.getId())
                    .shopId(shop != null ? shop.getId() : null)
                    .build();
            buyerNotificationRepository.save(notification);

            alert.setStatus(PriceAlertStatus.TRIGGERED);
            alert.setLastNotifiedPrice(newPrice);
            alert.setTriggeredAt(now);
            priceAlertRepository.save(alert);
        }
        return alerts.size();
    }

    private Product activeProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound("商品不存在"));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw BusinessException.notFound("商品已下架，无法设置降价提醒");
        }
        return product;
    }
}
