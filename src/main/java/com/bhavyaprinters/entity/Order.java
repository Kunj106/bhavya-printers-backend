package com.bhavyaprinters.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "gst_no", nullable = false)
    private String gstNo;

    @Column(name = "pan_no", nullable = false)
    private String panNo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private String mobile;

    @Column(nullable = false)
    private String email;

    /**
     * JSON array of order items stored as text.
     * Format: [{"productId":1,"productName":"...","quantity":2,"unitPrice":100.0,"totalPrice":200.0}]
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String items;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "gst_rate", nullable = false, precision = 4, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "gst_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal gstAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "upi_id")
    private String upiId;

    @Column(nullable = false)
    private String status = "Pending";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "Pending"; // Pending, Paid, Failed

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "bank_code")
    private String bankCode; // populated for netbanking payments, e.g. SBIN, BARB0

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "Pending";
        }
        if (paymentStatus == null) {
            paymentStatus = "Pending";
        }
    }
}
