package tn.esprit.msprojectservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("ThymeleafEmailConfig — Tests unitaires")
class ThymeleafEmailConfigTest {

    @Test
    @DisplayName("emailTemplateEngine — crée un SpringTemplateEngine non-null avec un resolver")
    void emailTemplateEngine_createsEngineWithResolver() {
        ThymeleafEmailConfig config = new ThymeleafEmailConfig();
        ApplicationContext ctx = mock(ApplicationContext.class);

        SpringTemplateEngine engine = config.emailTemplateEngine(ctx);

        assertThat(engine).isNotNull();
        assertThat(engine.getTemplateResolvers()).hasSize(1);
    }

    @Test
    @DisplayName("emailTemplateEngine — deux appels retournent des instances distinctes")
    void emailTemplateEngine_calledTwice_returnsDistinctInstances() {
        ThymeleafEmailConfig config = new ThymeleafEmailConfig();

        SpringTemplateEngine engine1 = config.emailTemplateEngine(mock(ApplicationContext.class));
        SpringTemplateEngine engine2 = config.emailTemplateEngine(mock(ApplicationContext.class));

        assertThat(engine1).isNotNull();
        assertThat(engine2).isNotNull();
        assertThat(engine1).isNotSameAs(engine2);
    }
}
