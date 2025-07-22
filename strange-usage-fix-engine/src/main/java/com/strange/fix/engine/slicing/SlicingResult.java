package com.strange.fix.engine.slicing;

import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class SlicingResult {

    private List<Integer> taintedLineNumbers;

    private String slicedCode;

    public SlicingResult( List<Integer> taintedLineNumbers,  String slicedCode) {
        this.taintedLineNumbers = taintedLineNumbers;
        this.slicedCode = slicedCode;
    }
}
