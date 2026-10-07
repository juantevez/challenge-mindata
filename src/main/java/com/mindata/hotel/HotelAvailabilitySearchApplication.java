package com.mindata.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HotelAvailabilitySearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelAvailabilitySearchApplication.class, args);
    }
}
