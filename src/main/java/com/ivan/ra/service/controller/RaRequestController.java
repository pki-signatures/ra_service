package com.ivan.ra.service.controller;

import com.ivan.ra.service.vo.ApproveRaRequest;
import com.ivan.ra.service.vo.RaRequest;
import com.ivan.ra.service.vo.RejectRaRequest;
import com.ivan.ra.service.vo.SearchRaRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

public class RaRequestController {

    @PostMapping(value = "/ra/v1/registration/authority/request", produces = "application/json", consumes = "application/json")
    public ResponseEntity createRequest(@Valid @RequestBody RaRequest request, HttpServletRequest httpRequest) {
        return null;
    }

    @PostMapping(value = "/ra/v1/registration/authority/validation/info")
    public ResponseEntity uploadValidationDocs(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return null;
    }

    @PutMapping(value = "/ra/v1/registration/authority/request", produces = "application/json", consumes = "application/json")
    public ResponseEntity updateRequest(@Valid @RequestBody RaRequest request, HttpServletRequest httpRequest) {
        return null;
    }

    @PostMapping(value = "/ra/v1/registration/authority/request/data", produces = "application/json", consumes = "application/json")
    public ResponseEntity getRequests(@Valid @RequestBody SearchRaRequest request, HttpServletRequest httpRequest) {
        return null;
    }

    @PostMapping(value = "/ra/v1/registration/authority/request/approve", consumes = "application/json")
    public ResponseEntity approveRequest(@Valid @RequestBody ApproveRaRequest request, HttpServletRequest httpRequest) {
        return null;
    }

    @PostMapping(value = "/ra/v1/registration/authority/request/reject", consumes = "application/json")
    public ResponseEntity rejectRequest(@Valid @RequestBody RejectRaRequest request, HttpServletRequest httpRequest) {
        return null;
    }

}
