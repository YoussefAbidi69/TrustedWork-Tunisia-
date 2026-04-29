package tn.esprit.userservice.service;

import tn.esprit.userservice.dto.SuspensionRecordDTO;

import java.util.List;

public interface ISuspensionService {

    /**
     * Suspend un utilisateur avec un motif obligatoire.
     * Crée un SuspensionRecord et met à jour le statut User.
     */
    SuspensionRecordDTO suspendUser(Long userId, String reason, String adminEmail);

    /**
     * Lève la suspension d'un utilisateur.
     * Ferme le SuspensionRecord actif et réactive le User.
     */
    SuspensionRecordDTO liftSuspension(Long userId, String adminEmail);

    /**
     * Retourne l'historique complet des suspensions d'un utilisateur.
     */
    List<SuspensionRecordDTO> getHistory(Long userId);

    /**
     * Retourne toutes les suspensions actives sur la plateforme.
     */
    List<SuspensionRecordDTO> getAllActive();
}