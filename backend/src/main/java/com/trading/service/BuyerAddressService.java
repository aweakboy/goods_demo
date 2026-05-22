package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.AddressValidationRequest;
import com.trading.dto.AddressValidationResult;
import com.trading.dto.BuyerAddressRequest;
import com.trading.entity.BuyerAddress;
import com.trading.enums.AddressValidationStatus;
import com.trading.repository.BuyerAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuyerAddressService {

    private static final int MAX_ADDRESS_COUNT = 20;

    private final BuyerAddressRepository buyerAddressRepository;
    private final AddressValidationService addressValidationService;

    public List<BuyerAddress> list(Long buyerId) {
        return buyerAddressRepository.findByBuyerIdOrderByDefaultAddressDescUpdatedAtDescIdDesc(buyerId);
    }

    @Transactional
    public BuyerAddress create(Long buyerId, BuyerAddressRequest request) {
        long count = buyerAddressRepository.countByBuyerId(buyerId);
        if (count >= MAX_ADDRESS_COUNT) {
            throw BusinessException.badRequest("常用地址最多保存20条");
        }

        AddressValidationResult validation = validate(request);
        boolean shouldDefault = count == 0 || Boolean.TRUE.equals(request.getDefaultAddress());
        if (shouldDefault) {
            buyerAddressRepository.clearDefaultByBuyerId(buyerId);
        }

        BuyerAddress address = BuyerAddress.builder()
                .buyerId(buyerId)
                .receiverName(trim(request.getReceiverName()))
                .receiverPhone(trim(request.getReceiverPhone()))
                .province(trim(request.getProvince()))
                .city(trim(request.getCity()))
                .district(trim(request.getDistrict()))
                .detailAddress(trim(request.getDetailAddress()))
                .fullAddress(buildFullAddress(request))
                .longitude(validation.getLongitude())
                .latitude(validation.getLatitude())
                .formattedAddress(validation.getFormattedAddress())
                .validationStatus(AddressValidationStatus.VALID)
                .defaultAddress(shouldDefault)
                .build();
        return buyerAddressRepository.save(address);
    }

    @Transactional
    public BuyerAddress update(Long buyerId, Long addressId, BuyerAddressRequest request) {
        BuyerAddress address = getOwnedAddress(buyerId, addressId);
        AddressValidationResult validation = validate(request);
        if (request.getDefaultAddress() != null) {
            if (Boolean.TRUE.equals(request.getDefaultAddress())) {
                buyerAddressRepository.clearDefaultByBuyerId(buyerId);
                address.setDefaultAddress(true);
            } else {
                address.setDefaultAddress(false);
            }
        }

        address.setReceiverName(trim(request.getReceiverName()));
        address.setReceiverPhone(trim(request.getReceiverPhone()));
        address.setProvince(trim(request.getProvince()));
        address.setCity(trim(request.getCity()));
        address.setDistrict(trim(request.getDistrict()));
        address.setDetailAddress(trim(request.getDetailAddress()));
        address.setFullAddress(buildFullAddress(request));
        address.setLongitude(validation.getLongitude());
        address.setLatitude(validation.getLatitude());
        address.setFormattedAddress(validation.getFormattedAddress());
        address.setValidationStatus(AddressValidationStatus.VALID);
        return buyerAddressRepository.save(address);
    }

    @Transactional
    public void delete(Long buyerId, Long addressId) {
        BuyerAddress address = getOwnedAddress(buyerId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getDefaultAddress());
        buyerAddressRepository.delete(address);
        if (wasDefault) {
            buyerAddressRepository.findFirstByBuyerIdOrderByUpdatedAtDescIdDesc(buyerId)
                    .ifPresent(next -> {
                        next.setDefaultAddress(true);
                        buyerAddressRepository.save(next);
                    });
        }
    }

    @Transactional
    public BuyerAddress setDefault(Long buyerId, Long addressId) {
        BuyerAddress address = getOwnedAddress(buyerId, addressId);
        buyerAddressRepository.clearDefaultByBuyerId(buyerId);
        address.setDefaultAddress(true);
        return buyerAddressRepository.save(address);
    }

    public BuyerAddress getValidAddressForOrder(Long buyerId, Long addressId) {
        BuyerAddress address = getOwnedAddress(buyerId, addressId);
        if (address.getValidationStatus() != AddressValidationStatus.VALID) {
            throw BusinessException.badRequest("地址未通过校验，请先编辑后再使用");
        }
        return address;
    }

    private BuyerAddress getOwnedAddress(Long buyerId, Long addressId) {
        BuyerAddress address = buyerAddressRepository.findById(addressId)
                .orElseThrow(() -> BusinessException.notFound("地址不存在"));
        if (!address.getBuyerId().equals(buyerId)) {
            throw BusinessException.forbidden("无权访问该地址");
        }
        return address;
    }

    private AddressValidationResult validate(BuyerAddressRequest request) {
        return addressValidationService.validateOrThrow(new AddressValidationRequest(
                request.getProvince(),
                request.getCity(),
                request.getDistrict(),
                request.getDetailAddress()
        ));
    }

    private String buildFullAddress(BuyerAddressRequest request) {
        return String.join("",
                trim(request.getProvince()),
                trim(request.getCity()),
                trim(request.getDistrict()),
                trim(request.getDetailAddress()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
