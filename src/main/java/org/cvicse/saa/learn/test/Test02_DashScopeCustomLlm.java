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
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
//                .baseUrl("https://llm-lp68jcoxmr9qifkd.cn-beijing.maas.aliyuncs.com/api/v1")
                .build();

        // =========================
        // 2. 创建 ChatModel
        // =========================
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder().model("qwen3.7-flash").build())
                .build();
        return chatModel;
    }
}
