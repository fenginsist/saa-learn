package org.cvicse.saa.learn.step01_baseAgent;

import org.springframework.ai.chat.model.ToolContext;

import java.util.function.BiFunction;

public class WeatherTool implements BiFunction<WeatherTool.Request, ToolContext, String> {

    public record Request(String city) {}

    @Override
    public String apply(Request request, ToolContext toolContext) {
        return "It's always sunny in " + request.city() + "!";
    }
}
