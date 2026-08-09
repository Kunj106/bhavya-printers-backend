package com.bhavyaprinters.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMeta
{

    @JsonProperty("notify_url")
    private String notifyUrl;

    @JsonProperty("return_url")
    private String returnUrl;
}
