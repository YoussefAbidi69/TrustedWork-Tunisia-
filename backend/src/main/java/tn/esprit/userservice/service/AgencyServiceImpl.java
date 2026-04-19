package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.userservice.dto.AgencyContextDto;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.repository.IAgencyMemberRepository;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgencyServiceImpl implements IAgencyServices {

    private final IAgencyRepository agencyRepository;
    private final IAgencyMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Agency createAgency(Agency agency, Long creatorId) {
        System.out.println("[AgencyService] createAgency - creatorId=" + creatorId + " name=" + agency.getName());

        if (creatorId == null) {
            throw new RuntimeException("creatorId est requis pour créer une agence.");
        }

        if (agencyRepository.existsByName(agency.getName())) {
            throw new RuntimeException("Une agence avec ce nom existe déjà. Veuillez choisir un autre nom.");
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable (id=" + creatorId + "). Veuillez vous reconnecter."));

        agency.setCreatedBy(creator);
        // Fallback for legacy DB column
        agency.setOwnerId(creator.getId()); 
        agency.setTier(AgencyTier.STARTER);
        agency.setActive(true);

        Agency savedAgency = agencyRepository.save(agency);
        System.out.println("[AgencyService] Agency saved with id=" + savedAgency.getId());

        // ── Automatic LEAD membership for the creator ─────────────────────────
        AgencyMember leadMember = AgencyMember.builder()
                .agency(savedAgency)
                .user(creator)
                .role(MemberRole.LEAD)
                .status(MemberStatus.ACTIVE)
                .workloadScore(0f)
                .build();

        memberRepository.save(leadMember);
        System.out.println("[AgencyService] LEAD membership created for userId=" + creatorId);

        return savedAgency;
    }

    @Override
    public List<Agency> getAllAgencies() {
        return agencyRepository.findAll();
    }

    @Override
    public Agency getAgencyById(Long id) {
        return agencyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agency not found"));
    }

    @Override
    public List<Agency> getAgenciesByCreator(Long creatorId) {
        return agencyRepository.findByCreatedById(creatorId);
    }

    @Override
    public void deleteAgency(Long id) {
        agencyRepository.deleteById(id);
    }

    @Override
    public Agency updateAgency(Long id, Agency updatedAgency) {
        Agency existingAgency = agencyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        if (updatedAgency.getName() != null && !updatedAgency.getName().equals(existingAgency.getName())) {
            if (agencyRepository.existsByName(updatedAgency.getName())) {
                throw new RuntimeException("Agency name already exists");
            }
            existingAgency.setName(updatedAgency.getName());
        }

        if (updatedAgency.getDescription() != null) {
            existingAgency.setDescription(updatedAgency.getDescription());
        }

        if (updatedAgency.getCountry() != null) {
            existingAgency.setCountry(updatedAgency.getCountry());
        }

        if (updatedAgency.getCity() != null) {
            existingAgency.setCity(updatedAgency.getCity());
        }

        if (updatedAgency.getActive() != null) {
            existingAgency.setActive(updatedAgency.getActive());
        }

        return agencyRepository.save(existingAgency);
    }

    @Override
    public AgencyContextDto getMyAgencyContext(Long userId) {
        List<AgencyMember> memberships = memberRepository.findByUserId(userId);

        List<AgencyContextDto.AgencyMembershipSummary> summaries = memberships.stream()
            .map(m -> AgencyContextDto.AgencyMembershipSummary.builder()
                .agencyId(m.getAgency().getId())
                .agencyName(m.getAgency().getName())
                .logoUrl(m.getAgency().getLogoUrl())
                .role(m.getRole().name())
                .status(m.getStatus().name())
                .joinedAt(m.getJoinedAt().toString())
                .build())
            .toList();

        return AgencyContextDto.builder()
            .hasMemberships(!memberships.isEmpty())
            .memberships(summaries)
            .build();
    }

    @Override
    public List<User> getAvailableFreelancers(Long agencyId) {
        // Verify agency exists
        agencyRepository.findById(agencyId)
            .orElseThrow(() -> new RuntimeException("Agency not found"));

        return userRepository.findAvailableFreelancersForAgency(agencyId, Role.FREELANCER);
    }

    @Override
    public List<Agency> getMyAgencies(Long userId) {
        return memberRepository.findByUserId(userId)
            .stream()
            .map(AgencyMember::getAgency)
            .distinct()
            .toList();
    }
}