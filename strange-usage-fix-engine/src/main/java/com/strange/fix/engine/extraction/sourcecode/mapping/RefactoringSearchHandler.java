package com.strange.fix.engine.extraction.sourcecode.mapping;

import gr.uom.java.xmi.diff.UMLModelDiff;
import lombok.Getter;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;

import java.util.ArrayList;
import java.util.List;

@Getter
public class RefactoringSearchHandler extends RefactoringHandler {

    private List<Refactoring> refactorings;

    private UMLModelDiff modelDiff;

    public RefactoringSearchHandler() {
        this.modelDiff = null;
        this.refactorings = new ArrayList<>();
    }

    @Override
    public void handleModelDiff(String commitId, List<Refactoring> refactoringsAtRevision, UMLModelDiff modelDiff) {
        super.handleModelDiff(commitId, refactoringsAtRevision, modelDiff);
        this.refactorings = refactoringsAtRevision;
        this.modelDiff = modelDiff;
    }
}
