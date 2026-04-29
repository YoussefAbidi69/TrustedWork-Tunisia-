package tn.esprit.freelancerprofileservice.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.ISkillAuthenticityService;
import tn.esprit.freelancerprofileservice.services.ISkillGapService;
import tn.esprit.freelancerprofileservice.services.ISkillService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SkillController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ISkillService skillService;

    @MockBean
    private ISkillAuthenticityService authenticityService;

    @MockBean
    private ISkillGapService skillGapService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    private Skill mockSkill() {
        Skill s = new Skill();
        s.setId(1L);
        s.setName("Java");
        return s;
    }

    @Test
    void shouldGetSkills() throws Exception {
        when(skillService.getMySkills(1L)).thenReturn(List.of(mockSkill()));

        mockMvc.perform(get("/api/skills/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Java"));

        verify(skillService).getMySkills(1L);
    }

    @Test
    void shouldDeleteSkill() throws Exception {
        mockMvc.perform(delete("/api/skills/1/user/1"))
                .andExpect(status().isNoContent());

        verify(skillService).deleteSkill(1L, 1L);
    }

    @Test
    void shouldGetAuthenticityScore() throws Exception {
        when(authenticityService.calculateAuthenticityScore(1L)).thenReturn(90.0);

        mockMvc.perform(get("/api/skills/1/authenticity"))
                .andExpect(status().isOk())
                .andExpect(content().string("90.0"));

        verify(authenticityService).calculateAuthenticityScore(1L);
    }
}