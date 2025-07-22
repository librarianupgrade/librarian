package com.strange.fix.engine.extraction.sourcecode.localization.visitor;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.FieldApiUsageLocation;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FieldApiUsageVisitor extends ApiLocationVisitor {

    private final List<FieldApiUsageLocation> fieldApiUsageLocationList;

    public FieldApiUsageVisitor( CompilationUnit compilationUnit,  File codeFile,  ApiSignature apiSignature) {
        super(compilationUnit, codeFile, apiSignature);
        this.fieldApiUsageLocationList = new ArrayList<>();
    }

    @Override
    public boolean visit(FieldAccess node) {
        IVariableBinding bind = node.resolveFieldBinding();
        if (bind != null) {
            ITypeBinding typeBinding = bind.getType();
            if (typeBinding != null) {
                String fieldName = bind.getName();
                String fieldClassName = ClassUtil.removeGenericType(typeBinding.getQualifiedName());
                if (isEqualField(fieldName, fieldClassName, apiSignature.getFieldName(), apiSignature.getClassName())) {
                    FieldApiUsageLocation fieldApiUsageLocation = extractFieldApiUsageLocation(node);
                    if (fieldApiUsageLocation != null) {
                        fieldApiUsageLocationList.add(fieldApiUsageLocation);
                    }
                }
            }
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedName node) {
        IBinding bind = node.resolveBinding();
        if (bind instanceof IVariableBinding variableBinding &&
                ((IVariableBinding) bind).isField()) {
            ITypeBinding typeBinding = variableBinding.getType();
            if (typeBinding != null) {
                String fieldName = bind.getName();
                String fieldClassName = ClassUtil.removeGenericType(typeBinding.getQualifiedName());
                if (isEqualField(fieldName, fieldClassName, apiSignature.getFieldName(), apiSignature.getClassName())) {
                    FieldApiUsageLocation fieldApiUsageLocation = extractFieldApiUsageLocation(node);
                    if (fieldApiUsageLocation != null) {
                        fieldApiUsageLocationList.add(fieldApiUsageLocation);
                    }
                }
            }
        }
        return super.visit(node);
    }

    private FieldApiUsageLocation extractFieldApiUsageLocation(ASTNode node) {
        // extract field usage from the field declaration
        FieldDeclaration ancestorFieldDeclaration = findAncestorFieldDeclaration(node);
        if (ancestorFieldDeclaration != null) {
            FieldApiUsageLocation fieldApiUsageLocation = new FieldApiUsageLocation();
            compilationUnit.getLineNumber(node.getStartPosition());
            int startOffset = node.getStartPosition();
            int endOffset = startOffset + node.getLength() - 1;
            fieldApiUsageLocation.setApiType(ApiTypeEnum.FIELD);
            fieldApiUsageLocation.setCompilationUnit(compilationUnit);
            fieldApiUsageLocation.setStartLineNumber(compilationUnit.getLineNumber(startOffset));
            fieldApiUsageLocation.setEndLineNumber(compilationUnit.getLineNumber(endOffset));
            fieldApiUsageLocation.setTargetApiSignature(apiSignature);
            fieldApiUsageLocation.setLocatedFile(codeFile);
            fieldApiUsageLocation.setFieldDeclaration(ancestorFieldDeclaration);

            ApiSignature locatedApiSignature = nodeToApiSignatureDef(ancestorFieldDeclaration);
            fieldApiUsageLocation.setLocatedApiSignature(locatedApiSignature);
            return fieldApiUsageLocation;
        }

        // extract field usage from the method declaration
        MethodDeclaration ancestorMethodDeclaration = findAncestorMethodDeclaration(node);
        if (ancestorMethodDeclaration != null) {
            FieldApiUsageLocation fieldApiUsageLocation = new FieldApiUsageLocation();
            compilationUnit.getLineNumber(node.getStartPosition());
            int startOffset = node.getStartPosition();
            int endOffset = startOffset + node.getLength() - 1;
            fieldApiUsageLocation.setApiType(ApiTypeEnum.FIELD);
            fieldApiUsageLocation.setCompilationUnit(compilationUnit);
            fieldApiUsageLocation.setStartLineNumber(compilationUnit.getLineNumber(startOffset));
            fieldApiUsageLocation.setEndLineNumber(compilationUnit.getLineNumber(endOffset));
            fieldApiUsageLocation.setTargetApiSignature(apiSignature);
            fieldApiUsageLocation.setLocatedFile(codeFile);
            fieldApiUsageLocation.setMethodDeclaration(ancestorMethodDeclaration);

            ApiSignature locatedApiSignature = nodeToApiSignatureDef(ancestorMethodDeclaration);
            fieldApiUsageLocation.setLocatedApiSignature(locatedApiSignature);
            return fieldApiUsageLocation;
        }
        return null;
    }

    private boolean isEqualField(String actualFieldName, String actualFieldClassName,
                                 String exceptedFieldName, String exceptedFieldClassName) {

        if (!Objects.equals(exceptedFieldName, actualFieldName)) return false;
        if (Objects.equals(actualFieldClassName, exceptedFieldClassName)) return true;

        String actualSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(actualFieldClassName));
        String exceptedSimpleClassName = ClassUtil.getSimpleClassName(ClassUtil.removeGenericType(exceptedFieldClassName));

        return Objects.equals(actualSimpleClassName, exceptedSimpleClassName);
    }

    @Override
    public List<ApiLocation> getApiLocations() {
        return new ArrayList<>(fieldApiUsageLocationList);
    }
}
