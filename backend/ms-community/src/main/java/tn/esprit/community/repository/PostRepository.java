package tn.esprit.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Post;
import tn.esprit.community.entity.Enum.PostStatus;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCommunity_IdOrderByIdDesc(Long communityId);
    List<Post> findByCommunity_IdAndStatusOrderByIdDesc(Long communityId, PostStatus status);
    List<Post> findByStatus(PostStatus status);
}
