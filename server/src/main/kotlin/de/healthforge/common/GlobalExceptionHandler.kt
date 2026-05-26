package de.healthforge.common

import com.fasterxml.jackson.annotation.JsonInclude
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

class ApiException(
    val status: HttpStatus,
    val errorCode: String,
    message: String,
) : RuntimeException(message)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val errorCode: String,
    val message: String?,
    val fieldErrors: Map<String, String>? = null,
)

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApi(e: ApiException): ResponseEntity<ErrorResponse> {
        log.warn("API error [{}] {}: {}", e.status.value(), e.errorCode, e.message)
        return ResponseEntity.status(e.status).body(
            ErrorResponse(
                status = e.status.value(),
                error = e.status.reasonPhrase,
                errorCode = e.errorCode,
                message = e.message,
            ),
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val fieldErrors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                status = 400,
                error = "Bad Request",
                errorCode = "VALIDATION_FAILED",
                message = "Validation failed",
                fieldErrors = fieldErrors,
            ),
        )
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(e: BadCredentialsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(
                status = 401,
                error = "Unauthorized",
                errorCode = "BAD_CREDENTIALS",
                message = "Invalid email or password",
            ),
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse(
                status = 403,
                error = "Forbidden",
                errorCode = "ACCESS_DENIED",
                message = e.message,
            ),
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                status = 500,
                error = "Internal Server Error",
                errorCode = "INTERNAL_ERROR",
                message = "An unexpected error occurred",
            ),
        )
    }
}
