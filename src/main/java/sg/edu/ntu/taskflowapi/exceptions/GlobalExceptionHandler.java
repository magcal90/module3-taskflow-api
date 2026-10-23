package sg.edu.ntu.taskflowapi.exceptions;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.extern.slf4j.Slf4j;



/** Converts application and validation exceptions into consistent API errors. */
@RestControllerAdvice
@Slf4j 
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTaskNotFoundException(TaskNotFoundException ex) {
        log.warn("Task request failed: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(StepNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleStepNotFoundException(StepNotFoundException ex) {
        log.warn("Step request failed: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {

        // Build an error message string from all errors
        String errorMessages = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .filter(message -> !message.isBlank())
                .collect(Collectors.joining(". "));

        log.warn("Request validation failed: {}", errorMessages);

        return new ErrorResponse(errorMessages, LocalDateTime.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception ex) {
        // Log the exception here if needed
        log.error("Unexpected request failure", ex);

        return new ErrorResponse("Something went wrong", LocalDateTime.now());

    }

}
