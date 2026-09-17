package org.cvicse.saa.learn.base.step08_HooksAndInterceptors.builtInImplementation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolemulator.ToolEmulatorInterceptor;
import org.jetbrains.annotations.NotNull;

public class ch08_LLMToolEmulator {

    public static void main(String[] args) {
        starter();
    }


    public static void starter() {

        DashScopeChatModel chatModel = getDashScopeChatModel();


        /**
         *在没有实际执行工具的情况下，使用 LLM 模拟工具的输出。
         *
         * 适用场景：
         *
         * 在演示或测试期间模拟 API；
         * 在开发过程中为工具提供占位符行为；
         * 在不产生实际成本或副作用的情况下测试 Agent 逻辑。
         */
        ReactAgent agent = ReactAgent.builder()
                .name("emulator_agent")
                .model(chatModel)
//                .tools(simulatedTool)
                .interceptors(ToolEmulatorInterceptor.builder().model(chatModel).build())
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
