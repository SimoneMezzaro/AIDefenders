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

DELETE FROM `assistant_prompts`;

INSERT INTO `assistant_prompts` (`Prompt`, `As_separate_context`, `Default_flag`) VALUES ('You are given the Java class between """ """.
"""<class_under_test>"""
<mutants(You are also given the following information about some mutants on the Java class:)>
<tests(Finally you are given some tests for the Java class:)>
Reply to the following question by providing
- a short answer using only one sentence
- an explanation of the answer using at most 100 words
- a code example
Provide the reply in JSON format with the following keys: answer, explanation, code.
', '1', '1');
