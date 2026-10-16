package exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import dto.response.ValidationError;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        
        ValidationError error = new ValidationError(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            LocalDateTime.now(),
            fieldError.getField(),
            fieldError.getRejectedValue(),
            fieldError.getDefaultMessage()
        );

        return ResponseEntity.badRequest().body(error);
    }

    // Jackson fails before any validation runs, e.g. "dateOfBirth": "01/15/1990" or "name": {}. Without this,
    // Spring's default error page answered with no field name.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationError> handleNotReadable(HttpMessageNotReadableException ex) {
        String field = null;
        Object rejected = null;
        String reason = "Request body is not valid JSON";

        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            field = mismatch.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));
            if (mismatch instanceof InvalidFormatException invalid) {
                rejected = invalid.getValue();
            }
            reason = mismatch.getTargetType() == LocalDate.class
                ? field + " must be a valid date in YYYY-MM-DD format"
                : field + " has the wrong type";
        }

        ValidationError error = new ValidationError(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            LocalDateTime.now(),
            field,
            rejected,
            reason
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ValidationError> handleResourceNotFound(ResourceNotFoundException ex) {
        ValidationError error = new ValidationError(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now(),
            ex.getResourceType(),
            ex.getResourceId(),
            "Resource not found"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ValidationError> handleInvalidOrder(InvalidOrderException ex) {
        ValidationError error = new ValidationError(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid order",
            LocalDateTime.now(),
            ex.getField(),
            null,
            ex.getReason()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ValidationError> handleInsufficientFunds(InsufficientFundsException ex) {
        ValidationError error = new ValidationError(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            LocalDateTime.now(),
            "balance",
            ex.getRequested(),
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(TransactionProcessingException.class)
    public ResponseEntity<ValidationError> handleTransactionProcessing(TransactionProcessingException ex) {
        ValidationError error = new ValidationError(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Order processing transaction failed",
            LocalDateTime.now(),
            "orderId",
            ex.getOrderId(),
            ex.getReason()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(AdminRoleNotAssignedException.class)
    public ResponseEntity<String> handleAdminRoleNotAssigned(AdminRoleNotAssignedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(SessionRejectedException.class)
    public ResponseEntity<Map<String, String>> handleSessionRejected(SessionRejectedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getCode(), "message", ex.getMessage()));
    }
}
