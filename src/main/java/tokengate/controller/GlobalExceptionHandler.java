package tokengate.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tokengate.exception.BusinessRuleException;
import tokengate.exception.EntityNotFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 Not Found (Объект не найден)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "ENTITY_NOT_FOUND", "message", ex.getMessage()));
    }

    // 409 Conflict (Нарушено бизнес-правило)
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "BUSINESS_RULE_VIOLATION", "message", ex.getMessage()));
    }

    // 400 Bad Request (Остальные ошибки, например, неверный JSON)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BAD_REQUEST", "message", ex.getMessage()));
    }
}