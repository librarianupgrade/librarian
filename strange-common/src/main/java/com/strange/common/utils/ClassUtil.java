package com.strange.common.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipException;

@Slf4j
public class ClassUtil {

    private static final Set<String> PRIMITIVES = new HashSet<>(
            Arrays.asList(
                    "byte", "short", "int", "long",
                    "float", "double", "char", "boolean", "void"
            )
    );

    public static boolean isPrimitiveType(String typeName) {
        if (typeName == null) {
            return false;
        }
        return PRIMITIVES.contains(typeName);
    }


    private static String removeSuffix(String className) {
        if (className.endsWith(".java")) {
            className = StrUtil.removeSuffix(className, ".java");
        }

        if (className.endsWith(".class")) {
            className = StrUtil.removeSuffix(className, ".class");
        }
        return className;
    }

    private static String processInnerClassName(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }

        String[] parts = className.split("\\.");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                result.append(".");
            }
            result.append(parts[i]);
        }
        String lastPart = parts[parts.length - 1];

        if (lastPart.startsWith("$")) {
            if (!result.isEmpty()) {
                result.append(".");
            }
            String processedLastPart = "$" + lastPart.substring(1).replace("$", ".");
            result.append(processedLastPart);
        } else {
            if (!result.isEmpty()) {
                result.append(".");
            }
            result.append(lastPart.replace("$", "."));
        }

        return result.toString();
    }

    @Data
    public static class ClassPair {
        public ClassPair(String packageName, String className) {
            this.packageName = packageName;
            this.className = className;
        }

        private String packageName; // package name of the class

        private String className; // fully qualified class name
    }

    public static Set<ClassPair> getClassNameFromJar(String jarPath) {
        Set<ClassPair> classPairSet = new HashSet<>();
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(FileUtil.file(jarPath));
        } catch (IOException e) {
            return classPairSet;
        }

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class") || entry.getName().endsWith(".java")) {
                String className = entry.getName().replace("/", ".");
                className = removeSuffix(className);
                String packageName = getPackageName(className);
                className = processInnerClassName(className);

                ClassPair classPair = new ClassPair(packageName, className);
                classPairSet.add(classPair);
            }
        }
        try {
            jarFile.close();
        } catch (IOException ignored) {
        }

        return classPairSet;
    }

    public static Set<ClassPair> getClassNameFromDirectory(String rootDirPath) {
        Set<ClassPair> classPairSet = new HashSet<>();
        Path root = Paths.get(rootDirPath);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return classPairSet;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.toString().toLowerCase();
                        return name.endsWith(".class") || name.endsWith(".java");
                    })
                    .forEach(p -> {
                        String relPath = root.relativize(p).toString();
                        String className = relPath
                                .replace(File.separatorChar, '.');     // e.g. "com/example/Foo.class" -> "com.example.Foo.class"

                        className = removeSuffix(className);

                        String packageName = getPackageName(className);
                        className = processInnerClassName(className);

                        classPairSet.add(new ClassPair(packageName, className));
                    });
        } catch (IOException ignored) {
        }

        return classPairSet;
    }

    public static List<Class> getClassesFromJar(String jarPath, List<String> envJars) throws IOException {
        List<Class> classList = new ArrayList<>();
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(FileUtil.file(jarPath));
        } catch (ZipException e) {
            return classList;
        }

        ClassLoader classLoader = JarUtil.loadClassFromJar(envJars.toArray(new String[]{}));

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                String className = entry.getName().replace("/", ".").replace(".class", "");

                Class<?> clazz = null;
                try {
                    clazz = classLoader.loadClass(className);
                } catch (Throwable ignored) {
                }

                if (clazz != null) {
                    classList.add(clazz);
                }
            }
        }
        jarFile.close();

        return classList;
    }

    public static byte[] getClassByte(String jarPath, String className) throws MalformedURLException, ClassNotFoundException {
        String classPrefix = className.substring(0, className.lastIndexOf('.') - 1);
        classPrefix = classPrefix.replaceAll("\\.", "/");
        String classResourceName = classPrefix + className.substring(className.lastIndexOf('.'));

        try (
                JarFile jarFile = new JarFile(jarPath);
                InputStream in = jarFile.getInputStream(jarFile.getJarEntry(classResourceName))
        ) {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            IOUtil.copy(in, bao);
            return bao.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    public static Boolean isArray(String className) {
        if (className == null) return false;
        return className.startsWith("[") || className.contains("[]");
    }

    public static byte[] getClassBytes(ClassLoader loader, String className) throws IOException {
        String internalName = className.replace('.', '/') + ".class";
        InputStream inputStream = loader.getResourceAsStream(internalName);
        if (inputStream != null) {
            return IOUtil.readBytes(inputStream);
        } else {
            return null;
        }
    }

    public static String getSimpleClassName(String className) {
        List<String> split = StrUtil.split(className, '.');
        return split.get(split.size() - 1);
    }

    public static boolean isEqualUnsafe(String simpleClassName, String className) {
        List<String> split = StrUtil.split(className, ".");
        String name = split.get(split.size() - 1);
        if ("*".equals(name)) return true;
        return Objects.equals(name, simpleClassName);
    }

    public static boolean isPrefix(String prefixName, String className) {
        List<String> prefixList = StrUtil.split(prefixName, ".");
        List<String> classNameList = StrUtil.split(className, ".");
        if (prefixList.size() > classNameList.size()) return false;

        for (int i = 0; i < prefixList.size(); i++) {
            if (!Objects.equals(prefixList.get(i), classNameList.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static String removeLastClassName(String className) {
        return getPackageName(className);
    }

    public static boolean isFirstLetterUpperCase(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return CharUtil.isLetterUpper(str.charAt(0));
    }

    public static String getPackageName(String className) {
        List<String> split = StrUtil.split(className, ".");
        split.remove(split.size() - 1);
        return StrUtil.join(".", split);
    }

    public static int getArrayDimension(String className) {
        int dimension = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\[\\])+$").matcher(className);
        if (matcher.find()) {
            dimension = matcher.group().length() / 2;
        }
        return dimension;
    }

    public static String removeArrayType(String className) {
        return className.replaceAll("(\\[\\])+$", "");
    }

    public static String removeGenericType(String className) {
        int idx = className.indexOf('<');
        className = idx == -1 ? className : className.substring(0, idx);
        className = className.replaceAll("new\\s*|<[^<>]*(?:<[^<>]*>[^<>]*)*>|\\{.*?}|\\(.*?\\)", "");
        return className;
    }

    public static List<String> parseParamTypeList(String paramTypeListStr) {
        List<String> result = new ArrayList<>();
        if (paramTypeListStr == null || paramTypeListStr.isEmpty()) {
            return result;
        }

        StringBuilder token = new StringBuilder();
        int depth = 0;

        for (char ch : paramTypeListStr.toCharArray()) {
            switch (ch) {
                case '<':
                    depth++;
                    break;
                case '>':
                    depth--;
                    break;
                case ',':
                    if (depth == 0) {
                        addToken(result, token);
                        continue;
                    }
                    break;
                default:
            }
            token.append(ch);
        }
        addToken(result, token);
        return result.stream().map(String::strip).toList();
    }

    private static void addToken(List<String> list, StringBuilder token) {
        String piece = token.toString().trim();
        if (!piece.isEmpty()) {
            int lt = piece.indexOf('<');
            if (lt >= 0) {
                piece = piece.substring(0, lt);
            }
            list.add(piece);
        }
        token.setLength(0);
    }
}
