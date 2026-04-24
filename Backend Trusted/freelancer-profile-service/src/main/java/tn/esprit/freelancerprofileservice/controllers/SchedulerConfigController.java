package tn.esprit.freelancerprofileservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.SchedulerConfigRequest;
import tn.esprit.freelancerprofileservice.entities.SchedulerConfig;
import tn.esprit.freelancerprofileservice.scheduler.ProfileScheduler;
import tn.esprit.freelancerprofileservice.services.ISchedulerConfigService;

import java.util.List;
import java.util.Map;

/**
 * Controller REST pour la gestion des configurations de schedulers.
 *
 * Endpoints exposés :
 *   GET  /api/scheduler/config              → liste toutes les configs
 *   GET  /api/scheduler/config/{jobName}    → détail d'un job
 *   PUT  /api/scheduler/config/{jobName}    → modifier cron/interval/enabled d'un job
 *   POST /api/scheduler/config/{jobName}/run→ déclencher manuellement un job (test / démo)
 *
 * Accessible depuis le backoffice Angular (port 4201).
 */
@RestController
@RequestMapping("/api/scheduler/config")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4201")
public class SchedulerConfigController {

    private final ISchedulerConfigService schedulerConfigService;

    // Injecté pour permettre le déclenchement manuel depuis le backoffice
    private final ProfileScheduler profileScheduler;

    /**
     * GET /api/scheduler/config
     * Retourne la liste complète des configurations de schedulers.
     *
     * @return liste des SchedulerConfig (JSON)
     */
    @GetMapping
    public ResponseEntity<List<SchedulerConfig>> getAllConfigs() {
        log.info("[API] GET /api/scheduler/config — lecture de toutes les configs");
        List<SchedulerConfig> configs = schedulerConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * GET /api/scheduler/config/{jobName}
     * Retourne la configuration d'un job spécifique.
     *
     * @param jobName nom du job (ex: "recalculateAllSkillScores")
     * @return SchedulerConfig correspondant (JSON)
     */
    @GetMapping("/{jobName}")
    public ResponseEntity<SchedulerConfig> getConfigByJobName(@PathVariable String jobName) {
        log.info("[API] GET /api/scheduler/config/{}", jobName);
        SchedulerConfig config = schedulerConfigService.getConfigByJobName(jobName);
        return ResponseEntity.ok(config);
    }

    /**
     * PUT /api/scheduler/config/{jobName}
     * Met à jour la configuration d'un job existant.
     * Corps de la requête (JSON) :
     * {
     *   "cronExpression": "0 0 2 * * *",
     *   "intervalMinutes": 1440,
     *   "enabled": true
     * }
     *
     * Utilise un DTO (SchedulerConfigRequest) et non l'entité directement
     * pour éviter le risque de mass assignment (SonarQube S4684).
     *
     * @param jobName nom du job à modifier
     * @param request DTO contenant les nouveaux paramètres
     * @return SchedulerConfig mis à jour (JSON)
     */
    @PutMapping("/{jobName}")
    public ResponseEntity<SchedulerConfig> updateConfig(
            @PathVariable String jobName,
            @RequestBody SchedulerConfigRequest request) {

        log.info("[API] PUT /api/scheduler/config/{} — cron={}, interval={}min, enabled={}",
                jobName,
                request.getCronExpression(),
                request.getIntervalMinutes(),
                request.getEnabled());

        // Mapping DTO → entité partielle pour le service
        SchedulerConfig patch = new SchedulerConfig();
        patch.setCronExpression(request.getCronExpression());
        patch.setIntervalMinutes(request.getIntervalMinutes());
        patch.setEnabled(request.getEnabled());

        SchedulerConfig saved = schedulerConfigService.updateConfig(jobName, patch);
        return ResponseEntity.ok(saved);
    }

    /**
     * POST /api/scheduler/config/{jobName}/run
     * Déclenche immédiatement un job, indépendamment de son intervalle.
     * Utile pour les tests, la démo jury, ou une exécution forcée par l'admin.
     *
     * @param jobName nom du job à exécuter maintenant
     * @return message de confirmation (JSON)
     */
    @PostMapping("/{jobName}/run")
    public ResponseEntity<Map<String, String>> runJobNow(@PathVariable String jobName) {
        log.info("[API] POST /api/scheduler/config/{}/run — déclenchement manuel", jobName);

        // Vérifie que le job existe avant de le déclencher
        schedulerConfigService.getConfigByJobName(jobName); // lève une exception si inconnu

        // Exécution asynchrone dans un thread séparé pour ne pas bloquer la réponse HTTP
        new Thread(() -> profileScheduler.triggerJobManually(jobName)).start();

        return ResponseEntity.ok(Map.of(
                "status",  "triggered",
                "jobName", jobName,
                "message", "Job '" + jobName + "' déclenché manuellement."
        ));
    }
}
