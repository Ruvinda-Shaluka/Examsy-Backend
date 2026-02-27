package lk.ijse.examsybackend.exception;

import lk.ijse.examsybackend.util.APIResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle Validation Errors (e.g., @NotBlank, @Min, @Size in DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(new APIResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                errors
        ), HttpStatus.BAD_REQUEST);
    }

    // 2. Handle Resource Not Found (JPA/Hibernate)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<APIResponse<String>> handleEntityNotFoundException(EntityNotFoundException ex) {
        return new ResponseEntity<>(new APIResponse<>(
                HttpStatus.NOT_FOUND.value(),
                "Requested Resource Not Found",
                ex.getMessage()
        ), HttpStatus.NOT_FOUND);
    }

    // 3. Handle Database Integrity Violations (e.g., Duplicate Keys/Emails)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse<String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(new APIResponse<>(
                HttpStatus.CONFLICT.value(),
                "Database Integrity Violation",
                "Possible duplicate entry or constraint violation"
        ), HttpStatus.CONFLICT);
    }

    // 4. Handle Invalid Parameter Types (e.g., passing "abc" for a Long ID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String error = String.format("Parameter '%s' should be of type %s", ex.getName(), ex.getRequiredType().getSimpleName());
        return new ResponseEntity<>(new APIResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                "Type Mismatch Error",
                error
        ), HttpStatus.BAD_REQUEST);
    }

    // 5. Handle Null Pointer Exceptions
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<APIResponse<String>> handleNullPointerException(NullPointerException ex) {
        return new ResponseEntity<>(new APIResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                "Null Value Exception",
                "A null pointer occurred. Check your input data."
        ), HttpStatus.BAD_REQUEST);
    }

    // 6. Global Catch-all for any other unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<String>> handleGlobalException(Exception ex) {
        return new ResponseEntity<>(new APIResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage()
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}