package com.bhavyaprinters.service;

import com.bhavyaprinters.dto.OrderDto;
import com.bhavyaprinters.dto.OrderInputDto;
import com.bhavyaprinters.dto.OrderItemDto;
import com.bhavyaprinters.dto.OrderItemInputDto;
import com.bhavyaprinters.entity.Order;
import com.bhavyaprinters.entity.Product;
import com.bhavyaprinters.repository.OrderRepository;
import com.bhavyaprinters.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<OrderDto> listOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Optional<OrderDto> getOrder(Long id) {
        return orderRepository.findById(id).map(this::toDto);
    }

    public List<OrderDto> getOrdersByBank(Long bankId) {
        return orderRepository.findByBankIdOrderByCreatedAtDesc(bankId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public OrderDto createOrder(OrderInputDto input) {
        List<OrderItemDto> enrichedItems = new ArrayList<>();
        double subtotal = 0;

        for (OrderItemInputDto item : input.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product " + item.getProductId() + " not found"));
            double unitPrice = product.getPrice().doubleValue();
            double totalPrice = unitPrice * item.getQuantity();
            subtotal += totalPrice;
            enrichedItems.add(new OrderItemDto(product.getId(), product.getName(),
                    item.getQuantity(), unitPrice, totalPrice));
        }

        double gstRate = input.getGstRate();
        double gstAmount = BigDecimal.valueOf(subtotal * gstRate / 100)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
        double total = BigDecimal.valueOf(subtotal + gstAmount)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(enrichedItems);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize items", e);
        }

        Order order = new Order();
        order.setBankId(input.getBankId());
        order.setBankName(input.getBankName());
        order.setBranchName(input.getBranchName());
        order.setGstNo(input.getGstNo());
        order.setPanNo(input.getPanNo());
        order.setAddress(input.getAddress());
        order.setMobile(input.getMobile());
        order.setEmail(input.getEmail());
        order.setItems(itemsJson);
        order.setSubtotal(BigDecimal.valueOf(subtotal));
        order.setGstRate(BigDecimal.valueOf(gstRate));
        order.setGstAmount(BigDecimal.valueOf(gstAmount));
        order.setTotal(BigDecimal.valueOf(total));
        order.setPaymentMethod(input.getPaymentMethod());
        order.setUpiId(input.getUpiId());
        order.setStatus("Pending");

        // COD orders are settled on delivery; gateway orders (UPI/Netbanking)
        // start Pending and flip to Paid only once Razorpay confirms the payment.
        order.setPaymentStatus("Pending");

        OrderDto saved = toDto(orderRepository.save(order));

        // Send SMS + WhatsApp confirmation to the bank
        notificationService.notifyOrderPlaced(
                saved.getMobile(),
                saved.getEmail(),
                saved.getBankName(),
                saved.getBranchName(),
                saved.getId(),
                saved.getTotal()
        );

        return saved;
    }

    @Transactional
    public Optional<OrderDto> updateOrderStatus(Long id, String status) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(status);
            OrderDto updated = toDto(orderRepository.save(order));

            // Notify the bank of the status change
            notificationService.notifyOrderStatusUpdate(
                    updated.getMobile(),
                    updated.getBankName(),
                    updated.getId(),
                    status
            );

            return updated;
        });
    }

    @Transactional
    public boolean deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            return false;
        }

        orderRepository.deleteById(id);
        return true;
    }

    public OrderDto toDto(Order o) {
        List<OrderItemDto> items;
        try {
            items = objectMapper.readValue(o.getItems(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            items = List.of();
        }

        return new OrderDto(
                o.getId(),
                o.getBankId(),
                o.getBankName(),
                o.getBranchName(),
                o.getGstNo(),
                o.getPanNo(),
                o.getAddress(),
                o.getMobile(),
                o.getEmail(),
                items,
                o.getSubtotal().doubleValue(),
                o.getGstRate().doubleValue(),
                o.getGstAmount().doubleValue(),
                o.getTotal().doubleValue(),
                o.getPaymentMethod(),
                o.getUpiId(),
                o.getStatus(),
                o.getPaymentStatus(),
                o.getBankCode(),
                o.getCreatedAt().toString()
        );
    }
}
