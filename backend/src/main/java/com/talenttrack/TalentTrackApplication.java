package com.talenttrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TalentTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalentTrackApplication.class, args);
    }
}
