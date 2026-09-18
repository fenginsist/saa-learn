package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * 路由模式中，使用大语言模型（LLM）动态决定将请求路由到哪个子Agent。这种模式非常适合需要智能选择不同专家Agent的场景。
 *
 * 流程：
 *
 * 路由Agent接收用户输入
 * LLM分析输入并决定最合适的子Agent
 * 选中的子Agent处理请求
 * 结果返回给用户
 *
 * 关键特性
 * 智能路由：LLM根据输入内容和子Agent的描述自动选择最合适的Agent
 * 灵活扩展：可以轻松添加新的专家Agent，LLM会自动识别并路由
 * 描述驱动：子Agent的 description 非常重要，它告诉LLM何时应该选择该Agent
 * 单次执行：每次请求只路由到一个Agent执行
 */
public class ch07_LlmRoutingAgent {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 创建专业化的子Agent
        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_agent")
                .model(chatModel)
                .description("擅长创作各类文章，包括散文、诗歌等文学作品")
                .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
                .outputKey("writer_output")
                .build();

        ReactAgent reviewerAgent = ReactAgent.builder()
                .name("reviewer_agent")
                .model(chatModel)
                .description("擅长对文章进行评论、修改和润色")
                .instruction("你是一个知名的评论家，擅长对文章进行评论和修改。" +
                        "对于散文类文章，请确保文章中必须包含对于西湖风景的描述。")
                .outputKey("reviewer_output")
                .build();

        ReactAgent translatorAgent = ReactAgent.builder()
                .name("translator_agent")
                .model(chatModel)
                .description("擅长将文章翻译成各种语言")
                .instruction("你是一个专业的翻译家，能够准确地将文章翻译成目标语言。")
                .outputKey("translator_output")
                .build();

        // 创建路由Agent
        LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
                .name("content_routing_agent")
                .description("根据用户需求智能路由到合适的专家Agent")
                .model(chatModel)
                .subAgents(List.of(writerAgent, reviewerAgent, translatorAgent))
                .build();

        // 使用 - LLM会自动选择最合适的Agent
        Optional<OverAllState> result1 = routingAgent.invoke("帮我写一篇关于春天的散文");
        System.out.println("LLM会路由到 writerAgent result1=" + result1);
        // LLM会路由到 writerAgent

        Optional<OverAllState> result2 = routingAgent.invoke("请帮我修改这篇文章：春天来了，花开了。");
        System.out.println("LLM会路由到 reviewerAgent result2=" + result2);
        // LLM会路由到 reviewerAgent

        Optional<OverAllState> result3 = routingAgent.invoke("请将以下内容翻译成英文：春暖花开");
        System.out.println("LLM会路由到 translatorAgent result3=" + result3);
        // LLM会路由到 translatorAgent

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
