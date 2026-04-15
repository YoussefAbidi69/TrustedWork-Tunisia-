package tn.esprit.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Post;
import tn.esprit.community.entity.Enum.PostStatus;
import tn.esprit.community.entity.Enum.PostType;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCommunityId(Long communityId);
    List<Post> findByType(PostType type);
    List<Post> findByStatus(PostStatus status);
    List<Post> findByCommunityIdAndTypeAndStatus(Long communityId, PostType type, PostStatus status);
}
