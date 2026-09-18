package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Optional;

public class ch03_ControlReasoningContent {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_agent")
                .model(chatModel)
                .returnReasoningContents(true)
                .outputKey("article")
                .build();

        ReactAgent reviewerAgent = ReactAgent.builder()
                .name("reviewer_agent")
                .model(chatModel)
                .instruction("请对文章进行评审修正：{article}，最终返回评审修正后的文章内容")
                .includeContents(true) // 包含上一个Agent的推理内容
                .returnReasoningContents(true)
                .outputKey("reviewed_article")
                .build();

        SequentialAgent blogAgent = SequentialAgent.builder()
                .name("blog_agent")
                .subAgents(List.of(writerAgent, reviewerAgent))
                .build();

        Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的散文");

        if (result.isPresent()) {
            // 消息历史将包含所有工具调用和推理过程
            List<Message> messages = (List<Message>) result.get().value("messages").orElse(List.of());
            System.out.println("消息数量: " + messages.size()); // 包含所有中间步骤
            /**
             * apiKey = sk-ws-H.EXXEMEX.Y2dm.MEUCIEXNlQepvp3qup3qIhm93T1vAOg-iRZ_jE93U8ZusX8sAiEAir78N9b9dGhpgafs_Vc_0NJAir88hd62l4OIj9KXR6k
             * 消息数量: 4
             *
             */
        }

    }

    @NotNull
    private static DashScopeChatModel getDashScopeChatModel() {
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
