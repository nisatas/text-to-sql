package com.school.demo.service;

import com.school.demo.dto.QueryResponse;
import com.school.demo.dto.FilterQueryRequest;
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
import java.text.Normalizer;
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
        validateQuestion(question);

        long t0 = System.nanoTime();
        String qKey = normalizeForCacheKey(question);
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

    public QueryResponse queryByFilters(FilterQueryRequest f) {
        if (f == null) {
            throw new IllegalArgumentException("Filtre boş olamaz.");
        }

        int limit = f.getLimit() == null ? 200 : Math.min(Math.max(f.getLimit(), 1), 1000);

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        where.append("""
                FROM grades g
                JOIN students s ON g.student_id = s.id
                JOIN classes c ON s.class_id = c.id
                WHERE 1=1
                """);

        if (f.getClassName() != null && !f.getClassName().isBlank()) {
            where.append(" AND c.class_name = ? ");
            params.add(f.getClassName().trim());
        }
        if (f.getGradeLevel() != null) {
            int gl = f.getGradeLevel();
            if (gl < 9 || gl > 12) {
                throw new IllegalArgumentException("Sınıf seviyesi 9-12 olmalı.");
            }
            where.append(" AND c.grade_level = ? ");
            params.add(gl);
        }
        if (f.getStudentNumber() != null && !f.getStudentNumber().isBlank()) {
            String sn = f.getStudentNumber().trim();
            if (sn.endsWith("*")) {
                where.append(" AND s.student_number ILIKE ? ");
                params.add(sn.substring(0, sn.length() - 1) + "%");
            } else {
                where.append(" AND s.student_number = ? ");
                params.add(sn);
            }
        }
        if (f.getStudentName() != null && !f.getStudentName().isBlank()) {
            where.append(" AND s.name ILIKE ? ");
            params.add("%" + f.getStudentName().trim() + "%");
        }
        if (f.getSubject() != null && !f.getSubject().isBlank()) {
            where.append(" AND g.subject = ? ");
            params.add(f.getSubject().trim());
        }
        if (f.getExamNo() != null) {
            int en = f.getExamNo();
            if (en != 1 && en != 2) {
                throw new IllegalArgumentException("Sınav no 1 veya 2 olmalı.");
            }
            where.append(" AND g.exam_no = ? ");
            params.add(en);
        }
        if (f.getMinScore() != null) {
            where.append(" AND g.score >= ? ");
            params.add(f.getMinScore());
        }
        if (f.getMaxScore() != null) {
            where.append(" AND g.score <= ? ");
            params.add(f.getMaxScore());
        }

        String mode = f.getOutputMode() == null
                ? "records"
                : f.getOutputMode().trim().toLowerCase(Locale.ROOT);

        String select;
        String groupOrderLimit;

        switch (mode) {
            case "avg_overall" -> {
                select = "SELECT AVG(g.score) AS average_score, COUNT(*) AS count\n";
                groupOrderLimit = "\nLIMIT 1";
            }
            case "avg_by_subject" -> {
                select = "SELECT g.subject, g.exam_no, AVG(g.score) AS average_score, COUNT(*) AS count\n";
                groupOrderLimit = "\nGROUP BY g.subject, g.exam_no\nORDER BY g.subject, g.exam_no\nLIMIT " + limit;
            }
            default -> {
                select = "SELECT s.name, s.student_number, c.class_name, g.subject, g.exam_no, g.score\n";
                groupOrderLimit = "\nORDER BY c.class_name, s.name, g.subject, g.exam_no\nLIMIT " + limit;
            }
        }

        String safeSql = select + where + groupOrderLimit;

        try {
            return jdbcTemplate.query(safeSql, params.toArray(), rs -> {
                ResultSetMetaData meta = rs.getMetaData();
                int n = meta.getColumnCount();

                List<String> columns = new ArrayList<>(n);
                for (int i = 1; i <= n; i++) {
                    columns.add(meta.getColumnLabel(i));
                }

                List<List<Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>(n);
                    for (int i = 1; i <= n; i++) {
                        row.add(rs.getObject(i));
                    }
                    rows.add(row);
                }

                QueryResponse r = new QueryResponse();
                r.setQuestion("filters");
                r.setSql(safeSql);
                r.setStatus("success");
                r.setColumns(columns);
                r.setRows(rows);
                r.setDebug(Map.of(
                        "rowCount", rows.size(),
                        "params", params
                ));
                return r;
            });
        } catch (Exception ex) {
            QueryResponse r = new QueryResponse();
            r.setQuestion("filters");
            r.setSql(safeSql);
            r.setStatus("error");
            r.setError("SQL çalıştırılırken hata oluştu.");
            r.setDebug(Map.of(
                    "exceptionType", ex.getClass().getName(),
                    "message", ex.getMessage() != null ? ex.getMessage() : "",
                    "params", params
            ));
            return r;
        }
    }

    private static String normalizeForCacheKey(String question) {
        if (question == null) {
            return "";
        }
        String s = question.trim().toLowerCase(Locale.ROOT);

        // Handle Turkish dotted/dotless i explicitly
        s = s.replace('ı', 'i').replace('İ', 'i');

        // Strip accents/diacritics: "öğrenci" -> "ogrenci"
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        // Drop punctuation/symbols, keep letters/digits/spaces only
        s = s.replaceAll("[^\\p{Alnum}\\s]+", " ");

        // Collapse whitespace
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private static void validateQuestion(String question) {
        String q = question == null ? "" : question.trim();
        if (q.length() < 3) {
            throw new IllegalArgumentException("Soru çok kısa.");
        }

        String s = normalizeForCacheKey(q);

        // Must contain at least one domain signal to avoid random queries from irrelevant prompts.
        boolean hasClass = s.matches(".*\\b(9|10|11|12)\\s*-\\s*[a-z]\\b.*");
        boolean hasGradeLevel = s.matches(".*\\b(9|10|11|12)\\b.*\\b(sinif|siniflar|sinifi)\\b.*") || s.matches(".*\\b(sinif|siniflar)\\b.*");
        boolean hasStudentNumber = s.matches(".*\\b(demo-?001|\\d{4}-\\d{4}|\\d{4}-\\d{2}[a-z]-\\d{2})\\b.*");
        boolean hasSubject = s.matches(".*\\b(matematik|fizik|kimya|turkce|biyoloji)\\b.*");
        boolean hasExam = s.matches(".*\\b(1|2)\\b.*\\b(sinav|sinavi)\\b.*") || s.contains("exam");
        boolean hasScoreKeyword = s.matches(".*\\b(not|puan|skor|ortalama|avg|average)\\b.*");
        boolean hasStudentKeyword = s.matches(".*\\b(ogrenci|ogrenciler|student|students)\\b.*");

        boolean signal = hasClass || hasGradeLevel || hasStudentNumber || hasSubject || hasExam || hasScoreKeyword || hasStudentKeyword;
        if (!signal) {
            throw new IllegalArgumentException(
                    "Bu soru öğrenci/sınıf/not verileriyle ilgili görünmüyor. " +
                            "Örn: '9-A sınıfı Matematik 2. sınav ortalaması' veya 'Matematik notu 50 altı öğrenciler'."
            );
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

        if (s.equalsIgnoreCase("UNSUPPORTED")) {
            throw new IllegalArgumentException("Soru bu sistem tarafından desteklenmiyor.");
        }

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
