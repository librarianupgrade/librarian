package com.strange.fix.engine.llm;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.strange.common.utils.JDTUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class LLMFixModel {

    private final static String CONFIG_FILE_NAME = "application.properties";

    private final static String SYSTEM_ROLE = "system";

    private final static String USER_ROLE = "user";

    private final static String LLM_ROLE = "assistant";

    private final static String SYSTEM_PROMPT = "You are an experienced Java developer specializing in migrating projects to newer versions of libraries, handling broken API changes effectively.";

    private final static String MODEL_NAME;

    private static final String API_KEY;

    private static final String BASE_URL;

    static {
        InputStream CONFIGURATION_INPUTSTREAM = new ClassPathResource(CONFIG_FILE_NAME).getStream();
        Properties properties = new Properties();
        try {
            properties.load(CONFIGURATION_INPUTSTREAM);
            API_KEY = (String) properties.get("config.llm.api-key");
            BASE_URL = (String) properties.get("config.llm.base-url");
            MODEL_NAME = (String) properties.get("config.llm.model-name");
        } catch (IOException e) {
            log.error("ReadPropertyError: ", e);
            throw new RuntimeException(e);
        }
    }

    private final List<JSONObject> chatHistoryList;

    public LLMFixModel() {
        this.chatHistoryList = new ArrayList<>();
        JSONObject system = JSONUtil.createObj().putOnce("role", SYSTEM_ROLE).putOnce("content", SYSTEM_PROMPT);
        chatHistoryList.add(system);
    }

    public String call(String message) {
        JSONObject userAsk = JSONUtil.createObj()
                .putOnce("role", USER_ROLE)
                .putOnce("content", message);
        this.chatHistoryList.add(userAsk);
        String answer = null;
        JSONObject result = null;
        try {
            // get the request body
            JSONObject requestBody = new JSONObject()
                    .putOnce("model", MODEL_NAME)
                    .putOnce("messages", new JSONArray(chatHistoryList));
            // request the LLM API
            String respBody = HttpRequest.post(BASE_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .execute().body();

            // get the answer from the JSON Object
            JSONObject jsonObject = JSONUtil.parseObj(respBody);
            JSONArray choices = jsonObject.getJSONArray("choices");
            result = choices.get(0, JSONObject.class, Boolean.TRUE);
        } catch (Exception e) {
            log.error("LLMAnswerError: ", e);
            return null;
        }

        if (result != null) {
            answer = result.getJSONObject("message").getStr("content");
            JSONObject llm = JSONUtil.createObj().putOnce("role", LLM_ROLE).putOnce("content", answer);
            this.chatHistoryList.add(llm);
        } else {
            this.chatHistoryList.remove(chatHistoryList.size() - 1);
            return null;
        }

        String answerCode = parserAnswer(answer);
        log.info("GetLLMAnswer: {}", answerCode.replaceAll("\n", ""));
        if (checkAnswer(answerCode)) {
            return answerCode;
        } else {
            return null;
        }
    }

    private String parserAnswer(String answerText) {
        if (answerText == null) return null;
        Pattern pattern = Pattern.compile("(?s)```\\w*\\R(.*?)```");
        Matcher matcher = pattern.matcher(answerText);
        List<String> codeBlockList = new ArrayList<>();
        while (matcher.find()) {
            String codeBlock = matcher.group(1).trim();
            codeBlockList.add(codeBlock);
        }
        return String.join("\n", codeBlockList);
    }

    private boolean checkAnswer(String answerCode) {
        CompilationUnit fixedCompilationUnit = null;
        try {
            fixedCompilationUnit = JDTUtil.parseCode(answerCode);
        } catch (Exception e) {
            log.error("ParseLLMAnswerError: ", e);
            return false;
        }
        return true;
    }
}
