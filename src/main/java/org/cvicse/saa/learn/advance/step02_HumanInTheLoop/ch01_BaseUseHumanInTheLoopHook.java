package org.cvicse.saa.learn.advance.step02_HumanInTheLoop;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.cvicse.saa.learn.advance.step01_contextEngineering.ch01_StateAwarePromptInterceptor;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

public class ch01_BaseUseHumanInTheLoopHook {

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


        // =========================
        // 3. 创建 Agent
        // =========================
        ReactAgent agent = ReactAgent.builder()
                .name("approval_agent")
                .model(chatModel)
//                .tools(writeFileTool, executeSqlTool, readDataTool)
                .hooks(List.of(humanInTheLoopHook))
                .saver(memorySaver)
                .build();
    }
}
