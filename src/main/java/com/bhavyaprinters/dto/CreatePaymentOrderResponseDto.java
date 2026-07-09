package com.bhavyaprinters.dto;

public record CreatePaymentOrderResponseDto(

    String razorpayOrderId,
    long amount,
    String currency,
    String keyId
) {
}
