package com.school.demo.service;

import com.school.demo.dto.QueryResponse;
import com.school.demo.llm.LlmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class QueryService {

    private final JdbcTemplate jdbcTemplate;
    private final LlmService llmService;
    private final boolean turkishSummaryEnabled;

    private static final Pattern YASAK_KELIMELER = Pattern.compile(
            "(?is)\\b(insert|update|delete|drop|alter|create|truncate|grant|revoke|copy|call|exec|execute|merge|do)\\b"
    );

    private static final List<String> TABLOLAR = List.of("classes", "students", "grades");

    public QueryService(
            JdbcTemplate jdbcTemplate,
            LlmService llmService,
            @Value("${llm.turkish-summary.enabled:true}") boolean turkishSummaryEnabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.llmService = llmService;
        this.turkishSummaryEnabled = turkishSummaryEnabled;
    }

    public QueryResponse query(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Soru boş olamaz.");
        }

        String uretilenSql = llmService.generateSql(question);
        String guvenliSql = sqlTemizle(uretilenSql);

        try {
            return jdbcTemplate.query(guvenliSql, rs -> {
                ResultSetMetaData meta = rs.getMetaData();
                int n = meta.getColumnCount();

                List<String> columns = new ArrayList<>(n);
                for (int i = 1; i <= n; i++) {
                    columns.add(meta.getColumnLabel(i));
                }

                List<List<Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    List<Object> satir = new ArrayList<>(n);
                    for (int i = 1; i <= n; i++) {
                        satir.add(rs.getObject(i));
                    }
                    rows.add(satir);
                }

                QueryResponse cevap = new QueryResponse();
                cevap.setQuestion(question);
                cevap.setSql(guvenliSql);
                cevap.setStatus("success");
                cevap.setColumns(columns);
                cevap.setRows(rows);

                Map<String, Object> dbg = new HashMap<>();
                dbg.put("rowCount", rows.size());
                dbg.put("generatedSqlRaw", uretilenSql);
                if (turkishSummaryEnabled) {
                    try {
                        cevap.setSummary(llmService.summarizeTableAnswer(question, columns, rows));
                    } catch (Exception ex) {
                        dbg.put("summaryError", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                    }
                }
                cevap.setDebug(dbg);
                return cevap;
            });
        } catch (Exception ex) {
            QueryResponse cevap = new QueryResponse();
            cevap.setQuestion(question);
            cevap.setSql(guvenliSql);
            cevap.setStatus("error");
            cevap.setError("SQL çalıştırılırken hata oluştu.");
            cevap.setDebug(Map.of(
                    "exceptionType", ex.getClass().getName(),
                    "generatedSqlRaw", uretilenSql,
                    "safeSql", guvenliSql,
                    "message", ex.getMessage() != null ? ex.getMessage() : ""
            ));
            return cevap;
        }
    }

    private String sqlTemizle(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL üretilemedi.");
        }

        String s = sql.trim()
                .replace("```sql", "")
                .replace("```", "")
                .trim();

        if (s.length() > 4000) {
            throw new IllegalArgumentException("SQL çok uzun.");
        }

        s = s.replace(";", "").trim();
        String lower = s.toLowerCase(Locale.ROOT);

        if (!lower.startsWith("select")) {
            throw new IllegalArgumentException("Sadece SELECT desteklenir.");
        }
        if (YASAK_KELIMELER.matcher(s).find()) {
            throw new IllegalArgumentException("SQL güvenlik filtresine takıldı.");
        }

        boolean tabloVar = TABLOLAR.stream().anyMatch(lower::contains);
        if (!tabloVar) {
            throw new IllegalArgumentException("Sorgu izin verilen tabloları kullanmıyor.");
        }

        if (!s.matches("(?is).*\\blimit\\b\\s+\\d+.*")) {
            s = s + " LIMIT 100";
        }
        return s;
    }
}
