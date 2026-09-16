package org.cvicse.saa.learn.base.step07_memory;

import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * 调用模型之前执行的 HOOK
 */
@HookPositions({HookPosition.BEFORE_MODEL})
public class ch11_TrimMessagesHook extends MessagesModelHook {

    @Override
    public String getName() {
        return "trim_messages";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        if (previousMessages.size() <= 3) {
            return new AgentCommand(previousMessages); // 无需更改
        }

// 保留第一条和最后几条消息
        Message firstMsg = previousMessages.get(0);
        List<Message> recentMessages = previousMessages.subList(
                previousMessages.size() - 3,
                previousMessages.size()
        );

        List<Message> trimmedMessages = new ArrayList<>();
        trimmedMessages.add(firstMsg);
        trimmedMessages.addAll(recentMessages);

// 使用 REPLACE 策略替换消息列表，只保留需要的消息
        return new AgentCommand(trimmedMessages, UpdatePolicy.REPLACE);
    }
}
