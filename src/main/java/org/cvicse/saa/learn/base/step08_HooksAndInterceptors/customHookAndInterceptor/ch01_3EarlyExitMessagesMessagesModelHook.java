package org.cvicse.saa.learn.base.step08_HooksAndInterceptors.customHookAndInterceptor;

import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;
import java.util.List;

@HookPositions({HookPosition.BEFORE_MODEL})
public class ch01_3EarlyExitMessagesMessagesModelHook extends MessagesModelHook {

    @Override
    public String getName() {
        return "early_exit";
    }

    @Override
    public List<JumpTo> canJumpTo() {
        return List.of(JumpTo.end);
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 检查某些条件，如果满足则提前退出
        if (shouldExit(previousMessages)) {
            return new AgentCommand(JumpTo.end, previousMessages);
        }
        return new AgentCommand(previousMessages);
    }

    private boolean shouldExit(List<Message> messages) {
        // 实现你的退出逻辑
        return false;
    }
}
