package com.strange.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class CodeMappingUtil {

    static class MethodInTargetLineVisitor extends ASTVisitor {
        private final Integer targetLine;

        private final CompilationUnit compilationUnit;

        @Getter
        @Setter
        private MethodDeclaration methodDeclaration;

        public MethodInTargetLineVisitor(Integer targetLine, CompilationUnit compilationUnit) {
            this.targetLine = targetLine;
            this.compilationUnit = compilationUnit;
            this.methodDeclaration = null;
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            int startLineNumber = compilationUnit.getLineNumber(node.getStartPosition());
            int endLineNumber = compilationUnit.getLineNumber(node.getStartPosition() + node.getLength() - 1);
            if (startLineNumber <= targetLine && targetLine <= endLineNumber) {
                setMethodDeclaration(node);
            }
            return super.visit(node);
        }
    }

    static class MethodMatchVisitor extends ASTVisitor {
        private final MethodDeclaration targetMethodDeclaration;

        @Getter
        @Setter
        private MethodDeclaration matchedMethodDeclaration;

        public MethodMatchVisitor(MethodDeclaration targetMethodDeclaration) {
            this.targetMethodDeclaration = targetMethodDeclaration;
            this.matchedMethodDeclaration = null;
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            if (isMatchedMethodDeclaration(targetMethodDeclaration, node)) {
                setMatchedMethodDeclaration(node);
            }
            return super.visit(node);
        }

        private boolean isMatchedMethodDeclaration(MethodDeclaration m1, MethodDeclaration m2) {
            @SuppressWarnings("unchecked")
            List<SingleVariableDeclaration> p1 = m1.parameters();
            @SuppressWarnings("unchecked")
            List<SingleVariableDeclaration> p2 = m2.parameters();

            if (Objects.equals(m1.getName().getIdentifier(), m2.getName().getIdentifier())
                    && p1.size() == p2.size()) {
                for (int i = 0; i < p1.size(); i++) {
                    SingleVariableDeclaration sv1 = p1.get(i);
                    SingleVariableDeclaration sv2 = p2.get(i);

                    if (!Objects.equals(sv1.getType().toString(), sv2.getType().toString())) {
                        return false;
                    }
                }
            } else {
                return false;
            }

            AbstractTypeDeclaration t1 = getParentTypeDeclaration(m1);
            AbstractTypeDeclaration t2 = getParentTypeDeclaration(m2);
            if (t1 != null && t2 != null) {
                return Objects.equals(t1.getName().getIdentifier(), t2.getName().getIdentifier());
            } else {
                return false;
            }
        }

        private AbstractTypeDeclaration getParentTypeDeclaration(MethodDeclaration m) {
            ASTNode node = m;
            while (node != null) {
                if (node instanceof AbstractTypeDeclaration) {
                    return (AbstractTypeDeclaration) node;
                } else {
                    node = node.getParent();
                }
            }
            return null;
        }
    }

    public static Integer mappingCodeWithLineNumber(File currentFile, File targetFile, Integer lineNumber) {
        try {
            String[] currentLines = Files.readAllLines(currentFile.toPath()).toArray(new String[0]);
            String[] newFileLines = Files.readAllLines(targetFile.toPath()).toArray(new String[0]);

            if (lineNumber < 1 || lineNumber > currentLines.length) {
                return lineNumber;
            }

            // Obtain the approximate range where the target lines reside, rather than mapping the entire file. 
            // If the line number falls within a method, then you only need to map all the line numbers inside that method
            List<Pair<String, Integer>> targetLineList = new ArrayList<>();

            CompilationUnit oldFileUnit = JDTUtil.parseCode(currentFile);
            MethodInTargetLineVisitor methodInTargetLineVisitor = new MethodInTargetLineVisitor(lineNumber, oldFileUnit);
            oldFileUnit.accept(methodInTargetLineVisitor);
            MethodDeclaration methodDeclaration = methodInTargetLineVisitor.getMethodDeclaration();
            if (methodDeclaration != null) {
                CompilationUnit newFileUnit = JDTUtil.parseCode(targetFile);
                MethodMatchVisitor methodMatchVisitor = new MethodMatchVisitor(methodDeclaration);
                newFileUnit.accept(methodMatchVisitor);
                MethodDeclaration matchedMethodDeclaration = methodMatchVisitor.getMatchedMethodDeclaration();
                if (matchedMethodDeclaration != null) {
                    int startLineNumber = newFileUnit.getLineNumber(matchedMethodDeclaration.getStartPosition());
                    int endLineNumber = newFileUnit.getLineNumber(matchedMethodDeclaration.getStartPosition() + matchedMethodDeclaration.getLength() - 1);

                    for (int i = startLineNumber; i <= endLineNumber; i++) {
                        targetLineList.add(new Pair<>(newFileLines[i - 1], i));
                    }
                }
            }

            if (CollUtil.isEmpty(targetLineList)) {
                for (int i = 0; i < newFileLines.length; i++) {
                    targetLineList.add(new Pair<>(newFileLines[i], i + 1));
                }
            }

            String referenceLine = currentLines[lineNumber - 1];
            int bestLine = -1;
            double bestScore = -1.0;

            for (Pair<String, Integer> pair : targetLineList) {
                String statement = pair.getKey();
                Integer line = pair.getValue();
                double score = StringSimilarityUtil.levenshteinSimilarity(referenceLine, statement);
                if (score > bestScore) {
                    bestScore = score;
                    bestLine = line;
                }
            }
            if (bestLine == -1) bestLine = lineNumber;
            return bestLine;
        } catch (Exception e) {
            return lineNumber;
        }
    }

    @Deprecated
    public static Map<Integer, Integer> mappingCode(File currentFile, File targetFile) {
        try {
            List<String> original = Files.readAllLines(Paths.get(currentFile.toURI()));
            List<String> revised = Files.readAllLines(Paths.get(targetFile.toURI()));

            Patch<String> patch = DiffUtils.diff(original, revised);
            List<AbstractDelta<String>> deltas = patch.getDeltas();
            deltas.sort(Comparator.comparingInt(d -> d.getTarget().getPosition()));
            Map<Integer, Integer> mapping = new HashMap<>();
            int origIdx = 0, revIdx = 0, deltaIdx = 0;

            while (revIdx < revised.size()) {
                if (deltaIdx < deltas.size()
                        && deltas.get(deltaIdx).getTarget().getPosition() == revIdx) {
                    AbstractDelta<String> d = deltas.get(deltaIdx);
                    Chunk<String> srcChunk = d.getSource();
                    Chunk<String> tgtChunk = d.getTarget();

                    int delOrigSize = srcChunk.getLines().size();
                    int delRevSize = tgtChunk.getLines().size();

                    for (int i = 0; i < delRevSize; i++) {
                        mapping.put(revIdx + i, -1);
                    }

                    revIdx += delRevSize;
                    origIdx += delOrigSize;
                    deltaIdx++;
                } else {
                    mapping.put(revIdx, origIdx);
                    revIdx++;
                    origIdx++;
                }
            }
            return mapping;
        } catch (Exception e) {
            log.warn("MappingCodeError: ", e);
            return new HashMap<>();
        }
    }
}
