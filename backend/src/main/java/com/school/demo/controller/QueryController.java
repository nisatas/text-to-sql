package com.school.demo.controller;

import com.school.demo.dto.QueryRequest;
import com.school.demo.dto.QueryResponse;
import com.school.demo.service.QueryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/test")
    public String test() {
        return "API calisiyor";
    }

    @PostMapping("/query")
    public QueryResponse query(@RequestBody(required = false) QueryRequest body) {
        if (body == null || body.getQuestion() == null || body.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("Soru boş olamaz.");
        }
        return queryService.query(body.getQuestion().trim());
    }
}
