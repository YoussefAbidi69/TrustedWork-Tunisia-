package tn.esprit.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.esprit.community.entity.Post;
import tn.esprit.community.service.impl.ValidationServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationServiceImplTest {

    private final ValidationServiceImpl validationService = new ValidationServiceImpl();

    @Test
    @DisplayName("shouldReturnFalse_whenPostIsNull")
    void shouldReturnFalse_whenPostIsNull() {
        assertThat(validationService.validate(null)).isFalse();
    }

    @Test
    @DisplayName("shouldReturnFalse_whenTitleIsBlank")
    void shouldReturnFalse_whenTitleIsBlank() {
        Post post = Post.builder().title(" ").content("content").build();

        assertThat(validationService.validate(post)).isFalse();
    }

    @Test
    @DisplayName("shouldReturnFalse_whenContentIsBlank")
    void shouldReturnFalse_whenContentIsBlank() {
        Post post = Post.builder().title("title").content(" ").build();

        assertThat(validationService.validate(post)).isFalse();
    }

    @Test
    @DisplayName("shouldReturnTrue_whenTitleAndContentArePresent")
    void shouldReturnTrue_whenTitleAndContentArePresent() {
        Post post = Post.builder().title("title").content("content").build();

        assertThat(validationService.validate(post)).isTrue();
    }
}
