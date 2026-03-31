package com.example.demo.application;

import com.example.demo.llm.api.IntentResult;
import com.example.demo.llm.api.LLMClient;
import com.example.demo.llm.api.LLMError;
import com.example.demo.llm.api.LLMRequest;
import com.example.demo.llm.api.LLMResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class ConsoleChatRunner implements ApplicationRunner {

    private final LLMClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double CONFIDENCE_THRESHOLD = 0.6;

    public ConsoleChatRunner(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("控制台对话已启动。输入 exit 退出。");

        while (true) {
            if (!scanner.hasNextLine()) {
                System.out.println("检测到输入流已关闭，程序退出。");
                break;
            }
            System.out.print("> ");
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) {
                break;
            }

            LLMResult judgeResult = llmClient.chat(new LLMRequest(input));
            if (!judgeResult.isSuccess()) {
                System.out.println("错误：意图判定失败 -> " + toChineseError(judgeResult.error()));
                continue;
            }

            try {
                IntentResult intent = objectMapper.readValue(judgeResult.rawText(), IntentResult.class);
                if (intent.getConfidence() < CONFIDENCE_THRESHOLD) {
                    System.out.println("拒绝回答：输入可信度不足");
                    System.out.println("原因: " + intent.getReason());
                    continue;
                }

                System.out.println("通过：意图 = " + intent.getIntent());
                System.out.println("置信度: " + intent.getConfidence());

                LLMResult answerResult = llmClient.generate(new LLMRequest(input));
                if (!answerResult.isSuccess()) {
                    System.out.println("错误：回答阶段失败 -> " + toChineseError(answerResult.error()));
                    continue;
                }

                System.out.println(">>> 模型回答:");
                System.out.println(answerResult.rawText());
            } catch (Exception e) {
                System.out.println("错误：意图 JSON 非法，已拒绝输出");
                System.out.println(judgeResult.rawText());
                e.printStackTrace(System.out);
            }
        }
    }

    private String toChineseError(LLMError error) {
        if (error == null) {
            return "未知错误";
        }
        return switch (error) {
            case CONNECTION_FAILED -> "连接失败";
            case EMPTY_RESPONSE -> "模型返回为空";
            case TIMEOUT -> "调用超时";
            case UNKNOWN_ERROR -> "未知错误";
        };
    }
}
