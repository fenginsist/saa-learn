package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ch04_ParallelAgent {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 创建多个专业化Agent
        ReactAgent proseWriterAgent = ReactAgent.builder()
                .name("prose_writer_agent")
                .model(chatModel)
                .description("专门写散文的AI助手")
                .instruction("你是一个知名的散文作家，擅长写优美的散文。" +
                        "用户会给你一个主题：{input}，你只需要创作一篇100字左右的散文。")
                .outputKey("prose_result")
                .build();

        ReactAgent poemWriterAgent = ReactAgent.builder()
                .name("poem_writer_agent")
                .model(chatModel)
                .description("专门写现代诗的AI助手")
                .instruction("你是一个知名的现代诗人，擅长写现代诗。" +
                        "用户会给你的主题是：{input}，你只需要创作一首现代诗。")
                .outputKey("poem_result")
                .build();

        ReactAgent summaryAgent = ReactAgent.builder()
                .name("summary_agent")
                .model(chatModel)
                .description("专门做内容总结的AI助手")
                .instruction("你是一个专业的内容分析师，擅长对主题进行总结和提炼。" +
                        "用户会给你一个主题：{input}，你只需要对这个主题进行简要总结。")
                .outputKey("summary_result")
                .build();

        // 创建并行Agent
        ParallelAgent parallelAgent = ParallelAgent.builder()
                .name("parallel_creative_agent")
                .description("并行执行多个创作任务，包括写散文、写诗和做总结")
                .mergeOutputKey("merged_results")
                .subAgents(List.of(proseWriterAgent, poemWriterAgent, summaryAgent))
                .mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
                .build();

        // 使用
        Optional<OverAllState> result = parallelAgent.invoke("以'西湖'为主题");

        if (result.isPresent()) {
            OverAllState state = result.get();

            // 访问各个Agent的输出
            state.value("prose_result").ifPresent(r ->
                    System.out.println("散文: " + r));
            state.value("poem_result").ifPresent(r ->
                    System.out.println("诗歌: " + r));
            state.value("summary_result").ifPresent(r ->
                    System.out.println("总结: " + r));

            // 访问合并后的结果
            state.value("merged_results").ifPresent(r ->
                    System.out.println("合并结果: " + r));
        }
        /**
         *
         */

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
