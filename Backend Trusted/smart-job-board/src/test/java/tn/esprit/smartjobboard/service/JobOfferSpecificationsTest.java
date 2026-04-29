package tn.esprit.smartjobboard.service;

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobOfferSpecifications")
class JobOfferSpecificationsTest {

    @Mock private Root<JobOffer> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;

    @Mock private Path pathMock;
    @Mock private Expression expressionMock;
    @Mock private Predicate predicateMock;

    @BeforeEach
    void setUp() {
        lenient().when(root.get(anyString())).thenReturn(pathMock);
        lenient().when(cb.equal(any(), any())).thenReturn(predicateMock);
        lenient().when(cb.conjunction()).thenReturn(predicateMock);
        lenient().when(cb.lower(any())).thenReturn(expressionMock);
        lenient().when(cb.like(any(), anyString())).thenReturn(predicateMock);
        lenient().when(cb.greaterThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicateMock);
        lenient().when(cb.lessThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicateMock);
        lenient().when(cb.and(any())).thenReturn(predicateMock);
        lenient().when(cb.or(any())).thenReturn(predicateMock);
    }

    @Nested
    @DisplayName("visibility()")
    class Visibility {
        @Test
        @DisplayName("should filter by client owner if mine=true and role=CLIENT")
        void clientMine() {
            Specification<JobOffer> spec = JobOfferSpecifications.visibility(10L, "CLIENT", true);
            spec.toPredicate(root, query, cb);

            verify(cb).equal(pathMock, 10L);
        }

        @Test
        @DisplayName("should filter by PUBLISHED if mine=false or role=FREELANCER")
        void published() {
            Specification<JobOffer> spec = JobOfferSpecifications.visibility(10L, "FREELANCER", true);
            spec.toPredicate(root, query, cb);

            verify(cb).equal(pathMock, JobOfferStatus.PUBLISHED);
        }
    }

    @Nested
    @DisplayName("categoryEquals()")
    class CategoryEquals {
        @Test
        @DisplayName("should return conjunction if category is null/blank")
        void nullCategory() {
            JobOfferSpecifications.categoryEquals(null).toPredicate(root, query, cb);
            JobOfferSpecifications.categoryEquals("  ").toPredicate(root, query, cb);
            verify(cb, times(2)).conjunction();
        }

        @Test
        @DisplayName("should filter by category")
        void validCategory() {
            JobOfferSpecifications.categoryEquals("IT").toPredicate(root, query, cb);
            verify(cb).equal(expressionMock, "it");
        }
    }

    @Nested
    @DisplayName("remoteEquals()")
    class RemoteEquals {
        @Test
        @DisplayName("should return conjunction if remote is null")
        void nullRemote() {
            JobOfferSpecifications.remoteEquals(null).toPredicate(root, query, cb);
            verify(cb).conjunction();
        }

        @Test
        @DisplayName("should filter by remote")
        void validRemote() {
            JobOfferSpecifications.remoteEquals(true).toPredicate(root, query, cb);
            verify(cb).equal(pathMock, true);
        }
    }

    @Nested
    @DisplayName("locationContains()")
    class LocationContains {
        @Test
        @DisplayName("should return conjunction if location is null/blank")
        void nullLocation() {
            JobOfferSpecifications.locationContains(null).toPredicate(root, query, cb);
            verify(cb).conjunction();
        }

        @Test
        @DisplayName("should filter by location")
        void validLocation() {
            JobOfferSpecifications.locationContains("NY").toPredicate(root, query, cb);
            verify(cb).like(expressionMock, "%ny%");
        }
    }

    @Nested
    @DisplayName("budgetOverlap()")
    class BudgetOverlap {
        @Test
        @DisplayName("should return conjunction if both null")
        void bothNull() {
            JobOfferSpecifications.budgetOverlap(null, null).toPredicate(root, query, cb);
            verify(cb).conjunction();
        }

        @Test
        @DisplayName("should filter by min and max")
        void validBudget() {
            JobOfferSpecifications.budgetOverlap(BigDecimal.valueOf(100), BigDecimal.valueOf(500)).toPredicate(root, query, cb);
            verify(cb).greaterThanOrEqualTo(any(Path.class), any(Comparable.class));
            verify(cb).lessThanOrEqualTo(any(Path.class), any(Comparable.class));
            verify(cb).and(any(Predicate[].class));
        }
    }

    @Nested
    @DisplayName("anySkillMatches()")
    class AnySkillMatches {
        @Mock private Join joinMock;

        @BeforeEach
        void setupJoin() {
            lenient().when(root.join(anyString(), any(JoinType.class))).thenReturn(joinMock);
            lenient().when(cb.literal(any())).thenReturn(expressionMock);
        }

        @Test
        @DisplayName("should return conjunction if skills null/empty")
        void emptySkills() {
            JobOfferSpecifications.anySkillMatches(null).toPredicate(root, query, cb);
            verify(cb).conjunction();
        }

        @Test
        @DisplayName("should build OR predicates for skills")
        void validSkills() {
            JobOfferSpecifications.anySkillMatches(List.of("Java")).toPredicate(root, query, cb);
            verify(query).distinct(true);
            verify(root).join("requiredSkills", JoinType.LEFT);
            verify(root).join("extractedSkills", JoinType.LEFT);
            verify(cb).or(any(Predicate[].class));
        }
    }
}
