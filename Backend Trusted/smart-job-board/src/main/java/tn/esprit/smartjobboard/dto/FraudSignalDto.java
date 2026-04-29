package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudSignalDto {
    private String code;
    private String message;
    private double weight;
}
