package com.bhavyaprinters.controller;

import com.bhavyaprinters.dto.CreatePaymentOrderResponseDto;
import com.bhavyaprinters.dto.ErrorResponseDto;
import com.bhavyaprinters.dto.MessageResponseDto;
import com.bhavyaprinters.dto.VerifyPaymentRequestDto;
import com.bhavyaprinters.entity.Order;
import com.bhavyaprinters.repository.OrderRepository;
import com.bhavyaprinters.service.RazorpayService;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController
{
    private final OrderRepository orderRepository;
    private final RazorpayService razorpayService;

    /** Step 1: create a Razorpay order tied to an existing internal order. */
    @PostMapping("/create/{orderId}")
    public ResponseEntity<?> createPaymentOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto("Order not found"));
        }
        if ("Paid".equals(order.getPaymentStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDto("Order already paid"));
        }
        try {
            JSONObject rpOrder = razorpayService.createOrder(order.getId(), order.getTotal());
            order.setRazorpayOrderId(rpOrder.getString("id"));
            orderRepository.save(order);

            return ResponseEntity.ok(new CreatePaymentOrderResponseDto(
                    rpOrder.getString("id"),
                    rpOrder.getLong("amount"),
                    rpOrder.getString("currency"),
                    razorpayService.getKeyId()
            ));
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Failed to initiate payment: " + e.getMessage()));
        }
    }

    /** Step 2: frontend calls this after Checkout reports success, to verify + finalize. */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@Valid @RequestBody VerifyPaymentRequestDto body) {
        Order order = orderRepository.findById(body.getOrderId()).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto("Order not found"));
        }

        boolean valid = razorpayService.verifySignature(
                body.getRazorpayOrderId(), body.getRazorpayPaymentId(), body.getRazorpaySignature());

        if (!valid) {
            order.setPaymentStatus("Failed");
            orderRepository.save(order);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDto("Payment verification failed"));
        }

        order.setPaymentStatus("Paid");
        order.setRazorpayPaymentId(body.getRazorpayPaymentId());
        orderRepository.save(order);

        return ResponseEntity.ok(new MessageResponseDto("Payment verified successfully"));
    }

    /** Manual override for admin — e.g. marking a Pay-after-Delivery (COD) order as paid once collected. */
    @PutMapping("/{orderId}/mark-paid")
    public ResponseEntity<?> markPaidManually(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto("Order not found"));
        }
        order.setPaymentStatus("Paid");
        orderRepository.save(order);
        return ResponseEntity.ok(new MessageResponseDto("Marked as paid"));
    }
}
