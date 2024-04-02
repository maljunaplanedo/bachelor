package ru.dbhub.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
public class ErrorControllerAdvice extends ResponseEntityExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(Exception.class)
    @Nullable
    public final ResponseEntity<Object> handleAnyException(Exception exception, WebRequest request) {
        try {
            return super.handleException(new ResponseStatusException(INTERNAL_SERVER_ERROR), request);
        } catch (Exception unreachable) {
            throw new RuntimeException(unreachable);
        }
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request
    ) {
        if (statusCode.is5xxServerError()) {
            logger.error("Exception occured during handling a request", ex);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
