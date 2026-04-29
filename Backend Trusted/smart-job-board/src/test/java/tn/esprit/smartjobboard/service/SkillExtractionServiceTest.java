package tn.esprit.smartjobboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SkillExtractionService")
class SkillExtractionServiceTest {

    private SkillExtractionService service;

    @BeforeEach
    void setUp() {
        service = new SkillExtractionService();
    }

    @Nested
    @DisplayName("extractFromDescription()")
    class ExtractFromDescription {

        @Test
        @DisplayName("should extract Java and Spring Boot from a typical job description")
        void typicalDescription() {
            String desc = "We need an experienced Java developer proficient in Spring Boot and Docker deployment.";
            List<String> skills = service.extractFromDescription(desc);

            assertThat(skills).contains("Java", "Spring Boot");
        }

        @Test
        @DisplayName("should be case-insensitive")
        void caseInsensitive() {
            String desc = "Proficient in REACT and TYPESCRIPT for front-end development.";
            List<String> skills = service.extractFromDescription(desc);

            assertThat(skills).contains("React", "TypeScript");
        }

        @Test
        @DisplayName("should extract multi-word skills like 'Machine Learning'")
        void multiWordSkills() {
            String desc = "Knowledge of machine learning and deep learning is required.";
            List<String> skills = service.extractFromDescription(desc);

            assertThat(skills).contains("Machine Learning", "Deep Learning");
        }

        @Test
        @DisplayName("should extract framework variants like Node.js")
        void frameworkVariants() {
            String desc = "Build APIs with Node.js for backend services.";
            List<String> skills = service.extractFromDescription(desc);

            assertThat(skills).contains("Node.js");
        }

        @Test
        @DisplayName("should return empty list for null description")
        void nullDescription() {
            List<String> skills = service.extractFromDescription(null);
            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank description")
        void blankDescription() {
            List<String> skills = service.extractFromDescription("   ");
            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("should not return duplicates")
        void noDuplicates() {
            String desc = "Java Java Java developer with Java experience";
            List<String> skills = service.extractFromDescription(desc);

            long javaCount = skills.stream().filter(s -> s.equalsIgnoreCase("Java")).count();
            assertThat(javaCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should canonicalize SpringBoot → Spring Boot")
        void canonicalizeSpringBoot() {
            String desc = "Must know SpringBoot for backend development.";
            List<String> skills = service.extractFromDescription(desc);

            assertThat(skills).contains("Spring Boot");
            assertThat(skills).doesNotContain("SpringBoot");
        }

        @Test
        @DisplayName("should canonicalize Vue.js → Vue")
        void canonicalizeVue() {
            String desc = "Front end with Vue.js framework.";
            List<String> skills = service.extractFromDescription(desc);

            assertThat(skills).contains("Vue");
        }

        @Test
        @DisplayName("should extract cloud platforms")
        void cloudPlatforms() {
            String desc = "Deploy on AWS and Azure cloud. Experience with Kubernetes is a plus.";
            List<String> skills = service.extractFromDescription(desc);

            assertThat(skills).contains("AWS", "Azure", "Kubernetes");
        }

        @Test
        @DisplayName("should handle description with no known skills")
        void noKnownSkills() {
            String desc = "We are looking for a team player who communicates well.";
            List<String> skills = service.extractFromDescription(desc);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("should extract C# and .NET correctly")
        void csharpAndDotnet() {
            String desc = "Develop applications using C# and .NET framework.";
            List<String> skills = service.extractFromDescription(desc);

            assertThat(skills).contains("C#", ".NET");
        }

        @Test
        @DisplayName("should extract CI/CD and DevOps")
        void cicdDevops() {
            String desc = "Set up CI/CD pipelines and follow DevOps practices.";
            List<String> skills = service.extractFromDescription(desc);

            assertThat(skills).contains("CI/CD", "DevOps");
        }
    }

    @Nested
    @DisplayName("dictionarySize()")
    class DictionarySize {

        @Test
        @DisplayName("should have a non-empty dictionary")
        void nonEmpty() {
            assertThat(service.dictionarySize()).isGreaterThan(50);
        }
    }
}
