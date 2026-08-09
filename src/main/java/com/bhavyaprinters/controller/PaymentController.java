package com.bhavyaprinters.controller;

import com.bhavyaprinters.dto.*;
import com.bhavyaprinters.entity.Order;
import com.bhavyaprinters.repository.OrderRepository;
import com.bhavyaprinters.service.CashfreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController
{
    private final OrderRepository orderRepository;
    private final CashfreeService cashfreeService;

    @PostMapping("/create/{orderId}")
    public ResponseEntity<?> createPayment(@PathVariable Long orderId) {

        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("Order not found"));
        }

        if ("Paid".equalsIgnoreCase(order.getPaymentStatus())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDto("Order is already paid"));
        }

        try {

            CashfreeOrderResponse response =
                    cashfreeService.createOrder(order);

            order.setCashfreeOrderId(response.getOrderId());

            orderRepository.save(order);

            return ResponseEntity.ok(
                    new CreatePaymentOrderResponseDto(
                            response.getPaymentSessionId(),
                            response.getOrderId(),
                            cashfreeService.getEnvironment()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto(e.getMessage()));

        }
    }

    @PutMapping("/{orderId}/mark-paid")
    public ResponseEntity<?> markPaid(@PathVariable Long orderId) {

        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("Order not found"));
        }

        order.setPaymentStatus("Paid");

        orderRepository.save(order);

        return ResponseEntity.ok(
                new MessageResponseDto("Payment marked as paid.")
        );
    }

}
