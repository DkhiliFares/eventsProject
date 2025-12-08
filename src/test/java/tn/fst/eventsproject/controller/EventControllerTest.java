package tn.fst.eventsproject.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import tn.fst.eventsproject.entities.Event;
import tn.fst.eventsproject.services.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAddEvent() throws Exception {
        Event event = new Event();
        event.setId(1L);
        event.setName("New Event");

        when(eventService.addEvent(any(Event.class))).thenReturn(event);

        mockMvc.perform(post("/event/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Event"));
    }

    @Test
    void testGetEvent() throws Exception {
        Event event = new Event();
        event.setId(10L);
        event.setName("Event 10");

        when(eventService.findEventById(10L)).thenReturn(event);

        mockMvc.perform(get("/event/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Event 10"));
    }
}
