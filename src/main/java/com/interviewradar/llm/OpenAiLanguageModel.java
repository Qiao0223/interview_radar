package com.interviewradar.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class OpenAiLanguageModel implements LanguageModel {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    /**
     * @param apiKey  OpenAI-兼容接口的 API Key (Bearer)
     * @param model   要调用的模型名称
     * @param baseUrl OpenAI 兼容通道的根 URL，例如：https://dashscope.aliyuncs.com/compatible-mode/v1
     */
    public OpenAiLanguageModel(String apiKey, String model, String baseUrl) {
        this.apiKey  = apiKey;
        this.model   = model;
        this.baseUrl = baseUrl;
    }

    @Override
    public String generate(String prompt) {
        String url = baseUrl.endsWith("/")
                ? baseUrl + "chat/completions"
                : baseUrl + "/chat/completions";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + apiKey);
            post.setHeader("Content-Type", "application/json");

            // 构造请求体
            JsonNode bodyNode = MAPPER.createObjectNode()
                    .put("model", model)
                    .set("messages", MAPPER.createArrayNode()
                            .add(MAPPER.createObjectNode()
                                    .put("role", "user")
                                    .put("content", prompt)
                            )
                    );
            StringEntity entity = new StringEntity(MAPPER.writeValueAsString(bodyNode), StandardCharsets.UTF_8);
            post.setEntity(entity);

            try (CloseableHttpResponse response = client.execute(post)) {
                int status = response.getStatusLine().getStatusCode();
                String respBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (status >= 200 && status < 300) {
                    JsonNode root = MAPPER.readTree(respBody);
                    JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
                    return contentNode.isTextual() ? contentNode.asText() : "";
                } else {
                    throw new RuntimeException("调用 OpenAI 兼容接口失败，状态码=" + status + "，返回内容=" + respBody);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("调用 OpenAI 兼容接口时发生异常", e);
        }
    }
}
