package com.bhavyaprinters.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashfreeOrderRequest
{
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("order_amount")
    private BigDecimal orderAmount;

    @JsonProperty("order_currency")
    private String orderCurrency;

    @JsonProperty("customer_details")
    private CustomerDetails customerDetails;

    @JsonProperty("order_meta")
    private OrderMeta orderMeta;
}
