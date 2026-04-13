package tn.esprit.community.validation;

import tn.esprit.community.post.Post;

public interface ValidationService {
    ValidationResult validate(Post post);
}
