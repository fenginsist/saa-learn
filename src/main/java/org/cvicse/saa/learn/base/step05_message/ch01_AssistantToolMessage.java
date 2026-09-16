package org.cvicse.saa.learn.base.step05_message;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.cvicse.saa.learn.base.step01_baseAgent.WeatherTool;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ch01_AssistantToolMessage {

    public static void main(String[] args) {
        starter();
    }

    public static void starter() {
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


        // ============================================================
        // 复现官方文档《手动函数调用》案例
        //
        // 关键点：
        //   默认情况下，chatModel 内部会自动执行工具并继续追问，直到拿到
        //   最终文字回答才返回，所以我们看不到"携带工具调用的中间消息"。
        //   （上一版用 ReactAgent.call() 就是这个原因导致最后没有输出）
        //
        //   这里用 internalToolExecutionEnabled(false) 关闭自动执行，
        //   让模型把"工具调用请求"原样返回，由我们手动打印和执行。
        // ============================================================
        DashScopeChatOptions chatOptions = DashScopeChatOptions.builder()
                .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                .withTemperature(0.7)
                .build();
        chatOptions.setToolCallbacks(List.of(weatherTool));
        chatOptions.setInternalToolExecutionEnabled(false);

        // 第一轮调用：只让模型决定是否需要调用工具
        UserMessage userMessage = new UserMessage("旧金山的天气怎么样");
        ChatResponse response = chatModel.call(new Prompt(userMessage, chatOptions));
        AssistantMessage aiMessage = response.getResult().getOutput();

        System.out.println("response=" + response);

        if (aiMessage.hasToolCalls()) {
            List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

            // ----------------- 官方文档原样案例 -----------------
            for (ToolCall toolCall : aiMessage.getToolCalls()) {
                System.out.println("Tool: " + toolCall.name());
                System.out.println("Args: " + toolCall.arguments());
                System.out.println("ID: " + toolCall.id());

                // 手动执行工具，得到真实结果
                String toolResult = weatherTool.call(toolCall.arguments());
                System.out.println("Result: " + toolResult);

                // 把执行结果封装成 tool 响应消息（id 必须对上模型给的调用 id）
                toolResponses.add(new ToolResponseMessage.ToolResponse(
                        toolCall.id(), toolCall.name(), toolResult));
            }

            // 把完整对话拼回去再次调用模型：用户问题 + 携带工具调用的 assistant + 工具结果
            ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
                    .responses(toolResponses)
                    .build();

            List<Message> messages = List.of(userMessage, aiMessage, toolResponseMessage);
            ChatResponse finalResponse = chatModel.call(new Prompt(messages, chatOptions));
            AssistantMessage finalMessage = finalResponse.getResult().getOutput();
            System.out.println("最终回答：" + finalMessage.getText());
        } else {
            // 模型认为不需要工具，直接回答
            System.out.println("模型未调用工具，直接回答：" + aiMessage.getText());
        }
    }
}
