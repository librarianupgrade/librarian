package com.strange.fix.engine.property;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import com.strange.fix.engine.property.visitor.ClassPropertyVisitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyExtractor {

    public static List<ClassProperty> getFileProperties(File javaCodeFile) {
        String sourceCode = new FileReader(javaCodeFile).readString();
        ASTParser p = ASTParser.newParser(AST.getJLSLatest());
        p.setKind(ASTParser.K_COMPILATION_UNIT);
        p.setResolveBindings(true);
        p.setBindingsRecovery(true);
        p.setStatementsRecovery(true);
        p.setEnvironment(null, null, null, true);
        p.setSource(sourceCode.toCharArray());
        p.setUnitName(FileUtil.getName(javaCodeFile));
        CompilationUnit compilationUnit = (CompilationUnit) p.createAST(new NullProgressMonitor());
        ClassPropertyVisitor classPropertyVisitor = new ClassPropertyVisitor(compilationUnit);
        compilationUnit.accept(classPropertyVisitor);
        return classPropertyVisitor.getClassPropertyList();
    }

    public static Map<String, ClassProperty> getFilePropertyMap(File javaCodeFile) {
        List<ClassProperty> fileProperties = getFileProperties(javaCodeFile);
        Map<String, ClassProperty> propertyMap = new HashMap<>();
        for (ClassProperty fileProperty : fileProperties) {
            propertyMap.put(fileProperty.getClassName(), fileProperty);
        }
        return propertyMap;
    }
}
