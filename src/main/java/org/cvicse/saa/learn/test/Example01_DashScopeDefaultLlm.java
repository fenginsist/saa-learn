package org.cvicse.saa.learn.test;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Example01_DashScopeDefaultLlm {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        ReactAgent agent = ReactAgent.builder()
                .name("coordinator_agent")
                .model(chatModel)
                .instruction("你需要调用写作工具来完成用户的写作请求。请根据用户需求，使用结构化的参数调用写作工具。")
                .build();

        Optional<OverAllState> result = agent.invoke("请写一篇关于春天的散文，大约100字");

        if (result.isPresent()) {
            OverAllState overAllState = result.get();
            System.out.println("overAllState=" + overAllState);

            Map<String, Object> data = overAllState.data();
            System.out.println("overAllState.data()=" + data);

            Object messagesObj = data.get("messages");
            System.out.println("-------------------打印---------------------");
            if (messagesObj instanceof List<?> messages) {
                for (Object messageObj : messages) {
                    System.out.println("message = " + messageObj);
                }
            }

            System.out.println("-------------------分类打印---------------------");
            if (messagesObj instanceof List<?> messages) {
                for (Object obj : messages) {
                    if (obj instanceof UserMessage userMessage) {
                        System.out.println("UserMessage USER:");
                        System.out.println(userMessage.getText());
                    } else if (obj instanceof AssistantMessage assistantMessage) {
                        String text = assistantMessage.getText();
                        System.out.println("AssistantMessage 最终文本：" + text);
                    } else if (obj instanceof ToolResponseMessage toolResponseMessage) {
                        System.out.println("ToolResponseMessage TOOL: " + toolResponseMessage);
                    } else {
                        System.out.println("UNKNOWN: " + obj);
                    }
                }
            }
        }

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
