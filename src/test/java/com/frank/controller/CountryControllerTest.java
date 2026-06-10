package com.frank.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CountryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listCountriesReturnsEmptyArrayWhenNoData() throws Exception {
        mockMvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void importRejectsBlankCountryName() throws Exception {
        mockMvc.perform(post("/api/v1/countries/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getByIdReturnsNotFoundForMissingCountry() throws Exception {
        mockMvc.perform(get("/api/v1/countries/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Country not found with id: 9999"));
    }
}
