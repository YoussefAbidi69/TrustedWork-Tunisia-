package tn.esprit.smartjobboard.dto;

import lombok.Data;
import java.util.List;

@Data
public class GenerateCoverLetterRequest {
    private String jobTitle;
    private String jobDescription;
    private String freelancerName;
    private List<String> skills;
    private String bio;
    private String pastProjects;
}
