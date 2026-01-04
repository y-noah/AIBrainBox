package com.example.demo.application;

import com.example.demo.llm.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class ConsoleChatRunner implements ApplicationRunner {

    private final LLMClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ====== 常量只放在 Runner 里 ======
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    public ConsoleChatRunner(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Console Chat started. 输入 exit 退出");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) {
                break;
            }

            /* ========== 第一次调用：Intent Judge ========== */
            LLMResult judgeResult = llmClient.chat(new LLMRequest(input));

            if (!judgeResult.isSuccess()) {
                System.out.println("❌ Intent Judge ERROR: " + judgeResult.error());
                continue;
            }

            try {
                // 1. 解析 Ollama 外层响应
                OllamaChatResponse ollama =
                        objectMapper.readValue(judgeResult.rawText(), OllamaChatResponse.class);

                // 2. 拿到模型返回的 JSON 字符串
                String intentJson = ollama.getMessage().getContent();

                System.out.println(">>> MODEL INTENT JSON:");
                System.out.println(intentJson);

                // 3. 解析 intent JSON
                IntentResult intent =
                        objectMapper.readValue(intentJson, IntentResult.class);

                // 4. confidence gate
                if (intent.getConfidence() < CONFIDENCE_THRESHOLD) {
                    System.out.println("❌ 不可信输入，拒绝回答");
                    System.out.println("reason: " + intent.getReason());
                    continue;
                }

                System.out.println("✅ intent: " + intent.getIntent());
                System.out.println("confidence: " + intent.getConfidence());

                /* ========== 第二次调用：真正回答 ========== */
                LLMResult answerResult = llmClient.generate(new LLMRequest(input));

                if (!answerResult.isSuccess()) {
                    System.out.println("❌ Answer LLM ERROR: " + answerResult.error());
                    continue;
                }

                // 直接输出原始回答（此处不再强制 JSON）
                System.out.println(">>> MODEL ANSWER:");
                System.out.println(answerResult.rawText());

            } catch (Exception e) {
                System.out.println("❌ JSON 非法，拒绝模型输出");
                System.out.println(judgeResult.rawText());
                e.printStackTrace(System.out);
            }
        }
    }
}
