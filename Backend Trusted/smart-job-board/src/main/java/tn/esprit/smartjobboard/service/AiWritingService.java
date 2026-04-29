package tn.esprit.smartjobboard.service;

import org.springframework.stereotype.Service;
import tn.esprit.smartjobboard.dto.GenerateCoverLetterRequest;

import java.util.List;

@Service
public class AiWritingService {

    public String generateCoverLetter(GenerateCoverLetterRequest request) {
        String jobTitle = safe(request.getJobTitle());
        String jobDesc = safe(request.getJobDescription());
        String freelancerName = safe(request.getFreelancerName());
        List<String> skills = request.getSkills();
        
        // Extract a core theme or challenge from the job description
        String coreChallenge = "delivering high-quality results for this project";
        if (jobDesc.toLowerCase().contains("urgent") || jobDesc.toLowerCase().contains("fast")) {
            coreChallenge = "shipping robust deliverables under tight deadlines";
        } else if (jobDesc.toLowerCase().contains("design") || jobDesc.toLowerCase().contains("ui")) {
            coreChallenge = "crafting intuitive user experiences that scale";
        } else if (jobDesc.toLowerCase().contains("api") || jobDesc.toLowerCase().contains("backend")) {
            coreChallenge = "architecting secure and high-performance backend systems";
        }

        // Extract top 2 skills
        String skill1 = (skills != null && skills.size() > 0) ? skills.get(0) : "my core technical expertise";
        String skill2 = (skills != null && skills.size() > 1) ? skills.get(1) : "proven problem-solving abilities";

        // Extract past project context (or mock it elegantly if empty)
        String pastProject = safe(request.getPastProjects());
        if (pastProject.isEmpty()) {
            if (skill1.toLowerCase().contains("java") || skill1.toLowerCase().contains("spring")) {
                pastProject = "Recently, I rebuilt a legacy monolithic application into a modern microservices architecture, reducing system latency by 40% and cutting infrastructure costs.";
            } else if (skill1.toLowerCase().contains("angular") || skill1.toLowerCase().contains("react")) {
                pastProject = "In my previous contract, I delivered a responsive enterprise dashboard that increased user engagement metrics by 35% within the first month of launch.";
            } else {
                pastProject = "On a recent critical engagement, I took complete ownership of the primary deliverable and shipped it two weeks ahead of schedule with zero major post-launch defects.";
            }
        } else {
            pastProject = "Drawing from my past work: " + pastProject + ", I know exactly how to drive measurable outcomes.";
        }

        // Paragraph 1: Direct opening focusing on the job and mapping skills
        String paragraph1 = String.format(
            "Your requirement for a %s is exactly the kind of challenge I specialize in. You need someone capable of %s, and my deep background in %s and %s maps directly to those goals.",
            jobTitle, coreChallenge, skill1, skill2
        );

        // Paragraph 2: One concrete past project / proof point
        String paragraph2 = pastProject;

        // Paragraph 3: Client results focus
        String paragraph3 = "By bringing me onto this project, you get a dedicated partner who eliminates technical bottlenecks, requires minimal onboarding, and delivers production-ready work that aligns with your business objectives.";

        // Close: Strong CTA
        String cta = "Let's schedule a 20-minute call this week to discuss how I can immediately accelerate your timeline.\n\nBest,\n" + freelancerName;

        return paragraph1 + "\n\n" + paragraph2 + "\n\n" + paragraph3 + "\n\n" + cta;
    }

    private String safe(String val) {
        return val == null ? "" : val.trim();
    }
}
