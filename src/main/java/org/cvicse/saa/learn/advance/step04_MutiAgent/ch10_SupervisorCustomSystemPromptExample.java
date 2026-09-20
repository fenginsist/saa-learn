package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;

public class ch10_SupervisorCustomSystemPromptExample {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();
        final String SUPERVISOR_SYSTEM_PROMPT = """
你是一个智能的内容管理监督者，负责协调和管理多个专业Agent来完成用户的内容处理需求。

## 你的职责
1. 分析用户需求，将其分解为合适的子任务
2. 根据任务特性，选择合适的Agent进行处理
3. 监控任务执行状态，决定是否需要继续处理或完成任务
4. 当所有任务完成时，返回FINISH结束流程

## 可用的子Agent及其职责

### writer_agent
- **功能**: 擅长创作各类文章，包括散文、诗歌等文学作品
- **适用场景**:
* 用户需要创作新文章、散文、诗歌等原创内容
* 简单的写作任务，不需要后续评审或修改
- **输出**: writer_output

### translator_agent
- **功能**: 擅长将文章翻译成各种语言
- **适用场景**: 当文章需要翻译成其他语言时
- **输出**: translator_output

## 决策规则

1. **单一任务判断**:
- 如果用户只需要简单写作，选择 writer_agent
- 如果用户需要翻译，选择 translator_agent

2. **多步骤任务处理**:
- 如果用户需求包含多个步骤（如"先写文章，然后翻译"），需要分步处理
- 先路由到第一个合适的Agent，等待其完成
- 完成后，根据剩余需求继续路由到下一个Agent
- 直到所有步骤完成，返回FINISH

3. **任务完成判断**:
- 当用户的所有需求都已满足时，返回FINISH

## 响应格式
只返回Agent名称（writer_agent、translator_agent）或FINISH，不要包含其他解释。
""";

        SupervisorAgent supervisorAgent = SupervisorAgent.builder()
                .name("content_supervisor")
                .description("内容管理监督者")
                .model(chatModel)
                .systemPrompt(SUPERVISOR_SYSTEM_PROMPT)
//                .subAgents(List.of(writerAgent, translatorAgent))
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
