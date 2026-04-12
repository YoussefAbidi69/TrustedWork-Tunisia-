package tn.esprit.msprojectservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.DeliverableDTO;
import tn.esprit.msprojectservice.entities.Deliverable;
import tn.esprit.msprojectservice.entities.DeliverableStatus;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.entities.Task;
import tn.esprit.msprojectservice.repositories.IDeliverableRepository;
import tn.esprit.msprojectservice.repositories.IProjectRepository;
import tn.esprit.msprojectservice.repositories.ITaskRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeliverableServiceImpl implements IDeliverableService {

    @Autowired
    private IDeliverableRepository deliverableRepository;

    @Autowired
    private IProjectRepository projectRepository;

    @Autowired
    private ITaskRepository taskRepository;

    @Override
    public DeliverableDTO submitDeliverable(Long projectId, DeliverableDTO deliverableDTO) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + projectId));

        Deliverable deliverable = DeliverableDTO.toEntity(deliverableDTO);
        deliverable.setProject(project);

        // Si le livrable est lié à une tâche (optionnel)
        if (deliverableDTO.getTaskId() != null) {
            Task task = taskRepository.findById(deliverableDTO.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Tâche non trouvée avec l'id : " + deliverableDTO.getTaskId()));
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
                .collect(Collectors.toList());
    }

    @Override
    public DeliverableDTO getDeliverableById(Long id) {
        Deliverable deliverable = deliverableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livrable non trouvé avec l'id : " + id));
        return DeliverableDTO.fromEntity(deliverable);
    }

    @Override
    public DeliverableDTO reviewDeliverable(Long id, DeliverableStatus status, String reviewComment) {
        Deliverable deliverable = deliverableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livrable non trouvé avec l'id : " + id));

        // Vérifier que le livrable est en attente de review
        if (deliverable.getStatus() != DeliverableStatus.SUBMITTED) {
            throw new RuntimeException("Ce livrable a déjà été reviewé. Statut actuel : " + deliverable.getStatus());
        }

        // Seuls APPROVED ou REJECTED sont acceptés pour la review
        if (status != DeliverableStatus.APPROVED && status != DeliverableStatus.REJECTED) {
            throw new RuntimeException("Le statut de review doit être APPROVED ou REJECTED");
        }

        deliverable.setStatus(status);
        deliverable.setReviewComment(reviewComment);
        deliverable.setReviewedAt(LocalDateTime.now());

        Deliverable updated = deliverableRepository.save(deliverable);
        return DeliverableDTO.fromEntity(updated);
    }

    @Override
    public void deleteDeliverable(Long id) {
        Deliverable deliverable = deliverableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livrable non trouvé avec l'id : " + id));
        deliverableRepository.delete(deliverable);
    }
}