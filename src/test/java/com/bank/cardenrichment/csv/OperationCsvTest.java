package com.bank.cardenrichment.csv;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationCsvTest {

    @Test
    void exportsEveryOperationWithTheTapixFieldsInOrder() {
        String csv = OperationCsv.convert(resource("/statements/sample.xml"));

        assertThat(csv.split("\r\n")).containsExactly(
                "contractId,bankCode,transactionId,cardNumber,acceptorName,merchantId,terminalId,"
                        + "city,country,zip,mcc,transactionTimestamp,amount,currency",
                "C-001,BCEE,OP-1,************1234,\"CAFE DE PARIS, GARE\",820014,18052001,"
                        + "LUXEMBOURG,LU,1616,5812,2026-10-12T08:15:00+02:00,4.40,EUR",
                "C-001,BCEE,OP-2,************1234,AMAZON EU,AMZ0001,,"
                        + "LUXEMBOURG,,,0742,2026-10-20T21:03:10+02:00,-50.00,EUR",
                "C-002,BCEE,OP-3,************9876,CACTUS,CAC01,,"
                        + "BERTRANGE,,,5411,2026-10-25T10:00:00+02:00,,");
    }

    @Test
    void neverWritesTheCardNumberInClear() {
        String csv = OperationCsv.convert(resource("/statements/sample.xml"));

        assertThat(csv).doesNotContain("4111").doesNotContain("5500");
    }

    @Test
    void contractWithoutOperationsGivesHeaderOnly() {
        String csv = OperationCsv.convert(xml("""
                <Bank><Contract>
                  <BankCode>B</BankCode><ServiceCode>S</ServiceCode><IssuerContracted>Y</IssuerContracted>
                  <ContractID>C</ContractID><BankAccount>A</BankAccount><Prepaid/>
                </Contract></Bank>
                """));

        assertThat(csv.split("\r\n")).hasSize(1);
    }

    @Test
    void rejectsXmlThatDoesNotFollowTheXsd() {
        // Operation without MCC
        String missingMcc = """
                <Bank><Contract>
                  <BankCode>B</BankCode><ServiceCode>S</ServiceCode><IssuerContracted>Y</IssuerContracted>
                  <ContractID>C</ContractID><BankAccount>A</BankAccount>
                  <Card><Operation>
                    <CardNumber>4111111111111234</CardNumber><CardExpiryDate>2812</CardExpiryDate>
                    <MerchantName>X</MerchantName><MerchantLocality>Y</MerchantLocality>
                    <OprDateAndTime>2026-10-12T08:15:00Z</OprDateAndTime>
                    <TransactionConditions>00</TransactionConditions><TrnTrtCode>05</TrnTrtCode>
                    <TransactionalDataElements><CardAcceptorID>1</CardAcceptorID></TransactionalDataElements>
                  </Operation></Card>
                </Contract></Bank>
                """;

        assertThatThrownBy(() -> OperationCsv.convert(xml(missingMcc)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MCC");
    }

    @Test
    void rejectsDoctypeToBlockExternalEntities() {
        String evil = """
                <?xml version="1.0"?>
                <!DOCTYPE Bank [<!ENTITY x SYSTEM "file:///etc/passwd">]>
                <Bank>&x;</Bank>
                """;

        assertThatThrownBy(() -> OperationCsv.convert(xml(evil))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void treatments() {
        assertThat(OperationCsv.maskPan("4111 1111 1111 1234")).isEqualTo("************1234");
        assertThat(OperationCsv.padMcc("742")).isEqualTo("0742");
        assertThat(OperationCsv.padMcc("5411")).isEqualTo("5411");
    }

    @Test
    void csvQuotesOnlyWhenNeeded() {
        assertThat(Csv.escape("plain")).isEqualTo("plain");
        assertThat(Csv.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(Csv.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(Csv.escape("a\nb")).isEqualTo("\"a\nb\"");
        assertThat(Csv.escape(null)).isEmpty();
    }

    static InputStream resource(String path) {
        return OperationCsvTest.class.getResourceAsStream(path);
    }

    static InputStream xml(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
