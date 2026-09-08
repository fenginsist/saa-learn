package org.cvicse.saa.learn.step06_tool;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.cvicse.saa.learn.step01_baseAgent.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
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
 * 快速开始
 */
public class ch01_QuickStartTool {

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

        String response = ChatClient.create(chatModel)
                .prompt("What day is tomorrow?")
                .tools(new DateTimeTools())
                .call()
                .content();

        System.out.println(response);

        String response2 = ChatClient.create(chatModel)
                .prompt("Can you set an alarm 10 minutes from now?")
                .tools(new DateTimeTools())
                .call()
                .content();

        System.out.println(response2);
    }
}
