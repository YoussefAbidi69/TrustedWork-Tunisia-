package tn.esprit.community.validation;

import org.springframework.stereotype.Service;
import tn.esprit.community.post.Post;

@Service
public class ValidationServiceImpl implements ValidationService {
    @Override
    public ValidationResult validate(Post post) {
        if (post.getContent() == null || post.getContent().isBlank()) {
            return ValidationResult.REJECTED;
        }
        if (post.getContent().length() < 50) {
            return ValidationResult.REJECTED;
        }
        if (post.isAiGenerated() && !post.isValidated()) {
            return ValidationResult.REJECTED;
        }
        return ValidationResult.APPROVED;
    }
}
