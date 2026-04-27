package tn.esprit.smartjobboard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tn.esprit.smartjobboard.entity.PlatformSetting;
import tn.esprit.smartjobboard.repository.PlatformSettingRepository;

/**
 * Seeds default platform settings on first startup (visible to admin analytics).
 */
@Component
@RequiredArgsConstructor
public class PlatformBootstrap implements ApplicationRunner {

    private final PlatformSettingRepository platformSettingRepository;

    @Value("${jobboard.platform.mock-average-budget:2000}")
    private String mockAverageBudget;

    @Override
    public void run(ApplicationArguments args) {
        if (platformSettingRepository.findBySettingKey("MOCK_AVG_BUDGET").isEmpty()) {
            PlatformSetting s = new PlatformSetting();
            s.setSettingKey("MOCK_AVG_BUDGET");
            s.setSettingValue(mockAverageBudget);
            platformSettingRepository.save(s);
        }
    }
}
