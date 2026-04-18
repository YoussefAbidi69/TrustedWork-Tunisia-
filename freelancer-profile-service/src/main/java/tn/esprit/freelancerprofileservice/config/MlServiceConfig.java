package tn.esprit.freelancerprofileservice.config;

/**
 * Configuration du micro-service ML Python (Flask port 5000)
 */
public class MlServiceConfig {

    // URL de base du service ML — utilisée par MlServiceClient
    public static final String ML_SERVICE_URL = "http://localhost:5000";

    private MlServiceConfig() {}
}