package com.strange.fix.engine.extraction.sourcecode.mapping;

import com.strange.fix.engine.extraction.sourcecode.mapping.handler.*;
import org.refactoringminer.api.RefactoringType;

public class RefactoringHandlerFactory {

    public static RefactoringHandler getHandler(RefactoringType refactoringType) {
        switch (refactoringType) {
            case CHANGE_PARAMETER_TYPE -> {
                return new ChangeParameterHandler();
            }
            case RENAME_METHOD -> {
                return new RenameMethodHandler();
            }
            case RENAME_CLASS -> {
                return new RenameClassHandler();
            }
            case MOVE_CLASS -> {
                return new MoveClassHandler();
            }
            case CHANGE_ATTRIBUTE_TYPE -> {
                return new ChangeAttributeTypeHandler();
            }
            case CHANGE_RETURN_TYPE -> {
                return new ChangeReturnTypeHandler();
            }
            case MOVE_PACKAGE -> {
                return new MovePackageHandler();
            }
            case RENAME_PACKAGE -> {
                return new RenamePackageHandler();
            }
            default -> {
                return null;
            }
        }
    }

    public static RefactoringHandler getClassHandler(RefactoringType refactoringType) {
        switch (refactoringType) {
            case RENAME_CLASS -> {
                return new RenameClassHandler();
            }
            case MOVE_CLASS -> {
                return new MoveClassHandler();
            }
            case CHANGE_ATTRIBUTE_TYPE -> {
                return new ChangeAttributeTypeHandler();
            }
            case MOVE_PACKAGE -> {
                return new MovePackageHandler();
            }
            case RENAME_PACKAGE -> {
                return new RenamePackageHandler();
            }
            default -> {
                return null;
            }
        }
    }

    public static RefactoringHandler getMethodHandler(RefactoringType refactoringType) {
        switch (refactoringType) {
            case CHANGE_PARAMETER_TYPE -> {
                return new ChangeParameterHandler();
            }
            case RENAME_METHOD -> {
                return new RenameMethodHandler();
            }
            case CHANGE_RETURN_TYPE -> {
                return new ChangeReturnTypeHandler();
            }
            default -> {
                return null;
            }
        }
    }
}
