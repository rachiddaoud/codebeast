package com.bank.cardenrichment.xml;

import com.bank.cardenrichment.statement.Bank;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

/**
 * Reads a card statement XML into the classes generated from {@code xsd/card-statement.xsd}.
 * Any file that does not follow the XSD is rejected. DTDs and external entities are refused.
 */
@Component
public class StatementReader {

    private final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();

    public StatementReader() throws Exception {
        marshaller.setClassesToBeBound(Bank.class);
        marshaller.setSchema(new ClassPathResource("xsd/card-statement.xsd"));
        marshaller.afterPropertiesSet();
    }

    /** @throws XmlMappingException if the XML is malformed or does not follow the XSD */
    public Bank read(InputStream xml) {
        return (Bank) marshaller.unmarshal(new StreamSource(xml));
    }
}
