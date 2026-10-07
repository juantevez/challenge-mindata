package com.mindata.hotel.infrastructure.config;

import com.mindata.hotel.application.port.in.CountSearchesUseCase;
import com.mindata.hotel.application.port.in.RegisterSearchUseCase;
import com.mindata.hotel.infrastructure.adapter.in.rest.SearchController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@EnableConfigurationProperties(AppProperties.class) // @ConfigurationPropertiesScan no aplica en los tests de slice
class CorsConfigTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RegisterSearchUseCase registerSearch;

    @MockitoBean
    CountSearchesUseCase countSearches;

    @Test
    void preflightFromAnAllowedOriginIsAccepted() throws Exception {
        mockMvc.perform(options("/search")
                        .header(HttpHeaders.ORIGIN, "http://localhost:8081")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpectAll(
                        status().isOk(),
                        header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8081"));
    }

    @Test
    void preflightFromAnUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/search")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpectAll(
                        status().isForbidden(),
                        header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
