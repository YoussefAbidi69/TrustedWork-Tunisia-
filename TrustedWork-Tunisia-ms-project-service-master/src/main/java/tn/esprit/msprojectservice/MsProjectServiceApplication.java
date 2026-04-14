package tn.esprit.msprojectservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication

public class MsProjectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsProjectServiceApplication.class, args);
    }

}
