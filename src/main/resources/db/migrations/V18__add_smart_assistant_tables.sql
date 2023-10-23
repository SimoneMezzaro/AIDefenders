CREATE TABLE `codedefenders`.`assistant_questions` (
    `ID` INT NOT NULL AUTO_INCREMENT,
    `Question` TEXT NOT NULL,
    `Answer` TEXT NULL DEFAULT NULL,
    `Timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `Player_ID` INT NOT NULL,
    `Useful` TINYINT(1) NULL DEFAULT NULL,
    PRIMARY KEY (`ID`),
    INDEX `fk_playerId_assistantQuestions_idx` (`Player_ID` ASC) VISIBLE,
    CONSTRAINT `fk_playerId_assistantQuestions`
        FOREIGN KEY (`Player_ID`)
        REFERENCES `codedefenders`.`players` (`ID`)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE `codedefenders`.`assistant_user_settings` (
    `ID` INT NOT NULL AUTO_INCREMENT,
    `User_ID` INT NOT NULL,
    `Assistant_type` ENUM('NONE', 'NOT_GUIDED') NOT NULL DEFAULT 'NONE',
    `Questions_number` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`ID`),
    INDEX `fk_userId_assistantUserSettings_idx` (`User_ID` ASC) VISIBLE,
    UNIQUE INDEX `User_ID_UNIQUE` (`User_ID` ASC) VISIBLE,
    CONSTRAINT `fk_userId_assistantUserSettings`
        FOREIGN KEY (`User_ID`)
            REFERENCES `codedefenders`.`users` (`User_ID`)
            ON DELETE CASCADE
            ON UPDATE CASCADE
);

DROP TRIGGER IF EXISTS `codedefenders`.`assistant_questions_AFTER_INSERT`;

DELIMITER $$
USE `codedefenders`$$
CREATE DEFINER = CURRENT_USER TRIGGER `codedefenders`.`assistant_questions_AFTER_INSERT` AFTER INSERT ON `assistant_questions` FOR EACH ROW
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
END$$
DELIMITER ;
