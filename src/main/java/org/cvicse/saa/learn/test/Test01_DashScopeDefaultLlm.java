package org.cvicse.saa.learn.test;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.jetbrains.annotations.NotNull;

public class Test01_DashScopeDefaultLlm {
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
                .build();

        // =========================
        // 2. 创建 ChatModel
        // =========================
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
        return chatModel;
    }
}
