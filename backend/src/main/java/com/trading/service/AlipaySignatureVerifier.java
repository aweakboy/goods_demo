package com.trading.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AlipaySignatureVerifier {

    public boolean verify(Map<String, String> params, String publicKey) throws AlipayApiException {
        return AlipaySignature.rsaCheckV1(params, publicKey, "UTF-8", "RSA2");
    }
}
