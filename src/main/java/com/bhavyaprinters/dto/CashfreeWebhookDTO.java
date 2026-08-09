package com.bhavyaprinters.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CashfreeWebhookDTO
{
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("cf_payment_id")
    private String cfPaymentId;
}
