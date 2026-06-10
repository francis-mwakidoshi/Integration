package com.frank.controller;

import com.frank.dto.CountryInfoResponse;
import com.frank.dto.CountryInfoUpdateRequest;
import com.frank.dto.CountryNameRequest;
import com.frank.service.CountryInfoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {

    private static final Logger log = LoggerFactory.getLogger(CountryController.class);

    private final CountryInfoService countryInfoService;

    public CountryController(CountryInfoService countryInfoService) {
        this.countryInfoService = countryInfoService;
    }

    @PostMapping("/import")
    public ResponseEntity<CountryInfoResponse> importCountry(@Valid @RequestBody CountryNameRequest request) {
        log.info("Received country import request name={}", request.name());
        CountryInfoResponse response = countryInfoService.importCountryByName(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CountryInfoResponse>> getAllCountries() {
        return ResponseEntity.ok(countryInfoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CountryInfoResponse> getCountryById(@PathVariable Long id) {
        return ResponseEntity.ok(countryInfoService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryInfoResponse> updateCountry(@PathVariable Long id,
                                                               @Valid @RequestBody CountryInfoUpdateRequest request) {
        return ResponseEntity.ok(countryInfoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCountry(@PathVariable Long id) {
        countryInfoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
