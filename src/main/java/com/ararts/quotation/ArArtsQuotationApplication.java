package com.ararts.quotation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ArArtsQuotationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArArtsQuotationApplication.class, args);
    }
}
