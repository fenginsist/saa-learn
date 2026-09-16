package org.cvicse.saa.learn.base.step01_baseAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class BaseAgent {


    private static String AI_DASHSCOPE_API_KEY = System.getenv("AI_DASHSCOPE_API_KEY");

    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {

        // 初始化 ChatModel
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(AI_DASHSCOPE_API_KEY)
                .build();

        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 定义天气查询工具（通过 BiFunction 接口实现）
        ToolCallback weatherTool = FunctionToolCallback.builder("get_weather", new WeatherTool())
                .description("Get weather for a given city")
                .inputType(WeatherTool.Request.class)
                .build();

        // 创建 agent
        ReactAgent agent = ReactAgent.builder()
                .name("weather_agent")
                .model(chatModel)
                .tools(weatherTool)
                .systemPrompt("You are a helpful assistant")
                .saver(new MemorySaver())
                .build();

        // 运行 agent
        AssistantMessage response = agent.call("what is the weather in San Francisco");
        System.out.println(response.getText());

        /**
         * 输出如下：
         *
         * That's a fun saying, but San Francisco's weather is actually quite variable! It's known for its microclimates, frequent fog (especially in summer), and mild temperatures year-round. While some areas might be sunny, others could be cool and foggy—even on the same day.
         *
         * Would you like me to check the current real-time weather for San Francisco?
         */
    }
}
