package tn.esprit.msprojectservice.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.DeliverableDTO;
import tn.esprit.msprojectservice.entities.Deliverable;
import tn.esprit.msprojectservice.entities.DeliverableStatus;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.entities.Task;
import tn.esprit.msprojectservice.repositories.IDeliverableRepository;
import tn.esprit.msprojectservice.repositories.IProjectRepository;
import tn.esprit.msprojectservice.repositories.ITaskRepository;
import tn.esprit.msprojectservice.exceptions.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliverableServiceImpl implements IDeliverableService {

    private static final Logger logger = LoggerFactory.getLogger(DeliverableServiceImpl.class);

    private static final String LIVRABLE_NOT_FOUND = "Livrable non trouve avec l'id : ";
    private static final String PROJET_NOT_FOUND   = "Projet non trouve avec l'id : ";
    private static final String TACHE_NOT_FOUND    = "Tache non trouvee avec l'id : ";

    private final IDeliverableRepository deliverableRepository;
    private final IProjectRepository     projectRepository;
    private final ITaskRepository        taskRepository;

    // Injection du service mail -- Phase 2
    private final IMailService mailService;

    @Override
    public DeliverableDTO submitDeliverable(Long projectId, DeliverableDTO deliverableDTO) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException(PROJET_NOT_FOUND + projectId));

        Deliverable deliverable = DeliverableDTO.toEntity(deliverableDTO);
        deliverable.setProject(project);

        if (deliverableDTO.getTaskId() != null) {
            Task task = taskRepository.findById(deliverableDTO.getTaskId())
                    .orElseThrow(() -> new EntityNotFoundException(TACHE_NOT_FOUND + deliverableDTO.getTaskId()));
            deliverable.setTask(task);
        }

        Deliverable saved = deliverableRepository.save(deliverable);
        return DeliverableDTO.fromEntity(saved);
    }

    @Override
    public List<DeliverableDTO> getDeliverablesByProjectId(Long projectId) {
        return deliverableRepository.findByProjectId(projectId)
                .stream()
                .map(DeliverableDTO::fromEntity)
                .toList();
    }

    @Override
    public DeliverableDTO getDeliverableById(Long id) {
        Deliverable deliverable = deliverableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(LIVRABLE_NOT_FOUND + id));
        return DeliverableDTO.fromEntity(deliverable);
    }

    @Override
    public DeliverableDTO reviewDeliverable(Long id, DeliverableStatus status, String reviewComment) {
        Deliverable deliverable = deliverableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(LIVRABLE_NOT_FOUND + id));

        if (deliverable.getStatus() != DeliverableStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Ce livrable a deja ete reviewe. Statut actuel : " + deliverable.getStatus());
        }

        if (status != DeliverableStatus.APPROVED && status != DeliverableStatus.REJECTED) {
            throw new IllegalArgumentException("Le statut de review doit etre APPROVED ou REJECTED");
        }

        deliverable.setStatus(status);
        deliverable.setReviewComment(reviewComment);
        deliverable.setReviewedAt(LocalDateTime.now());

        Deliverable updated = deliverableRepository.save(deliverable);

        // MAILING -- Notifier le freelancer apres la decision du client
        // MailServiceImpl recupere lui-meme l'email via UserServiceClient
        try {
            mailService.envoyerNotificationReviewLivrable(updated);
            logger.info("Email review livrable declenche -- ID: {} | Statut: {}", updated.getId(), status);
        } catch (Exception e) {
            // Le mail ne bloque jamais la sauvegarde en base
            logger.error("Echec envoi email review livrable {} : {}", updated.getId(), e.getMessage());
        }

        return DeliverableDTO.fromEntity(updated);
    }

    @Override
    public void deleteDeliverable(Long id) {
        Deliverable deliverable = deliverableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(LIVRABLE_NOT_FOUND + id));
        deliverableRepository.delete(deliverable);
    }
}