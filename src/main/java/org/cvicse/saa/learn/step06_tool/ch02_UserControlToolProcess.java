package org.cvicse.saa.learn.step06_tool;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

public class ch02_UserControlToolProcess {

    public static void main(String[] args) {
        starter();
    }

    public static void starter() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        DashScopeChatModel chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(new CustomerTools())
                .internalToolExecutionEnabled(false)
                .build();
        Prompt prompt = new Prompt("告诉我42号客户的更多信息", chatOptions);

        ChatResponse chatResponse = chatModel.call(prompt);

        while (chatResponse.hasToolCalls()) {
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);

            prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);

            chatResponse = chatModel.call(prompt);
            System.out.println("111");
        }

        System.out.println(chatResponse.getResult().getOutput().getText());
    }
}
