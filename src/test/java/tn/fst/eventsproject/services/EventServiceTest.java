package tn.fst.eventsproject.services;

import tn.fst.eventsproject.entities.Event;
import tn.fst.eventsproject.repositories.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void testAddEvent() {
        Event event = new Event();
        event.setName("Test Event");

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        Event result = eventService.addEvent(event);

        assertNotNull(result);
        assertEquals("Test Event", result.getName());
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void testFindEventById() {
        Event event = new Event();
        event.setId(1L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        Event result = eventService.findEventById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
}
