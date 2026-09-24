package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public class ch02_SequentialAgent {
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
                .description("专业写作Agent")
                .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答：{input}。")
                .outputKey("article")
                .build();

        ReactAgent reviewerAgent = ReactAgent.builder()
                .name("reviewer_agent")
                .model(chatModel)
                .description("专业评审Agent")
                .instruction("你是一个知名的评论家，擅长对文章进行评论和修改。" +
                        "对于散文类文章，请确保文章中必须包含对于西湖风景的描述。待评论文章： {article}" +
                        "最终只返回修改后的文章，不要包含任何评论信息。")
                .outputKey("reviewed_article")
                .build();

        // 创建顺序Agent
        SequentialAgent blogAgent = SequentialAgent.builder()
                .name("blog_agent")
                .description("根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论")
                .subAgents(List.of(writerAgent, reviewerAgent))
                .build();

        // 使用
        Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的散文");

        if (result.isPresent()) {
            OverAllState state = result.get();

            // 访问第一个Agent的输出
            state.value("article").ifPresent(article -> {
                if (article instanceof AssistantMessage) {
                    System.out.println("原始文章: " + ((AssistantMessage) article).getText());
                }
            });

            // 访问第二个Agent的输出
            state.value("reviewed_article").ifPresent(reviewedArticle -> {
                if (reviewedArticle instanceof AssistantMessage) {
                    System.out.println("评审后文章: " + ((AssistantMessage) reviewedArticle).getText());
                }
            });
        }

        /**
         * apiKey = sk-ws-H.EXXEMEX.Y2dm.MEUCIEXNlQepvp3qup3qIhm93T1vAOg-iRZ_jE93U8ZusX8sAiEAir78N9b9dGhpgafs_Vc_0NJAir88hd62l4OIj9KXR6k
         * 原始文章: 晨光初透，青石巷里浮着薄薄一层水汽。
         * 老槐树垂下几缕新绿，风过时，叶影在斑驳粉墙上轻轻游移。
         * 阿婆坐在门槛上剥豆，竹匾里青豆粒粒饱满，指尖沾着微凉的露意。
         * 隔壁传来收音机咿呀的越剧，断续如丝，混着酱油香、艾草香，在窄巷里缓缓流淌。
         * 我驻足片刻，忽然明白：所谓人间烟火，并非鼎沸喧嚣，而是这静默里，自有温热的呼吸与不倦的生机。
         * ——100字
         * 评审后文章: 晨光初透，青石巷里浮着薄薄一层水汽；抬眼望去，远处西湖如一砚初研的淡墨，苏堤柳色浸在微光里，断桥影细，湖面浮金碎银，偶有画舫划开涟漪，漾出几痕青黛。
         * 老槐树垂下几缕新绿，风过时，叶影在斑驳粉墙上轻轻游移。
         * 阿婆坐在门槛上剥豆，竹匾里青豆粒粒饱满，指尖沾着微凉的露意。
         * 隔壁传来收音机咿呀的越剧，断续如丝，混着酱油香、艾草香，在窄巷里缓缓流淌，仿佛也染上了西湖的温润气息。
         * 我驻足片刻，忽然明白：所谓人间烟火，并非鼎沸喧嚣，而是这静默里，自有温热的呼吸与不倦的生机。
         */
    }

    @NotNull
    private static DashScopeChatModel getDashScopeChatModel() {
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
