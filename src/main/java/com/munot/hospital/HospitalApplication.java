package com.munot.hospital;

import com.munot.hospital.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class HospitalApplication {
    public static void main(String[] args) {
        String tz = System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata");
        TimeZone.setDefault(TimeZone.getTimeZone(tz));
        SpringApplication.run(HospitalApplication.class, args);
    }
}
