package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.agent.flow.strategy.FlowGraphBuildingStrategy;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * ConditionalAgent
 *
 * <p>
 * 根据条件计算结果，从 conditionalAgents 中选择对应的 Agent 执行。
 *
 * <pre>
 *
 *                    ConditionalAgent
 *                           |
 *                           ↓
 *                  conditionEvaluator
 *                           |
 *              ┌────────────┴────────────┐
 *              ↓                         ↓
 *           "true"                    "false"
 *              ↓                         ↓
 *        urgentAgent              normalAgent
 *
 * </pre>
 */
public class ch11_ConditionalAgent extends FlowAgent {
    /**
     * 条件判定结果写入状态时使用的 key（与框架 ConditionEvaluator 保持一致）
     */
    public static final String CONDITION_RESULT_KEY = "_condition_result";

    /**
     * 默认分支：条件结果未命中任何分支时直接结束
     */
    private static final String DEFAULT_BRANCH = "default";

    private final Predicate<Map<String, Object>> condition;
    private final Agent trueAgent;
    private final Agent falseAgent;

    /**
     * 构造 ch11_ConditionalAgent
     */
    protected ch11_ConditionalAgent(ConditionalAgentBuilder builder) throws GraphStateException {
        super(
                builder.name,
                builder.description,
                builder.compileConfig,
                List.of(builder.trueAgent, builder.falseAgent)
        );
        this.condition = builder.condition;
        this.trueAgent = builder.trueAgent;
        this.falseAgent = builder.falseAgent;
    }

    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config)
            throws GraphStateException {

        // 分支映射：key 必须与条件判定返回值一致
        config.conditionalAgents(
                Map.of(
                        "true", trueAgent,
                        "false", falseAgent
                )
        );

        // =====================================================
        // 1. 节点名称
        // =====================================================

        String rootNodeName = this.name;
        String conditionNodeName = rootNodeName + "_condition";
        String trueNodeName = trueAgent.name();
        String falseNodeName = falseAgent.name();

        // =====================================================
        // 2. 创建 StateGraph（复用框架的 key 合并策略）
        // =====================================================
        StateGraph graph = new StateGraph(config.getName(), keyStrategyFactory());

        // =====================================================
        // 3. 根节点（透明节点，仅作为图入口）
        // =====================================================
        graph.addNode(rootNodeName, node_async(new TransparentNode()));
        graph.addEdge(START, rootNodeName);

        // =====================================================
        // 4. 创建条件判断节点
        // =====================================================
        graph.addNode(
                conditionNodeName,
                node_async(state -> {

                    Map<String, Object> data = state.data();
                    boolean result = condition.test(data);
                    String route = result ? "true" : "false";

                    System.out.println();
                    System.out.println("========== ConditionalAgent ==========");

                    System.out.println("input = " + data.get("input"));

                    System.out.println("condition = " + result);

                    System.out.println("route = " + route);

                    System.out.println("======================================");

                    // 状态更新：条件判定结果，供后面的条件边读取
                    return Map.of(CONDITION_RESULT_KEY, route);
                })
        );

        // =====================================================
        // 5. 添加 true / false 分支 Agent
        // =====================================================

        FlowGraphBuildingStrategy.addSubAgentNode(trueAgent, graph);

        FlowGraphBuildingStrategy.addSubAgentNode(falseAgent, graph);

        // =====================================================
        // 6. true / false Agent 最终结束
        // =====================================================

        graph.addEdge(trueNodeName, END);

        graph.addEdge(falseNodeName, END);

        // =====================================================
        // 7. 条件路由：按 _condition_result 选择分支
        // =====================================================

        graph.addConditionalEdges(
                conditionNodeName,
                edge_async(state ->
                        String.valueOf(
                                state.value(
                                        CONDITION_RESULT_KEY,
                                        DEFAULT_BRANCH
                                )
                        )
                ),
                Map.of(
                        "true", trueNodeName,
                        "false", falseNodeName,
                        DEFAULT_BRANCH, END
                )
        );

        // =====================================================
        // 8. 根节点 -> 条件节点
        // =====================================================

        graph.addEdge(rootNodeName, conditionNodeName);

        return graph;
    }

    /**
     * 状态 key 的合并策略
     *
     * <p>
     * 与框架默认规则保持一致：
     * messages 追加，input / 条件结果 / 各分支 Agent 的 outputKey 覆盖。
     */
    private KeyStrategyFactory keyStrategyFactory() {

        return () -> {

            Map<String, KeyStrategy> strategies = new HashMap<>();

            strategies.put("messages", new AppendStrategy(false));

            strategies.put("input", new ReplaceStrategy());

            strategies.put(CONDITION_RESULT_KEY, new ReplaceStrategy());

            for (Agent agent : List.of(trueAgent, falseAgent)) {
                if (agent instanceof BaseAgent baseAgent
                        && baseAgent.getOutputKey() != null) {

                    strategies.put(
                            baseAgent.getOutputKey(),
                            new ReplaceStrategy()
                    );
                }
            }

            return strategies;
        };
    }

    public static ConditionalAgentBuilder builder() {
        return new ConditionalAgentBuilder();
    }

    /**
     * Builder for ConditionalAgent
     */
    public static class ConditionalAgentBuilder
            extends FlowAgentBuilder<ch11_ConditionalAgent, ConditionalAgentBuilder> {

        /**
         * 条件判断器
         */
        private Predicate<Map<String, Object>> condition;
        /**
         * true 分支 Agent
         */
        private Agent trueAgent;

        /**
         * false 分支 Agent
         */
        private Agent falseAgent;

        public ConditionalAgentBuilder condition(Predicate<Map<String, Object>> condition) {
            this.condition = condition;
            return this;
        }

        public ConditionalAgentBuilder trueAgent(Agent trueAgent) {
            this.trueAgent = trueAgent;
            return this;
        }

        public ConditionalAgentBuilder falseAgent(Agent falseAgent) {
            this.falseAgent = falseAgent;
            return this;
        }

        /**
         * Builder 自身
         */
        @Override
        protected ConditionalAgentBuilder self() {
            return this;
        }

        /**
         * 参数校验
         */
        @Override
        protected void validate() {

            /*
             * 注意：
             * 这里不要直接调用 super.validate()
             * 也可以调用，因为 trueAgent 和 falseAgent
             * 最终都会作为 subAgents。
             */

            if (condition == null) {
                throw new IllegalArgumentException(
                        "condition must be set"
                );
            }


            if (trueAgent == null) {
                throw new IllegalArgumentException(
                        "trueAgent must be set"
                );
            }

            if (falseAgent == null) {
                throw new IllegalArgumentException(
                        "falseAgent must be set"
                );
            }

            this.subAgents = List.of(trueAgent, falseAgent);

            super.validate();
        }


        /**
         * 真正创建 ConditionalAgent
         *
         * <p>
         * 注意：
         * 当前版本 FlowAgentBuilder 要求实现的是 doBuild()
         */
        @Override
        public ch11_ConditionalAgent doBuild() {

            validate();

            try {
                return new ch11_ConditionalAgent(this);
            }
            catch (GraphStateException e) {
                throw new RuntimeException(
                        "Failed to create ConditionalAgent",
                        e
                );
            }
        }
    }
}
