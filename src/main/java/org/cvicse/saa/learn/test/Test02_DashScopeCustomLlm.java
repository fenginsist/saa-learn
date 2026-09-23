package org.cvicse.saa.learn.test;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.jetbrains.annotations.NotNull;

public class Test02_DashScopeCustomLlm {
    public static void main(String[] args) {
        starter();
    }

    public static void starter() {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        String call = chatModel.call("你是谁");
        System.out.println(call);
    }

    @NotNull
    private static DashScopeChatModel getDashScopeChatModel() {

        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 1. 创建 DashScope API
        // =========================
        // 【重要】DashScopeApi.Builder 默认 baseUrl = "https://dashscope.aliyuncs.com"，
        // 默认 completionsPath = "/api/v1/services/aigc/text-generation/generation"（老协议路径）。
        // baseUrl 里不能再带 "/api/v1"，否则会拼成 ".../api/v1/api/v1/services/..." → 服务端报
        // InvalidParameter: url error, please check url！
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
//                .baseUrl("https://llm-lp68jcoxmr9qifkd.cn-beijing.maas.aliyuncs.com")   // 专用/VPC 域名时只写域名根
                .build();

        // =========================
        // 2. 创建 ChatModel
        // =========================
        // 【重要】qwen3.7-flash 系新模型不在老的 text-generation 路径上提供服务，
        // 只有 /api/v1/services/aigc/multimodal-generation/generation（多模态路径）和
        // OpenAI 兼容模式 /compatible-mode/v1/chat/completions 才可用。
        // 不改 multiModel 时 DashScopeApi 会走老的 text-generation 路径，服务端返回：
        //   400 InvalidParameter: url error, please check url！
        // multiModel=true 会把请求路径切到多模态路径（该字段是 @JsonIgnore，不会进入请求体）。
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model("qwen3.7-flash-2026-07-15")
                        .multiModel(true)
                        .build())
                .build();
        return chatModel;
    }
}
