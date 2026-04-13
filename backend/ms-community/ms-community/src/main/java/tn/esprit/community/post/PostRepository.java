package tn.esprit.community.post;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCommunityId(Long communityId);
    List<Post> findByType(PostType type);
    List<Post> findByStatus(PostStatus status);
    List<Post> findByCommunityIdAndTypeAndStatus(Long communityId, PostType type, PostStatus status);
}
