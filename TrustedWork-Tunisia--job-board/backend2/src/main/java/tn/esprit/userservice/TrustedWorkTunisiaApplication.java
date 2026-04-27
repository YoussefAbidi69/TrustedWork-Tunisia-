package tn.esprit.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrustedWorkTunisiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrustedWorkTunisiaApplication.class, args);
    }
}