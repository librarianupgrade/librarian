package com.strange.fix.engine.extraction.sourcecode.mapping;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import gr.uom.java.xmi.diff.UMLModelDiff;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class RefactoringAnalyzer {

    private static final Integer MAX_EXECUTION_MINUTES = 2;

    private UMLModelDiff modelDiff;

    private List<Refactoring> refactorings;

    private final File oldLibrarySourceDir;

    private final File newLibrarySourceDir;

    // map the api from old library source code directory to new library source code directory
    protected RefactoringAnalyzer( File oldLibrarySourceDir,  File newLibrarySourceDir) {
        this.oldLibrarySourceDir = oldLibrarySourceDir;
        this.newLibrarySourceDir = newLibrarySourceDir;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(this::buildProjectDiff);
        // Submit the diff builder to its own thread
        try {
            // Wait up to 3 minutes
            future.get(MAX_EXECUTION_MINUTES, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.warn("buildProjectDiff timed out");
            future.cancel(true);  // attempt to interrupt
            // Fallback defaults
            this.refactorings = Collections.emptyList();
            this.modelDiff = null;
        } catch (Throwable e) {
            this.refactorings = Collections.emptyList();
            this.modelDiff = null;
        } finally {
            executor.shutdownNow();
        }
    }

    private void buildProjectDiff() {
        log.info("BeginBuildProjectDifferences for: {}:{}", oldLibrarySourceDir, newLibrarySourceDir);
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        RefactoringSearchHandler refactoringSearchHandler = new RefactoringSearchHandler();
        if (oldLibrarySourceDir.isDirectory() && newLibrarySourceDir.isDirectory()) {
            try {
                miner.detectAtDirectories(oldLibrarySourceDir, newLibrarySourceDir, refactoringSearchHandler);
            } catch (Exception ignored) {
                log.warn("BuildProjectDifferencesError");
            }
            this.refactorings = refactoringSearchHandler.getRefactorings();
            this.modelDiff = refactoringSearchHandler.getModelDiff();
        } else {
            this.refactorings = new ArrayList<>();
            this.modelDiff = null;
        }
    }

    public ApiSignature apiMapping(ApiSignature originalApiSignature) {
        ApiSignature apiSignature = originalApiSignature.clone();

        // check each refactoring
        for (Refactoring refactoring : refactorings) {
            RefactoringType refactoringType = refactoring.getRefactoringType();
            ApiTypeEnum brokenApiType = originalApiSignature.getBrokenApiType();
            RefactoringHandler handler = null;
            switch (brokenApiType) {
                case METHOD, METHOD_DEF -> handler = RefactoringHandlerFactory.getMethodHandler(refactoringType);
                case CLASS, CLASS_DEF -> handler = RefactoringHandlerFactory.getClassHandler(refactoringType);
                default -> handler = RefactoringHandlerFactory.getHandler(refactoringType);
            }

            if (handler == null) continue;
            ApiSignature mappedApiSignature = handler.handle(refactoring, apiSignature);
            if (mappedApiSignature != null) {
                apiSignature = mappedApiSignature;
            }
        }
        return apiSignature;
    }
}
