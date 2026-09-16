package org.cvicse.saa.learn.advance.step01_contextEngineering;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;

public class ch01_BaseStateDynamicPrompt {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
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

        // =========================
        // 3. 创建 Agent
        // =========================
        // 使用拦截器创建Agent
        ReactAgent agent = ReactAgent.builder()
                .name("context_aware_agent")
                .model(chatModel)
                .interceptors(new ch01_StateAwarePromptInterceptor())
                .build();

        AssistantMessage result = agent.call(
                "详细介绍你"
        );

        System.out.println(result.getText());
        // 输出:

    }
}
