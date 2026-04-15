package tn.esprit.community.service.impl;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.community.dto.lms.ProgressDTO;
import tn.esprit.community.dto.lms.ProgressWriteDTO;
import tn.esprit.community.entity.LessonProgress;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.LessonProgressRepository;
import tn.esprit.community.repository.LessonRepository;
import tn.esprit.community.service.LearningProgressService;

@Service
public class LearningProgressServiceImpl implements LearningProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;

    public LearningProgressServiceImpl(
            LessonProgressRepository lessonProgressRepository, LessonRepository lessonRepository) {
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonRepository = lessonRepository;
    }

    @Override
    public ProgressDTO getProgress(Long userId, Long lessonId) {
        return lessonProgressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .map(this::toDto)
                .orElseGet(
                        () -> ProgressDTO.builder()
                                .userId(userId)
                                .lessonId(lessonId)
                                .completed(false)
                                .completedAt(null)
                                .build());
    }

    @Override
    @Transactional
    public ProgressDTO saveProgress(ProgressWriteDTO dto) {
        if (dto.getUserId() == null || dto.getLessonId() == null) {
            throw new LearningNotFoundException("userId and lessonId are required");
        }
        lessonRepository.findById(dto.getLessonId()).orElseThrow(() -> new LearningNotFoundException("Lesson not found"));
        LessonProgress entity = lessonProgressRepository
                .findByUserIdAndLessonId(dto.getUserId(), dto.getLessonId())
                .orElseGet(
                        () -> LessonProgress.builder()
                                .userId(dto.getUserId())
                                .lessonId(dto.getLessonId())
                                .completed(false)
                                .build());
        entity.setCompleted(dto.isCompleted());
        entity.setCompletedAt(dto.isCompleted() ? Instant.now() : null);
        return toDto(lessonProgressRepository.save(entity));
    }

    private ProgressDTO toDto(LessonProgress p) {
        return ProgressDTO.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .lessonId(p.getLessonId())
                .completed(p.isCompleted())
                .completedAt(p.getCompletedAt())
                .build();
    }
}
