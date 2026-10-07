package com.mindata.hotel.infrastructure.adapter.in.rest;

import com.mindata.hotel.application.exception.SearchPublicationException;
import com.mindata.hotel.domain.exception.InvalidSearchException;
import com.mindata.hotel.domain.exception.SearchNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", Objects.toString(error.getDefaultMessage(), "valor inválido")))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido");
        problem.setTitle("Validación fallida");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, unreadableBodyDetail(ex));
        problem.setTitle("Solicitud mal formada");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Parámetros de query inválidos (p. ej. GET /count?searchId= en blanco). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<Map<String, String>> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error -> Map.of(
                        "field", Objects.toString(result.getMethodParameter().getParameterName(), "parámetro"),
                        "message", Objects.toString(error.getDefaultMessage(), "valor inválido"))))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Los parámetros de la solicitud no son válidos");
        problem.setTitle("Validación fallida");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Si Jackson sabe en qué campo falló (tipo incorrecto, decimal en una edad...), se lo indica al usuario. */
    private static String unreadableBodyDetail(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof DatabindException cause && !cause.getPath().isEmpty()) {
            String detail = "Valor inválido en el campo '" + jsonPath(cause.getPath()) + "'";
            if (cause instanceof MismatchedInputException mismatch && mismatch.getTargetType() == LocalDate.class) {
                return detail + ": se espera una fecha válida con formato dd/MM/yyyy";
            }
            return detail;
        }
        return "El cuerpo JSON está mal formado o no se puede leer";
    }

    /** [ages, 1] -> "ages[1]". */
    private static String jsonPath(List<JacksonException.Reference> path) {
        StringBuilder result = new StringBuilder();
        for (JacksonException.Reference reference : path) {
            if (reference.getPropertyName() != null) {
                if (!result.isEmpty()) {
                    result.append('.');
                }
                result.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                result.append('[').append(reference.getIndex()).append(']');
            }
        }
        return result.toString();
    }

    @ExceptionHandler(SearchNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(SearchNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Búsqueda no encontrada");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InvalidSearchException.class)
    ResponseEntity<ProblemDetail> handleInvalidSearch(InvalidSearchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Búsqueda inválida");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(SearchPublicationException.class)
    ResponseEntity<ProblemDetail> handlePublicationFailure(SearchPublicationException ex) {
        log.error("Search could not be published", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "La búsqueda no pudo registrarse en este momento, reintente");
        problem.setTitle("Mensajería no disponible");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}
