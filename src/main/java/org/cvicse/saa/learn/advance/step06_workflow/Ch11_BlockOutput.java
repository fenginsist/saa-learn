package org.cvicse.saa.learn.advance.step06_workflow;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class Ch11_BlockOutput {

    public static void main(String[] args) {
        try {
            starter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws Exception {
        Ch11_BlockOutput workflowExample = new Ch11_BlockOutput();

        workflowExample.executeWorkflow();

    }

    public void executeWorkflow() throws Exception {
        // 创建简单的工作流
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            strategies.put("output", new ReplaceStrategy());
            return strategies;
        };

        StateGraph workflow = new StateGraph(keyStrategyFactory);

        class SimpleNode implements NodeAction {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                String input = state.value("input", "").toString();
                return Map.of("output", "Processed: " + input);
            }
        }

        workflow.addNode("process", node_async(new SimpleNode()));

        workflow.addEdge(StateGraph.START, "process");
        workflow.addEdge("process", StateGraph.END);

        // 编译工作流
        CompileConfig compileConfig = CompileConfig.builder().build();
        CompiledGraph compiledGraph = workflow.compile(compileConfig);

        // 准备输入
        Map<String, Object> input = Map.of(
                "input", "请分析2024年AI行业发展趋势"
        );

        // 配置运行参数
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId("workflow-001")
                .build();

        // 执行工作流（同步调用）
        Optional<OverAllState> result = compiledGraph.invoke(input, runnableConfig);

        // 处理结果
        result.ifPresent(state -> {
            System.out.println("输入: " + state.value("input").orElse("无"));
            System.out.println("输出: " + state.value("output").orElse("无"));
        });

        System.out.println("工作流执行完成");

        /**
         * 输入: 请分析2024年AI行业发展趋势
         * 输出: Processed: 请分析2024年AI行业发展趋势
         * 工作流执行完成
         */
    }
}
