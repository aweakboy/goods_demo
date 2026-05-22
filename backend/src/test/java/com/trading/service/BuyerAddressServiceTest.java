package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.AddressValidationResult;
import com.trading.dto.BuyerAddressRequest;
import com.trading.entity.BuyerAddress;
import com.trading.enums.AddressValidationStatus;
import com.trading.repository.BuyerAddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerAddressServiceTest {

    @Mock BuyerAddressRepository buyerAddressRepository;
    @Mock AddressValidationService addressValidationService;

    @InjectMocks BuyerAddressService buyerAddressService;

    @Test
    void create_savesCoordinatesAndValidStatus() {
        BuyerAddressRequest request = validRequest();
        when(buyerAddressRepository.countByBuyerId(1L)).thenReturn(0L);
        when(addressValidationService.validateOrThrow(any())).thenReturn(validValidation());
        when(buyerAddressRepository.save(any(BuyerAddress.class))).thenAnswer(i -> {
            BuyerAddress address = i.getArgument(0);
            address.setId(10L);
            return address;
        });

        BuyerAddress saved = buyerAddressService.create(1L, request);

        assertEquals(10L, saved.getId());
        assertEquals("张三", saved.getReceiverName());
        assertEquals("浙江省杭州市西湖区文三路100号", saved.getFullAddress());
        assertEquals(new BigDecimal("120.1234567"), saved.getLongitude());
        assertEquals(AddressValidationStatus.VALID, saved.getValidationStatus());
        assertTrue(saved.getDefaultAddress());
        verify(buyerAddressRepository).clearDefaultByBuyerId(1L);
    }

    @Test
    void create_validationFails_doesNotSave() {
        when(buyerAddressRepository.countByBuyerId(1L)).thenReturn(0L);
        when(addressValidationService.validateOrThrow(any()))
                .thenThrow(BusinessException.badRequest("地址无法定位，请检查后重新填写"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> buyerAddressService.create(1L, validRequest()));

        assertEquals(400, ex.getStatus());
        verify(buyerAddressRepository, never()).save(any());
    }

    @Test
    void update_revalidatesAndUpdatesAddressFields() {
        BuyerAddress existing = BuyerAddress.builder()
                .id(5L)
                .buyerId(1L)
                .receiverName("旧姓名")
                .receiverPhone("13800138000")
                .province("浙江省")
                .city("杭州市")
                .district("西湖区")
                .detailAddress("旧地址")
                .fullAddress("浙江省杭州市西湖区旧地址")
                .validationStatus(AddressValidationStatus.VALID)
                .defaultAddress(true)
                .build();
        BuyerAddressRequest request = validRequest();
        request.setReceiverName("王五");
        request.setDetailAddress("文一路200号");
        request.setDefaultAddress(false);

        when(buyerAddressRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(addressValidationService.validateOrThrow(any())).thenReturn(validValidation());
        when(buyerAddressRepository.save(any(BuyerAddress.class))).thenAnswer(i -> i.getArgument(0));

        BuyerAddress updated = buyerAddressService.update(1L, 5L, request);

        assertEquals("王五", updated.getReceiverName());
        assertEquals("文一路200号", updated.getDetailAddress());
        assertEquals("浙江省杭州市西湖区文一路200号", updated.getFullAddress());
        assertEquals(AddressValidationStatus.VALID, updated.getValidationStatus());
        assertFalse(updated.getDefaultAddress());
    }

    @Test
    void setDefault_clearsOtherDefaults() {
        BuyerAddress address = BuyerAddress.builder()
                .id(7L)
                .buyerId(1L)
                .defaultAddress(false)
                .validationStatus(AddressValidationStatus.VALID)
                .build();

        when(buyerAddressRepository.findById(7L)).thenReturn(Optional.of(address));
        when(buyerAddressRepository.save(any(BuyerAddress.class))).thenAnswer(i -> i.getArgument(0));

        BuyerAddress saved = buyerAddressService.setDefault(1L, 7L);

        assertTrue(saved.getDefaultAddress());
        verify(buyerAddressRepository).clearDefaultByBuyerId(1L);
    }

    @Test
    void create_rejectsAddressCountLimit() {
        when(buyerAddressRepository.countByBuyerId(1L)).thenReturn(20L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> buyerAddressService.create(1L, validRequest()));

        assertEquals(400, ex.getStatus());
        verify(addressValidationService, never()).validateOrThrow(any());
        verify(buyerAddressRepository, never()).save(any());
    }

    @Test
    void delete_defaultAddressPromotesLatestRemainingAddress() {
        BuyerAddress deleting = BuyerAddress.builder().id(1L).buyerId(1L).defaultAddress(true).build();
        BuyerAddress next = BuyerAddress.builder().id(2L).buyerId(1L).defaultAddress(false).build();
        when(buyerAddressRepository.findById(1L)).thenReturn(Optional.of(deleting));
        when(buyerAddressRepository.findFirstByBuyerIdOrderByUpdatedAtDescIdDesc(1L)).thenReturn(Optional.of(next));
        when(buyerAddressRepository.save(any(BuyerAddress.class))).thenAnswer(i -> i.getArgument(0));

        buyerAddressService.delete(1L, 1L);

        verify(buyerAddressRepository).delete(deleting);
        assertTrue(next.getDefaultAddress());
    }

    @Test
    void foreignAddressAccess_throwsForbiddenWithoutValidation() {
        BuyerAddress foreign = BuyerAddress.builder().id(9L).buyerId(2L).build();
        when(buyerAddressRepository.findById(9L)).thenReturn(Optional.of(foreign));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> buyerAddressService.update(1L, 9L, validRequest()));

        assertEquals(403, ex.getStatus());
        verify(addressValidationService, never()).validateOrThrow(any());
        verify(buyerAddressRepository, never()).save(any());
    }

    private BuyerAddressRequest validRequest() {
        BuyerAddressRequest request = new BuyerAddressRequest();
        request.setReceiverName("张三");
        request.setReceiverPhone("13800138000");
        request.setProvince("浙江省");
        request.setCity("杭州市");
        request.setDistrict("西湖区");
        request.setDetailAddress("文三路100号");
        return request;
    }

    private AddressValidationResult validValidation() {
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
