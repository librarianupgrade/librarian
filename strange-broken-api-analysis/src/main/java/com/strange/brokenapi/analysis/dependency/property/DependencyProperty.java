package com.strange.brokenapi.analysis.dependency.property;

import cn.hutool.core.io.FileUtil;
import com.strange.brokenapi.analysis.dependency.DependencyNode;
import com.strange.brokenapi.analysis.dependency.maven.MavenDependencyNode;
import com.strange.common.utils.ClassUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;

@Data
@Slf4j
public class DependencyProperty {
    private static final Set<String> PRIMITIVE_TYPES = Set.of("byte", "long", "short", "int", "float", "double", "char", "boolean");

    private final File projectDir;

    private DependencyNode treeNode; // Dependency Tree Structure

    private final Map<DependencyNode, List<DependencyClass>> dependencyClassMap;  // dependency signature ---> List of DependencyClass

    private final List<File> dependencyJarList;

    private final Set<String> packageNameSet;

    private final Map<String, List<String>> classNameMap; // class simple name ---> qualified class name

    public DependencyProperty(File projectDir, DependencyNode dependencyNode, List<File> dependencyJarList) {
        this.projectDir = projectDir;
        this.dependencyClassMap = new HashMap<>();
        this.packageNameSet = new HashSet<>();
        this.treeNode = dependencyNode;
        this.dependencyJarList = dependencyJarList;
        this.classNameMap = new HashMap<>();
        processSelfProject();
        processClass(dependencyNode);
    }

    private void processSelfProject() {
        File classpathDir = FileUtil.file(projectDir, "target", "classes");
        File testClasspathDir = FileUtil.file(projectDir, "target", "test-classes");
        Set<ClassUtil.ClassPair> classPairSet = new HashSet<>();
        if (FileUtil.exist(classpathDir)) {
            classPairSet.addAll(ClassUtil.getClassNameFromDirectory(classpathDir.getAbsolutePath()));
        }
        if (FileUtil.exist(testClasspathDir)) {
            classPairSet.addAll(ClassUtil.getClassNameFromDirectory(classpathDir.getAbsolutePath()));
        }
        List<DependencyClass> dependencyClassList = new ArrayList<>();
        for (ClassUtil.ClassPair classPair : classPairSet) {
            DependencyClass dependencyClass = new DependencyClass(classPair.getPackageName(), classPair.getClassName());
            dependencyClassList.add(dependencyClass);
            packageNameSet.add(classPair.getPackageName());
            String className = classPair.getClassName();
            String simpleClassName = ClassUtil.getSimpleClassName(className);
            List<String> classNameList = classNameMap.getOrDefault(simpleClassName, new ArrayList<>());
            classNameList.add(className);
            classNameMap.put(simpleClassName, classNameList);
        }
        dependencyClassMap.put(MavenDependencyNode.SELF, dependencyClassList);
    }


    private void processClass(DependencyNode dependencyNode) {
        for (DependencyNode childrenNode : dependencyNode.getChildrenNodes()) {
            processClass(childrenNode);
        }

        File jarFile = dependencyNode.getJarFile();
        if (jarFile == null) return;

        List<DependencyClass> dependencyClassList = new ArrayList<>();
        for (ClassUtil.ClassPair classPair : ClassUtil.getClassNameFromJar(jarFile.getAbsolutePath())) {
            DependencyClass dependencyClass = new DependencyClass(classPair.getPackageName(), classPair.getClassName());
            dependencyClassList.add(dependencyClass);
            packageNameSet.add(classPair.getPackageName());
            String className = classPair.getClassName();
            String simpleClassName = ClassUtil.getSimpleClassName(className);
            List<String> classNameList = classNameMap.getOrDefault(simpleClassName, new ArrayList<>());
            classNameList.add(className);
            classNameMap.put(simpleClassName, classNameList);
        }

        dependencyClassMap.put(dependencyNode, dependencyClassList);
    }

    public DependencyNode getDependencyNodeByClassName(String className) {
        if (className == null) return null;
        if (PRIMITIVE_TYPES.contains(className)) return null;

        for (Map.Entry<DependencyNode, List<DependencyClass>> entry : dependencyClassMap.entrySet()) {
            List<DependencyClass> dependencyClassList = entry.getValue();
            for (DependencyClass dependencyClass : dependencyClassList) {
                if (dependencyClass.equals(className)) return entry.getKey();
            }
        }
        return null;
    }
}
