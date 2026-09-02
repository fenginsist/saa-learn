package org.cvicse.saa.learn.step05_message;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.cvicse.saa.learn.step01_baseAgent.WeatherTool;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class ch01_toolMessage {

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

        // 创建 DashScope API 实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // 创建 ChatModel
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 定义天气查询工具（通过 BiFunction 接口实现）
        ToolCallback weatherTool = FunctionToolCallback.builder("get_weather", new WeatherTool())
                .description("Get weather for a given city")
                .inputType(WeatherTool.Request.class)
                .build();

        System.out.println("--------------------------第一层：工具有没有注册？--------------------------");
        System.out.println("工具名称：" + weatherTool.getToolDefinition().name());
        System.out.println("工具描述：" + weatherTool.getToolDefinition().description());
        System.out.println("工具参数：" + weatherTool.getToolDefinition().inputSchema());
        System.out.println("--------------------------第一层：工具有没有注册？end--------------------------");

        System.out.println("--------------------------第二层：直接测试 WeatherTool--------------------------");
        WeatherTool weatherToolTest = new WeatherTool();
        String result = weatherToolTest.apply(
                new WeatherTool.Request("San Francisco"),
                null
        );
        System.out.println(result);
        System.out.println("--------------------------第二层：直接测试 WeatherTool end --------------------------");


        // 创建 Agent
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .tools(weatherTool)
                .model(chatModel)
                .build();

        AssistantMessage assistantMessage = agent.call("旧金山的天气怎么样");
        System.out.println(assistantMessage.toString());

        System.out.println("--------------------------------------------");

        if (assistantMessage.hasToolCalls()) {
            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                System.out.println("Tool: " + toolCall.name());
                System.out.println("Args: " + toolCall.arguments());
                System.out.println("ID: " + toolCall.id());
            }
        }

    }
}
