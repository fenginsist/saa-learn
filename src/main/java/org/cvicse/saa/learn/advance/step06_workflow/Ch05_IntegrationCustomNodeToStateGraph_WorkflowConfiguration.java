package org.cvicse.saa.learn.advance.step06_workflow;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

@Configuration
public class Ch05_IntegrationCustomNodeToStateGraph_WorkflowConfiguration {

    @Bean
    public StateGraph customWorkflowGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException {
        // 定义状态管理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("query", new ReplaceStrategy());
            strategies.put("processed_text", new ReplaceStrategy());
            strategies.put("queryVariants", new ReplaceStrategy());
            strategies.put("final_result", new ReplaceStrategy());
            return strategies;
        };

        // 构建 StateGraph
        StateGraph graph = new StateGraph(keyStrategyFactory);

        // 添加自定义 Node
        graph.addNode("processor", node_async(new Ch01_BaseNodeExample_TextProcessorNode()));
        graph.addNode("condition", node_async(new Ch01_BaseNodeExample_ConditionNode()));

        // 定义边（流程连接）
        graph.addEdge(StateGraph.START, "processor");
        graph.addEdge("processor", "condition");

        // 条件边：根据 condition node 的结果路由
        graph.addConditionalEdges(
                "condition",
                edge_async(state -> state.value("_condition_result", "short").toString()),
                Map.of(
                        "long", "processor",  // 长文本重新处理
                        "short", StateGraph.END  // 短文本结束
                )
        );

        return graph;
    }

}
