CREATE TABLE `assistant_general_settings` (
    `ID` INT NOT NULL AUTO_INCREMENT,
    `Name` VARCHAR(45) NOT NULL,
    `Value` TINYINT(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`ID`),
    UNIQUE INDEX `Name_UNIQUE` (`Name` ASC) VISIBLE
);

INSERT INTO `assistant_general_settings` (`Name`) VALUES ('ASSISTANT_ENABLED');

CREATE TABLE `assistant_prompts` (
    `ID` INT NOT NULL AUTO_INCREMENT,
    `Prompt` TEXT NOT NULL,
    `Timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `As_separate_context` TINYINT(1) NOT NULL DEFAULT '0',
    `Default_flag` TINYINT(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`ID`)
);

INSERT INTO `assistant_prompts` (`Prompt`, `As_separate_context`, `Default_flag`) VALUES ('I am given the Java class between """ """.
"""<class_under_test>"""
Reply to the following question by providing
- a short answer using only one sentence
- an explanation of the answer using at most 100 words
- a code example
Provide the reply in JSON format with the following keys: answer, explanation, code.
', '1', '1');

CREATE TABLE `assistant_questions` (
    `ID` INT NOT NULL AUTO_INCREMENT,
    `Question` TEXT NOT NULL,
    `Answer` TEXT NULL DEFAULT NULL,
    `Timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `Player_ID` INT NOT NULL,
    `Prompt_ID` INT NOT NULL,
    `Useful` TINYINT(1) NULL DEFAULT NULL,
    PRIMARY KEY (`ID`),
    INDEX `fk_playerId_assistantQuestions_idx` (`Player_ID` ASC) VISIBLE,
    INDEX `fk_promptId_assistantQuestions_idx` (`Prompt_ID` ASC) VISIBLE,
    CONSTRAINT `fk_playerId_assistantQuestions`
        FOREIGN KEY (`Player_ID`)
            REFERENCES `players` (`ID`)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION,
    CONSTRAINT `fk_promptId_assistantQuestions`
        FOREIGN KEY (`Prompt_ID`)
            REFERENCES `assistant_prompts` (`ID`)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION
);

CREATE TABLE `assistant_user_settings` (
    `ID` INT NOT NULL AUTO_INCREMENT,
    `User_ID` INT NOT NULL,
    `Assistant_type` ENUM('NONE', 'NOT_GUIDED') NOT NULL DEFAULT 'NONE',
    `Questions_number` INT NOT NULL DEFAULT '0',
    `Remaining_questions` INT NOT NULL DEFAULT '0',
    PRIMARY KEY (`ID`),
    UNIQUE INDEX `User_ID_UNIQUE` (`User_ID` ASC) VISIBLE,
    CONSTRAINT `fk_userId_assistantUserSettings`
        FOREIGN KEY (`User_ID`)
            REFERENCES `users` (`User_ID`)
            ON DELETE CASCADE
            ON UPDATE CASCADE
);

DROP TRIGGER IF EXISTS `assistant_questions_AFTER_INSERT`;

DELIMITER //
CREATE TRIGGER `assistant_questions_AFTER_INSERT` AFTER INSERT ON `assistant_questions` FOR EACH ROW
BEGIN
    UPDATE `assistant_user_settings` SET `assistant_user_settings`.`Questions_number` = (
        SELECT `Questions_number`
        FROM `assistant_user_settings` JOIN `players` ON `assistant_user_settings`.`User_ID` = `players`.`User_ID`
        WHERE NEW.`Player_ID` = `players`.`ID`
    ) + 1
    WHERE `assistant_user_settings`.`User_ID` = (
        SELECT `players`.`User_ID`
        FROM `players`
        WHERE NEW.`Player_ID` = `players`.`ID`
    );
END//
DELIMITER ;

DROP TRIGGER IF EXISTS `assistant_user_settings_BEFORE_UPDATE`;

DELIMITER //
CREATE TRIGGER `assistant_user_settings_BEFORE_UPDATE` BEFORE UPDATE ON `assistant_user_settings` FOR EACH ROW
BEGIN
    IF NEW.`Remaining_questions` < 0 THEN
        SET NEW.`Remaining_questions` = 0;
    END IF;
END//
DELIMITER ;

DROP TRIGGER IF EXISTS `assistant_user_settings_BEFORE_INSERT`;

DELIMITER //
CREATE TRIGGER `assistant_user_settings_BEFORE_INSERT` BEFORE INSERT ON `assistant_user_settings` FOR EACH ROW
BEGIN
    IF NEW.`Remaining_questions` < 0 THEN
        SET NEW.`Remaining_questions` = 0;
    END IF;
END//
DELIMITER ;
