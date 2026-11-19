package com.bank.cardenrichment.service;

import com.bank.cardenrichment.mapper.OperationCsvMapper;
import com.bank.cardenrichment.mapper.OperationCsvMapperImpl;
import com.bank.cardenrichment.xml.StatementReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.XmlMappingException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig({StatementReader.class, OperationCsvMapperImpl.class, OperationCsvService.class})
class OperationCsvServiceTest {

    static final String CONTRACT_START = """
            <Bank><Contract>
              <BankCode>B</BankCode><ServiceCode>S</ServiceCode><IssuerContracted>Y</IssuerContracted>
              <ContractID>C</ContractID><BankAccount>A</BankAccount>
            """;

    @Autowired
    OperationCsvService service;

    @Autowired
    OperationCsvMapper mapper;

    @Test
    void exportsEveryOperationInColumnOrderWithTreatments() {
        String csv = export(getClass().getResourceAsStream("/statements/sample.xml"));

        assertThat(csv.lines()).containsExactly(
                "transactionId,cardNumber,acceptorName,merchantId,terminalId,city,country,zip,mcc,"
                        + "transactionTimestamp,amount,currency",
                "OP-1,************1234,\"CAFE DE PARIS, GARE\",820014,18052001,LUXEMBOURG,LU,1616,5812,"
                        + "2026-10-12T08:15:00+02:00,4.40,EUR",
                "OP-2,************1234,AMAZON EU,AMZ0001,,LUXEMBOURG,,,0742,"
                        + "2026-10-20T21:03:10+02:00,-50.00,EUR",
                "OP-3,************9876,CACTUS,CAC01,,BERTRANGE,,,5411,"
                        + "2026-10-25T10:00:00+02:00,,");
    }

    @Test
    void neverWritesTheCardNumberInClear() {
        String csv = export(getClass().getResourceAsStream("/statements/sample.xml"));

        assertThat(csv).doesNotContain("4111").doesNotContain("5500");
    }

    @Test
    void noOperationsGivesHeaderOnly() {
        String csv = export(xml(CONTRACT_START + "<Prepaid/></Contract></Bank>"));

        assertThat(csv.lines()).hasSize(1);
    }

    @Test
    void rejectsXmlThatDoesNotFollowTheXsd() {
        String operationWithoutMcc = CONTRACT_START + """
                  <Card><Operation>
                    <CardNumber>4111111111111234</CardNumber><CardExpiryDate>2812</CardExpiryDate>
                    <MerchantName>X</MerchantName><MerchantLocality>Y</MerchantLocality>
                    <OprDateAndTime>2026-10-12T08:15:00Z</OprDateAndTime>
                    <TransactionConditions>00</TransactionConditions><TrnTrtCode>05</TrnTrtCode>
                    <TransactionalDataElements><CardAcceptorID>1</CardAcceptorID></TransactionalDataElements>
                  </Operation></Card>
                </Contract></Bank>
                """;

        assertThatThrownBy(() -> export(xml(operationWithoutMcc)))
                .isInstanceOf(XmlMappingException.class)
                .rootCause().hasMessageContaining("MCC");
    }

    @Test
    void rejectsDoctypeToBlockExternalEntities() {
        String evil = """
                <?xml version="1.0"?>
                <!DOCTYPE Bank [<!ENTITY x SYSTEM "file:///etc/passwd">]>
                <Bank>&x;</Bank>
                """;

        assertThatThrownBy(() -> export(xml(evil))).isInstanceOf(XmlMappingException.class);
    }

    @Test
    void treatments() {
        assertThat(mapper.maskPan("4111 1111 1111 1234")).isEqualTo("************1234");
        assertThat(mapper.padMcc("742")).isEqualTo("0742");
        assertThat(mapper.padMcc("5411")).isEqualTo("5411");
        assertThat(mapper.upper(" lu ")).isEqualTo("LU");
    }

    String export(InputStream xml) {
        StringWriter out = new StringWriter();
        service.export(xml, out);
        return out.toString();
    }

    static InputStream xml(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
