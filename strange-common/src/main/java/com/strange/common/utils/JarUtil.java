package com.strange.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JarUtil {

    public static ClassLoader loadClassFromJar(String... jarPaths) throws MalformedURLException {
        final Path[] jarPathArr = new Path[jarPaths.length];
        // get the path of the Jar package
        for (int i = 0; i < jarPaths.length; i++) {
            Path path = Paths.get(jarPaths[i]).toAbsolutePath();
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("invalid jar path: " + path);
            }
            jarPathArr[i] = path;
        }
        return loadClassFromJar(jarPathArr);
    }

    public static ClassLoader loadClassFromJar(Path... jarPaths) throws MalformedURLException {
        final List<URL> classPathUrls = new ArrayList<>(jarPaths.length);
        for (Path jarPath : jarPaths) {
            if (jarPath == null || !Files.exists(jarPath) || Files.isDirectory(jarPath)) {
                throw new IllegalArgumentException("invalid jar path: " + jarPath);
            }
            classPathUrls.add(jarPath.toUri().toURL());
        }
        return new URLClassLoader(classPathUrls.toArray(new URL[0]), ClassLoader.getSystemClassLoader().getParent());
    }

}
