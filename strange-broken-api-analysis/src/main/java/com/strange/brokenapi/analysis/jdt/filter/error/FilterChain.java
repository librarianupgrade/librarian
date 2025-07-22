package com.strange.brokenapi.analysis.jdt.filter.error;

import com.strange.brokenapi.analysis.jdt.filter.ProblemFilter;
import org.eclipse.jdt.core.compiler.IProblem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FilterChain implements ProblemFilter {

    private final List<ProblemFilter> filterList;

    public FilterChain(Map<String, File> javaCodeFileMap) {
        filterList = new ArrayList<>();
        addFilter(new LogFilter());
        addFilter(new BuilderFilter(javaCodeFileMap));
        addFilter(new SneakyThrowsFilter(javaCodeFileMap));
        addFilter(new ConstructorFilter(javaCodeFileMap));
        addFilter(new GetterFilter(javaCodeFileMap));
        addFilter(new SetterFilter(javaCodeFileMap));
        addFilter(new OtherFilter(javaCodeFileMap));
        addFilter(new EnumFlagFilter(javaCodeFileMap));
    }

    public void addFilter(ProblemFilter filter) {
        this.filterList.add(filter);
    }

    @Override
    public boolean filter(File sourceCodeFile, IProblem problem) {
        for (ProblemFilter filter : filterList) {
            if (!filter.filter(sourceCodeFile, problem)) return false;
        }
        return true;
    }
}
