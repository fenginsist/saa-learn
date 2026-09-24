package org.cvicse.saa.learn.advance.step06_workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

public class Ch03_ConditionEvaluatorNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String input = state.value("input", "").toString().toLowerCase();

        // 根据输入内容决定路由
        String route;
        if (input.contains("错误") || input.contains("异常")) {
            route = "error_handling";
        } else if (input.contains("数据") || input.contains("分析")) {
            route = "data_processing";
        } else if (input.contains("报告") || input.contains("总结")) {
            route = "report_generation";
        } else {
            route = "default";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("_condition_result", route);
        return result;
    }
}
