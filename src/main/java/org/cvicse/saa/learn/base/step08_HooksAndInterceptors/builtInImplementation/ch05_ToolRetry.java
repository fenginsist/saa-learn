package org.cvicse.saa.learn.base.step08_HooksAndInterceptors.builtInImplementation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import org.jetbrains.annotations.NotNull;

public class ch05_ToolRetry {

    public static void main(String[] args) {
        starter();
    }


    public static void starter() {

        DashScopeChatModel chatModel = getDashScopeChatModel();


        /**
         * 自动重试失败的工具调用，具有可配置的指数退避。
         *
         * 适用场景：
         *
         * 处理外部 API 调用中的瞬态故障；
         * 提高依赖网络的工具的可靠性；
         * 构建优雅处理临时错误的弹性 Agent。
         */
        ReactAgent agent = ReactAgent.builder()
                .name("resilient_agent")
                .model(chatModel)
//                .tools(searchTool, databaseTool)
                .interceptors(ToolRetryInterceptor.builder()
                        .maxRetries(2)
                        .onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE)
                        .build())
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
