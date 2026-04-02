package com.school.demo.llm;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LlmService {

    private final OllamaService ollamaService;
    private final PromptBuilder promptBuilder;
    private final SchemaProvider schemaProvider;
    private final SqlPostProcessor sqlPostProcessor;

    public LlmService(OllamaService ollamaService,
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

        String prompt = promptBuilder.buildTextToSqlPrompt(
                schemaProvider.getSchemaDescription(),
                normalizeQuestion(question)
        );

        String rawOutput = ollamaService.generate(prompt);
       
        String sql = sqlPostProcessor.clean(rawOutput);

        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Model SQL üretemedi.");
        }

        return sql;
    }

    private String normalizeQuestion(String question) {
        return question.trim();
    }

    private String extractSql(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return null;
        }

        String cleaned = rawOutput.trim();

        cleaned = cleaned.replace("```sql", "")
                .replace("```", "")
                .trim();

        Pattern selectPattern = Pattern.compile("(?is)(select\\b.*)");
        Matcher matcher = selectPattern.matcher(cleaned);
        if (matcher.find()) {
            cleaned = matcher.group(1).trim();
        }

        int semicolonIndex = cleaned.indexOf(";");
        if (semicolonIndex >= 0) {
            cleaned = cleaned.substring(0, semicolonIndex);
        }

        return cleaned.trim();
    }
}