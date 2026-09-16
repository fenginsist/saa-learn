package org.cvicse.saa.learn.advance.step02_HumanInTheLoop;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Optional;

/**
 * 和第二个类似，整个的 human in the loop 的案例
 * tool+hitl+响应中断
 */
public class ch03_EntireExampleHumanInTheLoop {

    public record Req(String query){};

    public static void main(String[] args) throws Exception {
        // 1. 配置检查点
        MemorySaver memorySaver = new MemorySaver();

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

        // 2. 创建人工介入Hook
        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("poem", ToolConfig.builder()
                        .description("请确认诗歌创作操作")
                        .build())
                .build();

        // 创建人工介入Hook
        ToolCallback poetToolCallback = FunctionToolCallback.builder(
                        "write_file",
                        (Req content) -> {
                            System.out.println("执行 write_file，内容：" + content.query);
                            return "文件写入成功";
                        })
                .description("向文件写入内容")
                .inputType(Req.class)
                .build();

        // 3. 创建Agent
        ReactAgent agent = ReactAgent.builder()
                .name("poet_agent")
                .model(chatModel)
                .tools(List.of(poetToolCallback))
                .hooks(List.of(humanInTheLoopHook))
                .saver(memorySaver)
                .build();

        String threadId = "user-session-001";
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 4. 第一次调用 - 触发中断
        System.out.println("=== 第一次调用：期望中断 ===");
        Optional<NodeOutput> result = agent.invokeAndGetOutput(
                "帮我写一首100字左右的诗",
                config
        );

        // 5. 检查中断并处理
        if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
            InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

            System.out.println("检测到中断，需要人工审批");
            List<InterruptionMetadata.ToolFeedback> toolFeedbacks =
                    interruptionMetadata.toolFeedbacks();

            for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
                System.out.println("工具: " + feedback.getName());
                System.out.println("参数: " + feedback.getArguments());
                System.out.println("描述: " + feedback.getDescription());
            }

            // 6. 模拟人工决策（这里选择批准）
            InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
                    .nodeId(interruptionMetadata.node())
                    .state(interruptionMetadata.state());

            toolFeedbacks.forEach(toolFeedback -> {
                InterruptionMetadata.ToolFeedback approvedFeedback =
                        InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
                                .build();
                feedbackBuilder.addToolFeedback(approvedFeedback);
            });

            InterruptionMetadata approvalMetadata = feedbackBuilder.build();

            // 7. 第二次调用 - 使用人工反馈恢复执行
            System.out.println("=== 第二次调用：使用批准决策恢复 ===");
                    RunnableConfig resumeConfig = RunnableConfig.builder()
                            .threadId(threadId)
                            .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvalMetadata)
                            .build();

            Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);

            if (finalResult.isPresent()) {
                System.out.println("执行完成");
                System.out.println("最终结果: " + finalResult.get());
            }
        }
    }
}