package tn.esprit.msprojectservice.services;

import tn.esprit.msprojectservice.dto.DeliverableDTO;
import tn.esprit.msprojectservice.entities.DeliverableStatus;

import java.util.List;

public interface IDeliverableService {

    DeliverableDTO submitDeliverable(Long projectId, DeliverableDTO deliverableDTO);

    List<DeliverableDTO> getDeliverablesByProjectId(Long projectId);

    DeliverableDTO getDeliverableById(Long id);

    DeliverableDTO reviewDeliverable(Long id, DeliverableStatus status, String reviewComment);

    void deleteDeliverable(Long id);
}