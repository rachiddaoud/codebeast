package com.bank.cardenrichment.service;

import com.bank.cardenrichment.api.model.CardTransaction;
import com.bank.cardenrichment.api.model.EnrichedCardTransaction;
import com.bank.cardenrichment.tapix.api.ShopsApi;
import com.bank.cardenrichment.tapix.model.FindResultComplete;
import com.bank.cardenrichment.tapix.model.RequestTX;
import com.bank.cardenrichment.tapix.model.RequestTXBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Calls Tapix for every transaction. Tapix forbids caching these calls, so there is no cache here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardEnrichmentService {

    private final ShopsApi tapix;
    private final TapixMapper mapper;

    public EnrichedCardTransaction enrich(CardTransaction tx) {
        RequestTX req = mapper.toTapixRequest(tx);
        FindResultComplete result = tapix.findByCardTransactionComplete(
                req.getDescription(), req.getMerchantId(), req.getPosId(), req.getCity(), req.getCountry(),
                req.getMcc(), req.getTransactionTimestamp(), req.getSourceId(), req.getClientId(),
                req.getCurrency(), req.getAmount(), req.getRefresh(), req.getEcommerceFlag(),
                req.getChannelCode(), req.getZip()).getBody();
        if (result == null) {
            throw new TapixException("Empty response from Tapix");
        }
        return mapper.toEnriched(tx.getTransactionId(), result);
    }

    public List<EnrichedCardTransaction> enrich(List<CardTransaction> txs) {
        RequestTXBatch batch = RequestTXBatch.builder()
                .requests(txs.stream().map(mapper::toTapixRequest).toList())
                .build();
        List<FindResultComplete> results = tapix.findByCardTransactionBatchComplete(batch).getBody();
        if (results == null || results.size() != txs.size()) {
            throw new TapixException("Tapix returned %s results for %d transactions"
                    .formatted(results == null ? "no" : results.size(), txs.size()));
        }
        log.debug("Tapix enriched {} transactions", txs.size());
        // Tapix keeps the request order
        return IntStream.range(0, txs.size())
                .mapToObj(i -> mapper.toEnriched(txs.get(i).getTransactionId(), results.get(i)))
                .toList();
    }
}
