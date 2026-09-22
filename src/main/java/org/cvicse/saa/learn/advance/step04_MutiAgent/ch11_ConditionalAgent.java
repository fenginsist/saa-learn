package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

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
 *              ┌────────────┼────────────┐
 *              ↓            ↓            ↓
 *           "urgent"     "normal"    "technical"
 *              ↓            ↓            ↓
 *        urgentAgent   normalAgent   technicalAgent
 *
 * </pre>
 */
public class ch11_ConditionalAgent extends FlowAgent {
    private final Function<OverAllState, String> conditionEvaluator;
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
        this.conditionEvaluator = builder.conditionEvaluator;
        this.trueAgent = builder.trueAgent;
        this.falseAgent = builder.falseAgent;
    }

    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config)
            throws GraphStateException {

        config.conditionalAgents(
                Map.of(
                        "true", trueAgent,
                        "false", falseAgent
                )
        );

        config.customProperty("condition", conditionEvaluator);

        return FlowGraphBuilder.buildGraph(
                "CONDITIONAL",
                config
        );
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
        private Function<OverAllState, String> conditionEvaluator;
        /**
         * true 分支 Agent
         */
        private Agent trueAgent;

        /**
         * false 分支 Agent
         */
        private Agent falseAgent;

        public ConditionalAgentBuilder conditionEvaluator(Function<OverAllState, String> conditionEvaluator) {
            this.conditionEvaluator = conditionEvaluator;
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

            if (conditionEvaluator == null) {
                throw new IllegalArgumentException("condition must be set");
            }

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
