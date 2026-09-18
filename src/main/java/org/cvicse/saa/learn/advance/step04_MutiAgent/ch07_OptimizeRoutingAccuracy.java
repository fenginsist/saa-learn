package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ch07_OptimizeRoutingAccuracy {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 1. 提供清晰明确的Agent描述
        ReactAgent codeAgent = ReactAgent.builder()
                .name("code_agent")
                .model(chatModel)
                .description("专门处理编程相关问题，包括代码编写、调试、重构和优化。" +
                        "擅长Java、Python、JavaScript等主流编程语言。")
                .instruction("你是一个资深的软件工程师...")
                .build();

        // 2. 明确Agent的职责边界
        ReactAgent businessAgent = ReactAgent.builder()
                .name("business_agent")
                .model(chatModel)
                .description("专门处理商业分析、市场研究和战略规划问题。" +
                        "不处理技术实现细节。")
                .instruction("你是一个资深的商业分析师...")
                .build();


        // 3. 使用不同领域的Agent避免重叠
        LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
                .name("multi_domain_router")
                .model(chatModel)
                .subAgents(List.of(codeAgent, businessAgent))
                .build();

    }

    @NotNull
    private static DashScopeChatModel getDashScopeChatModel() throws GraphRunnerException {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 创建 DashScope API
        // =========================
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // =========================
        // 创建 ChatModel
        // =========================
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
        return chatModel;
    }
}
