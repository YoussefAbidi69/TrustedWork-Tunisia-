package tn.esprit.freelancerprofileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling  // Active les tâches planifiées (Spring Scheduler)
public class FreelancerProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreelancerProfileServiceApplication.class, args);
    }
}