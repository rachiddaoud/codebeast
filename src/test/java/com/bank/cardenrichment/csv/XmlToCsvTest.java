package com.bank.cardenrichment.csv;

import com.bank.cardenrichment.csv.XmlToCsv.Column;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlToCsvTest {

    static final String XML = """
            <transactions>
              <transaction id="T1">
                <amount>12.5</amount>
                <currency>eur</currency>
                <merchant><name>Café, Paris</name><city>Paris</city></merchant>
              </transaction>
              <transaction id="T2">
                <amount>3</amount>
                <currency>usd</currency>
                <merchant><name>Joe's "Diner"</name></merchant>
              </transaction>
            </transactions>
            """;

    @Test
    void columnsComeOutInTheGivenOrderWithTransforms() {
        XmlToCsv converter = new XmlToCsv("//transaction", List.of(
                Column.of("merchant", "merchant/name"),
                Column.of("currency", "currency", String::toUpperCase),
                Column.of("id", "@id"),
                Column.of("amount_cents", "amount", v -> String.valueOf(Math.round(Double.parseDouble(v) * 100)))));

        assertThat(converter.convert(xml(XML))).isEqualTo(
                "merchant,currency,id,amount_cents\r\n"
                        + "\"Café, Paris\",EUR,T1,1250\r\n"
                        + "\"Joe's \"\"Diner\"\"\",USD,T2,300\r\n");
    }

    @Test
    void missingFieldGivesEmptyCell() {
        XmlToCsv converter = new XmlToCsv("//transaction", List.of(
                Column.of("id", "@id"),
                Column.of("city", "merchant/city")));

        assertThat(converter.convert(xml(XML))).isEqualTo("id,city\r\nT1,Paris\r\nT2,\r\n");
    }

    @Test
    void noRecordsGivesHeaderOnly() {
        XmlToCsv converter = new XmlToCsv("//transaction", List.of(Column.of("id", "@id")));

        assertThat(converter.convert(xml("<transactions/>"))).isEqualTo("id\r\n");
    }

    @Test
    void escapesLineBreaks() {
        assertThat(XmlToCsv.escape("a\nb")).isEqualTo("\"a\nb\"");
        assertThat(XmlToCsv.escape("plain")).isEqualTo("plain");
    }

    @Test
    void rejectsDoctypeToBlockExternalEntities() {
        String evil = """
                <?xml version="1.0"?>
                <!DOCTYPE t [<!ENTITY x SYSTEM "file:///etc/passwd">]>
                <transactions><transaction id="&x;"/></transactions>
                """;
        XmlToCsv converter = new XmlToCsv("//transaction", List.of(Column.of("id", "@id")));

        assertThatThrownBy(() -> converter.convert(xml(evil))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBadXPathUpFront() {
        assertThatThrownBy(() -> new XmlToCsv("//transaction", List.of(Column.of("x", "[["))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static InputStream xml(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
