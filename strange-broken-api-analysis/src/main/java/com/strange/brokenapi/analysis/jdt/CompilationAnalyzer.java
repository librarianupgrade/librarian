package com.strange.brokenapi.analysis.jdt;

import java.util.Set;

public abstract class CompilationAnalyzer {

    protected final static Set<String> SOURCE_DIR = Set.of("src", "Java", "JavaSource", "test", "tests");
}
