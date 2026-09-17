package org.cvicse.saa.learn.base.step08_HooksAndInterceptors.builtInImplementation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import org.jetbrains.annotations.NotNull;

public class ch07_LLMToolSelector {

    public static void main(String[] args) {
        starter();
    }


    public static void starter() {

        DashScopeChatModel chatModel = getDashScopeChatModel();


        /**
         * 使用一个 LLM 来决定在多个可用工具之间选择哪个工具。
         *
         * 适用场景：
         *
         * 当多个工具可以实现相似目标时；
         * 需要根据细微的上下文差异进行工具选择；
         * 动态选择最适合特定输入的工具。
         */
        ReactAgent agent = ReactAgent.builder()
                .name("smart_selector_agent")
                .model(chatModel)
//                .tools(tool1, tool2)
                .interceptors(ToolSelectionInterceptor.builder().build())
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
