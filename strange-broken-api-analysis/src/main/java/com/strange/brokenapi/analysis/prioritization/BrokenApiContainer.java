package com.strange.brokenapi.analysis.prioritization;

import com.strange.brokenapi.analysis.BrokenApiUsage;
import lombok.Data;
import lombok.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Data
public class BrokenApiContainer implements Iterable<BrokenApiUsage> {
    private File javaCodeFile;

    private List<BrokenApiUsage> brokenApiUsageList;

    private List<BrokenApiUsage> priorizedBrokenApiUsageList;

    public BrokenApiContainer( File javaCodeFile,  List<BrokenApiUsage> brokenApiUsageList) {
        this.javaCodeFile = javaCodeFile;
        this.brokenApiUsageList = brokenApiUsageList;
        prioritizeBrokenApiUsage();
    }

    private void prioritizeBrokenApiUsage() {
        priorizedBrokenApiUsageList = new ArrayList<>(brokenApiUsageList);
        priorizedBrokenApiUsageList.sort(new BrokenApiUsageComparator());
    }

    @Override
    public Iterator<BrokenApiUsage> iterator() {
        return priorizedBrokenApiUsageList.iterator();
    }
}
