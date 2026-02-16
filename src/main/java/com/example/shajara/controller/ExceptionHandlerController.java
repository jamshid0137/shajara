package com.example.shajara.controller;


import com.example.shajara.exception.AppBadException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;

@ControllerAdvice
public class ExceptionHandlerController extends ResponseEntityExceptionHandler {


    @Override//validatsiya xatoliklarini ushlash.
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", status.value());

        List<String> errors = new LinkedList<>();//xatoliklarni yigib olish
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getDefaultMessage());
        }
        body.put("errors", errors);
        return new ResponseEntity<>(body, headers, status);
    }



    @ExceptionHandler(AppBadException.class)//AppBadException qabul qilishi uchun shart !
    public ResponseEntity<String> handler(AppBadException e){
        return ResponseEntity.badRequest().body(e.getMessage());//badRequest() --400
    }


        @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handle(RuntimeException e){
         e.printStackTrace();
        return ResponseEntity.internalServerError().body(e.getMessage());//internalServerError --status 500.
    }

}
