package tn.esprit.community.service.impl;

import org.springframework.stereotype.Service;
import tn.esprit.community.entity.Post;
import tn.esprit.community.service.ValidationService;

@Service
public class ValidationServiceImpl implements ValidationService {
    @Override
    public boolean validate(Post post) {
        return post != null
                && post.getTitle() != null
                && !post.getTitle().isBlank()
                && post.getContent() != null
                && !post.getContent().isBlank();
    }
}
