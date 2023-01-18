package com.ivan.ra.service.controller;


import com.ivan.ra.service.vo.Errors;
import org.json.simple.JSONArray;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
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

    public void sendErrorResponse(String errorMessage, HttpServletResponse response) throws Exception {
        JSONArray errors = new JSONArray();
        errors.add(errorMessage);

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        PrintWriter writer = response.getWriter();
        writer.write(errors.toJSONString());
        writer.flush();
    }

}
