package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 自定义系统提示和指令
 * LlmRoutingAgent 支持通过 systemPrompt 和 instruction 来自定义路由决策行为，提供更精确的路由控制。
 *
 * 使用 SystemPrompt
 * systemPrompt 用于设置路由决策的系统提示，会替换默认的系统提示。你可以通过它提供详细的决策规则和上下文：
 *
 * 使用 Instruction
 * instruction 用于设置路由决策的用户指令，会作为 UserMessage 添加到消息列表中。你可以通过它提供额外的上下文信息或特定的路由指导：
 */
public class ch08_CustomizeSystemPromptsAndInstructions {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        final String ROUTING_SYSTEM_PROMPT = """
你是一个智能的内容路由Agent，负责根据用户需求将任务路由到最合适的专家Agent。

## 你的职责
1. 仔细分析用户输入的意图和需求
2. 根据任务特性，选择最合适的专家Agent
3. 确保路由决策准确、高效

## 可用的子Agent及其职责

### writer_agent
- **功能**: 擅长创作各类文章，包括散文、诗歌等文学作品
- **适用场景**:
* 用户需要创作新文章、散文、诗歌等原创内容
* 简单的写作任务
- **输出**: writer_output

### reviewer_agent
- **功能**: 擅长对文章进行评论、修改和润色
- **适用场景**:
* 用户需要修改、评审或优化现有文章
* 需要提高文章质量
- **输出**: reviewer_output

### translator_agent
- **功能**: 擅长将文章翻译成各种语言
- **适用场景**:
* 用户需要将内容翻译成其他语言
* 多语言转换需求
- **输出**: translator_output

## 决策规则

1. **写作任务**: 如果用户需要创作新内容，选择 writer_agent
2. **修改任务**: 如果用户需要修改或优化现有内容，选择 reviewer_agent
3. **翻译任务**: 如果用户需要翻译内容，选择 translator_agent

## 响应格式
只返回Agent名称（writer_agent、reviewer_agent、translator_agent），不要包含其他解释。
""";

        // 使用 instruction 提供额外的路由指导
        final String ROUTING_INSTRUCTION = """
请根据用户的需求，选择最合适的Agent来处理任务。

特别注意：
- 如果用户明确提到"写"、"创作"、"生成"等词汇，优先选择 writer_agent
- 如果用户提到"修改"、"优化"、"评审"等词汇，选择 reviewer_agent
- 如果用户提到"翻译"、"转换语言"等词汇，选择 translator_agent
""";

        LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
                .name("content_routing_agent")
                .description("根据用户需求智能路由到合适的专家Agent")
                .model(chatModel)
                .systemPrompt(ROUTING_SYSTEM_PROMPT)
                .instruction(ROUTING_INSTRUCTION)
//                .subAgents(List.of(writerAgent, reviewerAgent, translatorAgent))
                .build();

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
