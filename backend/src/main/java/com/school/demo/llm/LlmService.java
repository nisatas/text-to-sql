package com.school.demo.llm;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    /** SQL sonucuna göre düz Türkçe metin (ikinci Ollama çağrısı). */
    public String summarizeTableAnswer(String question, List<String> columns, List<List<Object>> rows) {
        String tablo = tabloOzetMetni(columns, rows);
        String prompt = """
                Aşağıdaki satırlar PostgreSQL sorgusunun gerçek sonucudur. Veri dışı bilgi uydurma.

                Kullanıcı sorusu: %s

                Sonuç (kolon başlıkları ve en fazla 25 satır):
                %s

                Görev: Soruyu Türkçe, kısa ve net yanıtla (yaklaşık 2–6 cümle).
                Sadece düz metin yaz. SQL, markdown, tablo veya liste işareti kullanma.
                """.formatted(question, tablo);
        String raw = ollamaService.generate(prompt);
        return raw == null ? "" : raw.trim();
    }

    private static String tabloOzetMetni(List<String> columns, List<List<Object>> rows) {
        if (columns == null || columns.isEmpty()) {
            return rows == null || rows.isEmpty() ? "Kayıt yok." : "(Kolon bilgisi yok, satır sayısı: " + rows.size() + ")";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(" | ", columns)).append("\n");
        List<List<Object>> satirlar = rows != null ? rows : List.of();
        int limit = Math.min(satirlar.size(), 25);
        for (int r = 0; r < limit; r++) {
            List<Object> satir = satirlar.get(r);
            List<String> hucreler = new ArrayList<>();
            for (int c = 0; c < columns.size(); c++) {
                String v = "";
                if (satir != null && c < satir.size() && satir.get(c) != null) {
                    v = satir.get(c).toString();
                }
                if (v.length() > 80) {
                    v = v.substring(0, 77) + "...";
                }
                hucreler.add(v);
            }
            sb.append(String.join(" | ", hucreler)).append("\n");
        }
        if (satirlar.size() > 25) {
            sb.append("... (toplam ").append(satirlar.size()).append(" satır; özette sadece ilki gösterildi)\n");
        }
        if (satirlar.isEmpty()) {
            return "Hiç satır dönmedi (0 kayıt).";
        }
        return sb.toString();
    }
}
