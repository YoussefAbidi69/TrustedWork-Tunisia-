-- =============================================================
-- Initialisation des configurations par défaut des schedulers
-- DB : trustedwork_profile_db
-- Table : scheduler_configs (créée automatiquement par Hibernate)
--
-- Utiliser INSERT IGNORE pour éviter les doublons
-- Exécuter manuellement si besoin (la table doit exister d'abord)
-- =============================================================

INSERT IGNORE INTO scheduler_configs (
    job_name,
    description,
    cron_expression,
    interval_minutes,
    enabled,
    last_run,
    updated_at
) VALUES
(
    'recalculateAllSkillScores',
    'Recalcul nocturne des scores d''authenticité des compétences',
    '0 0 1 * * *',
    1440,
    true,
    NULL,
    NOW()
),
(
    'updateRegionalRankings',
    'Mise à jour hebdomadaire du classement régional des freelancers',
    '0 0 0 * * MON',
    10080,
    true,
    NULL,
    NOW()
),
(
    'sendProfileCompletionReminders',
    'Rappels par email aux freelancers avec profil incomplet (< 60%)',
    '0 0 9 * * *',
    1440,
    true,
    NULL,
    NOW()
),
(
    'checkCertificationExpiry',
    'Vérification mensuelle des certifications expirées ou proches de l''expiration',
    '0 0 0 1 * *',
    43200,
    true,
    NULL,
    NOW()
);

-- Vérification
SELECT id, job_name, cron_expression, interval_minutes, enabled, last_run
FROM scheduler_configs
ORDER BY id;
