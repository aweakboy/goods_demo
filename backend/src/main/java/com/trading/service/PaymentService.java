package com.trading.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.trading.annotation.OperationLog;
import com.trading.common.BusinessException;
import com.trading.config.AlipayConfig;
import com.trading.entity.Order;
import com.trading.enums.OrderStatus;
import com.trading.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final AlipayConfig alipayConfig;
    private final OrderRepository orderRepository;
    private final AlipayClientFactory alipayClientFactory;
    private final AlipaySignatureVerifier alipaySignatureVerifier;
    private final MembershipService membershipService;

    public String createPayForm(Long orderId, BigDecimal totalAmount) {
        return createPayForm(String.valueOf(orderId), totalAmount, "订单" + orderId);
    }

    public String createPayForm(String outTradeNo, BigDecimal totalAmount, String subject) {
        try {
            AlipayClient client = alipayClientFactory.create();
            AlipayTradePagePayRequest req = new AlipayTradePagePayRequest();
            req.setNotifyUrl(alipayConfig.getNotifyUrl());
            req.setReturnUrl(alipayConfig.getReturnUrl());
            req.setBizContent(String.format(
                    "{\"out_trade_no\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
                    "\"total_amount\":\"%s\",\"subject\":\"%s\"}",
                    outTradeNo, totalAmount.toPlainString(), subject
            ));
            return client.pageExecute(req).getBody();
        } catch (AlipayApiException e) {
            throw BusinessException.badRequest("发起支付失败：" + e.getMessage());
        }
    }

    public void refund(String alipayTradeNo, BigDecimal amount, String outRequestNo) {
        try {
            AlipayClient client = alipayClientFactory.create();
            AlipayTradeRefundRequest req = new AlipayTradeRefundRequest();
            req.setBizContent(String.format(
                    "{\"trade_no\":\"%s\",\"refund_amount\":\"%s\",\"out_request_no\":\"%s\"}",
                    alipayTradeNo, amount.toPlainString(), outRequestNo
            ));
            AlipayTradeRefundResponse resp = client.execute(req);
            if (!resp.isSuccess()) {
                throw BusinessException.badRequest("支付宝退款失败：" + resp.getSubMsg());
            }
        } catch (AlipayApiException e) {
            throw BusinessException.badRequest("退款请求异常：" + e.getMessage());
        }
    }

    @OperationLog(module = "支付", action = "支付成功")
    @Transactional
    public boolean handleNotify(Map<String, String> params) {
        try {
            boolean verified = alipaySignatureVerifier.verify(params, alipayConfig.getAlipayPublicKey());
            if (!verified) {
                log.warn("Alipay notify signature verification failed, out_trade_no={}", params.get("out_trade_no"));
                return false;
            }
        } catch (AlipayApiException e) {
            log.warn("Alipay notify signature verification error: {}", e.getMessage());
            return false;
        }

        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.info("Ignoring non-success Alipay notify, out_trade_no={}, trade_status={}",
                    params.get("out_trade_no"), tradeStatus);
            return true;
        }

        try {
            String outTradeNo = params.get("out_trade_no");
            String alipayTradeNo = params.get("trade_no");
            if (outTradeNo != null && outTradeNo.startsWith("MEM-")) {
                membershipService.handlePaymentSuccess(MembershipService.parsePurchaseId(outTradeNo), alipayTradeNo);
                return true;
            }

            Long orderId = Long.parseLong(outTradeNo);
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("Alipay notify references missing order {}", orderId);
                return false;
            }
            return markOrderPaid(order, alipayTradeNo, "notify");
        } catch (RuntimeException e) {
            log.warn("Failed to handle Alipay notify, out_trade_no={}, error={}",
                    params.get("out_trade_no"), e.getMessage());
            return false;
        }
    }

    @Transactional
    public Order reconcileOrderPayment(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (!Objects.equals(order.getBuyerId(), buyerId)) {
            throw BusinessException.forbidden("无权核验该订单支付状态");
        }
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return order;
        }

        AlipayTradeQueryResponse response = queryTrade(orderId);
        if (response == null || !response.isSuccess()) {
            log.warn("Alipay trade query did not confirm order {}, code={}, subMsg={}",
                    orderId,
                    response != null ? response.getCode() : null,
                    response != null ? response.getSubMsg() : null);
            return order;
        }

        String tradeStatus = response.getTradeStatus();
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.info("Alipay trade query returned non-success status for order {}, trade_status={}",
                    orderId, tradeStatus);
            return order;
        }

        verifyTradeMatchesOrder(order, response);
        markOrderPaid(order, response.getTradeNo(), "reconcile");
        return order;
    }

    private AlipayTradeQueryResponse queryTrade(Long orderId) {
        try {
            AlipayTradeQueryRequest req = new AlipayTradeQueryRequest();
            req.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", orderId));
            return alipayClientFactory.create().execute(req);
        } catch (AlipayApiException e) {
            log.warn("Failed to query Alipay trade for order {}: {}", orderId, e.getMessage());
            return null;
        }
    }

    private void verifyTradeMatchesOrder(Order order, AlipayTradeQueryResponse response) {
        String expectedOrderId = String.valueOf(order.getId());
        if (!expectedOrderId.equals(response.getOutTradeNo())) {
            log.warn("Alipay trade order mismatch, local={}, remote={}", expectedOrderId, response.getOutTradeNo());
            throw BusinessException.badRequest("支付订单号不匹配，支付状态无法确认");
        }

        BigDecimal paidAmount;
        try {
            paidAmount = new BigDecimal(response.getTotalAmount());
        } catch (RuntimeException e) {
            log.warn("Alipay trade amount is invalid for order {}, amount={}", order.getId(), response.getTotalAmount());
            throw BusinessException.badRequest("支付金额无法确认");
        }

        if (order.getTotalAmount().compareTo(paidAmount) != 0) {
            log.warn("Alipay trade amount mismatch for order {}, local={}, remote={}",
                    order.getId(), order.getTotalAmount(), paidAmount);
            throw BusinessException.badRequest("支付金额不匹配，支付状态无法确认");
        }
    }

    private boolean markOrderPaid(Order order, String alipayTradeNo, String source) {
        if (order.getStatus() == OrderStatus.PAID) {
            if (order.getAlipayTradeNo() == null && alipayTradeNo != null) {
                order.setAlipayTradeNo(alipayTradeNo);
                orderRepository.save(order);
            }
            return true;
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Refusing to mark order {} as paid from {}, current status={}",
                    order.getId(), source, order.getStatus());
            return false;
        }

        order.setStatus(OrderStatus.PAID);
        order.setAlipayTradeNo(alipayTradeNo);
        orderRepository.save(order);
        return true;
    }
}
