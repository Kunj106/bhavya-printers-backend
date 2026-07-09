package com.bhavyaprinters.controller;

import com.bhavyaprinters.entity.Order;
import com.bhavyaprinters.repository.OrderRepository;
import com.bhavyaprinters.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class RazorPayWebhookController
{
    private final OrderRepository orderRepository;
    private final RazorpayService razorpayService;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/razorpay")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload,
                                           @RequestHeader("X-Razorpay-Signature") String signature) {

        if (!razorpayService.verifyWebhookSignature(payload, signature, webhookSecret)) {
            return ResponseEntity.status(400).body("Invalid signature");
        }

        JSONObject json = new JSONObject(payload);
        String event = json.getString("event");
        JSONObject paymentEntity = json.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String razorpayOrderId = paymentEntity.optString("order_id", null);
        String razorpayPaymentId = paymentEntity.optString("id", null);

        if (razorpayOrderId == null) {
            return ResponseEntity.ok("ignored");
        }

        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (order == null) {
            return ResponseEntity.ok("order not found, ignored");
        }

        if ("payment.captured".equals(event)) {
            order.setPaymentStatus("Paid");
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setBankCode(paymentEntity.optString("bank", null));
        } else if ("payment.failed".equals(event)) {
            order.setPaymentStatus("Failed");
        }

        orderRepository.save(order);
        return ResponseEntity.ok("processed");
    }
}
