package com.strange.fix.engine.similiarity;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import com.strange.fix.engine.normalization.ASTNormalizer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SimilarityService {

    private static final Predictor<String, float[]> PREDICTOR;

    static {
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/mchochlov/codebert-base-cd-ft")
                .optEngine("PyTorch")
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .optProgress(new ProgressBar())
                .build();
        ZooModel<String, float[]> model;
        try {
            model = ModelZoo.loadModel(criteria);
            PREDICTOR = model.newPredictor();
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new RuntimeException("SimilarityService Initialization Fail", e);
        }
    }

    private static double cosineSimilarity(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("VectorLengthsUnMatch");
        }
        double dot = 0, n1 = 0, n2 = 0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            n1 += v1[i] * v1[i];
            n2 += v2[i] * v2[i];
        }
        return dot / (Math.sqrt(n1) * Math.sqrt(n2));
    }

    public static CodeSimilarityResult calculateSimilarity( String originalCode,  String inputCode) {
        String normalizedCode = null;
        try {
            String normalizedOriginalCode = ASTNormalizer.normalize(originalCode);
            normalizedCode = ASTNormalizer.normalize(inputCode);
            float[] emb1 = PREDICTOR.predict(normalizedOriginalCode);
            float[] emb2 = PREDICTOR.predict(normalizedCode);
            double similarScore = cosineSimilarity(emb1, emb2);
            return new CodeSimilarityResult(inputCode, normalizedCode, similarScore);
        } catch (TranslateException e) {
            log.warn("CalculateCodeSimilarityError");
            return new CodeSimilarityResult(normalizedCode, inputCode, 0.0);
        }
    }

    public static List<CodeSimilarityResult> batchCalculateSimilarity( String src,  List<String> codeList) {
        List<CodeSimilarityResult> codeSimilarityResultList = new ArrayList<>();
        String normalizedSrc = ASTNormalizer.normalize(src);
        for (String code : codeList) {
            String normalizedCode = ASTNormalizer.normalize(code);
            CodeSimilarityResult codeSimilarityResult = calculateSimilarity(normalizedSrc, normalizedCode);
            codeSimilarityResultList.add(codeSimilarityResult);
        }
        return codeSimilarityResultList;
    }
}
