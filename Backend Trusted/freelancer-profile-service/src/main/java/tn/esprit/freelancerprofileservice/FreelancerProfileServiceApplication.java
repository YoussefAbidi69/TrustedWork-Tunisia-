package tn.esprit.freelancerprofileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableDiscoveryClient
@SpringBootApplication
@EnableScheduling
public class FreelancerProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreelancerProfileServiceApplication.class, args);
    }
}