package com.school.demo.llm;

import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private final OllamaService ollamaService;
    private final PromptBuilder promptBuilder;
    private final SchemaProvider schemaProvider;
    private final SqlPostProcessor sqlPostProcessor;

    public LlmService(
            OllamaService ollamaService,
            PromptBuilder promptBuilder,
            SchemaProvider schemaProvider,
            SqlPostProcessor sqlPostProcessor) {
        this.ollamaService = ollamaService;
        this.promptBuilder = promptBuilder;
        this.schemaProvider = schemaProvider;
        this.sqlPostProcessor = sqlPostProcessor;
    }

    public String generateSql(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Soru boş olamaz.");
        }

        String q = question.trim();
        String prompt = promptBuilder.buildTextToSqlPrompt(
                schemaProvider.getSchemaDescription(),
                q
        );

        String raw = ollamaService.generate(prompt);
        String sql = sqlPostProcessor.clean(raw);

        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Model SQL üretemedi.");
        }
        return sql;
    }
}
