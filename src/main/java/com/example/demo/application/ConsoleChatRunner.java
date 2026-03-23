package com.example.demo.application;

import com.example.demo.capability.*;
import com.example.demo.conversation.*;
import com.example.demo.llm.api.*;
import com.example.demo.rag.service.RAGService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Component
public class ConsoleChatRunner implements ApplicationRunner {

    private final LLMClient llmClient;
    private final CapabilityRegistry capabilityRegistry;
    private final ConversationManager conversationManager;
    private final ClarificationHandler clarificationHandler;
    private final RAGService ragService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double CONFIDENCE_THRESHOLD = 0.6;
    private static final double MATCH_CONFIDENCE_THRESHOLD = 0.7;
    private static final String SESSION_ID = "console";  // 控制台固定会话ID

    public ConsoleChatRunner(
            LLMClient llmClient,
            CapabilityRegistry capabilityRegistry,
            ConversationManager conversationManager,
            ClarificationHandler clarificationHandler,
            RAGService ragService) {
        this.llmClient = llmClient;
        this.capabilityRegistry = capabilityRegistry;
        this.conversationManager = conversationManager;
        this.clarificationHandler = clarificationHandler;
        this.ragService = ragService;
        runPhaseOneTests();
    }

    /**
     * 阶段一测试：验证数据模型层
     */
    private void runPhaseOneTests() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("         阶段一完成 - 数据模型层测试");
        System.out.println("=".repeat(60) + "\n");

        // 测试 1：工具注册
        System.out.println("【测试 1】工具注册表");
        List<CapabilityDescriptor> allTools = capabilityRegistry.getAll();
        System.out.println("✅ 共注册 " + allTools.size() + " 个工具");
        allTools.forEach(tool ->
                System.out.println("   - " + tool.capabilityId() + ": " + tool.name())
        );

        // 测试 2：工具查询
        System.out.println("\n【测试 2】根据 ID 查询工具");
        CapabilityDescriptor orderTool = capabilityRegistry.get("order.query_status");
        if (orderTool != null) {
            System.out.println("✅ 订单查询工具:");
            System.out.println("   名称: " + orderTool.name());
            System.out.println("   类型: " + orderTool.type());
            System.out.println("   必需参数: " + orderTool.getRequiredParameterNames());
            System.out.println("   示例问题数量: " + orderTool.exampleQuestions().size());
        }

        // 测试 3：参数校验
        System.out.println("\n【测试 3】参数格式校验");
        if (orderTool != null && !orderTool.parameters().isEmpty()) {
            ParameterDefinition param = orderTool.parameters().get(0);
            System.out.println("✅ 参数: " + param.name() + " (pattern: " + param.pattern() + ")");

            testParameterPattern(param.pattern(), "ORD123");
            testParameterPattern(param.pattern(), "ORD20240115001");
            testParameterPattern(param.pattern(), "123");
            testParameterPattern(param.pattern(), "ORDER123");
        }

        // 测试 4：匹配结果
        System.out.println("\n【测试 4】CapabilityMatchResult 功能");

        CapabilityMatchResult matched = CapabilityMatchResult.matched(
                "order.query_status",
                Map.of("order_id", "ORD123"),
                0.95,
                "测试匹配成功"
        );
        System.out.println("✅ 匹配成功结果:");
        System.out.println("   状态: " + matched.matchStatus());
        System.out.println("   工具ID: " + matched.matchedCapabilityId());
        System.out.println("   参数: " + matched.extractedParameters());
        System.out.println("   可执行: " + matched.isExecutable());

        System.out.println("\n" + "=".repeat(60));
        System.out.println("         测试完成 - 所有数据模型功能正常");
        System.out.println("=".repeat(60) + "\n");
    }

    private void testParameterPattern(String pattern, String value) {
        boolean matches = value.matches(pattern);
        String icon = matches ? "✅" : "❌";
        System.out.println("   " + icon + " \"" + value + "\" 匹配: " + matches);
    }

    @Override
    public void run(ApplicationArguments args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Console Chat started. 输入 exit 退出");
        System.out.println("输入 reset 可以重置对话\n");

        // 获取或创建对话上下文
        ConversationContext context = conversationManager.getOrCreateContext(SESSION_ID);

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) {
                break;
            }

            if ("reset".equalsIgnoreCase(input)) {
                conversationManager.resetContext(SESSION_ID);
                context = conversationManager.getOrCreateContext(SESSION_ID);
                System.out.println("✅ 对话已重置\n");
                continue;
            }

            try {
                processUserInput(input, context);
            } catch (Exception e) {
                System.out.println("❌ 处理过程中发生错误");
                e.printStackTrace(System.out);
                System.out.println();
            }
        }
    }

    /**
     * 处理用户输入（支持多轮对话）
     */
    private void processUserInput(String input, ConversationContext context) {
        System.out.println();

        // ========== 检查是否在等待澄清 ==========
        if (context.isAwaitingClarification()) {
            handleClarificationResponse(input, context);
            return;
        }

        // ========== 正常流程：新问题 ==========
        handleNewQuestion(input, context);
    }

    /**
     * 处理澄清响应（用户补充缺失的参数）
     */
    private void handleClarificationResponse(String input, ConversationContext context) {
        System.out.println("========== 处理补充信息 ==========");

        // 尝试提取缺失的参数
        CapabilityMatchResult updatedMatch = clarificationHandler.handleClarification(
                input,
                context
        );

        if (updatedMatch == null) {
            System.out.println("❌ 无法从您的输入中提取所需参数");
            System.out.println("   请重新输入完整问题，或输入 'reset' 重新开始");
            System.out.println();
            return;
        }

        System.out.println("✅ 已补充参数");
        System.out.println("   参数: " + updatedMatch.extractedParameters());
        System.out.println();

        // 获取之前保存的意图
        IntentResult intent = context.getPendingIntent();

        // 清除等待状态
        context.clearPending();

        // 直接进入 Stage 3 校验和执行
        handleMatchResult(intent, updatedMatch);

        // 记录对话轮次
        context.addTurn(ConversationTurn.clarification(
                input,
                "已补充参数，准备执行工具"
        ));
    }

    /**
     * 处理新问题
     */
    private void handleNewQuestion(String input, ConversationContext context) {
        // ========== Stage 1: Intent Judge ==========
        IntentResult intent = executeStageOne(input);
        if (intent == null) {
            return;
        }

        // 检查是否应该处理
        if (!intent.isShouldProcess()) {
            System.out.println("❌ 问题不应被处理");
            System.out.println("   原因: " + intent.getReason());
            System.out.println();
            return;
        }

        // 检查置信度
        if (intent.getConfidence() < CONFIDENCE_THRESHOLD) {
            System.out.println("❌ 不可信输入，拒绝处理");
            System.out.println("   置信度: " + intent.getConfidence());
            System.out.println("   原因: " + intent.getReason());
            System.out.println();
            return;
        }

        System.out.println("✅ Stage 1 通过");
        System.out.println("   意图: " + intent.getIntent());
        System.out.println("   问题类型: " + intent.getQuestionType());
        System.out.println("   置信度: " + intent.getConfidence());
        System.out.println();

        // ========== Stage 2: Capability Match ==========
        CapabilityMatchResult matchResult = executeStageTwo(input);
        if (matchResult == null) {
            return;
        }

        System.out.println("✅ Stage 2 完成");
        System.out.println("   匹配状态: " + matchResult.matchStatus());
        System.out.println("   置信度: " + matchResult.confidence());
        System.out.println();

        // ========== Stage 3: Decision ==========
        handleMatchResultWithContext(intent, matchResult, context);

        // 记录对话轮次
        context.addTurn(ConversationTurn.question(
                input,
                "已处理问题"
        ));
    }

    /**
     * Stage 1: Intent Judge
     * 判断问题是否应该处理，识别问题类型
     */
    private IntentResult executeStageOne(String userInput) {
        System.out.println("========== Stage 1: Intent Judge ==========");

        LLMResult judgeResult = llmClient.chat(new LLMRequest(userInput));

        if (!judgeResult.isSuccess()) {
            System.out.println("❌ Intent Judge 失败: " + judgeResult.error());
            System.out.println();
            return null;
        }

        try {
            // 解析 Ollama 外层响应
            OllamaChatResponse ollama = objectMapper.readValue(
                    judgeResult.rawText(),
                    OllamaChatResponse.class
            );

            // 拿到模型返回的 JSON 字符串
            String intentJson = ollama.getMessage().getContent();

            // 解析 intent JSON
            IntentResult intent = objectMapper.readValue(intentJson, IntentResult.class);
            intent.setOriginalQuestion(userInput); // 保存原始问题

            return intent;

        } catch (Exception e) {
            System.out.println("❌ Intent Judge JSON 解析失败");
            System.out.println("   原始响应: " + judgeResult.rawText());
            e.printStackTrace(System.out);
            System.out.println();
            return null;
        }
    }

    /**
     * Stage 2: Capability Match
     * 从工具列表中匹配最合适的工具
     */
    private CapabilityMatchResult executeStageTwo(String userInput) {
        System.out.println("========== Stage 2: Capability Match ==========");

        // 1. 获取工具列表的 JSON
        String toolsJson = capabilityRegistry.toJsonForLLM();

        // 2. 构建 Capability Match 的 Prompt
        String capabilityMatchPrompt = SystemPrompts.CAPABILITY_MATCH.formatted(toolsJson);

        // 3. 调用 LLM（使用自定义 SystemPrompt）
        LLMResult matchResult = llmClient.chat(
                new LLMRequest(capabilityMatchPrompt, userInput)
        );

        if (!matchResult.isSuccess()) {
            System.out.println("❌ Capability Match 失败: " + matchResult.error());
            System.out.println();
            return null;
        }

        try {
            // 解析 Ollama 外层响应
            OllamaChatResponse ollama = objectMapper.readValue(
                    matchResult.rawText(),
                    OllamaChatResponse.class
            );

            // 拿到模型返回的 JSON 字符串
            String matchJson = ollama.getMessage().getContent();

            // 解析 CapabilityMatchResult JSON
            CapabilityMatchResult result = objectMapper.readValue(
                    matchJson,
                    CapabilityMatchResult.class
            );

            return result;

        } catch (Exception e) {
            System.out.println("❌ Capability Match JSON 解析失败");
            System.out.println("   原始响应: " + matchResult.rawText());
            e.printStackTrace(System.out);
            System.out.println();
            return null;
        }
    }

    /**
     * Stage 3: 根据匹配结果决定下一步操作（支持多轮对话）
     */
    private void handleMatchResultWithContext(
            IntentResult intent,
            CapabilityMatchResult matchResult,
            ConversationContext context) {

        System.out.println("========== Stage 3: Decision ==========");

        switch (matchResult.matchStatus()) {
            case MATCHED:
                handleMatched(intent, matchResult);
                break;
            case NO_MATCH:
                handleNoMatch(matchResult);
                break;
            case MISSING_ENTITY:
                handleMissingEntityWithContext(matchResult, intent, context);
                break;
            case AMBIGUOUS:
                handleAmbiguous(matchResult);
                break;
        }

        System.out.println();
    }

    /**
     * Stage 3: 根据匹配结果决定下一步操作（不带上下文，用于补充参数后的处理）
     */
    private void handleMatchResult(IntentResult intent, CapabilityMatchResult matchResult) {
        System.out.println("========== Stage 3: Decision ==========");

        switch (matchResult.matchStatus()) {
            case MATCHED:
                handleMatched(intent, matchResult);
                break;
            case NO_MATCH:
                handleNoMatch(matchResult);
                break;
            case MISSING_ENTITY:
                handleMissingEntity(matchResult);
                break;
            case AMBIGUOUS:
                handleAmbiguous(matchResult);
                break;
        }

        System.out.println();
    }

    /**
     * 处理：匹配成功
     */
    private void handleMatched(IntentResult intent, CapabilityMatchResult matchResult) {
        // 1. 置信度校验
        if (matchResult.confidence() < MATCH_CONFIDENCE_THRESHOLD) {
            System.out.println("❌ 匹配置信度过低，拒绝执行");
            System.out.println("   置信度: " + matchResult.confidence());
            System.out.println("   阈值: " + MATCH_CONFIDENCE_THRESHOLD);
            return;
        }

        // 2. 获取工具描述
        CapabilityDescriptor capability = capabilityRegistry.get(matchResult.matchedCapabilityId());
        if (capability == null) {
            System.out.println("❌ 工具不存在: " + matchResult.matchedCapabilityId());
            return;
        }

        // 3. 参数完整性校验
        if (!validateParameters(capability, matchResult)) {
            return;
        }

        // 4. 参数格式校验
        if (!validateParameterFormat(capability, matchResult)) {
            return;
        }

        // 5. 问题类型与工具类型匹配性校验
        if (!validateToolTypeMatch(intent, capability)) {
            return;
        }

        // ========== 所有校验通过，开始执行 ==========
        System.out.println("✅ 所有校验通过，准备执行工具");
        System.out.println("   工具: " + capability.name());
        System.out.println("   工具ID: " + matchResult.matchedCapabilityId());
        System.out.println();

        // 如果是知识库类型，执行 RAG
        if (capability.type() == CapabilityType.KNOWLEDGE_BASE) {
            System.out.println("正在检索知识库...");
            String answer = ragService.askWithRAG(intent.getOriginalQuestion());
            System.out.println("\nAI (RAG) 回答:");
            System.out.println("-".repeat(40));
            System.out.println(answer);
            System.out.println("-".repeat(40));
        } else {
            System.out.println("   参数: " + matchResult.extractedParameters());
            System.out.println("⚠️  注意：当前普通工具阶段只展示匹配结果，暂不实际执行 Mock 逻辑");
        }
    }

    /**
     * 处理：无匹配
     */
    private void handleNoMatch(CapabilityMatchResult matchResult) {
        System.out.println("❌ 未找到匹配的工具");
        System.out.println("   原因: " + matchResult.reason());
        if (matchResult.userGuidance() != null) {
            System.out.println("   建议: " + matchResult.userGuidance());
        }
    }

    /**
     * 处理：缺少参数（不带上下文）
     */
    private void handleMissingEntity(CapabilityMatchResult matchResult) {
        System.out.println("❌ 缺少必需参数");
        System.out.println("   原因: " + matchResult.reason());
        if (matchResult.userGuidance() != null) {
            System.out.println("   提示: " + matchResult.userGuidance());
        }
    }

    /**
     * 处理：缺少参数（支持多轮对话）
     */
    private void handleMissingEntityWithContext(
            CapabilityMatchResult matchResult,
            IntentResult intent,
            ConversationContext context) {

        System.out.println("❌ 缺少必需参数");
        System.out.println("   原因: " + matchResult.reason());

        // 生成澄清问题
        String clarification = clarificationHandler.generateClarificationQuestion(matchResult);
        System.out.println();
        System.out.println(">>> " + clarification);

        // 设置上下文为等待澄清状态
        context.awaitClarification(intent, matchResult);

        System.out.println();
        System.out.println("💡 提示：直接输入所需信息即可，无需重复整个问题");
    }

    /**
     * 处理：模糊不清
     */
    private void handleAmbiguous(CapabilityMatchResult matchResult) {
        System.out.println("❌ 问题描述不清晰");
        System.out.println("   原因: " + matchResult.reason());
        if (matchResult.userGuidance() != null) {
            System.out.println("   建议: " + matchResult.userGuidance());
        }
    }

    /**
     * 校验参数完整性
     */
    private boolean validateParameters(CapabilityDescriptor capability,
                                       CapabilityMatchResult matchResult) {
        List<String> requiredParams = capability.getRequiredParameterNames();

        for (String paramName : requiredParams) {
            if (!matchResult.extractedParameters().containsKey(paramName)) {
                System.out.println("❌ 参数校验失败：缺少必需参数");
                System.out.println("   缺少参数: " + paramName);
                return false;
            }
        }

        System.out.println("✅ 参数完整性校验通过");
        return true;
    }

    /**
     * 校验参数格式
     */
    private boolean validateParameterFormat(CapabilityDescriptor capability,
                                            CapabilityMatchResult matchResult) {
        for (ParameterDefinition param : capability.parameters()) {
            if (param.pattern() == null || param.pattern().isBlank()) {
                continue; // 没有格式要求，跳过
            }

            Object value = matchResult.extractedParameters().get(param.name());
            if (value == null) {
                continue; // 参数不存在，已在完整性校验中处理
            }

            String valueStr = value.toString();
            if (!valueStr.matches(param.pattern())) {
                System.out.println("❌ 参数格式校验失败");
                System.out.println("   参数: " + param.name());
                System.out.println("   值: " + valueStr);
                System.out.println("   要求格式: " + param.pattern());
                return false;
            }
        }

        System.out.println("✅ 参数格式校验通过");
        return true;
    }

    /**
     * 校验问题类型与工具类型的匹配性
     * 防止用确定性查询的问题匹配到知识库
     */
    private boolean validateToolTypeMatch(IntentResult intent,
                                          CapabilityDescriptor capability) {
        // 如果问题类型是确定性查询，但匹配到了知识库，拒绝
        if (intent.getQuestionType() == QuestionType.DETERMINISTIC_QUERY
                && capability.type() == CapabilityType.KNOWLEDGE_BASE) {
            System.out.println("❌ 工具类型校验失败");
            System.out.println("   问题类型: " + intent.getQuestionType());
            System.out.println("   匹配的工具类型: " + capability.type());
            System.out.println("   原因: 确定性查询不能由知识库回答");
            return false;
        }

        System.out.println("✅ 工具类型校验通过");
        return true;
    }
}