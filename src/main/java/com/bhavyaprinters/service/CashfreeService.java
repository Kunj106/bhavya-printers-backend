package com.bhavyaprinters.service;

import com.bhavyaprinters.config.CashfreeConfig;
import com.bhavyaprinters.dto.CashfreeOrderRequest;
import com.bhavyaprinters.dto.CashfreeOrderResponse;
import com.bhavyaprinters.dto.CustomerDetails;
import com.bhavyaprinters.dto.OrderMeta;
import com.bhavyaprinters.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashfreeService
{
    private final WebClient cashfreeWebClient;
    private final CashfreeConfig cashfreeConfig;

    /**
     * Creates a Cashfree Order and returns the payment session.
     */
    public CashfreeOrderResponse createOrder(Order order) {

        log.info("Creating Cashfree Order for Internal Order ID: {}", order.getId());

        // Clean phone number
        String phone = order.getMobile().replaceAll("\\D", "");

        // Remove leading country code (91) if present
        if (phone.startsWith("91") && phone.length() == 12) {
            phone = phone.substring(2);
        }

        CustomerDetails customerDetails = new CustomerDetails(
                String.valueOf(order.getBankId()),
                order.getBankName(),
                order.getEmail(),
                phone
        );

        OrderMeta orderMeta = new OrderMeta(
                cashfreeConfig.getNotifyUrl(),
                cashfreeConfig.getReturnUrl()
        );

        CashfreeOrderRequest request = new CashfreeOrderRequest(
                "ORDER_" + order.getId(),
                order.getTotal(),
                "INR",
                customerDetails,
                orderMeta
        );

        CashfreeOrderResponse response = cashfreeWebClient
                .post()
                .uri("/orders")
                .header("x-client-id", cashfreeConfig.getClientId())
                .header("x-client-secret", cashfreeConfig.getClientSecret())
                .header("x-api-version", "2023-08-01")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        clientResponse ->
                                clientResponse.bodyToMono(String.class)
                                        .flatMap(error -> {
                                            log.error("Cashfree Error Response: {}", error);
                                            return Mono.error(new RuntimeException(error));
                                        })
                )
                .bodyToMono(CashfreeOrderResponse.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Cashfree returned an empty response.");
        }

        log.info("Cashfree Order Created Successfully");
        log.info("Cashfree Order ID: {}", response.getOrderId());
        log.info("Payment Session ID: {}", response.getPaymentSessionId());

        return response;
    }

    /**
     * Returns the configured Cashfree environment.
     */
    public String getEnvironment() {
        return cashfreeConfig.getEnvironment();
    }
}
