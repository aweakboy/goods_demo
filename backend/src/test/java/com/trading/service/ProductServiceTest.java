package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.ProductRequest;
import com.trading.entity.Product;
import com.trading.enums.ProductStatus;
import com.trading.repository.CategoryRepository;
import com.trading.repository.ProductRepository;
import com.trading.repository.ShopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ShopRepository shopRepository;
    @Mock PriceAlertService priceAlertService;

    @InjectMocks ProductService productService;

    @Test
    void updateProduct_priceDecrease_triggersPriceAlertsAfterSave() {
        Product product = product(BigDecimal.valueOf(100));
        ProductRequest request = request(BigDecimal.valueOf(80));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        productService.updateProduct(10L, request, 20L);

        verify(priceAlertService).processPriceChange(10L, BigDecimal.valueOf(100), BigDecimal.valueOf(80));
    }

    @Test
    void updateProduct_priceNotDecreased_doesNotTriggerPriceAlerts() {
        Product product = product(BigDecimal.valueOf(100));
        ProductRequest request = request(BigDecimal.valueOf(120));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        productService.updateProduct(10L, request, 20L);

        verify(priceAlertService, never()).processPriceChange(any(), any(), any());
    }

    @Test
    void updateProduct_forbidden_doesNotTriggerPriceAlerts() {
        Product product = product(BigDecimal.valueOf(100));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.updateProduct(10L, request(BigDecimal.valueOf(80)), 99L));

        assertEquals(403, ex.getStatus());
        verify(productRepository, never()).save(any());
        verify(priceAlertService, never()).processPriceChange(any(), any(), any());
    }

    private Product product(BigDecimal price) {
        return Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("商品A")
                .price(price)
                .stock(10)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    private ProductRequest request(BigDecimal price) {
        ProductRequest request = new ProductRequest();
        request.setName("商品A");
        request.setDescription("商品描述");
        request.setPrice(price);
        request.setStock(8);
        request.setStatus("ACTIVE");
        return request;
    }
}
