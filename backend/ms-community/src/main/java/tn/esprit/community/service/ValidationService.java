package tn.esprit.community.service;

import tn.esprit.community.entity.Enum.ValidationResult;
import tn.esprit.community.entity.Post;

public interface ValidationService {
    ValidationResult validate(Post post);
}
