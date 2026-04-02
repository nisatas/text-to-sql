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
        return "API çalışıyor 🚀";
    }

    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest body) {
        return queryService.query(body.getQuestion());
    }
}