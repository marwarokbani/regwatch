package com.regwatch.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.regwatch.llm.LlmUnavailableException;

/**
 * Erreurs renvoyées au format standard RFC 9457 (ProblemDetail).
 * La classe parente couvre déjà les erreurs Spring MVC (validation → 400, etc.).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(LlmUnavailableException.class)
    ProblemDetail handleLlmUnavailable(LlmUnavailableException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Aucun fournisseur LLM n'a pu répondre.");
        problem.setTitle("LLM indisponible");
        problem.setProperty("failures", e.failures());
        return problem;
    }
}
