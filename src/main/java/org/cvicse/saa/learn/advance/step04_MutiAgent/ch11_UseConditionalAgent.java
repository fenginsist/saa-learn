package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class ch11_UseConditionalAgent {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 创建两个分支Agent
        ReactAgent urgentAgent = ReactAgent.builder()
                .name("urgent_handler")
                .model(chatModel)
                .description("处理紧急请求")
                .instruction("你需要快速响应紧急情况...")
                .outputKey("urgent_result")
                .build();

        ReactAgent normalAgent = ReactAgent.builder()
                .name("normal_handler")
                .model(chatModel)
                .description("处理常规请求")
                .instruction("你可以详细分析和处理常规请求...")
                .outputKey("normal_result")
                .build();

        // 定义条件：检查输入是否包含"紧急"关键字
        Predicate<Map<String, Object>> isUrgent = state -> {
            Object input = state.get("input");
            if (input instanceof String) {
                return ((String) input).contains("紧急") || ((String) input).contains("urgent");
            }
            return false;
        };

        // 创建条件路由Agent
        ch11_ConditionalAgent conditionalAgent = ch11_ConditionalAgent.builder()
                .name("priority_router")
                .description("根据紧急程度路由请求")
                .condition(isUrgent)
                .trueAgent(urgentAgent)
                .falseAgent(normalAgent)
                .build();

        // 使用
        Optional<OverAllState> result1 = conditionalAgent.invoke("这是一个紧急问题需要立即处理");
        System.out.println("result1=" + result1);
        // 会路由到 urgentAgent

        Optional<OverAllState> result2 = conditionalAgent.invoke("请帮我分析一下这个问题");
        System.out.println("result2=" + result2);
        // 会路由到 normalAgent

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
