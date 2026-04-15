package tn.esprit.community.service.impl;

import org.springframework.stereotype.Service;
import tn.esprit.community.entity.Enum.PostType;
import tn.esprit.community.entity.Enum.ValidationResult;
import tn.esprit.community.entity.Post;
import tn.esprit.community.service.ValidationService;

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
        if (post.getType() == PostType.COURSE) {
            if (post.getFileUrl() == null || post.getFileUrl().isBlank()) {
                return ValidationResult.REJECTED;
            }
        }
        return ValidationResult.APPROVED;
    }
}
