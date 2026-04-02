package com.school.demo.service;

import com.school.demo.dto.QueryResponse;
import com.school.demo.llm.LlmService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class QueryService {

    private final JdbcTemplate jdbcTemplate;
    private final LlmService llmService;

    private static final Pattern PROHIBITED_SQL = Pattern.compile(
            "(?is)\\b(insert|update|delete|drop|alter|create|truncate|grant|revoke|copy|call|exec|execute|merge|do)\\b"
    );

    private static final List<String> ALLOWED_TABLES = List.of(
            "classes",
            "students",
            "grades"
    );

    public QueryService(JdbcTemplate jdbcTemplate, LlmService llmService) {
        this.jdbcTemplate = jdbcTemplate;
        this.llmService = llmService;
    }

    public QueryResponse query(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Soru boş olamaz.");
        }

        String sql = llmService.generateSql(question);
        String safeSql = sanitizeSql(sql);

        try {
            return jdbcTemplate.query(safeSql, rs -> {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                List<String> columns = new ArrayList<>(colCount);
                for (int i = 1; i <= colCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }

                List<List<Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>(colCount);
                    for (int i = 1; i <= colCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    rows.add(row);
                }

                QueryResponse response = new QueryResponse();
                response.setQuestion(question);
                response.setSql(safeSql);
                response.setStatus("success");
                response.setColumns(columns);
                response.setRows(rows);
                response.setDebug(Map.of(
                        "rowCount", rows.size(),
                        "generatedSqlRaw", sql
                ));
                return response;
            });
        } catch (Exception ex) {
            QueryResponse response = new QueryResponse();
            response.setQuestion(question);
            response.setSql(safeSql);
            response.setStatus("error");
            response.setError("SQL çalıştırılırken hata oluştu.");
            response.setDebug(Map.of(
                    "exceptionType", ex.getClass().getName(),
                    "generatedSqlRaw", sql,
                    "safeSql", safeSql,
                    "message", ex.getMessage()
            ));
            return response;
        }
    }

    private String sanitizeSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL üretilemedi.");
        }

        String cleaned = sql.trim()
                .replace("```sql", "")
                .replace("```", "")
                .trim();

        if (cleaned.length() > 4000) {
            throw new IllegalArgumentException("SQL çok uzun.");
        }

        cleaned = cleaned.replace(";", "").trim();

        String lower = cleaned.toLowerCase(Locale.ROOT);

        if (!lower.startsWith("select")) {
            throw new IllegalArgumentException("Sadece SELECT sorguları desteklenir.");
        }

        if (PROHIBITED_SQL.matcher(cleaned).find()) {
            throw new IllegalArgumentException("SQL güvenlik filtresi tarafından engellendi.");
        }

        boolean usesAllowedTable = ALLOWED_TABLES.stream().anyMatch(lower::contains);
        if (!usesAllowedTable) {
            throw new IllegalArgumentException("Sorgu izin verilen tabloları kullanmıyor.");
        }

        if (!cleaned.matches("(?is).*\\blimit\\b\\s+\\d+.*")) {
            cleaned = cleaned + " LIMIT 100";
        }

        return cleaned;
    }
}