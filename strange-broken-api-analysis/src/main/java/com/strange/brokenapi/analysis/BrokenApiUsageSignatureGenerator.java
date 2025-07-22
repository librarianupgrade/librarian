package com.strange.brokenapi.analysis;

import cn.hutool.core.io.file.FileReader;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import java.io.File;

public class BrokenApiUsageSignatureGenerator {

    private final static Integer FINGERPRINT_CONTEXT_SIZE = 2;

    public static String generate(BrokenApiUsage brokenApiUsage) throws BadLocationException {
        Integer errorLineNumber = brokenApiUsage.getErrorResult().getErrorLineNumber();
        File codeFile = brokenApiUsage.getErrorResult().getCodeFile();
        String sourceCode = new FileReader(codeFile).readString();
        IDocument doc = new Document(sourceCode);

        int maxLine = doc.getNumberOfLines();

        int fingerprintStartLine = Math.max(errorLineNumber - FINGERPRINT_CONTEXT_SIZE, 1);
        int fingerprintEndLine = Math.min(errorLineNumber + FINGERPRINT_CONTEXT_SIZE, maxLine);

        int startOffset = doc.getLineOffset(fingerprintStartLine - 1);
        int endOffset = doc.getLineOffset(fingerprintEndLine - 1) + doc.getLineLength(fingerprintEndLine - 1);
        String snippet = sourceCode.substring(startOffset, endOffset);
        return snippet.replaceAll("\\s+", "");
    }
}
