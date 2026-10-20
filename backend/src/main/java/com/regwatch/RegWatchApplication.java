package com.regwatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RegWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegWatchApplication.class, args);
    }
}
