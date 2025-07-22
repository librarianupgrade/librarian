package com.strange.fix.engine.extraction.javadoc;


import com.strange.fix.engine.extraction.javadoc.entity.ClassDeprecation;
import com.strange.fix.engine.extraction.javadoc.entity.FieldDeprecation;
import com.strange.fix.engine.extraction.javadoc.entity.MethodDeprecation;
import com.strange.fix.engine.extraction.javadoc.entity.ProjectDeprecation;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JavaDocDeprecationVisitor extends ASTVisitor {

    private static final String ANNO_SIGNATURE = "Deprecated";

    private static final String CLASS_LEVEL = "CLASS";

    private static final String METHOD_LEVEL = "METHOD";

    private static final String FIELD_LEVEL = "FIELD";

    private final ProjectDeprecation projectDeprecation;

    public JavaDocDeprecationVisitor(ProjectDeprecation projectDeprecation) {
        this.projectDeprecation = projectDeprecation;
    }

    @Override
    public boolean visit(TypeDeclaration n) {
        recordDeprecation(n, "CLASS");
        return super.visit(n);
    }

    @Override
    public boolean visit(EnumDeclaration n) {
        recordDeprecation(n, CLASS_LEVEL);
        return super.visit(n);
    }

    @Override
    public boolean visit(MethodDeclaration n) {
        recordDeprecation(n, METHOD_LEVEL);
        return super.visit(n);
    }

    @Override
    public boolean visit(FieldDeclaration n) {
        recordDeprecation(n, FIELD_LEVEL);
        return super.visit(n);
    }

    @Override
    public boolean visit(EnumConstantDeclaration n) {
        recordDeprecation(n, FIELD_LEVEL);
        return super.visit(n);
    }

    private void recordClassDeprecation(BodyDeclaration bodyDeclaration, String javaDocContent) {
        if (bodyDeclaration instanceof TypeDeclaration) {
            TypeDeclaration typeDeclaration = (TypeDeclaration) bodyDeclaration;
            ITypeBinding iTypeBinding = typeDeclaration.resolveBinding();
            ClassDeprecation classDeprecation = new ClassDeprecation();

            if (iTypeBinding != null) {
                String qualifiedName = iTypeBinding.getQualifiedName();
                classDeprecation.setOriginalClassName(qualifiedName);
            } else {
                classDeprecation.setOriginalClassName(typeDeclaration.getName().getFullyQualifiedName());
            }
            classDeprecation.setJavaDocContent(javaDocContent);
            projectDeprecation.addClassDeprecation(classDeprecation);
        } else if (bodyDeclaration instanceof EnumDeclaration) {
            EnumDeclaration enumDeclaration = (EnumDeclaration) bodyDeclaration;
            ITypeBinding iTypeBinding = enumDeclaration.resolveBinding();
            ClassDeprecation classDeprecation = new ClassDeprecation();

            if (iTypeBinding != null) {
                String qualifiedName = iTypeBinding.getQualifiedName();
                classDeprecation.setOriginalClassName(qualifiedName);
            } else {
                classDeprecation.setOriginalClassName(enumDeclaration.getName().getFullyQualifiedName());
            }
            classDeprecation.setJavaDocContent(javaDocContent);
            projectDeprecation.addClassDeprecation(classDeprecation);
        }
    }

    private void recordMethodDeprecation(BodyDeclaration bodyDeclaration, String javaDocContent) {
        if (bodyDeclaration instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
            MethodDeprecation methodDeprecation = new MethodDeprecation();

            String methodName = methodDeclaration.getName().getIdentifier();
            methodDeprecation.setOriginalMethodName(methodName);
            methodDeprecation.setJavaDocContent(javaDocContent);
            IMethodBinding iMethodBinding = methodDeclaration.resolveBinding();
            if (iMethodBinding != null) {
                ITypeBinding declaringClass = iMethodBinding.getDeclaringClass();
                String qualifiedName = declaringClass.getQualifiedName();
                methodDeprecation.setOriginalClassName(qualifiedName);
            }

            List<String> paramList = new ArrayList<>();
            if (iMethodBinding != null && iMethodBinding.getParameterTypes() != null) {
                ITypeBinding[] parameterTypes = iMethodBinding.getParameterTypes();
                for (ITypeBinding parameterType : parameterTypes) {
                    String qualifiedName = parameterType.getQualifiedName();
                    paramList.add(qualifiedName);
                }
            } else {
                for (Object parameter : methodDeclaration.parameters()) {
                    paramList.add(parameter.toString());
                }
            }
            methodDeprecation.setOriginalMethodParameters(paramList);
            projectDeprecation.addMethodDeprecation(methodDeprecation);
        }
    }


    private void recordFieldDeprecation(BodyDeclaration bodyDeclaration, String javaDocContent) {
        if (bodyDeclaration instanceof FieldDeclaration fieldDeclaration) {
            for (Object fObj : fieldDeclaration.fragments()) {
                VariableDeclarationFragment f =
                        (VariableDeclarationFragment) fObj;
                FieldDeprecation fieldDeprecation = new FieldDeprecation();
                String fieldName = f.getName().getIdentifier();
                fieldDeprecation.setOriginalFieldName(fieldName);
                fieldDeprecation.setJavaDocContent(javaDocContent);

                ITypeBinding declaringClass = f.resolveBinding().getDeclaringClass();
                if (declaringClass != null) {
                    String declaringClassName = declaringClass.getQualifiedName();
                    fieldDeprecation.setOriginalClassName(declaringClassName);
                }
                projectDeprecation.addFieldDeprecation(fieldDeprecation);
            }
        } else if (bodyDeclaration instanceof EnumConstantDeclaration) {
            EnumConstantDeclaration enumConstantDeclaration = (EnumConstantDeclaration) bodyDeclaration;
            FieldDeprecation fieldDeprecation = new FieldDeprecation();

            fieldDeprecation.setJavaDocContent(javaDocContent);
            fieldDeprecation.setOriginalFieldName(enumConstantDeclaration.getName().getFullyQualifiedName());

            IVariableBinding iVariableBinding = enumConstantDeclaration.resolveVariable();
            if (iVariableBinding != null) {
                fieldDeprecation.setOriginalClassName(iVariableBinding.getDeclaringClass().getQualifiedName());
            }
            projectDeprecation.addFieldDeprecation(fieldDeprecation);
        }
    }

    private void recordDeprecation(BodyDeclaration bodyDeclaration, String kind) {
        boolean anno = false;

        for (Object modObj : bodyDeclaration.modifiers()) {
            if (modObj instanceof Annotation) {
                Annotation ann = (Annotation) modObj;
                String annName = ann.getTypeName().getFullyQualifiedName();
                if (ANNO_SIGNATURE.equals(annName)) {
                    anno = true;
                    break;
                }
            }
        }

        if(bodyDeclaration.getJavadoc() != null) {
            String javaDoc = bodyDeclaration.getJavadoc().toString();
            if(javaDoc.toLowerCase().contains("deprecated")) {
                anno = true;
            }
        }

        String javaDocContent = "";
        Javadoc jd = bodyDeclaration.getJavadoc();
        if (jd != null) {
            @SuppressWarnings("unchecked")
            List<TagElement> tags = jd.tags();
            javaDocContent = (String) tags.stream()
                    .filter(t -> TagElement.TAG_DEPRECATED.equals(t.getTagName()))
                    .findFirst()
                    .map(t -> t.fragments()
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(""))
                    ).orElse("");
        }

        if (anno) {
            switch (kind) {
                case CLASS_LEVEL:
                    recordClassDeprecation(bodyDeclaration, javaDocContent);
                    break;
                case METHOD_LEVEL:
                    recordMethodDeprecation(bodyDeclaration, javaDocContent);
                    break;
                case FIELD_LEVEL:
                    recordFieldDeprecation(bodyDeclaration, javaDocContent);
                    break;
            }
        }
    }

}
