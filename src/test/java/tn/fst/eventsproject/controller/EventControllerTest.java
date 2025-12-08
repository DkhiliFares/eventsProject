package tn.fst.eventsproject.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import tn.fst.eventsproject.controllers.EventRestController;
import tn.fst.eventsproject.entities.Participant;
import tn.fst.eventsproject.services.IEventServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventRestController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IEventServices eventServices;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAddParticipant() throws Exception {
        Participant participant = new Participant();
        participant.setIdPart(1);
        participant.setNom("Test Name");
        participant.setPrenom("Test Surname");

        when(eventServices.addParticipant(any(Participant.class))).thenReturn(participant);

        mockMvc.perform(post("/event/addPart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(participant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Test Name"))
                .andExpect(jsonPath("$.prenom").value("Test Surname"));
    }
}
