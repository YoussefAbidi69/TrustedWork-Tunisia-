package tn.esprit.mscontractservicee.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.DeliveryProofSubmitRequest;
import tn.esprit.mscontractservicee.dto.ai.MilestoneAiPromptRequest;
import tn.esprit.mscontractservicee.dto.ai.MilestoneAiResponse;
import tn.esprit.mscontractservicee.dto.milestone.MilestoneCreateRequest;
import tn.esprit.mscontractservicee.dto.milestone.MilestoneResponse;
import tn.esprit.mscontractservicee.dto.milestone.MilestoneUpdateRequest;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.MilestoneStatus;
import tn.esprit.mscontractservicee.service.IContractAiGenerationService;
import tn.esprit.mscontractservicee.service.IContractService;
import tn.esprit.mscontractservicee.service.IDeliveryProofService;
import tn.esprit.mscontractservicee.service.IMilestoneService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneControllerTest {

    @Mock
    private IMilestoneService milestoneService;

    @Mock
    private IContractService contractService;

    @Mock
    private IDeliveryProofService deliveryProofService;

    @Mock
    private IContractAiGenerationService contractAiService;

    @InjectMocks
    private MilestoneController controller;

    private static Authentication auth(long cin, String... roles) {
        List<SimpleGrantedAuthority> authorities = roles == null
                ? List.of()
                : java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(String.valueOf(cin), "n/a", authorities);
    }

    private static Contract signedActiveContract(Long id, Long clientCin, Long freelancerCin) {
        Contract c = new Contract();
        c.setId(id);
        c.setClientCin(clientCin);
        c.setFreelancerCin(freelancerCin);
        c.setStatus(ContractStatus.ACTIVE);
        c.setDateSignature(LocalDateTime.now());
        return c;
    }

    @Test
    void testHealthTestEndpoint() {
        ResponseEntity<Map<String, String>> res = controller.test();
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertEquals("OK", res.getBody().get("status"));
    }

    @Test
    void testGenerateMilestoneDraft_DelegatesToAiService() {
        MilestoneAiPromptRequest req = new MilestoneAiPromptRequest();
        req.setPrompt("prompt");

        MilestoneAiResponse ai = MilestoneAiResponse.builder()
                .titre("t")
                .montant(new BigDecimal("10"))
                .build();
        when(contractAiService.generateMilestoneDraft(any())).thenReturn(ai);

        ResponseEntity<MilestoneAiResponse> res = controller.generateMilestoneDraft(req);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("t", res.getBody().getTitre());
    }

    @Test
    void testCreateMilestone_CreatedForOwnerInDraftContract() {
        MilestoneCreateRequest req = new MilestoneCreateRequest();
        req.setContractId(1L);
        req.setTitre("M1");
        req.setMontant(new BigDecimal("100"));

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setStatus(ContractStatus.DRAFT);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        Milestone saved = new Milestone();
        saved.setId(10L);
        saved.setContractId(1L);
        saved.setTitre("M1");
        saved.setMontant(new BigDecimal("100"));
        saved.setStatus(MilestoneStatus.PENDING);
        when(milestoneService.createMilestone(any(Milestone.class))).thenReturn(saved);

        ResponseEntity<MilestoneResponse> res = controller.createMilestone(auth(100, "ROLE_CLIENT"), req);
        assertEquals(201, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertEquals(10L, res.getBody().getId());
    }

    @Test
    void testCreateMilestone_ForbiddenForNonOwner() {
        MilestoneCreateRequest req = new MilestoneCreateRequest();
        req.setContractId(1L);

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setStatus(ContractStatus.DRAFT);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        Authentication authentication = auth(999, "ROLE_CLIENT");
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.createMilestone(authentication, req);
            }
        });
    }

    @Test
    void testGetMilestoneById_ForbiddenWhenNotParticipant() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        Authentication authentication = auth(999, "ROLE_CLIENT");
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.getMilestoneById(authentication, 10L);
            }
        });
    }

    @Test
    void testGetMilestoneById_AllowsClientParticipant() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        ResponseEntity<Milestone> res = controller.getMilestoneById(auth(100, "ROLE_CLIENT"), 10L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(10L, res.getBody().getId());
    }

    @Test
    void testGetDeliveryProofForMilestone_NotFoundWhenAbsent() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));

        Contract contract = signedActiveContract(1L, 100L, 200L);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        when(deliveryProofService.findForMilestone(10L)).thenReturn(Optional.empty());

        Authentication authentication = auth(100, "ROLE_CLIENT");
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.getDeliveryProofForMilestone(authentication, 10L);
            }
        });
    }

    @Test
    void testGetAllMilestones_AdminDelegatesToService() {
        Page<Milestone> page = Page.empty(PageRequest.of(0, 10));
        when(milestoneService.findAll(any())).thenReturn(page);

        ResponseEntity<Page<Milestone>> res = controller.getAllMilestones(auth(1, "ROLE_ADMIN"), 0, 10);
        assertEquals(200, res.getStatusCode().value());
        verify(milestoneService).findAll(any());
    }

    @Test
    void testGetMyMilestones_ClientBranch() {
        when(milestoneService.findForClientCin(100L)).thenReturn(List.of(new Milestone()));
        ResponseEntity<List<Milestone>> res = controller.getMyMilestones(auth(100, "ROLE_CLIENT"));
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void testUpdateMilestone_RejectsWhenContractNotDraft() {
        Milestone existing = new Milestone();
        existing.setId(10L);
        existing.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(existing));

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        Authentication authentication = auth(100, "ROLE_CLIENT");
        MilestoneUpdateRequest req = new MilestoneUpdateRequest();
        assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.updateMilestone(authentication, 10L, req);
            }
        });
    }

    @Test
    void testStartMilestone_FreelancerAllowedOnSignedActiveContract() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));

        when(contractService.findById(1L)).thenReturn(Optional.of(signedActiveContract(1L, 100L, 200L)));

        Milestone started = new Milestone();
        started.setId(10L);
        started.setStatus(MilestoneStatus.IN_PROGRESS);
        when(milestoneService.startMilestone(10L)).thenReturn(started);

        ResponseEntity<Milestone> res = controller.startMilestone(auth(200, "ROLE_FREELANCER"), 10L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(MilestoneStatus.IN_PROGRESS, res.getBody().getStatus());
    }

    @Test
    void testSubmitMilestone_NoProof_DelegatesToSubmit() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractService.findById(1L)).thenReturn(Optional.of(signedActiveContract(1L, 100L, 200L)));

        Milestone submitted = new Milestone();
        submitted.setId(10L);
        submitted.setStatus(MilestoneStatus.SUBMITTED);
        when(milestoneService.submitMilestone(10L)).thenReturn(submitted);

        ResponseEntity<Milestone> res = controller.submitMilestone(auth(200, "ROLE_FREELANCER"), 10L, null);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(MilestoneStatus.SUBMITTED, res.getBody().getStatus());
    }

    @Test
    void testSubmitMilestone_WithProof_DelegatesToSubmitWithProof() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractService.findById(1L)).thenReturn(Optional.of(signedActiveContract(1L, 100L, 200L)));

        DeliveryProofSubmitRequest proof = new DeliveryProofSubmitRequest(
                "file1.txt",
                "https://demo",
                "https://repo",
                "comment",
                "md5"
        );

        Milestone submitted = new Milestone();
        submitted.setId(10L);
        submitted.setStatus(MilestoneStatus.SUBMITTED);
        when(milestoneService.submitMilestoneWithProof(eq(10L), any())).thenReturn(submitted);

        ResponseEntity<Milestone> res = controller.submitMilestone(auth(200, "ROLE_FREELANCER"), 10L, proof);
        assertEquals(200, res.getStatusCode().value());
        verify(milestoneService).submitMilestoneWithProof(eq(10L), any());
    }

    @Test
    void testApproveMilestone_ClientAllowedWhenSigned() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractService.findById(1L)).thenReturn(Optional.of(signedActiveContract(1L, 100L, 200L)));

        Milestone approved = new Milestone();
        approved.setId(10L);
        approved.setStatus(MilestoneStatus.APPROVED);
        when(milestoneService.approveMilestone(10L, 100L)).thenReturn(approved);

        ResponseEntity<Milestone> res = controller.approveMilestone(auth(100, "ROLE_CLIENT"), 10L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(MilestoneStatus.APPROVED, res.getBody().getStatus());
    }

    @Test
    void testSubmitMilestone_NoProof_WhenFlagEnabled_ThrowsBadRequest() {
        ReflectionTestUtils.setField(controller, "requireDeliveryProofOnSubmit", true);

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractService.findById(1L)).thenReturn(Optional.of(signedActiveContract(1L, 100L, 200L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.submitMilestone(auth(200, "ROLE_FREELANCER"), 10L, null);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testApproveMilestone_RequiresProof_WhenFlagEnabledAndMissing_ThrowsBadRequest() {
        ReflectionTestUtils.setField(controller, "requireDeliveryProofOnApprove", true);

        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractService.findById(1L)).thenReturn(Optional.of(signedActiveContract(1L, 100L, 200L)));
        when(deliveryProofService.findForMilestone(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.approveMilestone(auth(100, "ROLE_CLIENT"), 10L);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testRejectMilestone_Delegates() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractService.findById(1L)).thenReturn(Optional.of(signedActiveContract(1L, 100L, 200L)));

        Milestone rejected = new Milestone();
        rejected.setId(10L);
        rejected.setStatus(MilestoneStatus.REJECTED);
        when(milestoneService.rejectMilestone(eq(10L), eq("reason"), any())).thenReturn(rejected);

        LocalDate newDeadline = LocalDate.now().plusDays(2);
        ResponseEntity<Milestone> res = controller.rejectMilestone(auth(100, "ROLE_CLIENT"), 10L, "reason", newDeadline);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(MilestoneStatus.REJECTED, res.getBody().getStatus());
    }

    @Test
    void testUpdateRejectedMilestoneDeadline_Delegates() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractService.findById(1L)).thenReturn(Optional.of(signedActiveContract(1L, 100L, 200L)));

        LocalDate newDeadline = LocalDate.now().plusDays(5);
        Milestone updated = new Milestone();
        updated.setId(10L);
        updated.setDeadline(newDeadline);
        when(milestoneService.updateRejectedMilestoneDeadline(10L, newDeadline)).thenReturn(updated);

        ResponseEntity<Milestone> res = controller.updateRejectedMilestoneDeadline(auth(100, "ROLE_CLIENT"), 10L, newDeadline);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(newDeadline, res.getBody().getDeadline());
    }

    @Test
    void testAutoApproveMilestone_RequiresProof_WhenFlagEnabledAndMissing_ThrowsBadRequest() {
        ReflectionTestUtils.setField(controller, "requireDeliveryProofOnApprove", true);
        when(deliveryProofService.findForMilestone(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.autoApproveMilestone(auth(1, "ROLE_ADMIN"), 10L);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testGetMyMilestones_FreelancerBranch() {
        when(milestoneService.findForSignedFreelancerCin(200L)).thenReturn(List.of(new Milestone()));
        ResponseEntity<List<Milestone>> res = controller.getMyMilestones(auth(200, "ROLE_FREELANCER"));
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void testGetMyMilestones_UnsupportedRole_ThrowsForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.getMyMilestones(auth(1, "ROLE_OTHER"));
            }
        });
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void testDeleteMilestone_AdminDelegates() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setStatus(ContractStatus.DRAFT);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        doNothing().when(milestoneService).deleteMilestone(10L);

        ResponseEntity<Void> res = controller.deleteMilestone(auth(1, "ROLE_ADMIN"), 10L);
        assertEquals(204, res.getStatusCode().value());
        verify(milestoneService).deleteMilestone(10L);
    }

    @Test
    void testDeleteMilestone_RejectsWhenContractNotDraft() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        milestone.setStatus(MilestoneStatus.PENDING);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));

        Contract contract = signedActiveContract(1L, 100L, 200L);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.deleteMilestone(auth(100, "ROLE_CLIENT"), 10L);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testDeleteMilestone_ClientRejectsWhenMilestoneNotPending() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setStatus(ContractStatus.DRAFT);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.deleteMilestone(auth(100, "ROLE_CLIENT"), 10L);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testGetMilestonesByContract_AdminDelegates() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setDateSignature(LocalDateTime.now());
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        when(milestoneService.findByContractId(1L)).thenReturn(List.of(new Milestone()));
        ResponseEntity<List<Milestone>> res = controller.getMilestonesByContract(auth(1, "ROLE_ADMIN"), 1L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
    }
    @Test
    void testUpdateMilestoneStatus_AdminAllowed() {
        Milestone m = new Milestone();
        m.setId(10L);
        m.setStatus(MilestoneStatus.IN_PROGRESS);
        when(milestoneService.updateStatus(10L, MilestoneStatus.APPROVED)).thenReturn(m);

        ResponseEntity<Milestone> res = controller.updateMilestoneStatus(auth(1, "ROLE_ADMIN"), 10L, MilestoneStatus.APPROVED);
        assertEquals(200, res.getStatusCode().value());
        verify(milestoneService).updateStatus(10L, MilestoneStatus.APPROVED);
    }
    @Test
    void testCreateMilestone_ContractNotDraft_ThrowsBadRequest() {
        MilestoneCreateRequest req = new MilestoneCreateRequest();
        req.setContractId(1L);

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setStatus(ContractStatus.ACTIVE); // Not DRAFT
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.createMilestone(auth(100, "ROLE_CLIENT"), req);
            }
        });
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testCreateMilestone_AdminBypasses_OwnerCheck() {
        MilestoneCreateRequest req = new MilestoneCreateRequest();
        req.setContractId(1L);
        req.setTitre("Admin Milestone");
        req.setMontant(new BigDecimal("200"));

        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(999L); // Different client
        contract.setStatus(ContractStatus.DRAFT);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        Milestone saved = new Milestone();
        saved.setId(5L);
        saved.setContractId(1L);
        saved.setTitre("Admin Milestone");
        saved.setMontant(new BigDecimal("200"));
        saved.setStatus(MilestoneStatus.PENDING);
        when(milestoneService.createMilestone(any())).thenReturn(saved);

        ResponseEntity<MilestoneResponse> res = controller.createMilestone(auth(1, "ROLE_ADMIN"), req);
        assertEquals(201, res.getStatusCode().value());
        assertEquals(5L, res.getBody().getId());
    }

    @Test
    void testGetMilestonesByContract_ForbiddenForNonParticipant() {
        Contract contract = new Contract();
        contract.setId(1L);
        contract.setClientCin(100L);
        contract.setFreelancerCin(200L);
        contract.setStatus(ContractStatus.ACTIVE);
        when(contractService.findById(1L)).thenReturn(Optional.of(contract));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.getMilestonesByContract(auth(999, "ROLE_CLIENT"), 1L);
            }
        });
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void testStartMilestone_ForbiddenForNonFreelancer() {
        Milestone milestone = new Milestone();
        milestone.setId(10L);
        milestone.setContractId(1L);
        when(milestoneService.findById(10L)).thenReturn(Optional.of(milestone));
        when(contractService.findById(1L)).thenReturn(Optional.of(signedActiveContract(1L, 100L, 200L)));

        // CIN 999 is not the freelancer (200)
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, new Executable() {
            @Override
            public void execute() {
                controller.startMilestone(auth(999, "ROLE_FREELANCER"), 10L);
            }
        });
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void testAutoApproveMilestone_Success() {
        ReflectionTestUtils.setField(controller, "requireDeliveryProofOnApprove", false);

        Milestone approved = new Milestone();
        approved.setId(10L);
        approved.setStatus(MilestoneStatus.AUTO_APPROVED);
        when(milestoneService.autoApproveMilestone(10L, 1L)).thenReturn(approved);

        ResponseEntity<Milestone> res = controller.autoApproveMilestone(auth(1, "ROLE_ADMIN"), 10L);
        assertEquals(200, res.getStatusCode().value());
        assertEquals(MilestoneStatus.AUTO_APPROVED, res.getBody().getStatus());
    }
}
