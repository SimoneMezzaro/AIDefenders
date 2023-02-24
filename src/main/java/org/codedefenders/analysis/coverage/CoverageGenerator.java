/*
 * Copyright (C) 2016-2023 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.analysis.coverage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.codedefenders.analysis.coverage.ast.AstCoverage;
import org.codedefenders.analysis.coverage.ast.AstCoverageVisitor;
import org.codedefenders.analysis.coverage.line.DetailedLine;
import org.codedefenders.analysis.coverage.line.DetailedLineCoverage;
import org.codedefenders.analysis.coverage.line.LineTokenAnalyser;
import org.codedefenders.analysis.coverage.line.LineTokenVisitor;
import org.codedefenders.analysis.coverage.line.LineTokens;
import org.codedefenders.analysis.coverage.line.NewLineCoverage;
import org.codedefenders.analysis.coverage.line.SimpleLineCoverage;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.util.JavaParserUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;

@ApplicationScoped
public class CoverageGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CoverageGenerator.class);
    private static final String JACOCO_REPORT_FILE = "jacoco.exec";

    /**
     * Reads the coverage data for a test execution and extends it.
     *
     * <p>The method requires the 'jacoco.exec' file to be present in the test folder.
     *
     * @param gameClass the CUT
     * @param testJavaFile the path to the test Java file (the 'jacoco.exe' file must exist in the same directory)
     * @return the extended line coverage
     */
    public NewLineCoverage generate(GameClass gameClass, Path testJavaFile)
            throws CoverageGeneratorException {
        final Path execFile = findJacocoExecFile(testJavaFile);
        final Collection<Path> relevantClassFiles = findRelevantClassFiles(gameClass);

        CoverageBuilder coverageBuilder = readJacocoCoverage(execFile, relevantClassFiles);
        DetailedLineCoverage originalCoverage = extractLineCoverage(coverageBuilder, gameClass);

        CompilationUnit compilationUnit = JavaParserUtils.parse(gameClass.getSourceCode())
                .orElseThrow(() -> new CoverageGeneratorException("Could not parse java file: " + gameClass.getJavaFile()));

        return generate(originalCoverage, compilationUnit);
    }

    public NewLineCoverage generate(DetailedLineCoverage originalCoverage, CompilationUnit compilationUnit) {
        AstCoverageVisitor astVisitor = new AstCoverageVisitor(originalCoverage);
        astVisitor.visit(compilationUnit, null);
        AstCoverage astCoverage = astVisitor.finish();

        LineTokens lineTokens = LineTokens.fromJaCoCo(originalCoverage);
        LineTokenVisitor lineTokenVisitor = new LineTokenVisitor(astCoverage, lineTokens);
        lineTokenVisitor.visit(compilationUnit, null);

        LineTokenAnalyser lineTokenAnalyser = new LineTokenAnalyser();
        return lineTokenAnalyser.analyse(lineTokens);
    }

    // TODO: this replicates the old behavior. replace this with better error handling
    public LineCoverage generateOrEmpty(GameClass gameClass, Path testJavaFile) {
        try {
            NewLineCoverage coverage = generate(gameClass, testJavaFile);
            return ((SimpleLineCoverage) coverage).toLineCoverage(); // see todo in toLineCoverage()
        } catch (CoverageGeneratorException e) {
            logger.error(e.getMessage(), e.getCause());
            return LineCoverage.empty();
        }
    }

    public CoverageBuilder readJacocoCoverage(Path execFile, Collection<Path> relevantClassFiles)
            throws CoverageGeneratorException {
        final ExecFileLoader execFileLoader = new ExecFileLoader();
        try {
            execFileLoader.load(execFile.toFile());
        } catch (IOException e) {
            throw new CoverageGeneratorException("Failed to load jacoco.exec file: " + execFile, e);
        }

        final ExecutionDataStore executionDataStore = execFileLoader.getExecutionDataStore();
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);

        for (Path classFile : relevantClassFiles) {
            try (InputStream is = Files.newInputStream(classFile)) {
                analyzer.analyzeClass(is, classFile.toString());
            } catch (IOException e) {
                throw new CoverageGeneratorException("Failed to analyze file: " + classFile, e);
            }
        }

        return coverageBuilder;
    }

    public Path findJacocoExecFile(Path testJavaFile)
            throws CoverageGeneratorException {
        final Path reportDirectory = testJavaFile.getParent();
        final Path execFile = reportDirectory.resolve(JACOCO_REPORT_FILE);
        if (!Files.isRegularFile(execFile)) {
            throw new CoverageGeneratorException("Could not find JaCoCo exec file for test: " + testJavaFile);
        }
        return execFile;
    }

    public Collection<Path> findRelevantClassFiles(GameClass gameClass) throws CoverageGeneratorException {
        final Path classFileFolder = Paths.get(gameClass.getClassFile()).getParent();

        try (Stream<Path> files = Files.list(classFileFolder)
                .filter(path -> path.toString().endsWith(".class"))) {
            return files.collect(Collectors.toList());
        } catch (IOException e) {
            throw new CoverageGeneratorException("Could not list class files for class: " + gameClass.getClassFile());
        }
    }

    public DetailedLineCoverage extractLineCoverage(CoverageBuilder coverageBuilder, GameClass gameClass) {
        DetailedLineCoverage coverage = new DetailedLineCoverage();

        for (ISourceFileCoverage sourceCoverage : coverageBuilder.getSourceFiles()) {
            if (!gameClass.getJavaFile().endsWith(sourceCoverage.getName())) {
                continue;
            }
            for (int line = sourceCoverage.getFirstLine(); line <= sourceCoverage.getLastLine(); line++) {
                final ILine lineCoverage = sourceCoverage.getLine(line);
                coverage.set(line, DetailedLine.fromJaCoCo(lineCoverage));
            }
        }

        return coverage;
    }

    public static class CoverageGeneratorException extends Exception {
        public CoverageGeneratorException(String message) {
            super(message);
        }

        public CoverageGeneratorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
