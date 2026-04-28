-- 1. Add threading support to collaboration_logs
ALTER TABLE `collaboration_logs`
  ADD COLUMN `reply_to_id` bigint(20) DEFAULT NULL COMMENT 'Self-FK for threaded replies',
  ADD COLUMN `task_ref_id` bigint(20) DEFAULT NULL COMMENT 'Optional task reference (task card)',
  ADD COLUMN `is_pinned`   bit(1)     NOT NULL DEFAULT 0,
  ADD COLUMN `pinned_at`   datetime(6) DEFAULT NULL,
  ADD COLUMN `pinned_by_id` bigint(20) DEFAULT NULL COMMENT 'FK → users.id (LEAD who pinned)',
  ADD COLUMN `is_deleted`  bit(1)     NOT NULL DEFAULT 0 COMMENT 'Soft delete flag';

ALTER TABLE `collaboration_logs`
  ADD CONSTRAINT `fk_collab_reply_to`  FOREIGN KEY (`reply_to_id`)  REFERENCES `collaboration_logs` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_collab_task_ref`  FOREIGN KEY (`task_ref_id`)  REFERENCES `tasks` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_collab_pinned_by` FOREIGN KEY (`pinned_by_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- 2. Drop the old single-URL attachment column (replaced by child table below)
ALTER TABLE `collaboration_logs`
  DROP COLUMN `attachment_url`;

-- 3. New table: message attachments (replaces attachment_url)
CREATE TABLE `message_attachments` (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT,
  `message_id`  bigint(20)   NOT NULL,
  `url`         varchar(500) NOT NULL,
  `filename`    varchar(255) NOT NULL,
  `file_type`   varchar(100) NOT NULL COMMENT 'MIME type e.g. image/png, application/pdf',
  `file_size`   bigint(20)   NOT NULL COMMENT 'Bytes',
  `uploaded_at` datetime(6)  NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_attach_message` FOREIGN KEY (`message_id`) REFERENCES `collaboration_logs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. New table: emoji reactions
CREATE TABLE `message_reactions` (
  `id`         bigint(20)   NOT NULL AUTO_INCREMENT,
  `message_id` bigint(20)   NOT NULL,
  `user_id`    bigint(20)   NOT NULL,
  `emoji`      varchar(10)  NOT NULL COMMENT 'One of: THUMBS_UP, CHECK, EYES, FIRE, QUESTION, PARTY',
  `reacted_at` datetime(6)  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reaction` (`message_id`, `user_id`, `emoji`),
  CONSTRAINT `fk_reaction_message` FOREIGN KEY (`message_id`) REFERENCES `collaboration_logs` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reaction_user`    FOREIGN KEY (`user_id`)    REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
