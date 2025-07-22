package com.strange.fix.engine.extraction.sourcecode.localization.visitor;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ClassApiDefinitionLocation;
import lombok.NonNull;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClassApiDefinitionVisitor extends ApiLocationVisitor {
    private final List<ClassApiDefinitionLocation> classApiDefinitionLocationList;

    public ClassApiDefinitionVisitor( CompilationUnit compilationUnit,  File codeFile,  ApiSignature apiSignature) {
        super(compilationUnit, codeFile, apiSignature);
        this.classApiDefinitionLocationList = new ArrayList<>();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        ITypeBinding typeBinding = node.resolveBinding();
        if (typeBinding != null) {
            String className = ClassUtil.removeGenericType(typeBinding.getQualifiedName());
            if (isEqualClassName(className, apiSignature.getClassName())) {
                ClassApiDefinitionLocation classApiDefinitionLocation = new ClassApiDefinitionLocation();
                classApiDefinitionLocation.setApiType(ApiTypeEnum.CLASS_DEF);
                classApiDefinitionLocation.setTargetApiSignature(apiSignature);
                classApiDefinitionLocation.setTypeDeclaration(node);
                classApiDefinitionLocation.setCompilationUnit(compilationUnit);
                classApiDefinitionLocation.setLocatedFile(codeFile);
                classApiDefinitionLocation.setStartLineNumber(compilationUnit.getLineNumber(node.getStartPosition()));
                classApiDefinitionLocation.setEndLineNumber(compilationUnit.getLineNumber(node.getStartPosition() + node.getLength() - 1));
                classApiDefinitionLocationList.add(classApiDefinitionLocation);
            }
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

    @Override
    public List<ApiLocation> getApiLocations() {
        return new ArrayList<>(classApiDefinitionLocationList);
    }
}
