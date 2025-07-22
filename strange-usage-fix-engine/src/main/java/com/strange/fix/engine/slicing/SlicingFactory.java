package com.strange.fix.engine.slicing;

import com.strange.brokenapi.analysis.ApiSignature;
import lombok.NonNull;

import java.io.File;
import java.util.List;

public class SlicingFactory {

    public static CodeSlicer getCodeSlicer( File slicedFile,  List<Integer> lineNumbers,  File jarFile,
                                            ApiSignature apiSignature) {

        if (check(slicedFile, lineNumbers.get(0))) {
            return new MethodStatementSlicer(slicedFile, lineNumbers, jarFile, apiSignature);
        } else {
            return new ClassStatementSlicer(slicedFile, lineNumbers, jarFile, apiSignature);
        }
    }

    private static boolean check(File sourceCodeFile, Integer lineNumber) {
        return StatementLocationChecker.isLineInMethodBody(sourceCodeFile, lineNumber);
    }
}
