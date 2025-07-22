package com.strange.common.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class JDTUtil {

    public static CompilationUnit parseCode(File codeFile) {
        if (codeFile == null || !codeFile.isFile()) {
            return null;
        }
        String code = new FileReader(codeFile, Charset.defaultCharset()).readString();

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(code.toCharArray()); // set source code
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(false);
        parser.setBindingsRecovery(false);
        return (CompilationUnit) parser.createAST(null); // generate AST
    }

    public static CompilationUnit parseCode(String codeFilePath) {
        if (codeFilePath == null) return null;
        return parseCode(FileUtil.file(codeFilePath));
    }

    public static List<String> getImports(File codeFile) {
        CompilationUnit cu = parseCode(codeFile);
        if (cu == null) return new ArrayList<>();

        List<String> importList = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(ImportDeclaration node) {
                if (node.toString().strip().endsWith("*;")) {
                    importList.add(node.getName().getFullyQualifiedName() + ".*");
                } else {
                    importList.add(node.getName().getFullyQualifiedName());
                }
                return super.visit(node);
            }
        });
        return importList;
    }

    /**
     * class simple ---> class fully qualified name
     *
     * @param codeFile
     * @return
     */
    public static Map<String, String> getImportMap(File codeFile) {
        List<String> importList = getImports(codeFile);
        Map<String, String> classMap = new HashMap<>();

        for (String className : importList) {
            List<String> split = StrUtil.split(className, '.');
            String simpleClassName = split.get(split.size() - 1);
            classMap.put(simpleClassName, className);
        }

        return classMap;
    }

    public static List<ImportDeclaration> getImportDeclarations(File codeFile) {
        CompilationUnit cu = parseCode(codeFile);
        if (cu == null) return Collections.emptyList();
        List<ImportDeclaration> importDeclarationList = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(ImportDeclaration node) {
                importDeclarationList.add(node);
                return super.visit(node);
            }
        });
        return importDeclarationList;
    }

    public static List<String> getClassAnnotations(File codeFile) {
        CompilationUnit cu = parseCode(codeFile);
        if (cu == null) return new ArrayList<>();
        List<String> imports = getImports(codeFile);
        List<String> annotationList = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(EnumDeclaration node) {
                List annotations = node.modifiers();
                for (Object annotationObj : annotations) {
                    if (annotationObj instanceof Annotation) {
                        Annotation annotation = (Annotation) annotationObj;
                        annotationList.add(annotation.getTypeName().getFullyQualifiedName());
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(TypeDeclaration node) {
                List<?> annotations = node.modifiers();
                for (Object annotationObj : annotations) {
                    if (annotationObj instanceof Annotation) {
                        Annotation annotation = (Annotation) annotationObj;
                        annotationList.add(annotation.getTypeName().getFullyQualifiedName());
                    }
                }
                return super.visit(node);
            }
        });

        List<String> fullyQualifiedAnnotationList = new ArrayList<>();
        for (String annotationName : annotationList) {
            for (String importClassName : imports) {
                if (isFullQualifiedName(importClassName, annotationName)) {
                    if (importClassName.endsWith((".*"))) {
                        String fullClassName = importClassName.substring(0, importClassName.length() - 1) + annotationName;
                        fullyQualifiedAnnotationList.add(fullClassName);
                    } else {
                        fullyQualifiedAnnotationList.add(importClassName);
                    }
                }
            }
        }
        return fullyQualifiedAnnotationList;
    }

    @Data
    public static class ClassField {
        private String fieldType;

        private String fieldName;

        public ClassField(String fieldType, String fieldName) {
            this.fieldType = fieldType;
            this.fieldName = fieldName;
        }

    }

    public static List<ClassField> getFieldsInClass(File codeFile) {
        List<ClassField> fieldList = new ArrayList<>();

        CompilationUnit cu = parseCode(codeFile);
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(EnumDeclaration node) {
                List<BodyDeclaration> bodyDeclarations = node.bodyDeclarations();
                for (BodyDeclaration body : bodyDeclarations) {
                    if (body instanceof FieldDeclaration) {
                        FieldDeclaration field = (FieldDeclaration) body;
                        List<VariableDeclarationFragment> fragments = field.fragments();
                        for (VariableDeclarationFragment fragment : fragments) {
                            String fieldName = fragment.getName().getIdentifier();

                            Type fieldType = field.getType();
                            String fieldTypeName = getTypeName(fieldType);
                            fieldList.add(new ClassField(fieldTypeName, fieldName));
                        }

                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(TypeDeclaration node) {
                FieldDeclaration[] fields = node.getFields();
                for (FieldDeclaration field : fields) {
                    List<VariableDeclarationFragment> fragments = field.fragments();
                    for (VariableDeclarationFragment fragment : fragments) {
                        // get field name
                        String fieldName = fragment.getName().getIdentifier();

                        // get field type name
                        Type fieldType = field.getType();
                        String fieldTypeName = getTypeName(fieldType);
                        fieldList.add(new ClassField(fieldTypeName, fieldName));
                    }
                }
                return super.visit(node);
            }

            // Helper method for getting type names
            private String getTypeName(Type type) {
                if (type instanceof SimpleType) {
                    return ((SimpleType) type).getName().getFullyQualifiedName();
                } else if (type instanceof ArrayType) {
                    ArrayType arrayType = (ArrayType) type;
                    return getTypeName(arrayType.getElementType()) + "[]";
                } else {
                    return type.toString();
                }
            }
        });
        return fieldList;
    }

    public static List<String> getFieldSetterAndGetter(File codeFile) {
        List<ClassField> fieldsInClass = getFieldsInClass(codeFile);
        List<String> methodNameList = new ArrayList<>();

        for (ClassField classField : fieldsInClass) {
            String getterMethodName;
            if ("boolean".equals(classField.getFieldType())) {
                String fieldName = classField.getFieldName();
                if (fieldName.length() >= 3 && fieldName.startsWith("is") && Character.isUpperCase(fieldName.charAt(2))) {
                    getterMethodName = fieldName;
                } else {
                    getterMethodName = "is" + capitalizeFirstLetter(fieldName);
                }
            } else {
                getterMethodName = "get" + capitalizeFirstLetter(classField.getFieldName());
            }
            methodNameList.add(getterMethodName);

            String setterMethodName;
            if ("boolean".equals(classField.getFieldType())) {
                String fieldName = classField.getFieldName();
                if (fieldName.length() >= 3 && fieldName.startsWith("is") && Character.isUpperCase(fieldName.charAt(2))) {
                    setterMethodName = "set" + capitalizeFirstLetter(fieldName.substring(2));
                } else {
                    setterMethodName = "set" + capitalizeFirstLetter(fieldName);
                }
            } else {
                setterMethodName = "set" + capitalizeFirstLetter(classField.getFieldName());
            }

            methodNameList.add(setterMethodName);
        }
        return methodNameList;
    }

    public static String getPackageName(File codeFile) {
        CompilationUnit cu = parseCode(codeFile);
        final String[] packageName = {null};

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(PackageDeclaration node) {
                // 获取包名
                packageName[0] = node.getName().getFullyQualifiedName();
                return super.visit(node);
            }
        });

        return packageName[0];
    }

    public static List<String> getSuperClassAndInterfaces(File codeFile) {
        CompilationUnit cu = parseCode(codeFile);
        List<String> superClassList = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                // get super class
                Type superclassType = node.getSuperclassType();
                if (superclassType != null) {
                    superClassList.add(superclassType.toString());
                }

                // get interfaces
                List interfaces = node.superInterfaceTypes();
                if (!interfaces.isEmpty()) {
                    for (Object iface : interfaces) {
                        superClassList.add(((Type) iface).toString());
                    }
                }
                return super.visit(node);
            }
        });

        Map<String, String> importMap = getImportMap(codeFile);
        String packageName = getPackageName(codeFile);

        return superClassList.stream()
                .map(className -> importMap.getOrDefault(className, packageName + "." + className))
                .collect(Collectors.toList());
    }

    private static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static boolean isFullQualifiedName(String fullyQualifiedName, String simpleName) {
        if (fullyQualifiedName.endsWith(".*")) {
            return true;
        } else {
            List<String> split = StrUtil.split(fullyQualifiedName, '.');
            if (split.isEmpty()) return false;
            return Objects.equals(split.get(split.size() - 1), simpleName);
        }
    }

    public static List<String> getFieldAnnotations(File codeFile, String fieldName) {
        CompilationUnit cu = parseCode(codeFile);
        List<String> fieldAnnotations = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(FieldDeclaration node) {
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) node.fragments().get(0);
                if (fragment.getName().getIdentifier().equals(fieldName)) {
                    List<IExtendedModifier> modifiers = node.modifiers();
                    for (IExtendedModifier modifier : modifiers) {
                        if (modifier.isAnnotation()) {
                            Annotation annotation = (Annotation) modifier;
                            fieldAnnotations.add(annotation.getTypeName().getFullyQualifiedName());
                        }
                    }
                }
                return super.visit(node);
            }
        });

        List<String> fieldAnnotationList = new ArrayList<>();
        Map<String, String> importMap = getImportMap(codeFile);
        for (String annotation : fieldAnnotations) {
            String completeAnnotation = importMap.getOrDefault(annotation, annotation);
            fieldAnnotationList.add(completeAnnotation);
        }
        return fieldAnnotationList;
    }

    public static List<String> getInnerClassNames(File codeFile) {
        CompilationUnit cu = parseCode(codeFile);
        List<String> innerClassNameList = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                innerClassNameList.add(node.getName().getIdentifier());
                return super.visit(node);
            }

            @Override
            public boolean visit(EnumDeclaration node) {
                innerClassNameList.add(node.getName().getIdentifier());
                return super.visit(node);
            }
        });
        return innerClassNameList;
    }

    public static String getTopClassName(File codeFile) {
        CompilationUnit cu = parseCode(codeFile);
        final String[] topClassName = new String[1];
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(AnnotationTypeDeclaration node) {
                if (node.isPackageMemberTypeDeclaration()) {
                    topClassName[0] = node.getName().getFullyQualifiedName();
                }
                return super.visit(node);
            }
        });
        return topClassName[0];
    }

    public static boolean isInnerClass(File codeFile, String innerClassName) {
        List<String> innerClassNames = getInnerClassNames(codeFile);
        return innerClassNames.contains(innerClassName);
    }

    public static String getInnerClassSourceCode(File codeFile, String innerClassName) {
        CompilationUnit cu = parseCode(codeFile);
        StringBuilder sb = new StringBuilder();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(ImportDeclaration node) {
                sb.append(node.toString());
                return super.visit(node);
            }
        });
        sb.append("\n");

        List<String> sourceCode = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                if (node.getName().getIdentifier().equals(innerClassName)) {
                    sourceCode.add(node.toString());
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(EnumDeclaration node) {
                if (node.getName().getIdentifier().equals(innerClassName)) {
                    sourceCode.add(node.toString());
                }
                return super.visit(node);
            }
        });

        sb.append(sourceCode.isEmpty() ? "" : sourceCode.get(0));
        return sb.toString();
    }

    public static Map<String, List<String>> getGenericBoundMapping(File codeFile) {
        CompilationUnit cu = parseCode(codeFile);
        Map<String, String> importMap = getImportMap(codeFile);
        String packageName = getPackageName(codeFile);
        Map<String, List<String>> genericMapping = new HashMap<>();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                List<TypeParameter> typeParameters = node.typeParameters();
                for (TypeParameter typeParam : typeParameters) {
                    List<Type> bounds = typeParam.typeBounds();
                    if (!bounds.isEmpty()) {
                        List<String> boundList = new ArrayList<>();
                        for (Type bound : bounds) {
                            String typeName = getTypeName(bound);
                            if (importMap.containsKey(typeName)) typeName = importMap.get(typeName);
                            else typeName = packageName + "." + typeName;
                            boundList.add(typeName);
                        }
                        genericMapping.put(typeParam.getName().getIdentifier(), boundList);
                    }
                }
                return super.visit(node);
            }

            private String getTypeName(Type type) {
                if (type instanceof SimpleType) {
                    return ((SimpleType) type).getName().getFullyQualifiedName();
                } else if (type instanceof ArrayType) {
                    ArrayType arrayType = (ArrayType) type;
                    return getTypeName(arrayType.getElementType()) + "[]";
                } else {
                    return type.toString();
                }
            }
        });

        return genericMapping;
    }

    public static List<String> getEnumConst(File codeFile) {
        CompilationUnit cu = parseCode(codeFile);
        List<String> enumConstants = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(EnumDeclaration node) {
                for (Object enumConstant : node.enumConstants()) {
                    EnumConstantDeclaration constant = (EnumConstantDeclaration) enumConstant;
                    String constantName = constant.getName().getIdentifier();
                    enumConstants.add(constantName);
                }
                return true;
            }

            @Override
            public boolean visit(TypeDeclaration node) {
                return true;
            }
        });

        return enumConstants;
    }

    public static List<Pair<String, Integer>> getFieldInfo(File codeFile) {
        List<Pair<String, Integer>> fieldInfoList = new ArrayList<>();
        CompilationUnit compilationUnit = parseCode(codeFile);
        compilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(FieldDeclaration node) {
                List<VariableDeclarationFragment> fragments = node.fragments();
                for (VariableDeclarationFragment fragment : fragments) {
                    String fieldName = fragment.getName().getIdentifier();
                    int lineNumber = compilationUnit.getLineNumber(node.getStartPosition());
                    fieldInfoList.add(new Pair<>(fieldName, lineNumber));
                }
                return super.visit(node);
            }
        });
        return fieldInfoList;
    }

    private static final Set<Class<? extends ASTNode>> CANDIDATE = Set.of(
            // statement level class
            Annotation.class,
            MarkerAnnotation.class,
            Statement.class,
            IfStatement.class,
            BreakStatement.class,
            ExpressionStatement.class,
            SwitchStatement.class,
            ConstructorInvocation.class,
            ReturnStatement.class,
            WhileStatement.class,
            // declaration level class
            ImportDeclaration.class,
            FieldDeclaration.class,
            VariableDeclaration.class,
            MethodDeclaration.class,
            TypeDeclaration.class
    );

    public static String getStatementByLineNumber(File codeFile, Integer startLine, Integer endLine) throws BadLocationException {
        String sourceCode = new FileReader(codeFile).readString();
        Document doc = new Document(sourceCode);
        int startOffset = doc.getLineOffset(startLine - 1);
        int endOffset = doc.getLineOffset(endLine - 1) + doc.getLineLength(endLine - 1);
        return doc.get(startOffset, endOffset - startOffset);
    }

    public static String getStatementByLineNumber(File codeFile, Integer line) {
        String sourceCode = new FileReader(codeFile).readString();
        return getStatementByLineNumber(sourceCode, line);
    }

    public static String getStatementByLineNumber(String sourceCode, Integer line) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(false);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        int offset;
        try {
            offset = cu.getPosition(line, 0);
        } catch (IllegalArgumentException e) {
            return null;
        }

        while (offset < sourceCode.length()
                && Character.isWhitespace(sourceCode.charAt(offset))
                && sourceCode.charAt(offset) != '\n') {
            offset++;
        }
        if (offset >= sourceCode.length() || sourceCode.charAt(offset) == '\n') return null;


        ASTNode leaf = NodeFinder.perform(cu, offset, 1);
        if (leaf == null) return null;

        ASTNode best = null, cur = leaf;
        while (cur != null) {
            if (covers(cu, cur, line) && isCandidate(cur)) {
                best = cur;
                break;
            }
            if (!covers(cu, cur, line)) {
                break;
            }
            best = cur;
            cur = cur.getParent();
        }
        if (best == null) return null;

        if (best instanceof TypeDeclaration td) {
            int start = td.getStartPosition();
            for (Object m : td.modifiers()) {
                if (m instanceof Annotation a) {
                    start = a.getStartPosition() + a.getLength();
                }
            }
            // skip Javadoc part
            Javadoc jd = td.getJavadoc();
            if (jd != null && jd.getStartPosition() + jd.getLength() > start) {
                start = jd.getStartPosition() + jd.getLength();
            }

            // skip whitespace
            while (start < sourceCode.length() &&
                    (sourceCode.charAt(start) == ' ' || sourceCode.charAt(start) == '\t' ||
                            sourceCode.charAt(start) == '\r' || sourceCode.charAt(start) == '\n'))
                start++;

            char delimiter = '{';
            int p = start;
            while (p < sourceCode.length() && sourceCode.charAt(p) != delimiter) p++;
            if (p < sourceCode.length()) p++;

            return sourceCode.substring(start, p).trim();
        }

        if (best instanceof MethodDeclaration md) {
            int start = md.getStartPosition();
            for (Object m : md.modifiers()) {
                if (m instanceof Annotation a) {
                    start = a.getStartPosition() + a.getLength();
                }
            }

            // skip Javadoc part
            Javadoc jd = md.getJavadoc();
            if (jd != null && jd.getStartPosition() + jd.getLength() > start) {
                start = jd.getStartPosition() + jd.getLength();
            }

            // skip whitespace
            while (start < sourceCode.length() &&
                    (sourceCode.charAt(start) == ' ' || sourceCode.charAt(start) == '\t' ||
                            sourceCode.charAt(start) == '\r' || sourceCode.charAt(start) == '\n'))
                start++;

            char delimiter = md.getBody() != null ? '{' : ';';
            int p = start;
            while (p < sourceCode.length() && sourceCode.charAt(p) != delimiter) p++;
            if (p < sourceCode.length()) p++;

            return sourceCode.substring(start, p).trim();
        }

        if (best instanceof IfStatement ifStatement) {
            int startPosition = ifStatement.getStartPosition();
            int length = ifStatement.getExpression().getLength();
            int lineNumber = cu.getLineNumber(startPosition + length);
            if (lineNumber >= line) {
                Expression condition = ifStatement.getExpression();
                return "if (" + condition.toString() + ")";
            } else {
                Statement statement = ifStatement.getElseStatement();
                if (statement instanceof IfStatement elseStatement) {
                    Expression condition = elseStatement.getExpression();
                    return "if (" + condition.toString() + ")";
                } else {
                    return statement.toString();
                }
            }
        }

        if (best instanceof ForStatement forStatement) {
            String init = (((List<?>) forStatement.initializers())
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")));

            String condition = forStatement.getExpression() != null ? forStatement.getExpression().toString() : "";

            String update = (((List<?>) forStatement.updaters())
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")));

            return "for (" + init + "; " + condition + "; " + update + ")";
        }

        if (best instanceof EnhancedForStatement enhancedForStatement) {
            SingleVariableDeclaration param = enhancedForStatement.getParameter();
            Expression expression = enhancedForStatement.getExpression();
            String paramStr = param.toString();
            String exprStr = expression.toString();
            return "for (" + paramStr + " : " + exprStr + ")";
        }


        if (best instanceof WhileStatement whileStatement) {
            Expression condition = whileStatement.getExpression();
            return "while (" + condition.toString() + ")";
        }

        if (best instanceof FieldDeclaration fieldDeclaration) {
            return getRawFieldDeclaration(sourceCode, fieldDeclaration);
        }

        return sourceCode.substring(best.getStartPosition(),
                        best.getStartPosition() + best.getLength() - 1)
                .trim();
    }

    private static boolean covers(CompilationUnit cu, ASTNode n, int line) {
        int start = n.getStartPosition();
        int end = start + n.getLength() - 1;
        return cu.getLineNumber(start) <= line
                && line <= cu.getLineNumber(end);
    }

    private static boolean isCandidate(ASTNode n) {
        if (n instanceof VariableDeclarationFragment) return false;
        if (n instanceof Block) return false;

        if (CANDIDATE.stream().anyMatch(c -> c.isAssignableFrom(n.getClass()))) {
            return true;
        }
        if (n instanceof Javadoc) {
            return true;
        }
        return false;
    }

    public static String getRawFieldDeclaration(String sourceCode, FieldDeclaration fieldDeclaration) {
        int fieldStartOffset = fieldDeclaration.getStartPosition();
        if (fieldDeclaration.getJavadoc() != null) {
            ASTNode jd = fieldDeclaration.getJavadoc();
            fieldStartOffset = jd.getStartPosition() + jd.getLength();
        }

        for (Object m : fieldDeclaration.modifiers()) {
            if (m instanceof Annotation ann) {
                int pos = ann.getStartPosition() + ann.getLength();
                if (pos > fieldStartOffset) fieldStartOffset = pos;
            }
        }

        int len = sourceCode.length();
        while (fieldStartOffset < len && Character.isWhitespace(sourceCode.charAt(fieldStartOffset))) {
            fieldStartOffset++;
        }

        int fieldEndOffset = fieldDeclaration.getStartPosition() + fieldDeclaration.getLength() - 1;
        return sourceCode.substring(fieldStartOffset, fieldEndOffset);
    }


    public static void main(String[] args) {
        File file = FileUtil.file("E:\\Collect_Client_Projects\\xmlet__GT__HtmlFlow__1\\src\\main\\java\\htmlflow\\HtmlVisitorBinder.java");
        System.out.println(JDTUtil.getStatementByLineNumber(new FileReader(file).readString(), 123));
    }
}
