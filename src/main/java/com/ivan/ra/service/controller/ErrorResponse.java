package com.ivan.ra.service.controller;


import com.ivan.ra.service.vo.Errors;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ErrorResponse {

    public ResponseEntity generateErrorResponse(String errorMessage) {
        List<String> errorsList = new ArrayList<>();
        errorsList.add(errorMessage);

        Errors errors = new Errors();
        errors.setErrors(errorsList);
        return ResponseEntity.badRequest().body(errors);
    }

}
