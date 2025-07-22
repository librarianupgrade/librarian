package com.strange.fix.engine.slicing;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileReader;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.LocateClassVisitor;
import com.strange.brokenapi.analysis.LocateFieldVisitor;
import com.strange.brokenapi.analysis.LocateMethodVisitor;
import com.strange.common.utils.ClassUtil;
import com.strange.common.utils.CodeUtil;
import com.strange.common.utils.JDTUtil;
import com.strange.fix.engine.slicing.visitor.SimpleNameMarkVisitor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.util.*;

@Slf4j
public class ClassStatementSlicer extends CodeSlicer {

    public ClassStatementSlicer( File slicedFile,  List<Integer> lineNumberList,
                                 File jarFile,  ApiSignature apiSignature) {
        super(slicedFile, lineNumberList, jarFile, apiSignature);
    }

    @Override
    public SlicingResult slicedCodeWithoutSyntaxCompletion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SlicingResult slicedCodeWithSyntaxCompletion() {
        String source = new FileReader(slicedFile).readString();

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        parser.setResolveBindings(false);
        CompilationUnit originUnit = (CompilationUnit) parser.createAST(null);

        ASTParser blankParser = ASTParser.newParser(AST.getJLSLatest());
        blankParser.setKind(ASTParser.K_COMPILATION_UNIT);
        blankParser.setSource("".toCharArray());
        CompilationUnit blankCU = (CompilationUnit) blankParser.createAST(null);

        addLocatedDeclaration(blankCU, originUnit, lineNumberList);

        List<ImportDeclaration> savedImportDeclarationList = getImportsForSlicedCode(blankCU);
        List<ImportDeclaration> imports = blankCU.imports();
        for (ImportDeclaration importDeclaration : savedImportDeclarationList) {
            ImportDeclaration copyImportDeclaration = (ImportDeclaration) ASTNode.copySubtree(blankCU.getAST(), importDeclaration);
            imports.add(copyImportDeclaration);
        }

        // set the package name
        if (originUnit.getPackage() != null) {
            PackageDeclaration copyPackageDeclaration = (PackageDeclaration) ASTNode.copySubtree(blankCU.getAST(), originUnit.getPackage());
            blankCU.setPackage(copyPackageDeclaration);
        }

        String slicedSourceCode = blankCU.toString();

        Document document = new Document(slicedSourceCode);
        // transform the CompilationUnit to source code string
        ASTRewrite rewrite = ASTRewrite.create(blankCU.getAST());

        // remove all Javadoc content
        blankCU.accept(new ASTVisitor() {
            @Override
            public boolean visit(Javadoc node) {
                rewrite.remove(node, null);
                return super.visit(node);
            }
        });

        TextEdit edits = rewrite.rewriteAST(document, null);
        try {
            edits.apply(document);
            String slicedCode = document.get();
            slicedCode = CodeUtil.removeJavaDoc(slicedCode
                    .replaceAll("\n", " ")
                    .replaceAll("\\s+", " ")
                    .strip());
            return new SlicingResult(lineNumberList, slicedCode);
        } catch (BadLocationException e) {
            log.warn("ClassStatementSlicingError: ", e);
        }
        return null;
    }

    private void addLocatedDeclaration(CompilationUnit blankUnit, CompilationUnit originUnit, List<Integer> lineNumberList) {
        int lineNumber = lineNumberList.get(0);
        LocateClassVisitor locateClassVisitor = new LocateClassVisitor(originUnit, lineNumber);
        originUnit.accept(locateClassVisitor);
        AbstractTypeDeclaration targetTypeDeclaration = locateClassVisitor.getTargetTypeDeclaration();

        List<TypeDeclaration> typeDeclarationList = new ArrayList<>();
        while (targetTypeDeclaration != null) {
            TypeDeclaration copyTypeDeclaration = (TypeDeclaration) ASTNode.copySubtree(blankUnit.getAST(), targetTypeDeclaration);
            List list = copyTypeDeclaration.bodyDeclarations();
            list.clear();
            typeDeclarationList.add(copyTypeDeclaration);
            if (targetTypeDeclaration.getParent() instanceof TypeDeclaration) {
                targetTypeDeclaration = (TypeDeclaration) targetTypeDeclaration.getParent();
            } else {
                targetTypeDeclaration = null;
            }
        }
        if (CollUtil.isEmpty(typeDeclarationList)) {
            return;
        }

        Collections.reverse(typeDeclarationList);
        for (int i = 0; i + 1 < typeDeclarationList.size(); i++) {
            TypeDeclaration parent = typeDeclarationList.get(i);
            TypeDeclaration son = typeDeclarationList.get(i + 1);
            parent.bodyDeclarations().add(son);
        }

        // retrieve broken method or broken field and add to TypeDeclaration
        TypeDeclaration typeDeclaration = typeDeclarationList.get(typeDeclarationList.size() - 1);
        LocateMethodVisitor locateMethodVisitor = new LocateMethodVisitor(originUnit, lineNumberList, typeDeclaration);
        locateClassVisitor.getTargetTypeDeclaration().accept(locateMethodVisitor);
        List<MethodDeclaration> targetMethodDeclarationList = locateMethodVisitor.getTargetMethodDeclarationList();
        // add method to type declaration if method is not null
        if (CollUtil.isNotEmpty(targetMethodDeclarationList)) {
            for (MethodDeclaration targetMethodDeclaration : targetMethodDeclarationList) {
                MethodDeclaration copyMethodDeclaration = (MethodDeclaration) ASTNode.copySubtree(blankUnit.getAST(), targetMethodDeclaration);
                Javadoc javadoc = copyMethodDeclaration.getJavadoc();
                if (javadoc != null) {
                    copyMethodDeclaration.setJavadoc(null);
                }
                Block body = copyMethodDeclaration.getBody();
                if (body != null) {
                    body.statements().clear();
                }
                typeDeclaration.bodyDeclarations().add(copyMethodDeclaration);
            }
        }

        LocateFieldVisitor locateFieldVisitor = new LocateFieldVisitor(originUnit, lineNumberList);
        locateClassVisitor.getTargetTypeDeclaration().accept(locateFieldVisitor);
        List<FieldDeclaration> targetFieldDeclarationList = locateFieldVisitor.getTargetFieldDeclarationList();
        // add field to type declaration if field is not null
        if (CollUtil.isNotEmpty(targetFieldDeclarationList)) {
            for (FieldDeclaration targetFieldDeclaration : targetFieldDeclarationList) {
                FieldDeclaration copyFieldDeclaration = (FieldDeclaration) ASTNode.copySubtree(blankUnit.getAST(), targetFieldDeclaration);
                typeDeclaration.bodyDeclarations().add(copyFieldDeclaration);
            }
        }
        // add the class header to CompilationUnit
        List<AbstractTypeDeclaration> types = blankUnit.types();
        types.add(typeDeclarationList.get(0));
    }

    private List<ImportDeclaration> getImportsForSlicedCode(CompilationUnit compilationUnit) {
        List<ImportDeclaration> importDeclarationList = JDTUtil.getImportDeclarations(slicedFile);
        List<ImportDeclaration> savedImportDeclarationList = new ArrayList<>();
        SimpleNameMarkVisitor simpleNameMarkVisitor = new SimpleNameMarkVisitor();
        compilationUnit.accept(simpleNameMarkVisitor);
        List<String> typeNameList = simpleNameMarkVisitor.getTypeNameList();
        Set<String> usedTypeName = new HashSet<>(typeNameList);

        for (ImportDeclaration importDeclaration : importDeclarationList) {
            if (importDeclaration.isOnDemand()) savedImportDeclarationList.add(importDeclaration);
            else {
                String fullyQualifiedName = importDeclaration.getName().getFullyQualifiedName();
                String simpleClassName = ClassUtil.getSimpleClassName(fullyQualifiedName);
                boolean isFound = false;
                for (String typeName : usedTypeName) {
                    String simpleTypeName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(typeName));
                    if (Objects.equals(simpleTypeName, simpleClassName)) {
                        isFound = true;
                        break;
                    }
                }
                if (isFound) {
                    savedImportDeclarationList.add(importDeclaration);
                }
            }
        }
        return savedImportDeclarationList;
    }
}
