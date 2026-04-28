package com.school.demo.service;

import com.school.demo.dto.QueryResponse;
import com.school.demo.llm.LlmService;
import com.school.demo.llm.SqlCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private final JdbcTemplate jdbcTemplate;
    private final LlmService llmService;
    private final SqlCache sqlCache;
    private final boolean turkishSummaryEnabled;

    private static final Pattern YASAK_KELIMELER = Pattern.compile(
            "(?is)\\b(insert|update|delete|drop|alter|create|truncate|grant|revoke|copy|call|exec|execute|merge|do)\\b"
    );

    private static final List<String> TABLOLAR = List.of("classes", "students", "grades");

    public QueryService(
            JdbcTemplate jdbcTemplate,
            LlmService llmService,
            SqlCache sqlCache,
            @Value("${llm.turkish-summary.enabled:true}") boolean turkishSummaryEnabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.llmService = llmService;
        this.sqlCache = sqlCache;
        this.turkishSummaryEnabled = turkishSummaryEnabled;
    }

    public QueryResponse query(String question, boolean includeSummary) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Soru boş olamaz.");
        }

        long t0 = System.nanoTime();
        String qKey = question.trim().toLowerCase(java.util.Locale.ROOT);
        String uretilenSql = sqlCache.get(qKey).orElse(null);
        boolean cacheHit = uretilenSql != null;

        long tAfterLlmSql;
        if (!cacheHit) {
            uretilenSql = llmService.generateSql(question);
            sqlCache.put(qKey, uretilenSql);
            tAfterLlmSql = System.nanoTime();
        } else {
            tAfterLlmSql = System.nanoTime();
        }
        final String uretilenSqlFinal = uretilenSql;
        final boolean cacheHitFinal = cacheHit;
        String guvenliSql = sqlTemizle(uretilenSql);

        try {
            long tBeforeDb = System.nanoTime();
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
                dbg.put("generatedSqlRaw", uretilenSqlFinal);
                dbg.put("cache", Map.of(
                        "sqlCacheHit", cacheHitFinal,
                        "sqlCacheSize", sqlCache.size()
                ));
                dbg.put("timingMs", Map.of(
                        "llmSql", (tAfterLlmSql - t0) / 1_000_000,
                        "db", (System.nanoTime() - tBeforeDb) / 1_000_000
                ));
                if (includeSummary && turkishSummaryEnabled) {
                    long tBeforeSummary = System.nanoTime();
                    try {
                        cevap.setSummary(llmService.summarizeTableAnswer(question, columns, rows));
                    } catch (Exception ex) {
                        dbg.put("summaryError", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                    } finally {
                        dbg.put("summaryMs", (System.nanoTime() - tBeforeSummary) / 1_000_000);
                    }
                }
                cevap.setDebug(dbg);
                log.info("query timings: llmSql={}ms db={}ms includeSummary={} rowCount={}",
                        (tAfterLlmSql - t0) / 1_000_000,
                        (System.nanoTime() - tBeforeDb) / 1_000_000,
                        includeSummary && turkishSummaryEnabled,
                        rows.size());
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
