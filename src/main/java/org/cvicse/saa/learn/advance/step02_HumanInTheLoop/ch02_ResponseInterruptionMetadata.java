package org.cvicse.saa.learn.advance.step02_HumanInTheLoop;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import java.util.List;
import java.util.Optional;

public class ch02_ResponseInterruptionMetadata {

    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 1. 创建 DashScope API
        // =========================
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // =========================
        // 2. 创建 ChatModel
        // =========================
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // =========================
        // 3. 创建 Agent
        // =========================
        ReactAgent agent = ReactAgent.builder()
                .name("approval_agent")
                .model(chatModel)
//                .tools(writeFileTool, executeSqlTool, readDataTool)
                .build();

        // 人工介入利用检查点机制。
        // 你必须提供线程ID以将执行与会话线程关联，
        // 以便可以暂停和恢复对话（人工审查所需）。
        String threadId = "user-session-123";
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 运行图直到触发中断
        Optional<NodeOutput> result = agent.invokeAndGetOutput("删除数据库中的旧记录", config);

        // 检查是否返回了中断
        if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
            InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

            // 中断包含需要审查的工具反馈
            List<InterruptionMetadata.ToolFeedback> toolFeedbacks =
                    interruptionMetadata.toolFeedbacks();

            for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
                System.out.println("工具: " + feedback.getName());
                System.out.println("参数: " + feedback.getArguments());
                System.out.println("描述: " + feedback.getDescription());
            }

            // 示例输出:
            // 工具: execute_sql
            // 参数: {"query": "DELETE FROM records WHERE created_at < NOW() - INTERVAL '30 days';"}
            // 描述: SQL执行操作需要审批
        }
    }
}
