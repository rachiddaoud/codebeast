package com.bank.cardenrichment.config;

import com.bank.cardenrichment.statement.Bank;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class StatementConfig {

    /** Reads card statements and rejects any file that does not follow the XSD. DTDs and external entities are off by default. */
    @Bean
    public Jaxb2Marshaller statementMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(Bank.class);
        marshaller.setSchema(new ClassPathResource("xsd/card-statement.xsd"));
        return marshaller;
    }
}
