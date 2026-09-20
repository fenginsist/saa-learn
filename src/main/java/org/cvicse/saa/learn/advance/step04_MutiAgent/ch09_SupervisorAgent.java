package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ch09_SupervisorAgent {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 创建专业化的子Agent
        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_agent")
                .model(chatModel)
                .description("擅长创作各类文章，包括散文、诗歌等文学作品")
                .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
                .outputKey("writer_output")
                .build();

        ReactAgent translatorAgent = ReactAgent.builder()
                .name("translator_agent")
                .model(chatModel)
                .description("擅长将文章翻译成各种语言")
                .instruction("你是一个专业的翻译家，能够准确地将文章翻译成目标语言。")
                .outputKey("translator_output")
                .build();

        ReactAgent mainAgent = ReactAgent.builder()
                .name("main_agent")
                .model(chatModel)
                .description("监督者主Agent，负责根据用户请求决定调用哪个子Agent")
                .systemPrompt("""
                你是一个智能的内容处理监督者。

                可用的子Agent：
                - writer_agent：负责写作
                - translator_agent：负责翻译

                你的任务是根据用户请求决定下一步调用哪个子Agent。

                路由决策时，只允许输出 JSON 数组：

                如果需要调用 writer_agent：
                ["writer_agent"]

                如果需要调用 translator_agent：
                ["translator_agent"]

                如果需要同时调用：
                ["writer_agent", "translator_agent"]

                如果任务已经完成：
                []

                除了上述 JSON 数组之外，不要输出任何其他内容。
                """)
                .instruction("用户的请求是：{input}")
                .outputKey("final_output")
                .build();

        // 创建监督者Agent
        SupervisorAgent supervisorAgent = SupervisorAgent.builder()
                .name("content_supervisor")
                .description("内容管理监督者，负责协调写作、翻译等任务")
                .model(chatModel)
                /**
                 * 官方文档没有 mainAgent，不加就报错，缺少 主agent。
                 */
                .mainAgent(mainAgent)
                .subAgents(List.of(writerAgent, translatorAgent))
                .build();

        // 使用 - 监督者会根据任务自动路由并支持多步骤处理
        Optional<OverAllState> result = supervisorAgent.invoke("帮我写一篇关于春天的短文");

        System.out.println("result: " + result.isPresent());
        System.out.println("result: " + result.map(OverAllState::toString).orElse(""));

        /**
         * apiKey = sk-ws-H.EXXEMEX.Y2dm.MEUCIEXNlQepvp3qup3qIhm93T1vAOg-iRZ_jE93U8ZusX8sAiEAir78N9b9dGhpgafs_Vc_0NJAir88hd62l4OIj9KXR6k
         * 15:38:40.524 [main] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- Calculated core pool size: 32 (CPU cores: 16)
         * 15:38:40.526 [main] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- Calculated maximum pool size: 64 (CPU cores: 16)
         * 15:38:40.526 [main] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- Calculated queue capacity: 1000
         * 15:38:40.657 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentNodeAction -- Invoking mainAgent 'main_agent' compiled graph with threadId: Optional[subgraph_main_agent]
         * 15:38:41.329 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentNodeAction -- MainAgentNodeAction: supervisor_next from last AssistantMessage = [writer_agent]
         * 15:38:41.339 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.SupervisorNodeFromState -- SupervisorNodeFromState: routingKey='supervisor_next', value='[writer_agent]', parsed agentNames=[writer_agent], validAgentNames=[writer_agent]
         * 15:38:52.284 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentNodeAction -- Invoking mainAgent 'main_agent' compiled graph with threadId: Optional[subgraph_main_agent]
         * 15:38:52.578 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentNodeAction -- MainAgentNodeAction: supervisor_next = FINISH from last AssistantMessage
         * 15:38:52.579 [main] INFO com.alibaba.cloud.ai.graph.agent.flow.node.MainAgentToSupervisorEdgeAction -- MainAgentToSupervisorEdgeAction: routing to END as value for key 'supervisor_next' is finish or empty: [FINISH]
         * result: true
         * result: {"OverAllState":{"data":{"_graph_execution_id_":"7a76a263-5a6c-4e74-b53e-b3654ccdc121","input":"帮我写一篇关于春天的短文","supervisor_next":["FINISH"],"writer_output":{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"f743f4ac-f7f0-9546-a00d-377039aa4b35","reasoningContent":""},"toolCalls":[],"media":[],"text":"好的，让我为您写一篇关于春天的短文——不单是描摹节气，而是让春天在纸上呼吸、生长、低语。\n\n**《春之手稿》**\n\n春天从来不是被“迎来”的，它是悄然修改世界的编辑，在你未落笔的稿纸边缘，先洇开了一小片湿痕。\n\n冬的句号尚未干透，泥土便松动了语法。荠菜花率先在墙缝里校对光的亮度，细茎微颤，像一句未经推敲却无比确凿的短句。柳条垂落，并非柔顺，而是把嫩芽当墨汁，在风里反复誊抄同一行绿意——那绿还带着水汽的涩味，一碰就沁出清冽的凉。\n\n孩子们蹲在溪边看蝌蚪，小手悬在半空不敢落下。他们比大人更懂：春天最怕惊扰。它正用蒲公英的绒伞校对风向，用燕子衔泥的弧线修订屋檐的倾斜度，用油菜花田浩荡的金黄，重排大地的标点——那是逗号，是顿号，是无数个等待破折号之后迸发的惊叹。\n\n最动人的，是某天清晨推开窗，忽然发觉空气有了质地：它不再锋利如刀，而像一块温润的绢，裹着青草初萌的微酸、泥土解冻的微腥、以及远处玉兰将绽未绽时，那一缕近乎透明的甜香。这气息不争不抢，却让人心头一松——仿佛被什么柔软而坚定的东西，轻轻擦去了整个冬天积下的薄霜。\n\n春天从不宣告主权。它只是把枯枝还给鸟巢，把冰凌还给云朵，把时间还给种子。它教人懂得：所谓新生，并非推倒重来，而是俯身拾起去年遗落的种籽，在裂缝里，重新签下自己的名字。\n\n（搁笔时，窗外一树早樱正簌簌飘落。我数到第七瓣，停住——有些美，本就不该数尽。）\n\n—— 您可愿与我一起，在这个春天，写一封不必寄出的信？"},"messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"帮我写一篇关于春天的短文"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"bd77bf3a-43db-9288-991a-c8003d860d5c","reasoningContent":""},"toolCalls":[],"media":[],"text":"[\"writer_agent\"]"},{"messageType":"USER","metadata":{"messageType":"USER"},"rendered":false,"text":"你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"f743f4ac-f7f0-9546-a00d-377039aa4b35","reasoningContent":""},"toolCalls":[],"media":[],"text":"好的，让我为您写一篇关于春天的短文——不单是描摹节气，而是让春天在纸上呼吸、生长、低语。\n\n**《春之手稿》**\n\n春天从来不是被“迎来”的，它是悄然修改世界的编辑，在你未落笔的稿纸边缘，先洇开了一小片湿痕。\n\n冬的句号尚未干透，泥土便松动了语法。荠菜花率先在墙缝里校对光的亮度，细茎微颤，像一句未经推敲却无比确凿的短句。柳条垂落，并非柔顺，而是把嫩芽当墨汁，在风里反复誊抄同一行绿意——那绿还带着水汽的涩味，一碰就沁出清冽的凉。\n\n孩子们蹲在溪边看蝌蚪，小手悬在半空不敢落下。他们比大人更懂：春天最怕惊扰。它正用蒲公英的绒伞校对风向，用燕子衔泥的弧线修订屋檐的倾斜度，用油菜花田浩荡的金黄，重排大地的标点——那是逗号，是顿号，是无数个等待破折号之后迸发的惊叹。\n\n最动人的，是某天清晨推开窗，忽然发觉空气有了质地：它不再锋利如刀，而像一块温润的绢，裹着青草初萌的微酸、泥土解冻的微腥、以及远处玉兰将绽未绽时，那一缕近乎透明的甜香。这气息不争不抢，却让人心头一松——仿佛被什么柔软而坚定的东西，轻轻擦去了整个冬天积下的薄霜。\n\n春天从不宣告主权。它只是把枯枝还给鸟巢，把冰凌还给云朵，把时间还给种子。它教人懂得：所谓新生，并非推倒重来，而是俯身拾起去年遗落的种籽，在裂缝里，重新签下自己的名字。\n\n（搁笔时，窗外一树早樱正簌簌飘落。我数到第七瓣，停住——有些美，本就不该数尽。）\n\n—— 您可愿与我一起，在这个春天，写一封不必寄出的信？"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"9a4c1825-4cbf-98c0-8e43-933b21eca54e","reasoningContent":""},"toolCalls":[],"media":[],"text":"[]"}],"final_output":{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"9a4c1825-4cbf-98c0-8e43-933b21eca54e","reasoningContent":""},"toolCalls":[],"media":[],"text":"[]"}}}}
         * 15:38:52.594 [default-executor-shutdown-hook] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- Shutting down ParallelNode default executor
         * 15:38:52.594 [default-executor-shutdown-hook] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- ParallelNode default thread pool terminated
         * 15:38:52.594 [default-executor-shutdown-hook] INFO com.alibaba.cloud.ai.graph.internal.node.ParallelNode -- ParallelNode default executor shut down successfully
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
