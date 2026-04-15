package tn.esprit.community.service;

import tn.esprit.community.dto.lms.ProgressDTO;
import tn.esprit.community.dto.lms.ProgressWriteDTO;

public interface LearningProgressService {

    ProgressDTO getProgress(Long userId, Long lessonId);

    ProgressDTO saveProgress(ProgressWriteDTO dto);
}
