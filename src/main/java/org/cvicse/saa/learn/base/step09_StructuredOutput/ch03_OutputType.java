package org.cvicse.saa.learn.base.step09_StructuredOutput;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.converter.BeanOutputConverter;

public class ch03_OutputType {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 1. 创建 DashScope API
        // =========================
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // =========================
        // 2. 创建 ChatModel
        // =========================
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // =========================
        // 3. 创建 Agent
        // =========================

        // 直接使用 outputType，框架会自动处理 schema 转换
        ReactAgent agent = ReactAgent.builder()
                .name("contact_extractor")
                .model(chatModel)
                .outputType(ContactInfo.class)
                .saver(new MemorySaver())
                .build();

        AssistantMessage result = agent.call(
                "从以下信息提取联系方式：张三，zhangsan@example.com，(555) 123-4567"
        );

        System.out.println(result.getText());
        // 输出:
        /**
         * {
         *   "email": "zhangsan@example.com",
         *   "name": "张三",
         *   "phone": "(555) 123-4567"
         * }
         */


    }


    // 定义输出类型
    public static class ContactInfo {
        private String name;
        private String email;
        private String phone;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

}
