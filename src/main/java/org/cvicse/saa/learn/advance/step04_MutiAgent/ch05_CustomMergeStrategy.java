package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import org.springframework.ai.chat.messages.Message;

import java.util.Map;

public class ch05_CustomMergeStrategy implements ParallelAgent.MergeStrategy {

    @Override
    public Map<String, Object> merge(Map<String, Object> mergedState, OverAllState state) {
        // 从每个Agent的状态中提取输出
        state.data().forEach((key, value) -> {
            if (key.endsWith("_result")) {
                Message message = (Message) value;
                Object existing = mergedState.get("all_results");
                if (existing == null) {
                    mergedState.put("all_results", message.getText());
                }
                else {
                    mergedState.put("all_results", existing + "---" + message.getText());
                }
            }
        });
        return mergedState;
    }
}