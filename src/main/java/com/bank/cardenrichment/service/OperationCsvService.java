package com.bank.cardenrichment.service;

import com.bank.cardenrichment.dto.OperationCsvRow;
import com.bank.cardenrichment.mapper.OperationCsvMapper;
import com.bank.cardenrichment.statement.Bank;
import com.bank.cardenrichment.statement.Card;
import com.bank.cardenrichment.statement.Contract;
import lombok.RequiredArgsConstructor;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Service;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.CsvWriteFeature;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.stream.Stream;

/** Exports the card operations of a statement as CSV, one line per operation. */
@Service
@RequiredArgsConstructor
public class OperationCsvService {

    // Strict check: quote only values that need it (comma, quote, line break)
    private static final CsvMapper CSV = CsvMapper.builder().enable(CsvWriteFeature.STRICT_CHECK_FOR_QUOTING).build();
    private static final CsvSchema SCHEMA = CSV.schemaFor(OperationCsvRow.class).withHeader();

    private final Jaxb2Marshaller statementMarshaller;
    private final OperationCsvMapper mapper;

    public void export(InputStream xml, Writer csv) {
        Bank bank = (Bank) statementMarshaller.unmarshal(new StreamSource(xml));
        List<OperationCsvRow> rows = bank.getContract().stream()
                .flatMap(OperationCsvService::cards)
                .flatMap(card -> card.getOperation().stream())
                .map(mapper::toCsvRow)
                .toList();
        CSV.writer(SCHEMA).writeValue(csv, rows);
    }

    /** A card sits under a cycle, a prepaid or the contract itself. */
    private static Stream<Card> cards(Contract contract) {
        return Stream.of(
                contract.getCycle().stream().flatMap(cycle -> cycle.getCard().stream()),
                contract.getPrepaid() == null ? Stream.<Card>empty() : contract.getPrepaid().getCard().stream(),
                contract.getCard().stream()).flatMap(s -> s);
    }
}
