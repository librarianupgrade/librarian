package com.strange.fix.engine;

import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.fix.engine.enums.FixEnum;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class FixResult {
    private Boolean fixStatus;

    private FixEnum fixType;

    private List<BrokenApiUsage> brokenApiUsageList;

    private List<BrokenApiUsage> addedBrokenApiUsageList;

    public boolean isFixed() {
        return fixType != FixEnum.NOT_FIXED;
    }

    public boolean isNotFixed() {
        return fixType == FixEnum.NOT_FIXED;
    }
}
