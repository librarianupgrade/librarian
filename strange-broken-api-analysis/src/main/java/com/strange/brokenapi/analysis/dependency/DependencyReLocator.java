package com.strange.brokenapi.analysis.dependency;

public final class DependencyReLocator {

    public static String relocate(DependencyNode dependencyNode) {
        if (dependencyNode == null) return null;
        return switch (dependencyNode.getShortSignature()) {
            case "commons-pool:commons-pool" -> "org.apache.commons:commons-pool2";
            case "org.apache.commons:commons-pool2" -> "commons-pool:commons-pool";
            case "org.apache.tika:tika-parsers" -> "org.apache.tika:tika-parser-pkg-module";
            case "org.apache.tika:tika-parser-pkg-module" -> "org.apache.tika:tika-parsers";
            case "commons-lang:commons-lang" -> "org.apache.commons:commons-lang3";
            case "org.apache.commons:commons-lang3" -> "commons-lang:commons-lang";
            default -> dependencyNode.getShortSignature();
        };
    }
}
