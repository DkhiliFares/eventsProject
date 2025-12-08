package tn.fst.eventsproject.services;

import tn.fst.eventsproject.entities.Participant;
import tn.fst.eventsproject.repositories.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private EventServicesImpl eventServices;

    @Test
    void testAddParticipant() {
        Participant participant = new Participant();
        participant.setNom("Test Name");
        participant.setPrenom("Test Surname");

        when(participantRepository.save(any(Participant.class))).thenReturn(participant);

        Participant result = eventServices.addParticipant(participant);

        assertNotNull(result);
        assertEquals("Test Name", result.getNom());
        verify(participantRepository, times(1)).save(participant);
    }
}
