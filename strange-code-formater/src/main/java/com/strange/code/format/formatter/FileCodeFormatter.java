package com.strange.code.format.formatter;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
public class FileCodeFormatter {

    private static final ThreadLocal<CodeFormatter> TL_FORMATTER = ThreadLocal.withInitial(() -> {
        Map<String, String> opts = DefaultCodeFormatterConstants.getEclipseDefaultSettings();
        return ToolFactory.createCodeFormatter(opts);
    });

    private final Path javaCodeFile;

    public FileCodeFormatter( File javaCodeFile) {
        this(javaCodeFile.toPath());
    }

    public FileCodeFormatter( Path javaCodeFile) {
        File file = javaCodeFile.toFile();
        if (!file.isFile()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid Java code file: '%s'. It must point to an existing java code file.",
                            javaCodeFile.toAbsolutePath()
                    )
            );
        }
        this.javaCodeFile = javaCodeFile;
    }

    public void startFormat( Path writeToFile) {
        try {
            String source = new FileReader(javaCodeFile.toFile()).readString();

            CodeFormatter fmt = TL_FORMATTER.get();
            TextEdit edit = fmt.format(CodeFormatter.K_COMPILATION_UNIT,
                    source, 0, source.length(), 0, System.lineSeparator());

            if (edit != null) {
                Document doc = new Document(source);
                edit.apply(doc);
                String out = doc.get();
                if (!out.equals(source)) {
                    new FileWriter(writeToFile.toFile()).write(out);
                }
            }
        } catch (Exception ex) {
            log.error("FileFormatError: {}, {}", javaCodeFile, ex.getMessage());
        }
    }

    public String startFormat() {
        try {
            String source = new FileReader(javaCodeFile.toFile()).readString();

            CodeFormatter fmt = TL_FORMATTER.get();
            TextEdit edit = fmt.format(CodeFormatter.K_COMPILATION_UNIT,
                    source, 0, source.length(), 0, System.lineSeparator());

            if (edit != null) {
                Document doc = new Document(source);
                edit.apply(doc);
                return doc.get();
            }
        } catch (Exception ex) {
            log.error("FileFormatError: {}, {}", javaCodeFile, ex.getMessage());
        }
        return null;
    }
}
