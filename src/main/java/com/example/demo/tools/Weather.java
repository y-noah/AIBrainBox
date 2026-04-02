package com.example.demo.tools;

import java.util.Random;


public class Weather {

    public static String getWeatherFromApi() {
        String city = "杭州";

        // 模拟网络延迟
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 模拟返回数据
        String[] weatherTypes = {"晴", "多云", "小雨", "雷阵雨", "阴"};
        Random random = new Random();

        String weather = weatherTypes[random.nextInt(weatherTypes.length)];
        int temperature = 25 + random.nextInt(10);

        // 模拟 JSON 风格返回
        return String.format(
                "{ \"city\": \"%s\", \"weather\": \"%s\", \"temperature\": \"%d°C\" }",
                city, weather, temperature
        );
    }
}
