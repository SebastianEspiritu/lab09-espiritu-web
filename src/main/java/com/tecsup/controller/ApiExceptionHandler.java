package com.tecsup.controller;

import com.tecsup.dto.MessageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MessageResponse> authentication(AuthenticationException exception) {
        return ResponseEntity.status(401).body(new MessageResponse("Credenciales incorrectas"));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<MessageResponse> validation(Exception exception) {
        return ResponseEntity.badRequest().body(new MessageResponse("Solicitud invalida"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponse> duplicate(DataIntegrityViolationException exception) {
        return ResponseEntity.badRequest().body(new MessageResponse("Error: Usuario o email ya existe"));
    }
}
