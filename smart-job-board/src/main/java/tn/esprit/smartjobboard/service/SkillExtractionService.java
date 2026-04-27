package tn.esprit.smartjobboard.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Case-insensitive dictionary scan of job descriptions; designed so an LLM backend can replace this later.
 */
@Service
public class SkillExtractionService {

    private static final List<String> DICTIONARY_SORTED;

    static {
        List<String> raw = List.of(
                "Spring Boot", "GitHub Actions", "React Native", "Machine Learning", "Scikit-learn",
                "TypeScript", "JavaScript", "Elasticsearch", "Tailwind CSS", "Microservices",
                "Google Cloud Platform", "Microsoft Azure", "User Experience", "User Interface",
                "PostgreSQL", "Kubernetes", "TensorFlow", "SpringBoot", "FastAPI", "Express",
                "Angular", "Next.js", "Node.js", "MongoDB", "Mongoose", "RabbitMQ", "GraphQL",
                "Terraform", "Ansible", "Jenkins", "Flutter", "Android", "Photoshop", "Bootstrap",
                "Django", "Laravel", "PyTorch", "Vue.js", "Vue", "React", "Swift", "Kotlin",
                "Docker", "Kafka", "Redis", "MySQL", "Agile", "Scrum", "HTML", "CSS", "REST",
                "Linux", "Git", "AWS", "GCP", "Azure", "Java", "Python", "PHP", "Ruby",
                "Rails", "Go", "Rust", "C++", "C#", ".NET", "SQL", "NoSQL", "iOS", "Figma",
                "Ruby on Rails", "Ruby", "Rails",
                "CI/CD", "DevOps", "Blockchain", "Solidity", "Ethereum", "Hadoop", "Spark",
                "Airflow", "dbt", "Snowflake", "BigQuery", "Tableau", "Power BI", "Looker",
                "Selenium", "Cypress", "JUnit", "Mockito", "Jest", "Mocha", "Webpack", "Vite",
                "Nginx", "Apache", "Grafana", "Prometheus", "OpenShift", "Helm", "ArgoCD",
                "NestJS", "Symfony", "CodeIgniter", "ASP.NET", "Blazor", "Electron", "Three.js",
                "Unity", "Unreal Engine", "Godot", "Qt", "MATLAB", "R Language", "Pandas", "NumPy",
                "OpenCV", "NLP", "Computer Vision", "Deep Learning", "MLOps", "Keras", "XGBoost",
                "Spring Security", "Spring Data", "Hibernate", "JPA", "JDBC", "OAuth2", "JWT",
                "WebSockets", "gRPC", "SOAP", "SOAP API", "Dart", "Svelte", "Nuxt", "Nuxt.js",
                "Webpack", "Rollup", "Parcel", "Storybook", "Redux", "NgRx", "Zustand", "MobX",
                "Elasticsearch", "Solr", "Cassandra", "DynamoDB", "Firebase", "Supabase", "Prisma",
                "TypeORM", "Sequelize", "Liquibase", "Flyway", "Maven", "Gradle", "Ant",
                "CircleCI", "Travis CI", "TeamCity", "Bamboo", "Splunk", "Datadog", "New Relic",
                "Penetration Testing", "OWASP", "SOC 2", "ISO 27001"
        );
        List<String> dedup = new ArrayList<>(new LinkedHashSet<>(raw));
        dedup.sort(Comparator.comparingInt(String::length).reversed());
        DICTIONARY_SORTED = List.copyOf(dedup);
    }

    /**
     * Extracts canonical skill phrases appearing in the description text.
     */
    public List<String> extractFromDescription(String description) {
        if (description == null || description.isBlank()) {
            return List.of();
        }
        String haystack = description.toLowerCase(Locale.ROOT);
        Set<String> found = new LinkedHashSet<>();
        for (String skill : DICTIONARY_SORTED) {
            if (containsAsPhrase(haystack, skill.toLowerCase(Locale.ROOT))) {
                found.add(canonicalize(skill));
            }
        }
        return new ArrayList<>(found);
    }

    private static String canonicalize(String skill) {
        if ("SpringBoot".equalsIgnoreCase(skill)) {
            return "Spring Boot";
        }
        if ("Vue.js".equalsIgnoreCase(skill)) {
            return "Vue";
        }
        if ("Nuxt.js".equalsIgnoreCase(skill)) {
            return "Nuxt";
        }
        return skill;
    }

    private static boolean containsAsPhrase(String hayLower, String needleLower) {
        String quoted = Pattern.quote(needleLower);
        Pattern p = Pattern.compile("(?U)(?<![a-z0-9#+./])" + quoted + "(?![a-z0-9#+./])");
        return p.matcher(hayLower).find();
    }

    public int dictionarySize() {
        return DICTIONARY_SORTED.size();
    }
}
