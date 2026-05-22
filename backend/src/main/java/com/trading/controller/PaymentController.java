package com.trading.controller;

import com.trading.config.AlipayConfig;
import com.trading.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AlipayConfig alipayConfig;

    @PostMapping("/notify")
    public ResponseEntity<String> notify(@RequestParam Map<String, String> params) {
        boolean success = paymentService.handleNotify(params);
        return ResponseEntity.ok(success ? "success" : "failure");
    }

    @GetMapping("/return")
    public ResponseEntity<Void> returnPage(@RequestParam("out_trade_no") String orderId) {
        String path = orderId != null && orderId.startsWith("MEM-")
                ? "/membership?paymentNo=" + orderId
                : "/payment/result?orderId=" + orderId;
        String redirectUrl = alipayConfig.getFrontendUrl() + path;
        return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
    }
}
