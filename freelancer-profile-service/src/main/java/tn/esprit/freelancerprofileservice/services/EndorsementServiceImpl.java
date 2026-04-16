package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.freelancerprofileservice.entities.Endorsement;
import tn.esprit.freelancerprofileservice.entities.Notification;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.EndorsementRepository;
import tn.esprit.freelancerprofileservice.repositories.NotificationRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.util.List;

/**
 * Gestion des endorsements des compétences.
 * Après chaque endorsement valide :
 *   1. Persistance MySQL de la notification (récupérable après reconnexion)
 *   2. Envoi WebSocket temps réel vers le freelancer concerné
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EndorsementServiceImpl implements IEndorsementService {

    private final EndorsementRepository      endorsementRepository;
    private final SkillRepository            skillRepository;
    private final NotificationRepository     notificationRepository;
    private final SimpMessagingTemplate      messagingTemplate;
    private final ISkillAuthenticityService  skillAuthenticityService;
    private final ISkillService              skillService;

    @Override
    public Endorsement addEndorsement(Long skillId, Long endorserId, String comment) {
        // Bloquer doublon
        if (endorsementRepository.existsByEndorserIdAndSkillId(endorserId, skillId)) {
            throw new DuplicateResourceException("Vous avez déjà validé cette compétence");
        }

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", skillId));

        // Bloquer auto-endorsement
        if (skill.getProfile().getUserId().equals(endorserId)) {
            throw new InvalidDataException("Vous ne pouvez pas valider vos propres compétences");
        }

        String cleanedComment = cleanComment(comment);

        Endorsement endorsement = Endorsement.builder()
                .endorserId(endorserId)
                .skill(skill)
                .comment(cleanedComment)
                .build();

        Endorsement savedEndorsement = endorsementRepository.save(endorsement);

        // Mise à jour du compteur et recalcul des scores
        long endorsementCount = endorsementRepository.countBySkillId(skillId);
        skill.setEndorsementCount((int) endorsementCount);
        skillRepository.save(skill);

        skillAuthenticityService.calculateAuthenticityScore(skillId);
        skillService.upgradeSkillLevelIfEligible(skillId);

        // Notification au freelancer propriétaire du skill
        envoyerNotificationEndorsement(skill, endorserId, endorsementCount);

        return savedEndorsement;
    }

    /**
     * Persiste + envoie en temps réel la notification d'endorsement.
     * Destinataire : le freelancer propriétaire du skill endorsé.
     */
    private void envoyerNotificationEndorsement(Skill skill, Long endorserId, long endorsementCount) {
        Long freelancerUserId = skill.getProfile().getUserId();
        if (freelancerUserId == null) return;

        String skillName = skill.getName();
        String message   = "Votre compétence \"" + skillName + "\" a reçu un nouvel endorsement"
                + " (" + endorsementCount + " au total)";

        String payload = "{\"skillId\":"    + skill.getId()
                + ",\"skillName\":\"" + skillName + "\""
                + ",\"endorserId\":" + endorserId
                + ",\"total\":"      + endorsementCount + "}";

        // Étape 1 — Persistance MySQL
        Notification notification = Notification.builder()
                .userId(freelancerUserId)
                .type("NEW_ENDORSEMENT")
                .message(message)
                .payload(payload)
                .read(false)
                .build();

        notificationRepository.save(notification);

        // Étape 2 — WebSocket temps réel
        String topic = "/topic/user/" + freelancerUserId + "/notifications";

        messagingTemplate.convertAndSend(topic, java.util.Map.of(
                "type",        "NEW_ENDORSEMENT",
                "skillId",     skill.getId(),
                "skillName",   skillName,
                "endorserId",  endorserId,
                "total",       endorsementCount,
                "message",     message,
                "createdAt",   java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Endorsement> getEndorsementsBySkill(Long skillId) {
        if (!skillRepository.existsById(skillId)) {
            throw new ResourceNotFoundException("Skill", skillId);
        }
        return endorsementRepository.findBySkillIdOrderByEndorsedAtDesc(skillId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countEndorsements(Long skillId) {
        if (!skillRepository.existsById(skillId)) {
            throw new ResourceNotFoundException("Skill", skillId);
        }
        return endorsementRepository.countBySkillId(skillId);
    }

    private String cleanComment(String comment) {
        if (comment == null) return null;
        String cleaned = comment.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? null : cleaned;
    }
}