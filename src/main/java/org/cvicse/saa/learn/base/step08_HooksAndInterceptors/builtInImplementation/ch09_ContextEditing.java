package org.cvicse.saa.learn.base.step08_HooksAndInterceptors.builtInImplementation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import org.jetbrains.annotations.NotNull;

public class ch09_ContextEditing {

    public static void main(String[] args) {
        starter();
    }


    public static void starter() {

        DashScopeChatModel chatModel = getDashScopeChatModel();


        /**
         *在将上下文发送给 LLM 之前对其进行修改，以注入、删除或修改信息。
         *
         * 适用场景：
         *
         * 向 LLM 提供额外的上下文或指令；
         * 从对话历史中删除不相关或冗余的信息；
         * 动态修改上下文以引导 Agent 的行为。
         */
        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("context_aware_agent")
                .model(chatModel)
                .interceptors(ContextEditingInterceptor.builder().trigger(120000).clearAtLeast(60000).build())
                .build();



    }

    @NotNull
    private static DashScopeChatModel getDashScopeChatModel() {
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
