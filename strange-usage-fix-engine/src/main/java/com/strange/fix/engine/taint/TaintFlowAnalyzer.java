package com.strange.fix.engine.taint;

import cn.hutool.core.lang.Pair;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.JDTUtil;
import soot.*;
import soot.jimple.BinopExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.*;

public class TaintFlowAnalyzer {

    private static final Integer MAX_ANALYSIS_DEPTH = 1;

    private final File taintedFile;

    private final ApiSignature apiSignature;

    private final SootMethod sootMethod;

    private final List<Integer> lineNumbers;

    private final String scopedClassName;

    private Set<FlowAbstraction> flowAbstractionSet; // flow abstraction set for one unit taint analysis

    private Set<FlowAbstraction> allFlowAbstractionSet; // all the flow abstraction for taint analysis

    private Set<String> taintedFieldNameSet; // tainted field name set

    public TaintFlowAnalyzer(File taintedFile, ApiSignature apiSignature, List<Integer> lineNumbers, SootMethod sootMethod, String scopedClassName) {
        this.taintedFile = taintedFile;
        this.apiSignature = apiSignature;
        this.sootMethod = sootMethod;
        this.lineNumbers = lineNumbers;
        this.scopedClassName = scopedClassName;
    }

    public List<Integer> taintAnalysis() {
        if (allFlowAbstractionSet == null) {
            this.taintedFieldNameSet = new HashSet<>();
            this.allFlowAbstractionSet = new HashSet<>();
            try {
                this.doAnalysis();
            } catch (Exception ignored) {
            }
        }

        Set<Integer> lineNumberSet = new HashSet<>(lineNumbers);
        for (FlowAbstraction flowAbstraction : allFlowAbstractionSet) {
            Unit source = flowAbstraction.getSource();
            if (source != null) {
                int sourceLine = source.getJavaSourceStartLineNumber();
                if (sourceLine > 0) {
                    lineNumberSet.add(sourceLine);
                }
            }
        }

        // add tainted field line number
        List<Pair<String, Integer>> fieldInfoList = JDTUtil.getFieldInfo(taintedFile);
        for (Pair<String, Integer> fieldInfo : fieldInfoList) {
            String fieldName = fieldInfo.getKey();
            Integer lineNumber = fieldInfo.getValue();
            if (taintedFieldNameSet.contains(fieldName)) {
                lineNumberSet.add(lineNumber);
            }
        }

        List<Integer> lineNumberList = new ArrayList<>(lineNumberSet);
        lineNumberList.sort(Integer::compareTo);
        return lineNumberList;
    }

    private void doAnalysis() {
        Body body = sootMethod.retrieveActiveBody();
        List<Unit> targetUnitList = findTargetedLineNumberUnit(body, lineNumbers);
        Set<FlowAbstraction> initialTaintedFlowAbstraction = getInitialTaintedFlowAbstraction(targetUnitList);
        for (Unit unit : targetUnitList) {
            Set<Unit> visitedUnitSet = new HashSet<>();
            flowAbstractionSet = new HashSet<>(initialTaintedFlowAbstraction);
            intraTaintAnalysis(null, sootMethod, unit, 0, visitedUnitSet);
            allFlowAbstractionSet.addAll(flowAbstractionSet);
        }
    }

    private void intraTaintAnalysis(List<Value> sourceInputParams, SootMethod sootMethod, Unit currentUnit, int depth, Set<Unit> visitedUnitSet) {
        if (depth > MAX_ANALYSIS_DEPTH) return;

        Queue<Unit> queue = new LinkedList<>();
        queue.add(currentUnit);
        UnitGraph graph = new ExceptionalUnitGraph(sootMethod.retrieveActiveBody());

        while (!queue.isEmpty()) {
            Unit node = queue.poll();
            visitedUnitSet.add(node);
            analysisUnit(sourceInputParams, sootMethod, node, depth);

            List<Unit> predUnits = graph.getPredsOf(node);
            for (Unit unit : predUnits) {
                if (!visitedUnitSet.contains(unit)) {
                    queue.add(unit);
                }
            }
        }
    }

    private List<Unit> findTargetedLineNumberUnit(Body body, List<Integer> targetedLineNumber) {
        List<Unit> targetedUnitList = new ArrayList<>();
        for (Unit unit : body.getUnits()) {
            if (targetedLineNumber.contains(unit.getJavaSourceStartLineNumber())) {
                targetedUnitList.add(unit);
            }
        }
        return targetedUnitList;
    }

    private void processOperationIsTaint(Set<FlowAbstraction> flowAbstractionSet, Unit unit, Value operation) {
        if (operation instanceof JStaticInvokeExpr staticInvokeExpr) {
            SootMethod invokedMethod = staticInvokeExpr.getMethod();
            if (invokedMethod.getName().equals(apiSignature.getMethodName())) {
                List<Value> args = staticInvokeExpr.getArgs();
                for (Value arg : args) {
                    if (arg instanceof Local local) {
                        FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                        flowAbstractionSet.add(flowAbstraction);
                    }
                }
            }
        } else if (operation instanceof JVirtualInvokeExpr virtualInvokeExpr) {
            Value base = virtualInvokeExpr.getBase();
            if (base instanceof Local local) {
                FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                flowAbstractionSet.add(flowAbstraction);
            }
            List<Value> args = virtualInvokeExpr.getArgs();
            for (Value arg : args) {
                if (arg instanceof Local local) {
                    FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                    flowAbstractionSet.add(flowAbstraction);
                }
            }
        } else if (operation instanceof JSpecialInvokeExpr specialInvokeExpr) {
            Value base = specialInvokeExpr.getBase();
            if (base instanceof Local local) {
                FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                flowAbstractionSet.add(flowAbstraction);
            } else if (base instanceof JInstanceFieldRef instanceFieldRef) {
                SootField field = instanceFieldRef.getField();
                FlowAbstraction flowAbstraction = new FlowAbstraction(unit, field);
                flowAbstractionSet.add(flowAbstraction);
                taintedFieldNameSet.add(field.getName());
            }
        } else if (operation instanceof JInstanceFieldRef instanceFieldRef) {
            SootField field = instanceFieldRef.getField();
            FlowAbstraction flowAbstraction = new FlowAbstraction(unit, field);
            flowAbstractionSet.add(flowAbstraction);
            taintedFieldNameSet.add(field.getName());
        } else if (operation instanceof JInterfaceInvokeExpr interfaceInvokeExpr) {
            Value base = interfaceInvokeExpr.getBase();
            if (base instanceof Local local) {
                if (!"this".equals(local.getName())) {
                    FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                    flowAbstractionSet.add(flowAbstraction);
                    for (Value arg : interfaceInvokeExpr.getArgs()) {
                        if (arg instanceof Local local2) {
                            FlowAbstraction flowAbstraction2 = new FlowAbstraction(unit, local2);
                            flowAbstractionSet.add(flowAbstraction2);
                        }
                    }
                }
            }
        } else if (operation instanceof Local local) {
            FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
            flowAbstractionSet.add(flowAbstraction);
        }
    }

    // init the tainted flow abstraction
    private Set<FlowAbstraction> getInitialTaintedFlowAbstraction(List<Unit> unitList) {
        Set<FlowAbstraction> flowAbstractionSet = new HashSet<>();
        for (Unit unit : unitList) {
            if (unit instanceof JAssignStmt assignStmt) {
                // if is assign statement
                Value leftOp = assignStmt.getLeftOp();
                Value rightOp = assignStmt.getRightOp();
                processOperationIsTaint(flowAbstractionSet, unit, leftOp);
                processOperationIsTaint(flowAbstractionSet, unit, rightOp);
            } else if (unit instanceof JInvokeStmt invokeStmt) {
                // if is invoke statement
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                if (invokeExpr instanceof JSpecialInvokeExpr specialInvokeExpr) {
                    Value base = specialInvokeExpr.getBase();
                    if (base instanceof Local local) {
                        if (!"this".equals(local.getName())) {
                            FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                            flowAbstractionSet.add(flowAbstraction);
                        }
                    } else if (base instanceof JInstanceFieldRef instanceFieldRef) {
                        SootField field = instanceFieldRef.getField();
                        FlowAbstraction flowAbstraction = new FlowAbstraction(unit, field);
                        flowAbstractionSet.add(flowAbstraction);
                        taintedFieldNameSet.add(field.getName());
                    }
                    List<Value> args = specialInvokeExpr.getArgs();
                    for (Value arg : args) {
                        if (arg instanceof Local local) {
                            FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                            flowAbstractionSet.add(flowAbstraction);
                        }
                    }
                } else if (invokeExpr instanceof JStaticInvokeExpr staticInvokeExpr) {
                    List<Value> args = staticInvokeExpr.getArgs();
                    for (Value arg : args) {
                        if (arg instanceof Local local) {
                            FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                            flowAbstractionSet.add(flowAbstraction);
                        }
                    }
                } else if (invokeExpr instanceof JInterfaceInvokeExpr interfaceInvokeExpr) {
                    Value base = interfaceInvokeExpr.getBase();
                    if (base instanceof Local local) {
                        if (!"this".equals(local.getName())) {
                            FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                            flowAbstractionSet.add(flowAbstraction);
                        }
                    }
                } else if (invokeExpr instanceof JInstanceFieldRef instanceFieldRef) {
                    SootField field = instanceFieldRef.getField();
                    FlowAbstraction flowAbstraction = new FlowAbstraction(unit, field);
                    flowAbstractionSet.add(flowAbstraction);
                    taintedFieldNameSet.add(field.getName());
                }
            }
        }
        return flowAbstractionSet;
    }

    protected void analysisUnit(List<Value> sourceInputParams, SootMethod sootMethod, Unit unit, int depth) {
        if (depth > MAX_ANALYSIS_DEPTH) return;

        if (unit instanceof JInvokeStmt invokeStmt) {
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (invokeExpr instanceof JSpecialInvokeExpr specialInvokeExpr) {
                // resolve the constructor method
                Value base = specialInvokeExpr.getBase();
                if (checkIsTaint(base)) {
                    List<Value> args = specialInvokeExpr.getArgs();
                    for (Value arg : args) {
                        if (arg instanceof Local local) {
                            FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                            flowAbstractionSet.add(flowAbstraction);
                        }
                    }
                }
            }
        } else if (unit instanceof JAssignStmt assignStmt) {
            Value leftOp = assignStmt.getLeftOp();
            Value rightOp = assignStmt.getRightOp();

            if (checkIsTaint(leftOp)) { // taint flow analysis part
                if (rightOp instanceof Local local) {
                    FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                    flowAbstractionSet.add(flowAbstraction);
                } else if (rightOp instanceof JArrayRef arrayRef) {
                    Value base = arrayRef.getBase();
                    Value index = arrayRef.getIndex();
                    if (base instanceof Local) {
                        FlowAbstraction flowAbstraction = new FlowAbstraction(unit, (Local) base);
                        flowAbstractionSet.add(flowAbstraction);
                    }
                    if (index instanceof Local) {
                        FlowAbstraction flowAbstraction = new FlowAbstraction(unit, (Local) index);
                        flowAbstractionSet.add(flowAbstraction);
                    }
                } else if (rightOp instanceof JInstanceFieldRef instanceFieldRef) {
                    Local local = (Local) instanceFieldRef.getBase();
                    SootField field = instanceFieldRef.getField();

                    FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local, field);
                    flowAbstractionSet.add(flowAbstraction);
                    taintedFieldNameSet.add(field.getName());
                } else if (rightOp instanceof JCastExpr castExpr) {
                    Value op = castExpr.getOp();
                    if (op instanceof Local local) {
                        FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                        flowAbstractionSet.add(flowAbstraction);
                    }
                } else if (rightOp instanceof JVirtualInvokeExpr virtualInvokeExpr) {
                    SootMethod invokeMethod = virtualInvokeExpr.getMethod();

                    if (scopedClassName == null || invokeMethod.getDeclaringClass().getName().equals(scopedClassName)) {
                        // If it is a method call from a source class
                        List<Value> args = virtualInvokeExpr.getArgs();
                        depthFirstTaintAnalysis(args, invokeMethod, depth + 1);
                    } else {
                        List<Value> args = virtualInvokeExpr.getArgs();
                        for (Value arg : args) {
                            if (arg instanceof Local local) {
                                FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                                flowAbstractionSet.add(flowAbstraction);
                            }
                        }

                        // resolve .toString(), .toJSONString(), .getString("xxx")
                        Value base = virtualInvokeExpr.getBase();
                        if (base instanceof Local local) {
                            FlowAbstraction flowAbstraction;
                            flowAbstraction = new FlowAbstraction(unit, local);
                            flowAbstractionSet.add(flowAbstraction);
                        }
                    }
                } else if (rightOp instanceof JSpecialInvokeExpr specialInvokeExpr) {
                    SootMethod invokeMethod = specialInvokeExpr.getMethod();

                    if (scopedClassName == null || invokeMethod.getDeclaringClass().getName().equals(scopedClassName)) {
                        List<Value> args = specialInvokeExpr.getArgs();
                        depthFirstTaintAnalysis(args, invokeMethod, depth + 1);
                    } else {
                        List<Value> args = specialInvokeExpr.getArgs();
                        for (Value arg : args) {
                            if (arg instanceof Local local) {
                                FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                                flowAbstractionSet.add(flowAbstraction);
                            }
                        }
                    }
                } else if (rightOp instanceof JStaticInvokeExpr staticInvokeExpr) {
                    SootMethod invokedMethod = staticInvokeExpr.getMethod();

                    if (scopedClassName == null || invokedMethod.getDeclaringClass().getName().equals(scopedClassName)) {
                        List<Value> args = staticInvokeExpr.getArgs();
                        depthFirstTaintAnalysis(args, invokedMethod, depth + 1);
                    } else {
                        List<Value> args = staticInvokeExpr.getArgs();
                        for (Value arg : args) {
                            if (arg instanceof Local local) {
                                FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                                flowAbstractionSet.add(flowAbstraction);
                            }
                        }
                    }
                } else if (rightOp instanceof JInterfaceInvokeExpr interfaceInvokeExpr) {
                    Value base = interfaceInvokeExpr.getBase();
                    if (base instanceof Local local) {
                        if (!"this".equals(local.getName())) {
                            FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                            flowAbstractionSet.add(flowAbstraction);
                        }
                    }
                } else if (rightOp instanceof BinopExpr binopExpr) {
                    Value op1 = binopExpr.getOp1();
                    Value op2 = binopExpr.getOp2();
                    if (op1 instanceof Local local) {
                        FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                        flowAbstractionSet.add(flowAbstraction);
                    }
                    if (op2 instanceof Local local) {
                        FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                        flowAbstractionSet.add(flowAbstraction);
                    }
                }
            }
        } else if (unit instanceof JReturnStmt returnStmt) {
            if (depth == 0) return;
            Value op = returnStmt.getOp();
            // default taint
            if (op instanceof Local local) {
                FlowAbstraction flowAbstraction = new FlowAbstraction(unit, local);
                flowAbstractionSet.add(flowAbstraction);
            } else {
                FlowAbstraction flowAbstraction = new FlowAbstraction(unit, op);
                flowAbstractionSet.add(flowAbstraction);
            }
        } else if (unit instanceof JIdentityStmt identityStmt) {
            Value leftValue = identityStmt.getLeftOp();
//            if (checkIsTaint(leftValue)) {
//                Local leftLocal = (Local) leftValue;
//                Body body = sootMethod.retrieveActiveBody();
//                List<Local> parameterLocals = body.getParameterLocals();
//                int i = parameterLocals.indexOf(leftLocal);
//                if (i != -1
//                        && (scopedClassName == null || sootMethod.getDeclaringClass().getName().equals(scopedClassName))) {
//                    assert i < sourceInputParams.size();
//                    // add a flow abstract
//                    Value value = sourceInputParams.get(i);
//                    if (value instanceof Local) {
//                        FlowAbstraction flowAbstraction = new FlowAbstraction(unit, (Local) value, null);
//                        flowAbstractionSet.add(flowAbstraction);
//                    }
//
//                }
//            }
        }
    }

    // inter-procedure taint analysis
    private void depthFirstTaintAnalysis(List<Value> sourceInputParams, SootMethod sootMethod, int depth) {
        if (depth > MAX_ANALYSIS_DEPTH) return;
        String name = sootMethod.getDeclaringClass().getName();

        if (scopedClassName != null && !Objects.equals(name, scopedClassName)) return;

        UnitGraph cfg = new ExceptionalUnitGraph(sootMethod.retrieveActiveBody());
        List<Unit> tails = cfg.getTails();
        Set<Unit> visitedUnitSet = new HashSet<>();
        for (Unit unit : tails) {
            intraTaintAnalysis(sourceInputParams, sootMethod, unit, depth, visitedUnitSet);
        }
    }

    // check whether value is tainted
    private boolean checkIsTaint(Value value) {
        if (value instanceof Local local) {
            for (FlowAbstraction flowAbstraction : flowAbstractionSet) {
                if (local.equals(flowAbstraction.getLocal())) {
                    return true;
                }
            }
        } else if (value instanceof JInstanceFieldRef instanceFieldRef) {
            SootField field = instanceFieldRef.getField();
            assert field != null;
            Local local = (Local) instanceFieldRef.getBase();
            for (FlowAbstraction flowAbstraction : flowAbstractionSet) {
                if (local.equals(flowAbstraction.getLocal())
                        && flowAbstraction.getField() != null
                        && field.getSignature().equals(flowAbstraction.getField().getSignature())) {
                    return true;
                }
            }
        } else if (value instanceof JArrayRef arrayRef) {
            Local local = (Local) arrayRef.getBase();
            for (FlowAbstraction flowAbstraction : flowAbstractionSet) {
                if (local.equals(flowAbstraction.getLocal())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<String> getTaintedFieldNameSet() {
        return taintedFieldNameSet;
    }
}
