package com.school.demo.dto;

public class FilterQueryRequest {
    private String className;      // e.g. 9-A
    private Integer gradeLevel;    // 9..12
    private String studentNumber;  // exact or prefix
    private String studentName;    // contains (ILIKE)
    private String subject;        // e.g. Matematik
    private Integer examNo;        // 1 or 2
    private Integer minScore;      // >=
    private Integer maxScore;      // <=
    private Integer limit;         // max rows (optional)
    /** records | avg_overall | avg_by_subject (optional) */
    private String outputMode;

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Integer getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(Integer gradeLevel) { this.gradeLevel = gradeLevel; }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Integer getExamNo() { return examNo; }
    public void setExamNo(Integer examNo) { this.examNo = examNo; }

    public Integer getMinScore() { return minScore; }
    public void setMinScore(Integer minScore) { this.minScore = minScore; }

    public Integer getMaxScore() { return maxScore; }
    public void setMaxScore(Integer maxScore) { this.maxScore = maxScore; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public String getOutputMode() { return outputMode; }
    public void setOutputMode(String outputMode) { this.outputMode = outputMode; }
}

