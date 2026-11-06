package com.bank.cardenrichment.support;

import com.bank.cardenrichment.api.model.CardTransaction;
import com.bank.cardenrichment.api.model.CardTransactionBatch;

import java.util.List;

/** Builders for the request objects sent to our API in tests. */
public final class CardTransactionFixtures {

    private CardTransactionFixtures() {
    }

    public static CardTransaction tescoPraha() {
        return CardTransaction.builder()
                .transactionId("TX-1")
                .acceptorName("TESCO PRAHA ZLICIN")
                .merchantId("820014")
                .terminalId("18052001")
                .city("PRAHA")
                .country("CZ")
                .mcc("5411")
                .channel(CardTransaction.ChannelEnum.POS)
                .build();
    }

    public static CardTransaction unknownShop() {
        return CardTransaction.builder()
                .transactionId("TX-2")
                .acceptorName("UNKNOWN SHOP")
                .build();
    }

    public static CardTransaction withoutAcceptorName() {
        return CardTransaction.builder().transactionId("TX-1").build();
    }

    public static CardTransactionBatch batchOfTescoAndUnknown() {
        return CardTransactionBatch.builder().transactions(List.of(tescoPraha(), unknownShop())).build();
    }
}
