package com.strange.fix.engine.normalization;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import com.strange.fix.engine.formatter.CodeFormatter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.util.Map;

public class ASTNormalizer {

    private static CompilationUnit getCompilationUnit(String javaFilePath, String sourceCode) {
        ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);

        Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_22);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_22);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_22);
        options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);

        astParser.setCompilerOptions(options);
        astParser.setSource(sourceCode.toCharArray());
        astParser.setUnitName(FileUtil.getName(javaFilePath));
        astParser.setEnvironment(null, null, null, true);
        astParser.setResolveBindings(true);
        astParser.setBindingsRecovery(true);
        astParser.setStatementsRecovery(true);
        return (CompilationUnit) (astParser.createAST(null));
    }


    public static String normalize(File javaFile) {
        String sourceCode = new FileReader(javaFile).readString();
        String outputSourceCode = sourceCode;
        CompilationUnit compilationUnit = getCompilationUnit(javaFile.getAbsolutePath(), sourceCode);

        AST ast = compilationUnit.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);
        try {
            compilationUnit.accept(new NameNormalizeVisitor(ast, rewrite));
            Document doc = new Document(sourceCode);
            TextEdit edits = rewrite.rewriteAST(doc, null);
            edits.apply(doc);
            outputSourceCode = doc.get();
            outputSourceCode = new CodeFormatter(outputSourceCode).startFormat();
        } catch (Exception ignored) {
        }
        return outputSourceCode;
    }

    public static String normalize(String sourceCode) {
        String outputSourceCode;
        CompilationUnit compilationUnit = getCompilationUnit("Temp.java", sourceCode);
        AST ast = compilationUnit.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        try {
            compilationUnit.accept(new NameNormalizeVisitor(ast, rewrite));
            Document doc = new Document(sourceCode);
            TextEdit edits = rewrite.rewriteAST(doc, null);
            edits.apply(doc);
            outputSourceCode = doc.get();
            outputSourceCode = new CodeFormatter(outputSourceCode).startFormat();
        } catch (Exception ignored) {
            return sourceCode;
        }
        return outputSourceCode;
    }
}
