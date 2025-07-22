package com.strange.fix.engine.check;

import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.brokenapi.analysis.dependency.DependencyTreeResolver;
import com.strange.brokenapi.analysis.dependency.property.DependencyProperty;
import com.strange.common.utils.ClassUtil;
import com.strange.fix.engine.llm.CodeElementMapper;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PatchChecker {

    private static final List<String> PASS_CLASS_NAME_LIST = List.of("java");

    private final BrokenApiUsage brokenApiUsage;

    private final CodeElementMapper codeElementMapper;

    public PatchChecker(BrokenApiUsage brokenApiUsage, CodeElementMapper codeElementMapper) {
        this.brokenApiUsage = brokenApiUsage;
        this.codeElementMapper = codeElementMapper;
    }

    public void check() {
        checkImportStatements();
    }

    private void checkImportStatements() {
        DependencyTreeResolver newTreeResolver = brokenApiUsage.getErrorResult().getNewTreeResolver();
        DependencyProperty dependencyProperty = newTreeResolver.getDependencyProperty();
        Map<String, List<String>> classNameMap = dependencyProperty.getClassNameMap();

        List<ImportDeclaration> addedImportStatementList = codeElementMapper.getAddedImportStatementList();
        for (ImportDeclaration importDeclaration : addedImportStatementList) {
            if (!importDeclaration.isOnDemand() && !importDeclaration.isStatic()) {
                String importedClassName = importDeclaration.getName().getFullyQualifiedName();
                if (isPassImport(importedClassName)) {
                    continue;
                }
                String simpleClassName = ClassUtil.getSimpleClassName(importedClassName);
                if (classNameMap.containsKey(simpleClassName)) {
                    List<String> classNameList = classNameMap.get(simpleClassName);
                    if (!isValidImport(importedClassName, classNameList)) {
                        String targetClassName = classNameList.get(0);
                        AST ast = importDeclaration.getAST();
                        importDeclaration.setName(ast.newName(targetClassName));
                    }
                }
            }
        }

        List<ImportDeclaration> deletedImportStatementList = codeElementMapper.getDeletedImportStatementList();
        Iterator<ImportDeclaration> it = deletedImportStatementList.iterator();
        while (it.hasNext()) {
            ImportDeclaration decl = it.next();
            if (!decl.isOnDemand() && !decl.isStatic()) {
                String fqName = decl.getName().getFullyQualifiedName();
                String simpleName = ClassUtil.getSimpleClassName(fqName);
                if (classNameMap.containsKey(simpleName)) {
                    List<String> classNameList = classNameMap.get(simpleName);
                    if (isValidImport(fqName, classNameList)) {
                        it.remove();
                    }
                }
            }
        }
    }

    private boolean isPassImport(String className) {
        for (String passClassHeader : PASS_CLASS_NAME_LIST) {
            if (className.startsWith(passClassHeader)) return true;
        }
        return false;
    }

    private boolean isValidImport(String importClassName, List<String> classNameList) {
        for (String className : classNameList) {
            if (Objects.equals(importClassName, className)) {
                return true;
            }
        }
        return false;
    }
}
