package br.com.caqi.compliance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CaqComplianceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaqComplianceApplication.class, args);
    }
}
