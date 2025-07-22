package com.strange.brokenapi.analysis.prioritization;

import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;

import java.util.Comparator;

public class BrokenApiUsageComparator implements Comparator<BrokenApiUsage> {

    private int getPriority(ApiTypeEnum brokenApiType) {
        if (brokenApiType == null) {
            return Integer.MAX_VALUE;
        }
        switch (brokenApiType) {
            case CLASS -> {
                return 0;
            }
            case FIELD -> {
                return 1;
            }
            case METHOD -> {
                return 2;
            }
            case CONSTRUCTOR -> {
                return 3;
            }
            case ABSTRACT_METHOD -> {
                return 4;
            }
            case IMPORT -> {
                return 5;
            }
            default -> {
                return Integer.MAX_VALUE;
            }
        }
    }

    @Override
    public int compare(BrokenApiUsage o1, BrokenApiUsage o2) {
        ApiTypeEnum brokenApiType1 = o1.getBrokenApiType();
        ApiTypeEnum brokenApiType2 = o2.getBrokenApiType();

        int cmp = getPriority(brokenApiType1) - getPriority(brokenApiType2);
        if (cmp != 0) return cmp;

        return o1.getErrorResult().getErrorLineNumber() - o2.getErrorResult().getErrorLineNumber();
    }
}
