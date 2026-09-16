package org.cvicse.saa.learn.base.step02_trueAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrueAgent2 {

    private static String AI_DASHSCOPE_API_KEY = System.getenv("AI_DASHSCOPE_API_KEY");


    static String SYSTEM_PROMPT = """
    You are an expert weather forecaster, who speaks in puns.
    
    You have access to two tools:
    
    - get_weather_for_location: use this to get the weather for a specific location
    - get_user_location: use this to get the user's location
    
    If a user asks you for the weather, make sure you know the location.
    If you can tell from the question that they mean wherever they are,
    use the get_user_location tool to find their location.
    """;


    public static void main(String[] args) {

        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        // 第一步：创建工具
        // 第二步：创建工具回调
        ToolCallback getWeatherTool = FunctionToolCallback
                .builder("getWeatherForLocation", new WeatherForLocationTool())
                .description("Get weather for a given city")
                .inputType(String.class)
                .build();

        ToolCallback getUserLocationTool = FunctionToolCallback
                .builder("getUserLocation", new UserLocationTool())
                .description("Retrieve user location based on user ID")
                .inputType(String.class)
                .build();

        // 第三步：配置模型
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(AI_DASHSCOPE_API_KEY)
                .build();

        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        // Note: model must be set when use options build.
                        .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .withTemperature(0.5)
                        .withMaxToken(1000)
                        .build())
                .build();

        // 高级功能一： 使用 outputSchema 定义输出格式
        String customSchema = """
            请按照以下JSON格式输出：
            {
                "title": "标题",
                "content": "内容",
                "style": "风格"
            }
            """;


        // 高级功能三： 配置最大迭代次数
        ModelCallLimitHook hook = ModelCallLimitHook.builder()
                .runLimit(5)  // 限制最多调用 5 次
                .exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)  // 超出限制时抛出异常
                .build();

        // 高级功能四： 使用 Hooks 扩展功能
        // 创建 hook
        Hook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("getWeatherTool", ToolConfig.builder().description("Please confirm tool execution.")
                        .build())
                .build();


        // 创建 agent
        ReactAgent agent = ReactAgent.builder()
                .name("weather_pun_agent")
                .model(chatModel)
                .hooks(hook, humanInTheLoopHook)                // 高级功能三： 配置最大迭代次数, // 高级功能四： 使用 Hooks 扩展功能
                .systemPrompt(SYSTEM_PROMPT)
                .tools(getUserLocationTool, getWeatherTool)
//                .outputType(ResponseFormat.class) // 这是输出类型
                .outputSchema(customSchema) // 高级功能一： 使用 outputSchema 定义输出格式 代替 .outputType(ResponseFormat.class)
                .saver(new MemorySaver())
                .build();


        // 高级功能二： 使用 invoke 方法获取完整状态
        Optional<OverAllState> result = agent.invoke("帮我写一首诗。");
        if (result.isPresent()) {
            OverAllState state = result.get();
            // 访问消息历史
            List<Message> messages = state.value("messages", new ArrayList<>());
            // 访问其他状态信息
            System.out.println(state);
            // 实际输出： {"OverAllState":{"data":{"_graph_execution_id_":"5bc935ca-bf52-4632-96bf-0380010a73b1","input":"帮我写一首诗。","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"帮我写一首诗。\r\n请按照以下JSON格式输出：\n{\n    \"title\": \"标题\",\n    \"content\": \"内容\",\n    \"style\": \"风格\"\n}\n"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"326b4d14-2d6d-9861-bcbb-ea4b4992650c","reasoningContent":""},"toolCalls":[],"media":[],"text":"{\n    \"title\": \"云卷云舒\",\n    \"content\": \"风起青萍末，云生足下柔。\\n不争晴与雨，自在卷还收。\\n雷公偶打盹，彩虹偷偷溜。\\n我本无心客，何须问去留？\",\n    \"style\": \"禅意俳风·带点气象幽默\"\n}"}]}}}
        }
    }
}
