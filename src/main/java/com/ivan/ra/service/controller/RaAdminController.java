package com.ivan.ra.service.controller;

import com.ivan.ra.service.vo.RaAdmin;
import com.ivan.ra.service.vo.RegisterRaRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@RestController
@Validated
public class RaAdminController {

    private static final Logger logger = LogManager.getLogger(RaAdminController.class);

    @PostMapping(value = "/ra/v1/registration/authority/admin", produces = "application/json", consumes = "application/json")
    public ResponseEntity registerRaAdmin(@Valid @RequestBody RaAdmin request, HttpServletRequest httpRequest) {
        // Check status of RA must not be disabled
        return null;
    }

    @PutMapping(value = "/ra/v1/registration/authority/admin", produces = "application/json", consumes = "application/json")
    public ResponseEntity updateRaAdmin(@Valid @RequestBody RaAdmin request, HttpServletRequest httpRequest) {
        return null;
    }

    @GetMapping(value = "/ra/v1/registration/authority/admin", produces = "application/json")
    public ResponseEntity getAllRaAdmins(HttpServletRequest httpRequest) {
        return null;
    }

    @GetMapping(value = "/ra/v1/registration/authority/admin/{name}", produces = "application/json")
    public ResponseEntity getRaAdmin(@RequestParam("name") @NotBlank String name, HttpServletRequest httpRequest) {
        return null;
    }
}