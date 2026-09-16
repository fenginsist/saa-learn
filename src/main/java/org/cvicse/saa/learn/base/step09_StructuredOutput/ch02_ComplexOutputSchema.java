package org.cvicse.saa.learn.base.step09_StructuredOutput;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.converter.BeanOutputConverter;

public class ch02_ComplexOutputSchema {

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

        // 使用 BeanOutputConverter 生成 outputSchema
        BeanOutputConverter<ProductReview> outputConverter = new BeanOutputConverter<>(ProductReview.class);
        String format = outputConverter.getFormat();

        // =========================
        // 3. 创建 Agent
        // =========================
        ReactAgent agent = ReactAgent.builder()
                .name("contact_extractor")
                .model(chatModel)
                .saver(new MemorySaver())
                .outputSchema(format)
                .build();

        AssistantMessage result = agent.call(
                "分析评价：这个产品很棒，5星好评。配送快速，但价格稍贵。"
        );

        System.out.println(result.getText());
        /**
        {
            "details": {
            "delivery": "快速",
                    "price": "稍贵"
        },
            "keyPoints": ["产品很棒", "配送快速", "价格稍贵"],
            "rating": 5,
                "sentiment": "positive"
        }
         */


    }



    // 定义输出类型（包含嵌套类）
    public static class ProductReview {
        private int rating;
        private String sentiment;
        private String[] keyPoints;
        private ReviewDetails details;

        // Getters and Setters
        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        public String getSentiment() { return sentiment; }
        public void setSentiment(String sentiment) { this.sentiment = sentiment; }
        public String[] getKeyPoints() { return keyPoints; }
        public void setKeyPoints(String[] keyPoints) { this.keyPoints = keyPoints; }
        public ReviewDetails getDetails() { return details; }
        public void setDetails(ReviewDetails details) { this.details = details; }

        public static class ReviewDetails {
            private String[] pros;
            private String[] cons;
            // Getters and Setters


            public String[] getPros() {
                return pros;
            }

            public void setPros(String[] pros) {
                this.pros = pros;
            }

            public String[] getCons() {
                return cons;
            }

            public void setCons(String[] cons) {
                this.cons = cons;
            }
        }
    }
}
