package com.school.demo.exception;

import com.school.demo.dto.QueryResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public QueryResponse handleBadRequest(IllegalArgumentException ex) {
        QueryResponse r = new QueryResponse();
        r.setStatus("error");
        r.setError(ex.getMessage());
        return r;
    }

    @ExceptionHandler(Exception.class)
    public QueryResponse handleOther(Exception ex) {
        QueryResponse r = new QueryResponse();
        r.setStatus("error");
        r.setError("Beklenmeyen hata.");
        r.setDebug(Map.of(
                "exceptionType", ex.getClass().getSimpleName(),
                "message", ex.getMessage() != null ? ex.getMessage() : ""
        ));
        return r;
    }
}
