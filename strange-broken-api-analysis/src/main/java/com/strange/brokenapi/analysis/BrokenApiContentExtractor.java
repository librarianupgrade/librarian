package com.strange.brokenapi.analysis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileReader;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.util.*;

public class BrokenApiContentExtractor {

    public static String extractBrokenContent( File codeFile,  List<Integer> lineNumberList) throws BadLocationException {
        // need to stasify the line number list is not empty, and all line number must in the same class
        if (CollUtil.isEmpty(lineNumberList)) {
            throw new RuntimeException("lineNumberList must not be empty");
        }

        String source = new FileReader(codeFile).readString();

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        parser.setResolveBindings(false);
        CompilationUnit originUnit = (CompilationUnit) parser.createAST(null);


        ASTParser blankParser = ASTParser.newParser(AST.getJLSLatest());
        blankParser.setKind(ASTParser.K_COMPILATION_UNIT);
        blankParser.setSource("".toCharArray());
        CompilationUnit blankCU = (CompilationUnit) blankParser.createAST(null);

        addImportStatements(blankCU, originUnit);
        addClassDeclaration(blankCU, originUnit, lineNumberList);

        // transform the CompilationUnit to source code string
        ASTRewrite rewrite = ASTRewrite.create(blankCU.getAST());
        Document document = new Document(blankCU.toString());
        TextEdit edits = rewrite.rewriteAST(document, null);
        edits.apply(document);
        return document.get();
    }

    private static void addImportStatements(CompilationUnit blankUnit, CompilationUnit originUnit) {
        AST ast = blankUnit.getAST();
        PackageDeclaration packageDeclaration = originUnit.getPackage();
        if (packageDeclaration != null) {
            PackageDeclaration newPkg = (PackageDeclaration)
                    ASTNode.copySubtree(ast, packageDeclaration);
            blankUnit.setPackage(newPkg);
        }

        @SuppressWarnings("unchecked")
        List<ImportDeclaration> imports = originUnit.imports();
        for (ImportDeclaration importDeclaration : imports) {
            ImportDeclaration newImp = (ImportDeclaration) ASTNode.copySubtree(ast, importDeclaration);
            @SuppressWarnings("unchecked")
            List<ImportDeclaration> newImports = blankUnit.imports();
            newImports.add(newImp);
        }
    }

    private static void addClassDeclaration(CompilationUnit blankUnit, CompilationUnit originUnit, List<Integer> lineNumberList) {
        int lineNumber = lineNumberList.get(0);
        LocateClassVisitor locateClassVisitor = new LocateClassVisitor(originUnit, lineNumber);
        originUnit.accept(locateClassVisitor);
        ASTNode targetTypeDeclaration = locateClassVisitor.getTargetTypeDeclaration();
        List<AbstractTypeDeclaration> typeDeclarationList = new ArrayList<>();

        Map<String, FieldDeclaration[]> fieldDeclarationMap = new HashMap<>();

        while (targetTypeDeclaration != null) {
            if (targetTypeDeclaration instanceof AbstractTypeDeclaration abstractTypeDeclaration) {
                AbstractTypeDeclaration copyTypeDeclaration = (AbstractTypeDeclaration) ASTNode.copySubtree(blankUnit.getAST(), abstractTypeDeclaration);
                if (targetTypeDeclaration instanceof TypeDeclaration typeDeclaration) {
                    fieldDeclarationMap.put(typeDeclaration.getName().getIdentifier(), typeDeclaration.getFields());
                }
                List list = copyTypeDeclaration.bodyDeclarations();
                list.clear();
                typeDeclarationList.add(copyTypeDeclaration);
            }
            targetTypeDeclaration = targetTypeDeclaration.getParent();
        }
        if (CollUtil.isEmpty(typeDeclarationList)) {
            return;
        }

        Collections.reverse(typeDeclarationList);
        for (int i = 0; i + 1 < typeDeclarationList.size(); i++) {
            AbstractTypeDeclaration parent = typeDeclarationList.get(i);
            AbstractTypeDeclaration son = typeDeclarationList.get(i + 1);
            parent.bodyDeclarations().add(son);
        }

        AbstractTypeDeclaration typeDeclaration = typeDeclarationList.get(typeDeclarationList.size() - 1); // choose the last one type declaration

        // retrieve broken method or broken field and add to TypeDeclaration
        LocateMethodVisitor locateMethodVisitor = new LocateMethodVisitor(originUnit, lineNumberList, typeDeclaration);
        locateClassVisitor.getTargetTypeDeclaration().accept(locateMethodVisitor);
        List<MethodDeclaration> targetMethodDeclarationList = locateMethodVisitor.getTargetMethodDeclarationList();
        // add method to type declaration if method is not null
        if (CollUtil.isNotEmpty(targetMethodDeclarationList)) {
            for (MethodDeclaration targetMethodDeclaration : targetMethodDeclarationList) {
                MethodDeclaration copyMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(blankUnit.getAST(), targetMethodDeclaration);
                typeDeclaration.bodyDeclarations().add(copyMethodDeclaration);
            }
        }

//        LocateFieldVisitor locateFieldVisitor = new LocateFieldVisitor(originUnit, lineNumberList);
//        locateClassVisitor.getTargetTypeDeclaration().accept(locateFieldVisitor);
//        FieldDeclaration targetFieldDeclaration = locateFieldVisitor.getTargetFieldDeclaration();
//        // add field to type declaration if field is not null
//        FieldDeclaration addedFieldDeclaration = null;
//        if (targetFieldDeclaration != null) {
//            addedFieldDeclaration = (FieldDeclaration) ASTNode.copySubtree(blankUnit.getAST(), targetFieldDeclaration);
//            typeDeclaration.bodyDeclarations().add(addedFieldDeclaration);
//
//        }

        // add the related field to prompt code
        if (fieldDeclarationMap.containsKey(typeDeclaration.getName().getIdentifier())) {
            FieldDeclaration[] fieldDeclarations = fieldDeclarationMap.get(typeDeclaration.getName().getIdentifier());
            if (fieldDeclarations != null) {
                for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
                    FieldDeclaration copyFieldDeclaration = (FieldDeclaration) ASTNode.copySubtree(blankUnit.getAST(), fieldDeclaration);
                    typeDeclaration.bodyDeclarations().add(copyFieldDeclaration);
                }
            }
        }

        // add the class header to CompilationUnit
        List<AbstractTypeDeclaration> types = blankUnit.types();
        types.add(typeDeclarationList.get(0));
    }
}
