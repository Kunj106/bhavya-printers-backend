package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponseDTO
{
    private String paymentSessionId;

    private String cashfreeOrderId;

    private String environment;
}
