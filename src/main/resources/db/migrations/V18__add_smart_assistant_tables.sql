CREATE TABLE `codedefenders`.`assistant_questions`
(
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
