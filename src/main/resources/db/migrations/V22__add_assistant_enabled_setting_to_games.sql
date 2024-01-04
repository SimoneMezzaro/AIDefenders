ALTER TABLE `games`
    ADD COLUMN `AssistantEnabled` TINYINT(1) NULL DEFAULT '0' AFTER `Classroom_ID`;
