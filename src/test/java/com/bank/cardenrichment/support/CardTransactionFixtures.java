package com.bank.cardenrichment.support;

import com.bank.cardenrichment.api.model.CardTransaction;

/** Request DTOs sent to our API, using the Tapix parameter names. */
public final class CardTransactionFixtures {

    private CardTransactionFixtures() {
    }

    public static CardTransaction tescoPraha() {
        CardTransaction tx = new CardTransaction();
        tx.setCustomId("TX-1");
        tx.setDescription("TESCO PRAHA ZLICIN");
        tx.setMerchantId("820014");
        tx.setPosId("18052001");
        tx.setCity("PRAHA");
        tx.setCountry("CZ");
        tx.setMcc("5411");
        tx.setChannelCode("POS");
        return tx;
    }

    public static CardTransaction withoutDescription() {
        CardTransaction tx = new CardTransaction();
        tx.setCustomId("TX-1");
        return tx;
    }
}
