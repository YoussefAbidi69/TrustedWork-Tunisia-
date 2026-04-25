package tn.esprit.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Block;

public interface BlockRepository extends JpaRepository<Block, Long> {
	List<Block> findBySection_IdOrderByOrderIndexAsc(Long sectionId);
}
