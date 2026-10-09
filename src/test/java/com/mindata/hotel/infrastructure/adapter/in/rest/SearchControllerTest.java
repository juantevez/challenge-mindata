package com.mindata.hotel.infrastructure.adapter.in.rest;

import com.mindata.hotel.application.exception.SearchPublicationException;
import com.mindata.hotel.application.port.in.CountSearchesUseCase;
import com.mindata.hotel.application.port.in.RegisterSearchUseCase;
import com.mindata.hotel.domain.exception.SearchNotFoundException;
import com.mindata.hotel.domain.model.HotelSearch;
import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.domain.model.SearchCount;
import com.mindata.hotel.domain.model.SearchId;
import com.mindata.hotel.infrastructure.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@EnableConfigurationProperties(AppProperties.class)
class SearchControllerTest {

    private static final String VALID_BODY = """
            {"hotelId": "1234aBc", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30, 29, 1, 3]}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RegisterSearchUseCase registerSearch;

    @MockitoBean
    CountSearchesUseCase countSearches;

    @Test
    void searchReturnsTheGeneratedIdAndForwardsAnImmutableDomainSearch() throws Exception {
        when(registerSearch.register(any(HotelSearch.class))).thenReturn(new SearchId("abc-123"));

        postSearch(VALID_BODY).andExpectAll(
                status().isOk(),
                jsonPath("$.searchId").value("abc-123"));

        ArgumentCaptor<HotelSearch> captor = ArgumentCaptor.forClass(HotelSearch.class);
        verify(registerSearch).register(captor.capture());
        HotelSearch search = captor.getValue();
        assertAll(
                () -> assertThat(search.hotelId()).isEqualTo("1234aBc"),
                () -> assertThat(search.checkIn()).isEqualTo(LocalDate.of(2026, 10, 16)),
                () -> assertThat(search.checkOut()).isEqualTo(LocalDate.of(2026, 10, 18)),
                () -> assertThat(search.ages()).containsExactly(30, 29, 1, 3));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidPayloads")
    void searchRejectsInvalidPayloadsWith400(String description, String body) throws Exception {
        postSearch(body).andExpect(status().isBadRequest());

        verifyNoInteractions(registerSearch);
    }

    static Stream<Arguments> invalidPayloads() {
        return Stream.of(
                Arguments.of("blank hotelId", """
                        {"hotelId": " ", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30]}"""),
                Arguments.of("missing hotelId", """
                        {"checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30]}"""),
                Arguments.of("hotelId too long", """
                        {"hotelId": "%s", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30]}"""
                        .formatted("h".repeat(65))),
                Arguments.of("ISO date instead of dd/MM/yyyy", """
                        {"hotelId": "h", "checkIn": "2026-10-16", "checkOut": "18/10/2026", "ages": [30]}"""),
                Arguments.of("impossible calendar date", """
                        {"hotelId": "h", "checkIn": "31/11/2026", "checkOut": "02/12/2026", "ages": [30]}"""),
                Arguments.of("month 13", """
                        {"hotelId": "h", "checkIn": "16/13/2026", "checkOut": "18/10/2026", "ages": [30]}"""),
                Arguments.of("date as a number", """
                        {"hotelId": "h", "checkIn": 20742, "checkOut": "18/10/2026", "ages": [30]}"""),
                Arguments.of("missing checkOut", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "ages": [30]}"""),
                Arguments.of("checkOut equal to checkIn", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "16/10/2026", "ages": [30]}"""),
                Arguments.of("checkOut before checkIn", """
                        {"hotelId": "h", "checkIn": "18/10/2026", "checkOut": "16/10/2026", "ages": [30]}"""),
                Arguments.of("empty ages", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": []}"""),
                Arguments.of("missing ages", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026"}"""),
                Arguments.of("negative age", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30, -1]}"""),
                Arguments.of("age above 120", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [121]}"""),
                Arguments.of("null age", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30, null]}"""),
                Arguments.of("fractional age", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30.5]}"""),
                Arguments.of("too many ages", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [%s]}"""
                        .formatted("1,".repeat(20) + "1")),
                Arguments.of("broken JSON", "{\"hotelId\": "),
                Arguments.of("empty body", ""));
    }

    @ParameterizedTest(name = "[{index}] {0}: {1}")
    @MethodSource("invalidPayloadMessages")
    void searchTellsTheUserWhatIsWrong(String field, String message, String body) throws Exception {
        postSearch(body).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.errors[?(@.field == '%s')].message".formatted(field)).value(message));
    }

    static Stream<Arguments> invalidPayloadMessages() {
        return Stream.of(
                Arguments.of("stayOrderValid", "checkOut debe ser posterior a checkIn", """
                        {"hotelId": "h", "checkIn": "18/10/2026", "checkOut": "16/10/2026", "ages": [30]}"""),
                Arguments.of("ages[1]", "debe ser mayor o igual que 0", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30, -1]}"""),
                Arguments.of("ages[1]", "no debe ser nulo", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30, null]}"""),
                Arguments.of("hotelId", "no debe estar en blanco", """
                        {"hotelId": null, "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30]}"""),
                Arguments.of("hotelId", "no debe estar en blanco", """
                        {"hotelId": "", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30]}"""),
                Arguments.of("checkIn", "no debe ser nulo", """
                        {"hotelId": "h", "checkIn": "", "checkOut": "18/10/2026", "ages": [30]}"""),
                Arguments.of("checkOut", "no debe ser nulo", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "ages": [30]}"""),
                Arguments.of("ages", "no debe estar vacío", """
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": []}"""));
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("unreadableFields")
    void searchPointsToTheFieldThatCouldNotBeRead(String body, String detail) throws Exception {
        postSearch(body).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.detail").value(detail));
    }

    static Stream<Arguments> unreadableFields() {
        return Stream.of(
                Arguments.of("""
                        {"hotelId": "h", "checkIn": "16/10/2026", "checkOut": "18/10/2026", "ages": [30, 2.5]}""",
                        "Valor inválido en el campo 'ages[1]'"),
                Arguments.of("""
                        {"hotelId": "h", "checkIn": "31/11/2026", "checkOut": "02/12/2026", "ages": [30]}""",
                        "Valor inválido en el campo 'checkIn': se espera una fecha válida con formato dd/MM/yyyy"));
    }

    @Test
    void searchReturns503WhenTheMessageCannotBePublished() throws Exception {
        when(registerSearch.register(any(HotelSearch.class)))
                .thenThrow(new SearchPublicationException("kafka down", new RuntimeException()));

        postSearch(VALID_BODY).andExpect(status().isServiceUnavailable());
    }

    @Test
    void countReturnsTheSearchAndTheNumberOfEqualSearches() throws Exception {
        SearchId id = new SearchId("abc-123");
        HotelSearch search = new HotelSearch(
                "1234aBc", LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18), List.of(3, 29, 30, 1));
        when(countSearches.count(id)).thenReturn(new SearchCount(new RegisteredSearch(id, search), 100));

        mockMvc.perform(get("/count").param("searchId", "abc-123")).andExpectAll(
                status().isOk(),
                jsonPath("$.searchId").value("abc-123"),
                jsonPath("$.search.hotelId").value("1234aBc"),
                jsonPath("$.search.checkIn").value("16/10/2026"),
                jsonPath("$.search.checkOut").value("18/10/2026"),
                jsonPath("$.search.ages[0]").value(3),
                jsonPath("$.search.ages.length()").value(4),
                jsonPath("$.count").value(100));
    }

    @Test
    void countReturns404ForAnUnknownSearchId() throws Exception {
        when(countSearches.count(any(SearchId.class))).thenThrow(new SearchNotFoundException(new SearchId("nope")));

        mockMvc.perform(get("/count").param("searchId", "nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void countRequiresASearchId() throws Exception {
        mockMvc.perform(get("/count")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/count").param("searchId", " ")).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.errors[0].field").value("searchId"),
                jsonPath("$.errors[0].message").value("no debe estar en blanco"));

        verify(countSearches, never()).count(any());
    }

    private ResultActions postSearch(String body) throws Exception {
        return mockMvc.perform(post("/search").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
