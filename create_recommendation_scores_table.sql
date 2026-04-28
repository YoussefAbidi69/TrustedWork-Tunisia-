CREATE TABLE IF NOT EXISTS `freelancer_recommendation_scores` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agency_id` BIGINT NOT NULL,
  `freelancer_id` BIGINT NOT NULL,
  `recommendation_score` FLOAT NOT NULL,
  `skill_match_score` FLOAT NOT NULL,
  `trust_score` FLOAT NOT NULL,
  `availability_score` FLOAT NOT NULL,
  `experience_score` FLOAT NOT NULL,
  `similarity_score` FLOAT NOT NULL,
  `location_score` FLOAT NOT NULL,
  `explanation` VARCHAR(1000) DEFAULT NULL,
  `computed_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_agency_freelancer` (`agency_id`, `freelancer_id`),
  FOREIGN KEY (`agency_id`) REFERENCES `agencies`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`freelancer_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
