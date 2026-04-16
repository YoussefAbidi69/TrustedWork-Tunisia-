package tn.esprit.freelancerprofileservice.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO pour la mise à jour d'une configuration de scheduler.
 * Remplace l'entité JPA dans le @RequestBody du controller
 * pour éviter le risque de mass assignment (SonarQube S4684).
 *
 * Seuls les 3 champs modifiables sont exposés :
 *   - cronExpression  : nouvelle expression cron
 *   - intervalMinutes : nouvel intervalle en minutes
 *   - enabled         : activer / désactiver le job
 */
@Getter
@Setter
@NoArgsConstructor
public class SchedulerConfigRequest {

    private String  cronExpression;
    private Integer intervalMinutes;
    private Boolean enabled;
}
