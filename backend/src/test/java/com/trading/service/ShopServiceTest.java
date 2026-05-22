package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.AddressValidationResult;
import com.trading.dto.ShopRequest;
import com.trading.dto.ShopResponse;
import com.trading.entity.Shop;
import com.trading.enums.AddressValidationStatus;
import com.trading.repository.ProductRepository;
import com.trading.repository.ShopFavoriteRepository;
import com.trading.repository.ShopRepository;
import com.trading.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ShopServiceTest {

    @Mock ShopRepository shopRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock AddressValidationService addressValidationService;
    @Mock ShopFavoriteRepository shopFavoriteRepository;

    @InjectMocks ShopService shopService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_missingAddress_throwsBadRequest() {
        ShopRequest req = validRequest();
        req.setDistrict("");

        BusinessException ex = assertThrows(BusinessException.class, () -> shopService.register(1L, req));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("店铺地址不完整"));
        verify(shopRepository, never()).save(any());
    }

    @Test
    void register_success_savesStructuredAddress() {
        ShopRequest req = validRequest();
        when(shopRepository.findBySellerId(1L)).thenReturn(Optional.empty());
        when(shopRepository.existsByName("测试店铺")).thenReturn(false);
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(shopRepository.save(any(Shop.class))).thenAnswer(i -> {
            Shop shop = i.getArgument(0);
            shop.setId(10L);
            return shop;
        });

        ShopResponse response = shopService.register(1L, req);

        assertEquals("浙江省", response.getProvince());
        assertEquals("杭州市", response.getCity());
        assertEquals("西湖区", response.getDistrict());
        assertEquals("文三路100号", response.getDetailAddress());
        assertEquals("浙江省杭州市西湖区文三路100号", response.getFullAddress());
        assertEquals(new BigDecimal("120.1234567"), response.getLongitude());
        assertEquals(new BigDecimal("30.1234567"), response.getLatitude());
        assertEquals("VALID", response.getAddressValidationStatus());
    }

    @Test
    void register_addressValidationFails_doesNotSave() {
        ShopRequest req = validRequest();
        when(shopRepository.findBySellerId(1L)).thenReturn(Optional.empty());
        when(shopRepository.existsByName("测试店铺")).thenReturn(false);
        when(addressValidationService.validateOrThrow(any()))
                .thenThrow(BusinessException.badRequest("地址无法定位，请检查后重新填写"));

        BusinessException ex = assertThrows(BusinessException.class, () -> shopService.register(1L, req));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("地址无法定位"));
        verify(shopRepository, never()).save(any());
    }

    @Test
    void update_success_updatesStructuredAddress() {
        Shop shop = Shop.builder()
                .id(10L)
                .sellerId(1L)
                .name("旧店铺")
                .province("浙江省")
                .city("杭州市")
                .district("西湖区")
                .detailAddress("旧地址")
                .fullAddress("浙江省杭州市西湖区旧地址")
                .build();
        ShopRequest req = validRequest();
        req.setDetailAddress("新地址88号");

        when(shopRepository.findBySellerId(1L)).thenReturn(Optional.of(shop));
        when(shopRepository.existsByNameAndIdNot("测试店铺", 10L)).thenReturn(false);
        when(addressValidationService.validateOrThrow(any())).thenReturn(validAddress());
        when(shopRepository.save(any(Shop.class))).thenAnswer(i -> i.getArgument(0));

        ShopResponse response = shopService.update(1L, req);

        assertEquals("新地址88号", response.getDetailAddress());
        assertEquals("浙江省杭州市西湖区新地址88号", response.getFullAddress());
        assertEquals("VALID", response.getAddressValidationStatus());
    }

    @Test
    void update_addressValidationFails_keepsOriginalAddress() {
        Shop shop = Shop.builder()
                .id(10L)
                .sellerId(1L)
                .name("旧店铺")
                .province("浙江省")
                .city("杭州市")
                .district("西湖区")
                .detailAddress("旧地址")
                .fullAddress("浙江省杭州市西湖区旧地址")
                .longitude(new BigDecimal("120.0000000"))
                .latitude(new BigDecimal("30.0000000"))
                .addressValidationStatus(AddressValidationStatus.VALID)
                .build();
        ShopRequest req = validRequest();

        when(shopRepository.findBySellerId(1L)).thenReturn(Optional.of(shop));
        when(shopRepository.existsByNameAndIdNot("测试店铺", 10L)).thenReturn(false);
        when(addressValidationService.validateOrThrow(any()))
                .thenThrow(BusinessException.badRequest("地址与行政区不匹配，请检查后重新填写"));

        BusinessException ex = assertThrows(BusinessException.class, () -> shopService.update(1L, req));

        assertEquals(400, ex.getStatus());
        assertEquals("旧地址", shop.getDetailAddress());
        assertEquals(new BigDecimal("120.0000000"), shop.getLongitude());
        verify(shopRepository, never()).save(any());
    }

    private ShopRequest validRequest() {
        ShopRequest req = new ShopRequest();
        req.setName("测试店铺");
        req.setDescription("测试简介");
        req.setProvince("浙江省");
        req.setCity("杭州市");
        req.setDistrict("西湖区");
        req.setDetailAddress("文三路100号");
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
}
