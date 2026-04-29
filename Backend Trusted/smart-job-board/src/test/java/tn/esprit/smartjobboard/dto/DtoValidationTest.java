package tn.esprit.smartjobboard.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTO Validation")
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("ApplicationCreateRequest")
    class ApplicationCreateRequestTests {

        @Test
        @DisplayName("should pass validation with valid data")
        void validRequest() {
            ApplicationCreateRequest req = new ApplicationCreateRequest();
            req.setJobOfferId(1L);
            req.setCoverLetter("This is a valid cover letter that is at least 10 characters long.");
            req.setProposedRate(BigDecimal.valueOf(1000));
            req.setDeclaredSkills(List.of("Java", "Docker"));

            Set<ConstraintViolation<ApplicationCreateRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should reject null jobOfferId")
        void nullJobOfferId() {
            ApplicationCreateRequest req = new ApplicationCreateRequest();
            req.setCoverLetter("Valid cover letter text here.");
            req.setProposedRate(BigDecimal.valueOf(100));

            Set<ConstraintViolation<ApplicationCreateRequest>> violations = validator.validate(req);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                    .contains("jobOfferId");
        }

        @Test
        @DisplayName("should reject blank coverLetter")
        void blankCoverLetter() {
            ApplicationCreateRequest req = new ApplicationCreateRequest();
            req.setJobOfferId(1L);
            req.setCoverLetter("");
            req.setProposedRate(BigDecimal.valueOf(100));

            Set<ConstraintViolation<ApplicationCreateRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should reject coverLetter shorter than 10 characters")
        void tooShortCoverLetter() {
            ApplicationCreateRequest req = new ApplicationCreateRequest();
            req.setJobOfferId(1L);
            req.setCoverLetter("Short");
            req.setProposedRate(BigDecimal.valueOf(100));

            Set<ConstraintViolation<ApplicationCreateRequest>> violations = validator.validate(req);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                    .contains("coverLetter");
        }

        @Test
        @DisplayName("should reject coverLetter longer than 8000 characters")
        void tooLongCoverLetter() {
            ApplicationCreateRequest req = new ApplicationCreateRequest();
            req.setJobOfferId(1L);
            req.setCoverLetter("x".repeat(8001));
            req.setProposedRate(BigDecimal.valueOf(100));

            Set<ConstraintViolation<ApplicationCreateRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should accept coverLetter of exactly 8000 characters")
        void maxLengthCoverLetter() {
            ApplicationCreateRequest req = new ApplicationCreateRequest();
            req.setJobOfferId(1L);
            req.setCoverLetter("x".repeat(8000));
            req.setProposedRate(BigDecimal.valueOf(100));

            Set<ConstraintViolation<ApplicationCreateRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should reject null proposedRate")
        void nullProposedRate() {
            ApplicationCreateRequest req = new ApplicationCreateRequest();
            req.setJobOfferId(1L);
            req.setCoverLetter("Valid cover letter here.");

            Set<ConstraintViolation<ApplicationCreateRequest>> violations = validator.validate(req);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                    .contains("proposedRate");
        }

        @Test
        @DisplayName("should reject zero proposedRate")
        void zeroProposedRate() {
            ApplicationCreateRequest req = new ApplicationCreateRequest();
            req.setJobOfferId(1L);
            req.setCoverLetter("Valid cover letter here.");
            req.setProposedRate(BigDecimal.ZERO);

            Set<ConstraintViolation<ApplicationCreateRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("JobOfferCreateRequest")
    class JobOfferCreateRequestTests {

        @Test
        @DisplayName("should pass validation with valid data")
        void validRequest() {
            JobOfferCreateRequest req = new JobOfferCreateRequest();
            req.setTitle("Senior Java Developer");
            req.setDescription("Building enterprise applications.");
            req.setCategory("IT");
            req.setBudgetMin(BigDecimal.valueOf(500));
            req.setBudgetMax(BigDecimal.valueOf(5000));

            Set<ConstraintViolation<JobOfferCreateRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should reject blank title")
        void blankTitle() {
            JobOfferCreateRequest req = new JobOfferCreateRequest();
            req.setTitle("");
            req.setDescription("Desc");
            req.setCategory("IT");
            req.setBudgetMin(BigDecimal.ZERO);
            req.setBudgetMax(BigDecimal.TEN);

            Set<ConstraintViolation<JobOfferCreateRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should reject title exceeding 255 chars")
        void titleTooLong() {
            JobOfferCreateRequest req = new JobOfferCreateRequest();
            req.setTitle("x".repeat(256));
            req.setDescription("Desc");
            req.setCategory("IT");
            req.setBudgetMin(BigDecimal.ZERO);
            req.setBudgetMax(BigDecimal.TEN);

            Set<ConstraintViolation<JobOfferCreateRequest>> violations = validator.validate(req);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                    .contains("title");
        }

        @Test
        @DisplayName("should reject null budgetMin")
        void nullBudgetMin() {
            JobOfferCreateRequest req = new JobOfferCreateRequest();
            req.setTitle("Job");
            req.setDescription("Desc");
            req.setCategory("IT");
            req.setBudgetMax(BigDecimal.TEN);

            Set<ConstraintViolation<JobOfferCreateRequest>> violations = validator.validate(req);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                    .contains("budgetMin");
        }

        @Test
        @DisplayName("should reject blank category")
        void blankCategory() {
            JobOfferCreateRequest req = new JobOfferCreateRequest();
            req.setTitle("Job");
            req.setDescription("Desc");
            req.setCategory("");
            req.setBudgetMin(BigDecimal.ZERO);
            req.setBudgetMax(BigDecimal.TEN);

            Set<ConstraintViolation<JobOfferCreateRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("ApplicationStatusUpdateRequest")
    class ApplicationStatusUpdateRequestTests {

        @Test
        @DisplayName("should pass with valid status")
        void validStatus() {
            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
            req.setStatus(tn.esprit.smartjobboard.entity.ApplicationStatus.SHORTLISTED);

            Set<ConstraintViolation<ApplicationStatusUpdateRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should reject null status")
        void nullStatus() {
            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();

            Set<ConstraintViolation<ApplicationStatusUpdateRequest>> violations = validator.validate(req);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                    .contains("status");
        }
    }
}
