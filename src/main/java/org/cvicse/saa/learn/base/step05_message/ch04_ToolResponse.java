package org.cvicse.saa.learn.base.step05_message;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * 工具消息用于将单个工具执行的结果传回模型。
 */
public class ch04_ToolResponse {
    public static void main(String[] args) {
        starter();
    }

    public static void starter() {

        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // 创建 DashScope API 实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        // 创建 ChatModel
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 在模型进行工具调用后
        AssistantMessage aiMessage = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall(
                                "call_123",
                                "tool",
                                "get_weather",
                                "{\"location\": \"San Francisco\"}"
                        )
                ))
                .build();

        // 执行工具并创建结果消息
        String weatherResult = "晴朗，22°C";
        ToolResponseMessage toolMessage = ToolResponseMessage.builder()
                .responses(List.of(
                        new ToolResponseMessage.ToolResponse("call_123", "get_weather", weatherResult)
                ))
                .build();

        // 继续对话
        List<org.springframework.ai.chat.messages.Message> messages = List.of(
                new UserMessage("旧金山的天气怎么样？"),
                aiMessage,      // 模型的工具调用
                toolMessage     // 工具执行结果
        );
        ChatResponse response = chatModel.call(new Prompt(messages));
        System.out.println("response = " + response.getResult().getOutput());
        System.out.println("response = " + response.getResult().getOutput().getText());
    }
}
