package org.cvicse.saa.learn.base.step03_Agents;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

/**
 * model的高级配置
 */
public class Ch02_ModelBaseSet {

    private static String AI_DASHSCOPE_API_KEY = System.getenv("AI_DASHSCOPE_API_KEY");


    public static void main(String[] args) throws GraphRunnerException {
        starter();
    }

    public static void starter() throws GraphRunnerException {
        // 创建 DashScope API 实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(AI_DASHSCOPE_API_KEY)
                .build();

        // 创建 ChatModel
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                .withTemperature(0.7)    // 控制随机性
                .withMaxToken(2000)      // 最大输出长度
                .withTopP(0.9)           // 核采样参数
                .build())
                .build();

        // 创建 Agent
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .build();

        AssistantMessage assistantMessage = agent.call("你是谁");
        System.out.println(assistantMessage.toString());
        // AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=你好！我是通义千问（Qwen），阿里巴巴集团旗下的超大规模语言模型。我能够回答问题、创作文字，比如写故事、写公文、写邮件、写剧本、逻辑推理、编程等等，还能表达观点，玩游戏等。如果你有任何问题或需要帮助，欢迎随时告诉我！😊, metadata={search_info=, role=ASSISTANT, messageType=ASSISTANT, finishReason=STOP, id=a1bb5bf4-a442-9643-aa0f-ce309e7236ed, reasoningContent=}]
    }
}
