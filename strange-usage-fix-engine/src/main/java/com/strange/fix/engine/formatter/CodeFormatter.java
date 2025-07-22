package com.strange.fix.engine.formatter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import java.util.Map;

@Slf4j
public class CodeFormatter {
    public String methodCode;

    public CodeFormatter(String methodCode) {
        this.methodCode = methodCode;
    }

    public String startFormat() {
        try {
            Map<String, String> options = DefaultCodeFormatterConstants.getEclipseDefaultSettings();
            org.eclipse.jdt.core.formatter.CodeFormatter formatter = ToolFactory.createCodeFormatter(options);
            TextEdit edit = formatter.format(
                    org.eclipse.jdt.core.formatter.CodeFormatter.K_COMPILATION_UNIT,
                    methodCode,
                    0,
                    methodCode.length(),
                    0,
                    System.lineSeparator());

            if (edit != null) {
                IDocument doc = new Document(methodCode);
                edit.apply(doc);
                return doc.get();
            }
        } catch (Exception e) {
            log.error("FormatCodeError: ", e);
        }
        log.warn("MethodCodeFormatterReturnNull");
        return this.methodCode;
    }
}
