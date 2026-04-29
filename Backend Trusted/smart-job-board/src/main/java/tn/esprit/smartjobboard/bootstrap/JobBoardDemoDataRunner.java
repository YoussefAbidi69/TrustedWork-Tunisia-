package tn.esprit.smartjobboard.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs the demo data loader once at startup when enabled (after JPA schema is ready).
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "jobboard.demo-data.enabled", havingValue = "true")
public class JobBoardDemoDataRunner implements ApplicationRunner {

    private final JobBoardDemoDataLoader jobBoardDemoDataLoader;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jobBoardDemoDataLoader.loadIfEmpty();
        } catch (Exception e) {
            log.error("Demo data loading failed: {}", e.getMessage(), e);
        }
    }
}
