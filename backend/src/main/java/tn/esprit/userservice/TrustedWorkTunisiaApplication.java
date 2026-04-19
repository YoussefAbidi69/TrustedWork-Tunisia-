package tn.esprit.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class TrustedWorkTunisiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrustedWorkTunisiaApplication.class, args);
    }
}