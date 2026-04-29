package tn.esprit.smartjobboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("SemanticSkillService")
class SemanticSkillServiceTest {

    private SemanticSkillService service;

    @BeforeEach
    void setUp() {
        service = new SemanticSkillService();
        service.buildGraph();
    }

    @Nested
    @DisplayName("norm()")
    class Normalization {

        @Test
        @DisplayName("should lowercase and trim")
        void lowercaseTrim() {
            assertThat(service.norm("  Spring Boot ")).isEqualTo("spring boot");
        }

        @Test
        @DisplayName("should return empty string for null")
        void nullInput() {
            assertThat(service.norm(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("expand()")
    class Expansion {

        @Test
        @DisplayName("should expand react to include next.js")
        void reactExpansion() {
            Set<String> expanded = service.expand(List.of("React"));
            assertThat(expanded).contains("react", "next.js");
        }

        @Test
        @DisplayName("should expand java to include spring boot")
        void javaExpansion() {
            Set<String> expanded = service.expand(List.of("Java"));
            assertThat(expanded).contains("java", "spring boot");
        }

        @Test
        @DisplayName("should expand docker to include kubernetes")
        void dockerExpansion() {
            Set<String> expanded = service.expand(List.of("Docker"));
            assertThat(expanded).contains("docker", "kubernetes");
        }

        @Test
        @DisplayName("should skip blank/null entries")
        void blankEntries() {
            Set<String> expanded = service.expand(List.of("", " ", "Java"));
            assertThat(expanded).doesNotContain("");
            assertThat(expanded).contains("java");
        }

        @Test
        @DisplayName("should handle unknown skill without neighbors")
        void unknownSkill() {
            Set<String> expanded = service.expand(List.of("COBOL"));
            assertThat(expanded).containsExactly("cobol");
        }

        @Test
        @DisplayName("should expand bidirectionally")
        void bidirectional() {
            Set<String> fromJS = service.expand(List.of("JavaScript"));
            assertThat(fromJS).contains("typescript");

            Set<String> fromTS = service.expand(List.of("TypeScript"));
            assertThat(fromTS).contains("javascript");
        }
    }

    @Nested
    @DisplayName("skillMatchPercent()")
    class SkillMatch {

        @Test
        @DisplayName("should return 50% when both lists are empty")
        void bothEmpty() {
            double pct = service.skillMatchPercent(List.of(), List.of());
            assertThat(pct).isEqualTo(50.0);
        }

        @Test
        @DisplayName("should return 0% when one list is empty")
        void oneEmpty() {
            double pct = service.skillMatchPercent(List.of("Java"), List.of());
            assertThat(pct).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 100% for identical skill sets")
        void perfectMatch() {
            double pct = service.skillMatchPercent(List.of("Java"), List.of("java"));
            // After expansion both contain {java, spring boot} and candidate also {java, spring boot}
            // So intersection = union → 100%
            assertThat(pct).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should give partial match for overlapping expanded skills")
        void partialMatch() {
            // Required: React, SQL — expands to {react, next.js, sql, postgresql}
            // Candidate: JavaScript, PostgreSQL — expands to {javascript, typescript, postgresql, sql}
            // Intersection: {sql, postgresql} = 2
            // Union: {react, next.js, sql, postgresql, javascript, typescript} = 6
            // → 33.33%
            double pct = service.skillMatchPercent(
                    List.of("React", "SQL"),
                    List.of("JavaScript", "PostgreSQL"));
            assertThat(pct).isCloseTo(33.33, within(1.0));
        }

        @Test
        @DisplayName("should boost match through semantic neighbors (react ~ next.js)")
        void semanticBoost() {
            // Required: React — expands to {react, next.js}
            // Candidate: Next.js — expands to {next.js, react}
            // 100% match via expansion
            double pct = service.skillMatchPercent(List.of("React"), List.of("Next.js"));
            assertThat(pct).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should handle case-insensitive matching")
        void caseInsensitive() {
            double pct = service.skillMatchPercent(List.of("DOCKER"), List.of("docker"));
            assertThat(pct).isEqualTo(100.0);
        }
    }
}
