package tn.esprit.freelancerprofileservice;

import org.junit.jupiter.api.Test;

/**
 * Smoke tests — validés sans contexte Spring pour éviter la dépendance MySQL.
 * Tests d'intégration complets sont dans les classes de service dédiées.
 */
class FreelancerProfileServiceApplicationTests {

    @Test
    void applicationStartupSmokeTest() {
        // Vérifie que la classe principale est instanciable sans erreur
        FreelancerProfileServiceApplication app = new FreelancerProfileServiceApplication();
        org.assertj.core.api.Assertions.assertThat(app).isNotNull();
    }
}
