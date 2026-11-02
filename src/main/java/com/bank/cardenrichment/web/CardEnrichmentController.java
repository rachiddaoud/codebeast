package com.bank.cardenrichment.web;

import com.bank.cardenrichment.api.CardEnrichmentApi;
import com.bank.cardenrichment.api.model.CardTransaction;
import com.bank.cardenrichment.api.model.CardTransactionBatch;
import com.bank.cardenrichment.api.model.EnrichedCardTransaction;
import com.bank.cardenrichment.api.model.EnrichedCardTransactionBatch;
import com.bank.cardenrichment.service.CardEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CardEnrichmentController implements CardEnrichmentApi {

    private final CardEnrichmentService service;

    @Override
    @PreAuthorize("hasAuthority('SCOPE_card-enrichment:read')")
    public ResponseEntity<EnrichedCardTransaction> enrichCardTransaction(CardTransaction cardTransaction) {
        return ResponseEntity.ok(service.enrich(cardTransaction));
    }

    @Override
    @PreAuthorize("hasAuthority('SCOPE_card-enrichment:read')")
    public ResponseEntity<EnrichedCardTransactionBatch> enrichCardTransactions(CardTransactionBatch batch) {
        return ResponseEntity.ok(new EnrichedCardTransactionBatch(service.enrich(batch.getTransactions())));
    }
}
