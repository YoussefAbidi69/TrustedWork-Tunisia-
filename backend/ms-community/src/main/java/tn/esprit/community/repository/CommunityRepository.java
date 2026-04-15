package tn.esprit.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Community;

public interface CommunityRepository extends JpaRepository<Community, Long> {
}
