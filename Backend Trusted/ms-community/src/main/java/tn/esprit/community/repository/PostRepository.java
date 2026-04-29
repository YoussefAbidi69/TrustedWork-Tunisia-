package tn.esprit.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Post;
import tn.esprit.community.entity.enums.PostStatus;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCommunityIdOrderByIdDesc(Long communityId);
    List<Post> findByCommunityIdAndStatusOrderByIdDesc(Long communityId, PostStatus status);
    List<Post> findByStatus(PostStatus status);
}
