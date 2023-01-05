package com.ivan.ra.service.controller;

import com.ivan.ra.service.vo.Errors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

@ControllerAdvice
class ErrorHandlingControllerAdvice {

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    Errors onConstraintValidationException(ConstraintViolationException e) {
        Errors errors = new Errors();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            errors.getErrors().add(violation.getMessage());
        }
        return errors;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    Errors onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Errors errors = new Errors();
        for (org.springframework.validation.FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.getErrors().add(fieldError.getDefaultMessage());
        }
        return errors;
    }
}
