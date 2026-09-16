package org.cvicse.saa.learn.advance.step02_HumanInTheLoop;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * 官网只提供了一点 响应中断   的片段，
 * 这里我加上了第一个案例的 human in the loop
 * 又加上了 三个工具的模拟，以及模拟人输入的后续操作。
 *
 *
 *
 * 流程：
 * 1. 创建 DashScopeChatModel，作为 ReactAgent 的大模型。
 * 2. 创建 write_file、read_data、execute_sql 三个 Tool，并注册到 ReactAgent。
 * 3. 创建 HumanInTheLoopHook，配置 write_file 和 execute_sql 执行前需要人工审批。
 * 4. 创建 MemorySaver，用于保存 Agent 执行过程中的 Checkpoint 状态。
 * 5. 创建 RunnableConfig，设置 threadId，用于标识本次 Agent 会话。
 * 6. 第一次调用 invokeAndGetOutput()，Agent 根据用户需求让大模型决定是否调用 Tool。
 * 7. Agent 判断需要执行 execute_sql，触发 HumanInTheLoopHook 暂停执行并返回 InterruptionMetadata。
 * 8. 程序读取 InterruptionMetadata，获取待审批的 Tool 名称、参数和描述，并等待用户输入。
 * 9. 用户输入 y 后构造 APPROVED 审批结果，并通过 RunnableConfig 携带人工反馈。
 * 10. 第二次调用 invokeAndGetOutput()，使用相同 threadId 加载 Checkpoint，并恢复之前被暂停的 Agent。
 * 11. Agent 根据人工审批结果执行 execute_sql Tool，并将 Tool 执行结果返回给大模型。
 * 12. 大模型继续完成后续推理，最终返回 Agent 的执行结果。
 */
public class ch02_ResponseInterruptionMetadata {

    public record Req(String query){};

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



        // 创建人工介入Hook
        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("write_file", ToolConfig.builder()
                        .description("文件写入操作需要审批")
                        .build())
                .approvalOn("execute_sql", ToolConfig.builder()
                        .description("SQL执行操作需要审批")
                        .build())
                .build();

        // 配置检查点保存器（人工介入需要检查点来处理中断）
        MemorySaver memorySaver = new MemorySaver();

        // 1. 模拟写文件
        ToolCallback writeFileTool = FunctionToolCallback.builder(
                        "write_file",
                        (Req content) -> {
                            System.out.println("执行 write_file，内容：" + content.query);
                            return "文件写入成功";
                        })
                .description("向文件写入内容")
                .inputType(Req.class)
                .build();

        // 2. 模拟查询数据
        ToolCallback readDataTool = FunctionToolCallback.builder(
                        "read_data",
                        (Req condition) -> {
                            System.out.println("执行 read_data，条件：" + condition.query);
                            return "查询成功：[{id:1,name:'张三'},{id:2,name:'李四'}]";
                        })
                .description("查询业务数据")
                .inputType(Req.class)
                .build();

        // 3. 模拟执行 SQL
        ToolCallback executeSqlTool = FunctionToolCallback.builder(
                        "execute_sql",
                        (Req sql) -> {
                            System.out.println("执行 execute_sql，SQL：" + sql.query);
                            return "SQL执行成功";
                        })
                .description("执行数据库SQL")
                .inputType(Req.class)
                .build();

        // =========================
        // 3. 创建 Agent
        // =========================
        ReactAgent agent = ReactAgent.builder()
                .name("approval_agent")
                .model(chatModel)
                .tools(writeFileTool, executeSqlTool, readDataTool)
                .hooks(humanInTheLoopHook)
                .saver(memorySaver)
                .build();

        // 人工介入利用检查点机制。
        // 你必须提供线程ID以将执行与会话线程关联，
        // 以便可以暂停和恢复对话（人工审查所需）。
        String threadId = "user-session-123";
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 运行图直到触发中断
        /**
         * invokeAndGetOutput() = 执行 Agent，并获取 Agent 执行过程结束时产生的 NodeOutput。
         * 方法	你可以怎么理解
         * invoke()	执行 Agent
         * invokeAndGetOutput()	执行 Agent，并拿到这次执行的 NodeOutput
         * invokeAndGetOutput() + HITL	执行 → 可能中断 → 返回 InterruptionMetadata → 再次调用恢复
         *
         */
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

                /**
                 * 自行补充，与官方文档无关
                 */
                System.out.print("是否允许执行？(y/n): ");
                Scanner scanner = new Scanner(System.in);
                continus(scanner, feedback, interruptionMetadata, threadId, agent);
            }


            // 示例输出:
            // 工具: execute_sql
            // 参数: {"query": "DELETE FROM records WHERE created_at < NOW() - INTERVAL '30 days';"}
            // 描述: SQL执行操作需要审批
        }
    }

    private static void continus(Scanner scanner, InterruptionMetadata.ToolFeedback feedback, InterruptionMetadata interruptionMetadata, String threadId, ReactAgent agent) throws GraphRunnerException {


        String input = scanner.nextLine();

        InterruptionMetadata.ToolFeedback.FeedbackResult decision;

        if ("y".equalsIgnoreCase(input)) {
            decision = InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED;
        } else {
            decision = InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED;
        }

        // 构造人工审批结果
        InterruptionMetadata.ToolFeedback feedbackResult =
                InterruptionMetadata.ToolFeedback.builder(feedback)
                        .result(decision)
                        .build();

        // 构造完整的人工反馈
        InterruptionMetadata.Builder feedbackBuilder =
                InterruptionMetadata.builder()
                        .nodeId(interruptionMetadata.node())
                        .state(interruptionMetadata.state());

        feedbackBuilder.addToolFeedback(feedbackResult);

        InterruptionMetadata approvalMetadata =
                feedbackBuilder.build();

        // 使用相同 threadId 恢复 Agent
        RunnableConfig resumeConfig =
                RunnableConfig.builder()
                        .threadId(threadId)
                        .addMetadata(
                                RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY,
                                approvalMetadata
                        )
                        .build();

        System.out.println("\n正在恢复 Agent...\n");

        Optional<NodeOutput> finalResult =
                agent.invokeAndGetOutput("", resumeConfig);

        if (finalResult.isPresent()) {
            System.out.println("最终结果：");
            System.out.println(finalResult.get());
        }
    }
}
