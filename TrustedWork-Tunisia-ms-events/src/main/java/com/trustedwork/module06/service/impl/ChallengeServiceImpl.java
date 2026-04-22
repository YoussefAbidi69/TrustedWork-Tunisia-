package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.ChallengeDTO;
import com.trustedwork.module06.entity.Challenge;
import com.trustedwork.module06.entity.ChallengeParticipation;
import com.trustedwork.module06.enums.ChallengeStatus;
import com.trustedwork.module06.enums.ParticipationStatus;
import com.trustedwork.module06.mapper.ChallengeMapper;
import com.trustedwork.module06.repository.ChallengeParticipationRepository;
import com.trustedwork.module06.repository.ChallengeRepository;
import com.trustedwork.module06.repository.EventRegistrationRepository;
import com.trustedwork.module06.repository.UserBadgeRepository;
import com.trustedwork.module06.service.ChallengeService;
import com.trustedwork.module06.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipationRepository participationRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final GamificationService gamificationService;

    @Override
    public List<ChallengeDTO> getAllChallenges() {
        return challengeRepository.findAll().stream()
                .map(ChallengeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChallengeDTO> getActiveChallenges(Long userId) {
        List<Challenge> challenges = challengeRepository.findByStatus(ChallengeStatus.ACTIVE);
        List<ChallengeParticipation> participations = participationRepository.findByUserId(userId);

        return challenges.stream().map(c -> {
            ChallengeDTO dto = ChallengeMapper.toDTO(c);
            Optional<ChallengeParticipation> p = participations.stream()
                    .filter(part -> part.getChallenge().getId().equals(c.getId()))
                    .findFirst();
            dto.setCurrentParticipation(p.map(ChallengeMapper::toParticipationDTO).orElse(null));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public ChallengeDTO createChallenge(ChallengeDTO challengeDTO) {
        Challenge challenge = ChallengeMapper.toEntity(challengeDTO);
        if (challenge.getStatus() == null) challenge.setStatus(ChallengeStatus.ACTIVE);
        return ChallengeMapper.toDTO(challengeRepository.save(challenge));
    }

    @Override
    @Transactional
    public ChallengeDTO updateChallenge(Long id, ChallengeDTO challengeDTO) {
        Challenge existing = challengeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));
        
        Challenge updated = ChallengeMapper.toEntity(challengeDTO);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setXpReward(updated.getXpReward());
        existing.setDeadline(updated.getDeadline());
        existing.setStatus(updated.getStatus());
        existing.setChallengeTypeCode(updated.getChallengeTypeCode());

        return ChallengeMapper.toDTO(challengeRepository.save(existing));
    }

    @Override
    public void deleteChallenge(Long id) {
        challengeRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void joinChallenge(Long userId, Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));
        
        participationRepository.findByUserIdAndChallengeId(userId, challengeId).ifPresent(p -> {
            throw new RuntimeException("Vous avez déjà rejoint cette mission");
        });

        ChallengeParticipation p = ChallengeParticipation.builder()
                .userId(userId)
                .challenge(challenge)
                .status(ParticipationStatus.JOINED)
                .build();
        participationRepository.save(p);
    }

    @Override
    @Transactional
    public void succeedChallenge(Long userId, Long challengeId) {
        ChallengeParticipation p = participationRepository.findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new RuntimeException("Participation non trouvée"));
        
        if (p.getStatus() != ParticipationStatus.JOINED) {
            throw new RuntimeException("Cette mission n'est pas en cours");
        }

        Challenge c = p.getChallenge();
        String typeCode = c.getChallengeTypeCode();

        // --- REAL VERIFICATION ENGINE ---
        boolean isVerified = false;

        if (typeCode == null || typeCode.isEmpty() || typeCode.equals("MANUAL")) {
            // Permettre la validation manuelle pour les types non configurés
            isVerified = true;
        } else if (typeCode.equals("REG_EVENT")) {
            // Vérifier si l'utilisateur est inscrit à au moins un événement
            isVerified = !eventRegistrationRepository.findByUserId(userId).isEmpty();
            if (!isVerified) throw new RuntimeException("Action requise : Vous devez vous inscrire à au moins un événement !");
        } else if (typeCode.equals("FIRST_BADGE")) {
            // Vérifier si l'utilisateur possède au moins un badge
            isVerified = !userBadgeRepository.findByUserId(userId).isEmpty();
            if (!isVerified) throw new RuntimeException("Action requise : Vous devez gagner au moins un badge d'abord !");
        } else {
            // Par défaut, si le code est inconnu
            isVerified = true; 
        }

        if (isVerified) {
            p.setStatus(ParticipationStatus.SUCCESS);
            p.setCompletedAt(java.time.LocalDateTime.now());
            participationRepository.save(p);
        }
    }

    @Override
    @Transactional
    public void claimReward(Long userId, Long challengeId) {
        ChallengeParticipation p = participationRepository.findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new RuntimeException("Participation non trouvée"));
        
        if (p.getStatus() != ParticipationStatus.SUCCESS) {
            throw new RuntimeException("La mission n'est pas encore réussie");
        }

        Challenge c = p.getChallenge();
        gamificationService.addXp(userId, c.getXpReward(), "Récompense Mission : " + c.getTitle());
        
        p.setStatus(ParticipationStatus.CLAIMED);
        participationRepository.save(p);
    }
}
