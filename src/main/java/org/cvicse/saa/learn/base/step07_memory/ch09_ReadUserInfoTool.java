package org.cvicse.saa.learn.base.step07_memory;

import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;
import java.util.function.BiFunction;

public class ch09_ReadUserInfoTool implements BiFunction<ch09_ReadUserInfoTool.Request, ToolContext, String> {

    public record Request(String query) {}

    @Override
    public String apply(ch09_ReadUserInfoTool.Request query, ToolContext toolContext) {

        System.out.println();
        System.out.println("============================================================");
        System.out.println("③ Tool 开始执行");
        System.out.println("============================================================");

        // ----------------------------------------------------
        // 1. 打印大模型传给 Tool 的参数
        // ----------------------------------------------------

        System.out.println("Tool Name: get_user_info");

        System.out.println();
        System.out.println("Tool Arguments:");

        System.out.println("{");
        System.out.println("  \"query\": \"" + query.query() + "\"");
        System.out.println("}");

        // ----------------------------------------------------
        // 2. 打印 ToolContext
        // ----------------------------------------------------

        System.out.println();
        System.out.println("ToolContext:");

        Map<String, Object> context = toolContext.getContext();
        System.out.println(context);


        // ----------------------------------------------------
        // 3. 获取 user_id
        // ----------------------------------------------------
        String userId = context.get("user_id").toString();
        System.out.println("user_id = " + userId);

        // ----------------------------------------------------
        // 4. 执行业务逻辑
        // ----------------------------------------------------
        String result;
        if ("user_123".equals(userId)) {
            result =  "用户是 John Smith";
        } else {
            result = "未知用户";
        }

        // ----------------------------------------------------
        // 5. 打印 Tool 返回结果
        // ----------------------------------------------------

        System.out.println();
        System.out.println("Tool Result:");
        System.out.println(result);
        System.out.println();
        System.out.println("============================================================");
        System.out.println("③ Tool 执行结束");
        System.out.println("============================================================");

        return result;
    }
}
