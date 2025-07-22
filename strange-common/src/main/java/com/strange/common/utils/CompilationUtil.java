package com.strange.common.utils;

import cn.hutool.core.io.FileUtil;
import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class CompilationUtil {

    public static File compileJavaFileToJar(File javaCodeFile, File classFileOutputDir, File outputJarFile) {
        File file = compileJavaFile(javaCodeFile, classFileOutputDir);
        return createJarFile(file, outputJarFile);
    }

    private static File compileJavaFile(File javaCodeFile, File classFileOutputDir) {
        // compile args
        String[] compileArgs = new String[]{
                "-d", classFileOutputDir.getAbsolutePath(),
                javaCodeFile.getAbsolutePath()
        };

        BatchCompiler.compile(compileArgs, new PrintWriter(System.out), new PrintWriter(System.out), new CompilationProgress() {
            @Override
            public void begin(int i) {
            }

            @Override
            public void done() {
            }

            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public void setTaskName(String s) {
            }

            @Override
            public void worked(int i, int i1) {
            }
        });

        String packageName = JDTUtil.getPackageName(javaCodeFile);
        String firstName = packageName.split("\\.")[0];
        return FileUtil.file(classFileOutputDir, firstName);
    }

    private static File createJarFile(File outputDirectory, File outputJarPath) {
        try {
            FileOutputStream jarFile = new FileOutputStream(outputJarPath);
            JarOutputStream jarOut = new JarOutputStream(jarFile);

            addFilesToJar(outputDirectory, jarOut, "");

            jarOut.close();
            jarFile.close();
        } catch (IOException ignored) {
        }
        return outputJarPath;
    }

    private static void addFilesToJar(File file, JarOutputStream jarOut, String parentDir) throws IOException {
        if (file.isDirectory()) {
            String dirName = parentDir + file.getName() + File.separator;


            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    addFilesToJar(child, jarOut, dirName);
                }
            }
        } else {
            if (file.getName().endsWith(".class")) {
                FileInputStream fis = new FileInputStream(file);
                JarEntry entry = new JarEntry(parentDir + file.getName());
                jarOut.putNextEntry(entry);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    jarOut.write(buffer, 0, length);
                }
                fis.close();
                jarOut.closeEntry();
            }
        }
    }
}
