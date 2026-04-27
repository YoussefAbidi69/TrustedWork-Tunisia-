package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.userservice.dto.AgencyAnalyticsDto;
import tn.esprit.userservice.dto.AgencyContextDto;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.repository.IAgencyMemberRepository;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.repository.ITaskAssignmentRepository;
import tn.esprit.userservice.repository.ITaskRepository;
import tn.esprit.userservice.repository.UserRepository;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgencyServiceImpl implements IAgencyServices {

    private final IAgencyRepository agencyRepository;
    private final IAgencyMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final tn.esprit.userservice.repository.IAgencyInvitationRepository invitationRepository;
    private final tn.esprit.userservice.mapper.AgencyInvitationMapper invitationMapper;
    private final ITaskRepository taskRepository;
    private final ITaskAssignmentRepository taskAssignmentRepository;

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
        return agencyRepository.findByActiveTrue();
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

    private void requireLead(Long agencyId, Long userId) {
        if (userId == null) throw new RuntimeException("User ID is required for access control");
        AgencyMember member = memberRepository.findByAgencyIdAndUserId(agencyId, userId)
                .orElseThrow(() -> new RuntimeException("User is not a member of this agency"));
        if (member.getRole() != MemberRole.LEAD) {
            throw new RuntimeException("Only a LEAD can perform this action");
        }
    }

    @Override
    public void deleteAgency(Long id, Long requestingUserId) {
        requireLead(id, requestingUserId);
        agencyRepository.deleteById(id);
    }

    @Override
    public Agency updateAgency(Long id, Agency updatedAgency, Long requestingUserId) {
        requireLead(id, requestingUserId);
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

        boolean owns = memberships.stream()
            .anyMatch(m -> m.getRole() == MemberRole.LEAD);

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

        List<tn.esprit.userservice.dto.AgencyInvitationResponseDto> invites = invitationRepository.findByReceiverIdAndStatus(userId, InvitationStatus.PENDING)
            .stream().map(invitationMapper::toResponseDto).toList();

        return AgencyContextDto.builder()
            .hasMemberships(!memberships.isEmpty())
            .ownsAnAgency(owns)
            .memberships(summaries)
            .pendingInvitationCount(invites.size())
            .pendingInvitations(invites)
            .build();
    }

    @Override
    public List<User> getAvailableFreelancers(Long agencyId, Long requestingUserId, String skill, String search) {
        // Verify agency exists
        agencyRepository.findById(agencyId)
            .orElseThrow(() -> new RuntimeException("Agency not found"));
        
        // Verify LEAD role
        requireLead(agencyId, requestingUserId);

        return userRepository.findAvailableFreelancersForAgencyFiltered(agencyId, Role.FREELANCER, skill, search);
    }

    @Override
    public List<Agency> getMyAgencies(Long userId) {
        return memberRepository.findByUserId(userId)
            .stream()
            .map(AgencyMember::getAgency)
            .distinct()
            .toList();
    }

    @Override
    public AgencyAnalyticsDto getAgencyAnalytics(Long agencyId, Long requestingUserId) {
        // 1. Verify access
        requireLead(agencyId, requestingUserId);

        // 2. Task Counts
        long total = taskRepository.countByAgencyId(agencyId);
        long completed = taskRepository.countByAgencyIdAndStatus(agencyId, TaskStatus.TERMINE);
        long cancelled = taskRepository.countByAgencyIdAndStatus(agencyId, TaskStatus.ANNULE);

        // 3. Average Task Days (Lead Time)
        List<TaskAssignment> allAssignments = taskAssignmentRepository.findByMemberAgencyId(agencyId);
        double avgDays = allAssignments.stream()
            .filter(a -> a.getCompletedAt() != null)
            .mapToLong(a -> Duration.between(a.getAssignedAt(), a.getCompletedAt()).toDays())
            .average()
            .orElse(0.0);

        // 4. Top Members Ranking
        List<AgencyMember> members = memberRepository.findByAgencyId(agencyId);
        
        // Optimisation : Grouper les assignments par membre une seule fois
        java.util.Map<Long, List<TaskAssignment>> assignmentsByMember = allAssignments.stream()
            .collect(java.util.stream.Collectors.groupingBy(a -> a.getMember().getId()));

        List<AgencyAnalyticsDto.MemberRankingDto> ranking = members.stream()
            .<AgencyAnalyticsDto.MemberRankingDto>map(m -> {
                List<TaskAssignment> mAssignments = assignmentsByMember.getOrDefault(m.getId(), java.util.Collections.emptyList());
                
                double avgScore = mAssignments.stream()
                    .mapToDouble(TaskAssignment::getCompletionScore)
                    .average()
                    .orElse(0.0);
                
                long count = mAssignments.stream()
                    .filter(a -> a.getCompletedAt() != null)
                    .count();

                return AgencyAnalyticsDto.MemberRankingDto.builder()
                    .memberId(m.getId())
                    .fullName(m.getUser().getFirstName() + " " + m.getUser().getLastName())
                    .avatarUrl(m.getUser().getPhoto())
                    .averageCompletionScore(avgScore)
                    .completedTaskCount(count)
                    .build();
            })
            .sorted(java.util.Comparator.comparingDouble(AgencyAnalyticsDto.MemberRankingDto::getAverageCompletionScore).reversed())
            .limit(10)
            .collect(java.util.stream.Collectors.toList());

        return AgencyAnalyticsDto.builder()
            .totalTasks(total)
            .completedTasks(completed)
            .cancelledTasks(cancelled)
            .averageTaskDays(avgDays)
            .topMembers(ranking)
            .build();
    }
}