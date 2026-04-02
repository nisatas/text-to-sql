package com.school.demo.exception;

import com.school.demo.dto.QueryResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public QueryResponse handleBadRequest(IllegalArgumentException ex) {
        QueryResponse response = new QueryResponse();
        response.setStatus("error");
        response.setError(ex.getMessage());
        response.setDebug(Map.of(
                "exceptionType", ex.getClass().getSimpleName()
        ));
        return response;
    }

    @ExceptionHandler(Exception.class)
    public QueryResponse handleUnhandled(Exception ex) {
        QueryResponse response = new QueryResponse();
        response.setStatus("error");

        List<String> causeChain = new ArrayList<>();
        Throwable t = ex;
        while (t != null) {
            String msg = t.getMessage();
            causeChain.add(t.getClass().getName() + (msg == null ? "" : ": " + msg));
            t = t.getCause();
        }

        response.setError("Beklenmeyen bir hata oluştu.");
        response.setDebug(Map.of(
                "exceptionType", ex.getClass().getName(),
                "causeChain", causeChain
        ));
        return response;
    }
}