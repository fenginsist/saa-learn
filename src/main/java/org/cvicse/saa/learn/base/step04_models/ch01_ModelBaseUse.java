package org.cvicse.saa.learn.base.step04_models;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;

public class ch01_ModelBaseUse {


    public static void main(String[] args) {
        starter();
    }

    public static void starter() {

        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // 创建 DashScope API 实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // 创建 ChatModel
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 使用字符串直接调用
        String response = chatModel.call("介绍一下Spring框架");
        System.out.println(response);
    }
}
