package com.bank.cardenrichment.support;

import java.util.Map;

/**
 * Request bodies sent to our API, using the Tapix parameter names.
 * Plain maps on purpose: tests do not depend on the generated DTOs.
 */
public final class CardTransactionFixtures {

    private CardTransactionFixtures() {
    }

    public static Map<String, Object> tescoPraha() {
        return Map.of(
                "customId", "TX-1",
                "description", "TESCO PRAHA ZLICIN",
                "merchantId", "820014",
                "posId", "18052001",
                "city", "PRAHA",
                "country", "CZ",
                "mcc", "5411",
                "channelCode", "POS");
    }

    public static Map<String, Object> withoutDescription() {
        return Map.of("customId", "TX-1");
    }
}
