package com.strange.brokenapi.analysis.dependency.maven;

import cn.hutool.core.util.ReUtil;
import org.apache.maven.model.Model;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Properties;


public class PomHelper {

    private static final String REGEX = "\\$\\{(.+?)\\}";

    public static String normalize(File pomFile, String string) {
        Model model = getModel(pomFile);
        if (model == null) return string;
        Properties properties = model.getProperties();
        properties.put("project.basedir", pomFile.getParentFile().getAbsolutePath());

        String normalizedString = string;
        try {
            normalizedString = ReUtil.replaceAll(string, REGEX, matchResult -> {
                String key = matchResult.group(1);
                return properties.getOrDefault(key, key).toString();
            });
        } catch (Exception ignored) {
        }
        return normalizedString;
    }

    public static Dependency getDependency(File pomFile, String groupId, String artifactId) {
        Model model = getModel(pomFile);
        if (model == null) return null;
        List<Dependency> dependencies = model.getDependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            for (Dependency dependency : dependencies) {
                if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                    return dependency;
                }
            }
        }
        return null;
    }

    private static Model getModel(File pomFile) {
        try {
            FileReader fileReader = new FileReader(pomFile.getAbsolutePath());
            MavenXpp3Reader reader = new MavenXpp3Reader();
            return reader.read(fileReader);
        } catch (Exception e) {
            return null;
        }
    }
}
