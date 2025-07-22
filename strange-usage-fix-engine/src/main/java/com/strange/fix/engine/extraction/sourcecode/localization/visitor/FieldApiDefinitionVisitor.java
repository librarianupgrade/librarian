package com.strange.fix.engine.extraction.sourcecode.localization.visitor;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.FieldApiDefinitionLocation;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FieldApiDefinitionVisitor extends ApiLocationVisitor {
    private final List<FieldApiDefinitionLocation> fieldApiDefinitionLocationList;

    public FieldApiDefinitionVisitor( CompilationUnit compilationUnit,  File codeFile,  ApiSignature apiSignature) {
        super(compilationUnit, codeFile, apiSignature);
        this.fieldApiDefinitionLocationList = new ArrayList<>();
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        Type typeNode = node.getType();
        ITypeBinding typeBinding = typeNode.resolveBinding();
        if (typeBinding != null) {
            String fieldClassName = ClassUtil.removeGenericType(typeBinding.getQualifiedName());
            for (Object fragObj : node.fragments()) {
                VariableDeclarationFragment frag = (VariableDeclarationFragment) fragObj;
                String fieldName = frag.getName().getIdentifier();
                IVariableBinding varBinding = frag.resolveBinding();
                if (varBinding != null) {
                    ITypeBinding declaringClassViaBinding = varBinding.getDeclaringClass();
                    if (declaringClassViaBinding != null) {
                        String fieldBelongedClassName = ClassUtil.removeGenericType(declaringClassViaBinding.getQualifiedName());
                        if (isEqualClassName(apiSignature.getClassName(), fieldClassName) && Objects.equals(apiSignature.getFieldName(), fieldName)
                                && isEqualClassName(apiSignature.getFieldBelongedClassName(), fieldBelongedClassName)) {
                            FieldApiDefinitionLocation fieldApiDefinitionLocation = new FieldApiDefinitionLocation();
                            int startOffset = node.getStartPosition();
                            int endOffset = startOffset + node.getLength() - 1;
                            fieldApiDefinitionLocation.setFieldDeclaration(node);
                            fieldApiDefinitionLocation.setLocatedFile(codeFile);
                            fieldApiDefinitionLocation.setCompilationUnit(compilationUnit);
                            fieldApiDefinitionLocation.setApiType(ApiTypeEnum.FIELD_DEF);
                            fieldApiDefinitionLocation.setStartLineNumber(compilationUnit.getLineNumber(startOffset));
                            fieldApiDefinitionLocation.setEndLineNumber(compilationUnit.getLineNumber(endOffset));
                            fieldApiDefinitionLocationList.add(fieldApiDefinitionLocation);
                        }
                    }
                }
            }
        }

        return super.visit(node);
    }

    @Override
    public List<ApiLocation> getApiLocations() {
        return new ArrayList<>(fieldApiDefinitionLocationList);
    }

    private boolean isEqualClassName(String exceptedClassName, String actualClassName) {
        if (Objects.equals(exceptedClassName, actualClassName)) return true;
        exceptedClassName = ClassUtil.removeGenericType(exceptedClassName);
        actualClassName = ClassUtil.removeGenericType(actualClassName);
        if (Objects.equals(exceptedClassName, actualClassName)) return true;
        return Objects.equals(ClassUtil.getSimpleClassName(exceptedClassName), ClassUtil.getSimpleClassName(actualClassName));
    }
}
