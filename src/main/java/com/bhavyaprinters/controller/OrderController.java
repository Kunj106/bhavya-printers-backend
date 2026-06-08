package com.bhavyaprinters.controller;

import com.bhavyaprinters.dto.*;
import com.bhavyaprinters.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private static final Set<String> VALID_STATUSES =
            Set.of("Pending", "Confirmed", "Processing", "Delivered", "Cancelled");

    @GetMapping
    public ResponseEntity<List<OrderDto>> listOrders() {
        return ResponseEntity.ok(orderService.listOrders());
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderInputDto input) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(input));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        return orderService.getOrder(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponseDto("Order not found")));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id,
                                                @Valid @RequestBody OrderStatusUpdateDto body) {
        if (!VALID_STATUSES.contains(body.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("Invalid status"));
        }
        return orderService.updateOrderStatus(id, body.getStatus())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponseDto("Order not found")));
    }

    @GetMapping("/bank/{bankId}")
    public ResponseEntity<List<OrderDto>> getBankOrders(@PathVariable Long bankId) {
        return ResponseEntity.ok(orderService.getOrdersByBank(bankId));
    }
}
