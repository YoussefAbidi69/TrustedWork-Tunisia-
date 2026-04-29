package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferFlagDto {
    private Long id;
    private Long jobOfferId;
    private String signalName;
    private double signalWeight;
    private String description;
    private LocalDateTime detectedAt;
}
