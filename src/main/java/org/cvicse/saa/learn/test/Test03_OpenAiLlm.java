package org.cvicse.saa.learn.test;

import org.jetbrains.annotations.NotNull;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

public class Test03_OpenAiLlm {
    public static void main(String[] args) {
        starter();
    }

    public static void starter() {
        OpenAiChatModel chatModel = getOpenAiChatModel();

        String call = chatModel.call("你是谁");
        System.out.println(call);
    }

    @NotNull
    private static OpenAiChatModel getOpenAiChatModel() {

        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 1. 创建 OpenAiApi API
        // =========================
        // 【重要】Spring AI 的 OpenAiApi.Builder 默认 completionsPath = "/v1/chat/completions"
        // （spring-ai-openai-1.1.2：OpenAiApi.java:2064），最终 URL = baseUrl + completionsPath。
        // 所以 baseUrl 里绝不能再带 "/v1"，否则真实请求会变成
        //   .../compatible-mode/v1/v1/chat/completions  → 404 Not Found（就是之前报的 404 - 空响应体）。
        // 正确写法：baseUrl 只写到 /compatible-mode（公网百炼同理：https://dashscope.aliyuncs.com/compatible-mode）
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .baseUrl("https://llm-lp68jcoxmr9qifkd.cn-beijing.maas.aliyuncs.com/compatible-mode")
                // 另一种等价写法：保留带 /v1 的 baseUrl，同时显式指定补全路径
                // .baseUrl("https://llm-lp68jcoxmr9qifkd.cn-beijing.maas.aliyuncs.com/compatible-mode/v1")
                // .completionsPath("/chat/completions")
                .build();

        // =========================
        // 2. 创建 ChatModel
        // =========================
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model("qwen3.7-flash-2026-07-15").build())
                .build();
    }
}
