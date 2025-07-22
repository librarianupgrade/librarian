package com.strange.fix.engine.fixer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.locate.ErrorProblemLocation;
import com.strange.common.utils.ClassUtil;
import com.strange.common.utils.JDTUtil;
import com.strange.common.utils.SootUtil;
import com.strange.common.utils.TempFileUtil;
import com.strange.fix.engine.FixEngine;
import com.strange.fix.engine.FixFileProcessor;
import com.strange.fix.engine.FixResult;
import com.strange.fix.engine.enums.FixEnum;
import com.strange.fix.engine.formatter.CodeFormatter;
import com.strange.fix.engine.konwledge.LibraryDatabaseManager;
import com.strange.fix.engine.llm.LLMFixModel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;

import java.beans.Introspector;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
public class RuleCodeFixer extends CodeFixer {

    @Override
    public FixResult fix( FixFileProcessor fixFileProcessor,  LLMFixModel llmFixModel,  File rootDir,  File projectDir,
                          BrokenApiUsage brokenApiUsage,  List<BrokenApiUsage> brokenApiUsageList,
                          File libraryDatabaseDir,  Integer maxRetryCount,  File cacheDir, int fixDepth) {
        if (brokenApiUsage == null || brokenApiUsage.getApiSignature() == null) {
            return FixResult.builder().fixStatus(false).fixType(FixEnum.NOT_FIXED)
                    .brokenApiUsageList(brokenApiUsageList).addedBrokenApiUsageList(Collections.emptyList()).build();
        }

        ErrorResult errorResult = brokenApiUsage.getErrorResult();
        File codeFile = errorResult.getCodeFile();
        File processedCodeFile = fixFileProcessor.getFile(codeFile);
        File fixCodeFile;
        if (Objects.equals(errorResult.getErrorType(), "MethodMustOverrideOrImplement")) {
            fixCodeFile = removeOverrideAnnotation(brokenApiUsage, processedCodeFile, cacheDir);
        } else if (Objects.equals(errorResult.getErrorType(), "AbstractMethodMustBeImplemented")) {
            fixCodeFile = addOverriddenMethod(brokenApiUsage, processedCodeFile, libraryDatabaseDir, cacheDir);
        } else {
            throw new RuntimeException(String.format(
                    "RuleCodeFixer cannot handle error type '%s' (file: %s)",
                    errorResult.getErrorType(),
                    codeFile.getAbsolutePath()
            ));
        }

        FixResult fixResult = checkFixResult(fixFileProcessor, rootDir,
                brokenApiUsage, brokenApiUsageList, fixCodeFile, codeFile, cacheDir);

        FixFileProcessor clonedFixFileProcessor = fixFileProcessor.clone();

        if (fixResult.isFixed()) {
            if (fixCodeFile != null) {
                clonedFixFileProcessor.addFixFile(codeFile.getAbsolutePath(), fixCodeFile);
            }
            List<BrokenApiUsage> currentBrokenApiUsageList = fixResult.getBrokenApiUsageList();
            List<BrokenApiUsage> addedBrokenApiUsageList = fixResult.getAddedBrokenApiUsageList();
            boolean fixSuccessStatus = false;

            if (CollUtil.isNotEmpty(addedBrokenApiUsageList)) {
                // if it has newly added broken API usage

                boolean allAddedBrokenApiUsageFixed = true;
                for (BrokenApiUsage addedBrokenApiUsage : addedBrokenApiUsageList) {
                    ArrayList<BrokenApiUsage> nextBrokenApiUsageList = new ArrayList<>(currentBrokenApiUsageList);
                    nextBrokenApiUsageList.addAll(addedBrokenApiUsageList);
                    FixResult nextFixResult = FixEngine.fix(clonedFixFileProcessor, llmFixModel, rootDir, projectDir, addedBrokenApiUsage, nextBrokenApiUsageList,
                            libraryDatabaseDir, maxRetryCount, cacheDir, fixDepth + 1);
                    List<BrokenApiUsage> nextAddedBrokenApiUsageList = nextFixResult.getAddedBrokenApiUsageList();
                    if (CollUtil.isNotEmpty(nextAddedBrokenApiUsageList)) {
                        allAddedBrokenApiUsageFixed = false;
                        break;
                    }
                }
                if (allAddedBrokenApiUsageFixed) {
                    log.info("All added broken api usage fixed");
                    fixFileProcessor.setTempFixFileMap(clonedFixFileProcessor.getTempFixFileMap());
                    fixSuccessStatus = true;
                }
            } else {
                // if it is having no newly added broken API usage
                fixFileProcessor.setTempFixFileMap(clonedFixFileProcessor.getTempFixFileMap());
                fixSuccessStatus = true;
            }
            if (fixSuccessStatus) {
                // fix file processor has added the fix code file
                return checkFixResult(fixFileProcessor, rootDir, brokenApiUsage, brokenApiUsageList, null, codeFile, cacheDir);
            }
        }

        return FixResult.builder().fixStatus(false).fixType(FixEnum.NOT_FIXED)
                .brokenApiUsageList(brokenApiUsageList).addedBrokenApiUsageList(Collections.emptyList()).build();
    }

    private static File removeOverrideAnnotation(BrokenApiUsage brokenApiUsage, File errorCodeFile, File cacheDir) {
        String sourceCode = new FileReader(errorCodeFile).readString();
        Integer errorLineNumber = brokenApiUsage.getErrorResult().getErrorLineNumber();

        CompilationUnit compilationUnit = JDTUtil.parseCode(errorCodeFile);
        ASTRewrite rewriter = ASTRewrite.create(compilationUnit.getAST());
        FindMethodVisitor findMethodVisitor = new FindMethodVisitor(compilationUnit, errorLineNumber);
        compilationUnit.accept(findMethodVisitor);
        MethodDeclaration targetMethodDeclaration = findMethodVisitor.getTargetMethodDeclaration();
        if (targetMethodDeclaration != null) {
            @SuppressWarnings("unchecked")
            List<IExtendedModifier> mods = targetMethodDeclaration.modifiers();
            for (Object o : mods) {
                if (o instanceof Annotation ann) {
                    if ("Override".equals(ann.getTypeName().getFullyQualifiedName())) {
                        rewriter.remove(ann, null);
                        break;
                    }
                }
            }

            Document document = new Document(sourceCode);
            TextEdit edits = rewriter.rewriteAST(document, null);
            try {
                edits.apply(document);
                String fixCode = new CodeFormatter(document.get()).startFormat();
                File fixCodeFile = FileUtil.createTempFile("Temp_" + FileNameUtil.getPrefix(errorCodeFile) + "_", ".java.cache", cacheDir, true);
                return new FileWriter(fixCodeFile).write(fixCode);
            } catch (Exception e) {
                log.warn("RemoveOverrideAnnotationError: ", e);
            }
        }
        return null;
    }

    static class FindMethodVisitor extends ASTVisitor {
        @Getter
        private MethodDeclaration targetMethodDeclaration;

        private final Integer targetLine;

        public final CompilationUnit compilationUnit;

        public FindMethodVisitor(CompilationUnit compilationUnit, Integer targetLine) {
            this.compilationUnit = compilationUnit;
            this.targetLine = targetLine;
            this.targetMethodDeclaration = null;
        }

        public boolean visit(MethodDeclaration node) {
            int startLine = compilationUnit.getLineNumber(node.getStartPosition());
            int endLine = compilationUnit.getLineNumber(node.getStartPosition() + node.getLength());
            if (targetLine >= startLine && targetLine <= endLine) {
                this.targetMethodDeclaration = node;
            }
            return super.visit(node);
        }
    }

    private static File addOverriddenMethod(BrokenApiUsage brokenApiUsage, File errorCodeFile, File libraryDatabaseDir, File cacheDir) {
        DefaultProblem problem = brokenApiUsage.getErrorResult().getProblem();
        String[] arguments = problem.getArguments();
        String methodName = arguments[0];
        String belongedClassName = arguments[2];
        List<String> paramTypeList = ClassUtil.parseParamTypeList(arguments[1]);

        String addedTargetSimpleClassName = ClassUtil.getSimpleClassName(arguments[3]);

        SootMethod sootMethod = getOverriddenMethod(brokenApiUsage, methodName, paramTypeList, belongedClassName, libraryDatabaseDir);
        if (sootMethod == null) {
            log.error("OverriddenMethodIsNull");
            return null;
        }

        CompilationUnit compilationUnit = JDTUtil.parseCode(errorCodeFile);
        ClassMatchVisitor classMatchVisitor = new ClassMatchVisitor(addedTargetSimpleClassName);
        compilationUnit.accept(classMatchVisitor);

        ASTNode targetTypeDeclaration = classMatchVisitor.getModifiedASTNode();
        if (targetTypeDeclaration != null) {
            String fixCode = generateAddedOverriddenMethodCode(errorCodeFile, compilationUnit, targetTypeDeclaration, sootMethod, methodName, paramTypeList);
            if (fixCode != null) {
                File fixCodeFile = TempFileUtil.createTempFile(cacheDir, errorCodeFile);
                return new FileWriter(fixCodeFile).write(fixCode);
            }
        }

        return null;
    }

    private static org.eclipse.jdt.core.dom.Type getASTType(AST ast, String qualifiedClassName) {
        String cleanQualifiedClassName = ClassUtil.removeArrayType(ClassUtil.removeGenericType(qualifiedClassName));
        String simpleClassName = ClassUtil.getSimpleClassName(cleanQualifiedClassName);
        org.eclipse.jdt.core.dom.Type simpleType = null;

        if (ClassUtil.isPrimitiveType(simpleClassName)) {
            simpleType = switch (simpleClassName) {
                case "int" -> ast.newPrimitiveType(PrimitiveType.INT);
                case "byte" -> ast.newPrimitiveType(PrimitiveType.BYTE);
                case "short" -> ast.newPrimitiveType(PrimitiveType.SHORT);
                case "long" -> ast.newPrimitiveType(PrimitiveType.LONG);
                case "float" -> ast.newPrimitiveType(PrimitiveType.FLOAT);
                case "double" -> ast.newPrimitiveType(PrimitiveType.DOUBLE);
                case "char" -> ast.newPrimitiveType(PrimitiveType.CHAR);
                case "boolean" -> ast.newPrimitiveType(PrimitiveType.BOOLEAN);
                case "void" -> ast.newPrimitiveType(PrimitiveType.VOID);
                default -> ast.newPrimitiveType(PrimitiveType.INT);
            };
        } else {
            simpleType = ast.newSimpleType(ast.newSimpleName(simpleClassName));
        }


        int arrayDimension = ClassUtil.getArrayDimension(qualifiedClassName);

        for (int i = 0; i < arrayDimension; i++) {
            simpleType = ast.newArrayType(simpleType);
        }
        return simpleType;
    }

    private static Modifier getMethodModifier(AST ast, SootMethod sootMethod) {
        if (sootMethod.isPublic()) {
            return ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
        } else if (sootMethod.isPrivate()) {
            return ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
        } else if (sootMethod.isProtected()) {
            return ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
        }
        return ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
    }

    private static String getMethodArgName(String className, int index) {
        if (ClassUtil.isPrimitiveType(className)) {
            return className.substring(0, 1).toLowerCase() + index;
        } else {
            return Introspector.decapitalize(className) + index;
        }
    }

    static class ClassMatchVisitor extends ASTVisitor {
        private final String simpleClassName;

        @Getter
        @Setter
        private ASTNode modifiedASTNode;

        public ClassMatchVisitor(String simpleClassName) {
            this.simpleClassName = simpleClassName;
        }

        @Override
        public boolean visit(ClassInstanceCreation node) {
            AnonymousClassDeclaration anon = node.getAnonymousClassDeclaration();
            if (anon != null
                    && isEqualClassName(node.getType().toString(), simpleClassName)) {
                this.modifiedASTNode = anon;
            }
            return super.visit(node);
        }

        @Override
        public boolean visit(TypeDeclaration node) {
            String identifier = node.getName().getIdentifier();
            if (isEqualClassName(identifier, simpleClassName)) {
                this.modifiedASTNode = node;
            }
            return super.visit(node);
        }

        private boolean isEqualClassName(String exceptedClassName, String actualClassName) {
            if (Objects.equals(exceptedClassName, actualClassName)) return true;
            exceptedClassName = ClassUtil.removeGenericType(exceptedClassName);
            actualClassName = ClassUtil.removeGenericType(actualClassName);
            if (Objects.equals(exceptedClassName, actualClassName)) return true;
            return Objects.equals(ClassUtil.getSimpleClassName(exceptedClassName), ClassUtil.getSimpleClassName(actualClassName));
        }
    }

    private static String generateAddedOverriddenMethodCode(File codeFile, CompilationUnit compilationUnit, ASTNode typeDeclaration, SootMethod sootMethod,
                                                            String methodName, List<String> paramTypeList) {
        AST ast = compilationUnit.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);
        MethodDeclaration newMethod = ast.newMethodDeclaration();
        ListRewrite importsRewrite = rewrite.getListRewrite(
                compilationUnit,
                CompilationUnit.IMPORTS_PROPERTY
        );
        // set method name
        newMethod.setName(ast.newSimpleName(methodName));

        // set return type
        String returnTypeName = sootMethod.getReturnType().toQuotedString();
        String returnClassName = ClassUtil.removeArrayType(ClassUtil.removeGenericType(returnTypeName));
        newMethod.setReturnType2(getASTType(ast, returnTypeName));
        if (!ClassUtil.isPrimitiveType(returnClassName)) {
            ImportDeclaration returnTypeImportStatement = ast.newImportDeclaration();
            returnTypeImportStatement.setName(ast.newName(returnClassName));
            importsRewrite.insertLast(returnTypeImportStatement, null);
        }

        // add @Override annotation
        MarkerAnnotation overrideAnno = ast.newMarkerAnnotation();
        overrideAnno.setTypeName(ast.newSimpleName("Override"));
        newMethod.modifiers().add(0, overrideAnno);

        // add parameter
        int index = 0;
        for (String paramType : paramTypeList) {
            String paramTypeName = ClassUtil.removeArrayType(ClassUtil.removeGenericType(paramType));
            String simpleParamTypeName = ClassUtil.getSimpleClassName(paramTypeName);
            SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
            param.setType(getASTType(ast, paramType));
            param.setName(ast.newSimpleName(getMethodArgName(simpleParamTypeName, index)));
            newMethod.parameters().add(param);

            if (!ClassUtil.isPrimitiveType(paramTypeName)) {
                ImportDeclaration paramImportStatement = ast.newImportDeclaration();
                paramImportStatement.setName(ast.newName(paramTypeName));
                importsRewrite.insertLast(paramImportStatement, null);
            }
            index += 1;
        }

        // add parameter
        newMethod.modifiers().add(getMethodModifier(ast, sootMethod));

        // set method body
        Block body = ast.newBlock();
        ThrowStatement throwStmt = ast.newThrowStatement();
        ClassInstanceCreation exc = ast.newClassInstanceCreation();
        exc.setType(ast.newSimpleType(ast.newSimpleName("UnsupportedOperationException")));
        throwStmt.setExpression(exc);
        body.statements().add(throwStmt);
        newMethod.setBody(body);

        ChildListPropertyDescriptor bodyDesc = null;
        for (Object raw : typeDeclaration.structuralPropertiesForType()) {
            StructuralPropertyDescriptor desc = (StructuralPropertyDescriptor) raw;
            if (desc instanceof ChildListPropertyDescriptor
                    && "bodyDeclarations".equals(desc.getId())) {
                bodyDesc = (ChildListPropertyDescriptor) desc;
                break;
            }
        }

        if (bodyDesc == null) return null;

        ListRewrite listRewrite = rewrite.getListRewrite(
                typeDeclaration,
                bodyDesc
        );
        listRewrite.insertLast(newMethod, null);

        // transform to code
        String sourceCode = new FileReader(codeFile).readString();
        Document document = new Document(sourceCode);
        try {
            TextEdit astEdits = rewrite.rewriteAST(document, null);
            astEdits.apply(document);
            return new CodeFormatter(document.get()).startFormat();
        } catch (BadLocationException e) {
            return null;
        }
    }

    private static SootMethod getOverriddenMethod(BrokenApiUsage brokenApiUsage, String methodName,
                                                  List<String> paramTypeList, String belongedClassName, File libraryDatabaseDir) {
        ErrorProblemLocation errorProblemLocation = brokenApiUsage.getErrorResult().getErrorProblemLocation();
        String groupId = errorProblemLocation.getGroupId();
        String artifactId = errorProblemLocation.getArtifactId();
        String newVersion = errorProblemLocation.getNewVersion();
        LibraryDatabaseManager libraryDatabaseManager = new LibraryDatabaseManager(libraryDatabaseDir);
        File libraryJarFile = libraryDatabaseManager.getLibraryJarFile(groupId, artifactId, newVersion);
        SootUtil.initializeSoot(List.of(libraryJarFile));

        SootClass sootClass = Scene.v().getSootClass(ClassUtil.removeGenericType(belongedClassName));

        List<SootMethod> sootMethodList = sootClass.getMethods();
        if (CollUtil.isNotEmpty(sootMethodList)) {
            for (SootMethod sootMethod : sootMethodList) {
                if (isEqualMethod(sootMethod, methodName, paramTypeList)) {
                    return sootMethod;
                }
            }
        }
        return null;
    }

    private static boolean isEqualMethod(SootMethod sootMethod, String
            targetMethodName, List<String> targetParameters) {
        if (!Objects.equals(targetMethodName, sootMethod.getName())) {
            return false;
        }
        if (targetParameters.size() != sootMethod.getParameterCount()) return false;

        List<Type> parameterTypes = sootMethod.getParameterTypes();

        for (int i = 0; i < targetParameters.size(); i++) {
            String targetSimpleParamClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(targetParameters.get(i)));
            String actualClassName = parameterTypes.get(i).toQuotedString().replaceAll("\\$", ".");
            String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(actualClassName));
            if (!Objects.equals(targetSimpleParamClassName, actualSimpleClassName)) return false;
        }
        return true;
    }
}
