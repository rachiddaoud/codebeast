package com.bank.cardenrichment.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

/** One CSV line per card operation. The CSV columns follow the order below. */
@JsonPropertyOrder({
        "transactionId", "cardNumber", "acceptorName", "merchantId", "terminalId",
        "city", "country", "zip", "mcc", "transactionTimestamp", "amount", "currency"})
public record OperationCsvRow(
        String transactionId,
        String cardNumber,
        String acceptorName,
        String merchantId,
        String terminalId,
        String city,
        String country,
        String zip,
        String mcc,
        String transactionTimestamp,
        BigDecimal amount,
        String currency) {
}
