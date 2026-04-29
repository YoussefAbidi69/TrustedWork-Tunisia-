package tn.esprit.smartjobboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.esprit.smartjobboard.dto.GenerateCoverLetterRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiWritingService")
class AiWritingServiceTest {

    private AiWritingService service;

    @BeforeEach
    void setUp() {
        service = new AiWritingService();
    }

    @Test
    @DisplayName("should generate cover letter with dynamic core challenge and past projects")
    void generateWithDynamicContent() {
        GenerateCoverLetterRequest request = new GenerateCoverLetterRequest();
        request.setJobTitle("Backend Developer");
        request.setJobDescription("Need an urgent API fix and urgent robust delivery.");
        request.setFreelancerName("John Doe");
        request.setSkills(List.of("Java", "Spring Boot"));
        request.setPastProjects("");

        String result = service.generateCoverLetter(request);

        assertThat(result).contains("Backend Developer");
        assertThat(result).contains("John Doe");
        assertThat(result).contains("shipping robust deliverables under tight deadlines"); // core challenge condition "urgent" triggers this phrase
        assertThat(result).contains("Java");
        assertThat(result).contains("Spring Boot");
        assertThat(result).contains("microservices architecture"); // Java past project heuristic
    }

    @Test
    @DisplayName("should generate cover letter for frontend roles")
    void generateFrontend() {
        GenerateCoverLetterRequest request = new GenerateCoverLetterRequest();
        request.setJobTitle("Frontend Developer");
        request.setJobDescription("Design intuitive UI for users.");
        request.setFreelancerName("Jane Doe");
        request.setSkills(List.of("React", "TypeScript"));
        request.setPastProjects("");

        String result = service.generateCoverLetter(request);

        assertThat(result).contains("Frontend Developer");
        assertThat(result).contains("crafting intuitive user experiences"); // UI/design heuristics
        assertThat(result).contains("React");
        assertThat(result).contains("TypeScript");
        assertThat(result).contains("responsive enterprise dashboard"); // React past project heuristic
    }

    @Test
    @DisplayName("should include user provided past project")
    void generateUserProject() {
        GenerateCoverLetterRequest request = new GenerateCoverLetterRequest();
        request.setJobTitle("Cloud Engineer");
        request.setJobDescription("Manage AWS infrastructure and deploy apps.");
        request.setFreelancerName("Alice");
        request.setSkills(List.of("AWS", "Terraform"));
        request.setPastProjects("Migrated 500 servers to AWS");

        String result = service.generateCoverLetter(request);

        assertThat(result).contains("Cloud Engineer");
        assertThat(result).contains("Migrated 500 servers to AWS");
        assertThat(result).doesNotContain("responsive enterprise dashboard");
    }

    @Test
    @DisplayName("should handle null and empty inputs safely")
    void handleNulls() {
        GenerateCoverLetterRequest request = new GenerateCoverLetterRequest();
        // everything null

        String result = service.generateCoverLetter(request);

        assertThat(result).isNotNull();
        assertThat(result).contains("my core technical expertise");
    }
}
