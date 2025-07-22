package com.strange.common.utils;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class URLUtil {
    public static URL[] stringsToUrls(String[] paths) throws MalformedURLException {
        URL[] urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
            urls[i] = new File(paths[i]).toURI().toURL();
        }
        return urls;
    }

    public static URL[] stringsToUrls(List<String> pathList) throws MalformedURLException {
        return stringsToUrls(pathList.toArray(new String[0]));
    }

    public static String join(String... args) {
        if (args == null || args.length == 0) {
            return "";
        }

        String url = args[0];

        for (int i = 1; i < args.length; i++) {
            if (url.endsWith("/")) {
                url += args[i];
            } else {
                url += "/" + args[i];
            }
        }
        return url;
    }

    /**
     * file1 相对于 file2的相对路径
     *
     * @param file1
     * @param file2
     * @return
     */
    public static String getRelativePath(File file1, File file2) {
        // 转换为 Path 对象
        Path path1 = Paths.get(file1.getAbsolutePath());
        Path path2 = Paths.get(file2.getAbsolutePath());
        Path relativizePath = path2.relativize(path1);
        return relativizePath.toString();
    }

    public static void main(String[] args) {
        System.out.println(getRelativePath(FileUtil.file("/Users/example/project/src/main/java/org/asd/xx"),
                FileUtil.file("/Users/example/project/src/main/java")));
    }
}
