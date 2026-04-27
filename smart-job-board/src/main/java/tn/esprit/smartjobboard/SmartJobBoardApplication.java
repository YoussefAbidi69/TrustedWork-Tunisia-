package tn.esprit.smartjobboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartJobBoardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartJobBoardApplication.class, args);
    }
}
