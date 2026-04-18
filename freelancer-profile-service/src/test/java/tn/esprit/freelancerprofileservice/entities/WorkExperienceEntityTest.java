package tn.esprit.freelancerprofileservice.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WorkExperienceEntityTest {

    @Test
    void prePersist_shouldSetIsCurrentFalse_whenNull() {
        WorkExperience exp = new WorkExperience();
        exp.setIsCurrent(null);
        exp.setStartDate(LocalDate.of(2022, 1, 1));

        exp.prePersist();

        assertThat(exp.getIsCurrent()).isFalse();
    }

    @Test
    void prePersist_shouldNullifyEndDate_whenIsCurrentTrue() {
        WorkExperience exp = new WorkExperience();
        exp.setIsCurrent(true);
        exp.setEndDate(LocalDate.of(2023, 12, 31));
        exp.setStartDate(LocalDate.of(2022, 1, 1));

        exp.prePersist();

        assertThat(exp.getIsCurrent()).isTrue();
        assertThat(exp.getEndDate()).isNull();
    }

    @Test
    void prePersist_shouldKeepEndDate_whenIsCurrentFalse() {
        WorkExperience exp = new WorkExperience();
        exp.setIsCurrent(false);
        exp.setEndDate(LocalDate.of(2023, 12, 31));
        exp.setStartDate(LocalDate.of(2022, 1, 1));

        exp.prePersist();

        assertThat(exp.getIsCurrent()).isFalse();
        assertThat(exp.getEndDate()).isEqualTo(LocalDate.of(2023, 12, 31));
    }

    @Test
    void preUpdate_shouldNullifyEndDate_whenIsCurrentTrue() {
        WorkExperience exp = new WorkExperience();
        exp.setIsCurrent(true);
        exp.setEndDate(LocalDate.of(2024, 6, 1));

        exp.preUpdate();

        assertThat(exp.getEndDate()).isNull();
    }

    @Test
    void preUpdate_shouldSetIsCurrentFalse_whenNull() {
        WorkExperience exp = new WorkExperience();
        exp.setIsCurrent(null);

        exp.preUpdate();

        assertThat(exp.getIsCurrent()).isFalse();
    }
}
