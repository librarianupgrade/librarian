package com.strange.fix.engine.extraction.sourcecode.localization.visitor;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ClassApiUsageLocation;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ClassApiUsageVisitor extends ApiLocationVisitor {

    private final List<ClassApiUsageLocation> classApiUsageLocations;

    public ClassApiUsageVisitor( CompilationUnit compilationUnit,  File codeFile,  ApiSignature apiSignature) {
        super(compilationUnit, codeFile, apiSignature);
        this.classApiUsageLocations = new ArrayList<>();
    }

    // the Class use in the statement
    @Override
    public boolean visit(SimpleName node) {
        IBinding binding = node.resolveBinding();
        if (binding != null) {
            if (binding instanceof ITypeBinding typeBinding) {
                String qualifiedName = typeBinding.getQualifiedName();

                if (isEqualClassName(apiSignature.getClassName(), qualifiedName)) {
                    Statement stmt = findEnclosingStatement(node);
                    if (stmt != null) {
                        MethodDeclaration ancestorMethodDeclaration = findAncestorMethodDeclaration(stmt);
                        if (ancestorMethodDeclaration != null) {
                            ClassApiUsageLocation classApiUsageLocation = new ClassApiUsageLocation();
                            ApiSignature locatedApiSignature = nodeToApiSignatureDef(ancestorMethodDeclaration);
                            classApiUsageLocation.setLocatedApiSignature(locatedApiSignature);

                            classApiUsageLocation.setTargetApiSignature(apiSignature);
                            classApiUsageLocation.setApiType(ApiTypeEnum.CLASS);
                            classApiUsageLocation.setStatement(stmt);
                            classApiUsageLocation.setLocatedFile(codeFile);
                            classApiUsageLocation.setStartLineNumber(compilationUnit.getLineNumber(stmt.getStartPosition()));
                            classApiUsageLocation.setEndLineNumber(compilationUnit.getLineNumber(
                                    stmt.getStartPosition() + stmt.getLength() - 1));
                            classApiUsageLocations.add(classApiUsageLocation);
                        }
                    }
                }
            }
        }
        return super.visit(node);
    }

    // the Class use in the field declaration
    @Override
    public boolean visit(FieldDeclaration node) {
        VariableDeclarationFragment frag = (VariableDeclarationFragment) node.fragments().get(0);
        IVariableBinding variableBinding = frag.resolveBinding();
        if (variableBinding != null) {
            ITypeBinding typeBinding = variableBinding.getType();
            String fieldTypeName = ClassUtil.removeGenericType(typeBinding.getQualifiedName());
            if (isEqualClassName(fieldTypeName, apiSignature.getClassName())) {
                ClassApiUsageLocation classApiUsageLocation = new ClassApiUsageLocation();
                ApiSignature locatedApiSignature = nodeToApiSignatureDef(node);
                classApiUsageLocation.setLocatedApiSignature(locatedApiSignature);

                classApiUsageLocation.setApiType(ApiTypeEnum.CLASS);
                classApiUsageLocation.setTargetApiSignature(apiSignature);
                classApiUsageLocation.setFieldDeclaration(node);
                classApiUsageLocation.setLocatedFile(codeFile);
                classApiUsageLocation.setStartLineNumber(compilationUnit.getLineNumber(getFieldDeclarationStartPosition(node)));
                classApiUsageLocation.setEndLineNumber(compilationUnit.getLineNumber(
                        node.getStartPosition() + node.getLength() - 1));
                classApiUsageLocations.add(classApiUsageLocation);
            }
        }
        return super.visit(node);
    }

    private Integer getFieldDeclarationStartPosition(FieldDeclaration node) {
        int typeOffset = node.getType().getStartPosition();
        return typeOffset;
    }

    // the Class use in the return type or parameter of the method declaration
    @Override
    public boolean visit(MethodDeclaration node) {
        Block body = node.getBody();
        // if the body is null, just skip this method declaration
        if (body == null) return super.visit(node);

        String methodBelongedClassName = getEnclosingClassName(node);
        if (methodBelongedClassName == null || isEqualClassName(methodBelongedClassName, apiSignature.getClassName()))
            return super.visit(node);

        // check the return type
        Type returnType = node.getReturnType2();
        if (returnType != null && isTargetType(returnType)) {
            ClassApiUsageLocation classApiUsageLocation = new ClassApiUsageLocation();
            ApiSignature locatedApiSignature = nodeToApiSignatureDef(node);
            classApiUsageLocation.setLocatedApiSignature(locatedApiSignature);

            classApiUsageLocation.setApiType(ApiTypeEnum.CLASS);
            classApiUsageLocation.setTargetApiSignature(apiSignature);
            classApiUsageLocation.setMethodDeclaration(node);
            classApiUsageLocation.setLocatedFile(codeFile);
            classApiUsageLocation.setStartLineNumber(compilationUnit.getLineNumber(getMethodHeaderStartPosition(node)));
            classApiUsageLocation.setEndLineNumber(compilationUnit.getLineNumber(body.getStartPosition()));
            classApiUsageLocations.add(classApiUsageLocation);
            return super.visit(node);
        }

        // check the parameter type
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> params = node.parameters();
        for (SingleVariableDeclaration param : params) {
            Type paramType = param.getType();
            if (isTargetType(paramType)) {
                ClassApiUsageLocation classApiUsageLocation = new ClassApiUsageLocation();
                ApiSignature locatedApiSignature = nodeToApiSignatureDef(node);
                classApiUsageLocation.setLocatedApiSignature(locatedApiSignature);

                classApiUsageLocation.setTargetApiSignature(apiSignature);
                classApiUsageLocation.setMethodDeclaration(node);
                classApiUsageLocation.setLocatedFile(codeFile);
                classApiUsageLocation.setStartLineNumber(compilationUnit.getLineNumber(getMethodHeaderStartPosition(node)));
                classApiUsageLocation.setEndLineNumber(compilationUnit.getLineNumber(body.getStartPosition()));
                classApiUsageLocations.add(classApiUsageLocation);
            }
        }
        return super.visit(node);
    }

    private int getMethodHeaderStartPosition(MethodDeclaration methodDeclaration) {
        List<?> modifiers = methodDeclaration.modifiers();
        if (!modifiers.isEmpty()) {
            ASTNode firstModifier = (ASTNode) modifiers.get(0);
            return firstModifier.getStartPosition();
        } else {
            Type returnType = methodDeclaration.getReturnType2();
            if (returnType != null) {
                return returnType.getStartPosition();
            } else {
                SimpleName methodName = methodDeclaration.getName();
                return methodName.getStartPosition();
            }
        }
    }

    public static String getEnclosingClassName(MethodDeclaration methodDeclaration) {
        ASTNode parent = methodDeclaration.getParent();
        while (parent != null && !(parent instanceof TypeDeclaration)) {
            parent = parent.getParent();
        }
        if (parent != null) {
            TypeDeclaration typeDecl = (TypeDeclaration) parent;
            return typeDecl.getName().getIdentifier();
        } else {
            return null;
        }
    }

    private boolean isTargetType(Type type) {
        ITypeBinding binding = type.resolveBinding();
        if (binding != null) {
            String qualifiedName = binding.getQualifiedName();
            return isEqualClassName(apiSignature.getClassName(), qualifiedName);
        } else {
            String className = apiSignature.getClassName();
            String simpleClassName = ClassUtil.getSimpleClassName(className);
            return Objects.equals(simpleClassName, type.toString())
                    || Objects.equals(className, type.toString());
        }
    }

    private Statement findEnclosingStatement(SimpleName node) {
        ASTNode p = node;
        while (p != null && !(p instanceof Statement)) {
            p = p.getParent();
        }
        return (Statement) p;
    }

    private boolean isEqualClassName(String exceptedClassName, String actualClassName) {
        if (Objects.equals(exceptedClassName, actualClassName)) return true;
        exceptedClassName = ClassUtil.removeGenericType(exceptedClassName);
        actualClassName = ClassUtil.removeGenericType(actualClassName);
        if (Objects.equals(exceptedClassName, actualClassName)) return true;
        return Objects.equals(ClassUtil.getSimpleClassName(exceptedClassName), ClassUtil.getSimpleClassName(actualClassName));
    }

    @Override
    public List<ApiLocation> getApiLocations() {
        return new ArrayList<>(classApiUsageLocations);
    }
}
