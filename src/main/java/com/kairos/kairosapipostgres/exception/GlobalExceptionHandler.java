package com.kairos.kairosapipostgres.exception;

import com.kairos.kairosapipostgres.dto.response.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ShelfEmployeeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleShelfEmployeeNotFound(ShelfEmployeeNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ShelfEmployeeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleShelfEmployeeConflict(ShelfEmployeeAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(EmployeeAisleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeAisleNotFound(EmployeeAisleNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(EmployeeAisleAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeAisleConflict(EmployeeAisleAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ShelfNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleShelfNotFound(ShelfNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(AisleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAisleNotFound(AisleNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(AisleAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleAisleConflict(AisleAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }


    @ExceptionHandler(SectorNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSectorNotFound(SectorNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(SectorAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleSectorConflict(SectorAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleProductConflict(ProductAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, "Operation conflicts with existing data or database constraints", Map.of());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryNotFound(CategoryNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryConflict(CategoryAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(UserNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomerNotFound(CustomerNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(UserAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(EmployeeAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeConflict(EmployeeAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomerConflict(CustomerAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CpfInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCpf(CpfInvalidException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        return response(HttpStatus.BAD_REQUEST, "Invalid request", errors);
    }

    @ExceptionHandler(InvalidEmployeePositionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidEmployeePosition(
            InvalidEmployeePositionException exception
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String message,
            Map<String, String> validationErrors
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                validationErrors
        ));
    }
}
