package org.cvicse.saa.learn.advance.step05_AgentTool;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public class Ch07_MutiChildAgentTool {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        OpenAiChatModel chatModel = getOpenAiChatModel();

        // 创建写作Agent
        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_agent")
                .model(chatModel)
                .description("专门负责创作文章和内容生成")
                .instruction("你是一个专业作家，擅长各类文章创作。")
                .build();

        // 创建翻译Agent
        ReactAgent translatorAgent = ReactAgent.builder()
                .name("translator_agent")
                .model(chatModel)
                .description("专门负责文本翻译工作")
                .instruction("你是一个专业翻译，能够准确翻译多种语言。")
                .build();

        // 创建总结Agent
        ReactAgent summarizerAgent = ReactAgent.builder()
                .name("summarizer_agent")
                .model(chatModel)
                .description("专门负责内容总结和提炼")
                .instruction("你是一个内容总结专家，擅长提炼关键信息。")
                .build();

        // 创建主Agent，集成多个工具
        ReactAgent multiToolAgent = ReactAgent.builder()
                .name("multi_tool_coordinator")
                .model(chatModel)
                .instruction("你可以访问多个专业工具：写作、翻译和总结。" +
                        "根据用户需求选择合适的工具来完成任务。")
                .tools(
                        AgentTool.getFunctionToolCallback(writerAgent),
                        AgentTool.getFunctionToolCallback(translatorAgent),
                        AgentTool.getFunctionToolCallback(summarizerAgent)
                )
                .build();

        // 使用 - 主Agent会根据需求自动选择合适的工具
        Optional<OverAllState> result = multiToolAgent.invoke("请写一篇关于AI的文章，然后翻译成英文，最后给出摘要");


        System.out.println("Optional<OverAllState> result=" + result);
        System.out.println("-------------------方式一----------------------");
        result.ifPresent(overAllState -> System.out.println("overAllState=" + overAllState));

        System.out.println("-------------------方式二----------------------");
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

        /**
         *
         *
         * */
    }

    @NotNull
    private static OpenAiChatModel getOpenAiChatModel() {

        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 1. 创建 OpenAiApi API
        // =========================
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .baseUrl("https://llm-lp68jcoxmr9qifkd.cn-beijing.maas.aliyuncs.com/compatible-mode")
                .build();

        // =========================
        // 2. 创建 ChatModel
        // =========================
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model("qwen3.7-flash-2026-07-15").build())
                .build();
    }
}
