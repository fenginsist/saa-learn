package org.cvicse.saa.learn.base.step08_HooksAndInterceptors.customHookAndInterceptor;

import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import org.springframework.ai.chat.messages.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@HookPositions({HookPosition.BEFORE_MODEL})
public class ch02_2removeMessageModelHook extends ModelHook {

    @Override
    public String getName() {
        return "";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        Optional<Object> messagesOpt = state.value("messages");
        if (!messagesOpt.isPresent()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = (List<Message>) messagesOpt.get();

        // 构建新的消息列表，保持原顺序
        List<Object> newMessages = new ArrayList<>();
        for (Message msg : messages) {
            // 根据条件决定保留或删除
            if (shouldKeep(msg)) {
                newMessages.add(msg); // 保留消息
            } else {
                newMessages.add(RemoveByHash.of(msg)); // 标记删除
            }
        }

        return CompletableFuture.completedFuture(Map.of("messages", newMessages));
    }

    private boolean shouldKeep(Message msg) {
        // 实现你的保留逻辑
        return true;
    }


}
