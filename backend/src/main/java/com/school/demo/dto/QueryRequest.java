package com.school.demo.dto;

public class QueryRequest {

    private String question;
    /** Optional: when true, backend will also generate a Turkish summary via a second LLM call. */
    private Boolean includeSummary;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Boolean getIncludeSummary() {
        return includeSummary;
    }

    public void setIncludeSummary(Boolean includeSummary) {
        this.includeSummary = includeSummary;
    }
}