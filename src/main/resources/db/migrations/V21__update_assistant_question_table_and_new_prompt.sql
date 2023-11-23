ALTER TABLE `assistant_questions`
    ADD COLUMN `Show_answer_code` TINYINT(1) NOT NULL DEFAULT '0' AFTER `Answer`;

DROP TRIGGER IF EXISTS `assistant_questions_AFTER_INSERT`;

DELIMITER //
CREATE TRIGGER `assistant_questions_AFTER_INSERT` AFTER INSERT ON `assistant_questions` FOR EACH ROW
BEGIN
    UPDATE `assistant_user_settings`
    SET `assistant_user_settings`.`Questions_number` = `assistant_user_settings`.`Questions_number` + 1
    WHERE `assistant_user_settings`.`User_ID` = (
        SELECT `players`.`User_ID`
        FROM `players`
        WHERE NEW.`Player_ID` = `players`.`ID`
    );
END//
DELIMITER ;
