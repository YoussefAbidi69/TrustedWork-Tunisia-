package tn.esprit.mscontractservicee.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO used only for calling the external FastAPI ML service.
 * The ML service expects snake_case JSON properties.
 */
@Value
@Builder
public class MlProjectRequest implements Serializable {
    String description;
    double budget;

    @JsonProperty("deadline_days")
    int deadlineDays;

    String category;

    @JsonProperty("optimization_mode")
    String optimizationMode;
}

