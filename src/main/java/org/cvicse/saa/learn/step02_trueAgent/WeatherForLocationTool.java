package org.cvicse.saa.learn.step02_trueAgent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.function.BiFunction;

// 天气查询工具
public class WeatherForLocationTool implements BiFunction<WeatherForLocationTool.Request, ToolContext, String> {

    public record Request(String city) {}


    @Override
    public String apply(
            @ToolParam(description = "The city name") WeatherForLocationTool.Request city,
            ToolContext toolContext) {
        return "It's always sunny in " + city.city() + "!";
    }
}