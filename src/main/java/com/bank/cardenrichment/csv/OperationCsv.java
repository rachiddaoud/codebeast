package com.bank.cardenrichment.csv;

import com.bank.cardenrichment.csv.Csv.Column;
import com.bank.cardenrichment.statement.Bank;
import com.bank.cardenrichment.statement.Card;
import com.bank.cardenrichment.statement.Contract;
import com.bank.cardenrichment.statement.Cycle;
import com.bank.cardenrichment.statement.Operation;
import com.bank.cardenrichment.statement.TransactionalDataElements;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Exports the card operations of a bank statement (see {@code xsd/card-statement.xsd}) as CSV,
 * one line per operation, with the fields we send to Tapix.
 * <p>
 * To add, remove or reorder a column, edit {@link #COLUMNS}.
 */
public final class OperationCsv {

    /** One operation with the contract it belongs to. */
    public record Row(Contract contract, Operation operation) {

        TransactionalDataElements data() {
            return operation.getTransactionalDataElements();
        }
    }

    public static final List<Column<Row>> COLUMNS = List.of(
            Column.of("contractId", r -> r.contract().getContractID()),
            Column.of("bankCode", r -> r.contract().getBankCode()),
            Column.of("transactionId", r -> r.operation().getOperationID()),
            Column.of("cardNumber", r -> maskPan(r.operation().getCardNumber())),
            Column.of("acceptorName", r -> r.operation().getMerchantName().strip()),
            Column.of("merchantId", r -> r.data().getCardAcceptorID()),
            Column.of("terminalId", r -> r.data().getCardAcceptorTerminalID()),
            Column.of("city", r -> r.operation().getMerchantLocality().strip()),
            Column.of("country", r -> upper(r.operation().getMerchantCountry())),
            Column.of("zip", r -> r.operation().getMerchantZip()),
            Column.of("mcc", r -> padMcc(r.operation().getMCC())),
            Column.of("transactionTimestamp", r -> r.operation().getOprDateAndTime().toXMLFormat()),
            Column.of("amount", r -> map(r.operation().getAmount(), a -> a.toPlainString())),
            Column.of("currency", r -> upper(r.operation().getCurrency())));

    private static final JAXBContext JAXB = jaxbContext();
    private static final Schema SCHEMA = schema();

    private OperationCsv() {
    }

    public static String convert(InputStream xml) {
        StringWriter out = new StringWriter();
        convert(xml, out);
        return out.toString();
    }

    public static void convert(InputStream xml, Writer out) {
        Csv.write(COLUMNS, rows(read(xml)), out);
    }

    /** Every card operation, wherever its card sits: under a cycle, a prepaid or the contract itself. */
    static List<Row> rows(Bank bank) {
        return bank.getContract().stream()
                .flatMap(contract -> cards(contract)
                        .flatMap(card -> card.getOperation().stream())
                        .map(op -> new Row(contract, op)))
                .toList();
    }

    private static Stream<Card> cards(Contract contract) {
        Stream<Card> inCycles = contract.getCycle().stream().flatMap((Cycle c) -> c.getCard().stream());
        Stream<Card> prepaid = contract.getPrepaid() == null ? Stream.empty() : contract.getPrepaid().getCard().stream();
        return Stream.of(inCycles, prepaid, contract.getCard().stream()).flatMap(s -> s);
    }

    /** Parses and validates against the XSD. DTDs and external entities are refused. */
    static Bank read(InputStream xml) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            Unmarshaller unmarshaller = JAXB.createUnmarshaller();
            unmarshaller.setSchema(SCHEMA);
            return (Bank) unmarshaller.unmarshal(factory.createXMLStreamReader(xml));
        } catch (JAXBException | XMLStreamException e) {
            throw new IllegalArgumentException("Invalid card statement: " + rootMessage(e), e);
        }
    }

    // --- field treatments ---

    /** Keeps the last 4 digits only: the PAN must never leave in clear. */
    static String maskPan(String pan) {
        String digits = pan.replaceAll("\\D", "");
        return digits.length() <= 4 ? digits : "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }

    /** MCCs are 4 digits: "742" becomes "0742". */
    static String padMcc(String mcc) {
        String s = mcc.strip();
        return s.length() >= 4 ? s : "0".repeat(4 - s.length()) + s;
    }

    private static String upper(String s) {
        return map(s, v -> v.strip().toUpperCase(Locale.ROOT));
    }

    private static <T> String map(T value, Function<T, String> f) {
        return value == null ? null : f.apply(value);
    }

    // --- setup ---

    private static JAXBContext jaxbContext() {
        try {
            return JAXBContext.newInstance(Bank.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Schema schema() {
        try {
            return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(OperationCsv.class.getResource("/xsd/card-statement.xsd"));
        } catch (SAXException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String rootMessage(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
