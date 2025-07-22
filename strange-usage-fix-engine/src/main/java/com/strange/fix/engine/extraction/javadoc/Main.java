package com.strange.fix.engine.extraction.javadoc;

import com.strange.fix.engine.extraction.javadoc.entity.MethodDeprecation;
import com.strange.fix.engine.extraction.javadoc.entity.ProjectDeprecation;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        ProjectDeprecation projectDeprecation = JavaDocDeprecationExtractor.extractDocInDirectory("E:\\java_projects\\DrStrange\\strange-usage-fix-engine\\src\\main\\resources\\poi-4.1.2-sources");
        MethodDeprecation classDeprecation = projectDeprecation.findMethodDeprecation("org.apache.poi.ss.usermodel.Cell", "getCellTypeEnum", List.of());
        if (classDeprecation != null) {
            System.out.println(classDeprecation.getJavaDocContent());
        } else {
            System.out.println(classDeprecation);
        }
    }
}
