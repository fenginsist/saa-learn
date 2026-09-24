package org.cvicse.saa.learn.advance.step06_workflow;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.MultiCommandAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.HashMap;
import java.util.List;

import static com.alibaba.cloud.ai.graph.action.AsyncMultiCommandAction.node_async;

public class Ch12_PerformanceOptimizationSuggestions {

    public static void main(String[] args) {
        try {
            starter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws Exception {
        OpenAiChatModel chatModel = getOpenAiChatModel();

        Ch12_PerformanceOptimizationSuggestions workflowExample = new Ch12_PerformanceOptimizationSuggestions();
        workflowExample.buildWorkflow(chatModel);

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

    private void buildWorkflow(OpenAiChatModel chatModel) throws GraphStateException {
        // 定义状态管理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            return strategies;
        };

        // 构建包含 Agent 的工作流
        StateGraph workflow = new StateGraph(keyStrategyFactory);

        // 并行执行示例
        workflow.addNode("parallel_start", (AsyncNodeAction) node_async((MultiCommandAction) new TransparentNode()));

        // 添加多个并行 Agent（注意：outputKey 在 builder 中设置）
        ReactAgent agent1 = ReactAgent.builder()
                .name("agent1")
                .model(chatModel)
                .instruction("Agent 1 的指令")
                .outputKey("result1")
                .build();

        ReactAgent agent2 = ReactAgent.builder()
                .name("agent2")
                .model(chatModel)
                .instruction("Agent 2 的指令")
                .outputKey("result2")
                .build();

        ReactAgent agent3 = ReactAgent.builder()
                .name("agent3")
                .model(chatModel)
                .instruction("Agent 3 的指令")
                .outputKey("result3")
                .build();

        workflow.addNode("agent1", agent1.asNode(true, false));
        workflow.addNode("agent2", agent2.asNode(true, false));
        workflow.addNode("agent3", agent3.asNode(true, false));

        // 聚合结果
            workflow.addNode("aggregator", (AsyncNodeAction) node_async((MultiCommandAction) new Ch04_ParallelResultAggregatorNode("merged_result")));

        // 设置并行执行
        workflow.addEdge(StateGraph.START, "parallel_start");
        workflow.addEdge("parallel_start", List.of("agent1", "agent2", "agent3"));
        workflow.addEdge(List.of("agent1", "agent2", "agent3"), "aggregator");
        workflow.addEdge("aggregator", StateGraph.END);
    }

}
