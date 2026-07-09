package com.bhavyaprinters.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RazorpayService
{
    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient client() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }

    /** Creates a Razorpay order tied to our internal order's total, so Checkout can open against it. */
    public JSONObject createOrder(Long internalOrderId, BigDecimal totalAmount) throws RazorpayException {
        JSONObject request = new JSONObject();
        long amountInPaise = totalAmount.multiply(BigDecimal.valueOf(100)).longValueExact();
        request.put("amount", amountInPaise);
        request.put("currency", "INR");
        request.put("receipt", "order_" + internalOrderId);
        request.put("notes", new JSONObject().put("internalOrderId", String.valueOf(internalOrderId)));

        com.razorpay.Order rpOrder = client().orders.create(request);
        return new JSONObject(rpOrder.toString());
    }

    /** Verifies the signature Razorpay Checkout returns after a successful payment. */
    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    /** Verifies a webhook payload's authenticity against your configured webhook secret. */
    public boolean verifyWebhookSignature(String payload, String signature, String webhookSecret) {
        try {
            return Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    public String getKeyId() {
        return keyId;
    }
}

