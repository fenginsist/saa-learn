package org.cvicse.saa.learn.step07_memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.cvicse.saa.learn.step01_baseAgent.WeatherTool;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.Map;

/**
 *
 */
public class ch12_AfterModelHook {

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
        // 3. 创建天气工具
        // =========================
        ToolCallback weatherTool = FunctionToolCallback.builder("get_weather", new WeatherTool())
                .description("Get weather for a given city")
                .inputType(WeatherTool.Request.class)
                .build();

        // =========================
        // 4. 创建 Agent
        // =========================
        ReactAgent agent = ReactAgent.builder()
                .name("secure_agent")
                .model(chatModel)
                .hooks(new ch12_ValidateResponseHook())
                .saver(new MemorySaver())
                .build();
        // =========================
        // 5. 创建上下文
        // =========================
        Map<String, Object> context = Map.of("user_name", "John Smith");

        // =========================
        // 6. 创建 RunnableConfig
        // =========================
        RunnableConfig config = RunnableConfig.builder()
                        .addMetadata("user_name", "John Smith")
                        .build();

        AssistantMessage message = agent.call("北京，啥天气", config);
        System.out.println("message = " + message.getText());

    }
}
