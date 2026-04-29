package com.school.demo.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final JdbcTemplate jdbcTemplate;

    public MetaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/classes")
    public List<String> classes() {
        return jdbcTemplate.queryForList(
                "SELECT class_name FROM classes ORDER BY class_name",
                String.class
        );
    }

    @GetMapping("/subjects")
    public List<String> subjects() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT subject FROM grades ORDER BY subject",
                String.class
        );
    }
}

