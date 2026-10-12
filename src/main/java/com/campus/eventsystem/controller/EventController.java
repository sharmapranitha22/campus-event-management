package com.campus.eventsystem.controller;

import com.campus.eventsystem.model.Event;
import com.campus.eventsystem.model.Registration;
import com.campus.eventsystem.model.User;
import com.campus.eventsystem.repository.EventRepository;
import com.campus.eventsystem.repository.RegistrationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class EventController {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public EventController(EventRepository eventRepository,
                           RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    @GetMapping("/events")
    public String listEvents(Model model, HttpSession session) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        model.addAttribute("currentUser", currentUser);

        List<Event> events;

        if (currentUser != null && "ORGANIZER".equals(currentUser.getRole())) {
            events = eventRepository.findByOrganizer(currentUser);
        } else {
            events = eventRepository.findAll();
        }

        for (Event e : events) {
            e.setRegisteredCount(registrationRepository.countByEvent(e));
        }

        model.addAttribute("events", events);

        return "events";
    }

    @GetMapping("/events/new")
    public String showCreateForm(HttpSession session) {
        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null || !"ORGANIZER".equals(currentUser.getRole())) {
            return "redirect:/events";
        }
        return "create-event";
    }

    @PostMapping("/events/create")
    public String createEvent(@RequestParam String title,
                              @RequestParam String description,
                              @RequestParam String venue,
                              @RequestParam int maxParticipants,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime eventDateTime,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime registrationDeadline,
                              @RequestParam(required = false) String teamEvent,
                              @RequestParam(required = false, defaultValue = "1") int minTeamSize,
                              @RequestParam(required = false, defaultValue = "1") int maxTeamSize,
                              HttpSession session) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null || !"ORGANIZER".equals(currentUser.getRole())) {
            return "redirect:/events";
        }

        boolean isTeam = teamEvent != null;

        if (isTeam && (minTeamSize < 1 || maxTeamSize < minTeamSize)) {
            return "redirect:/events/new";
        }

        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setVenue(venue);
        event.setMaxParticipants(maxParticipants);
        event.setEventDateTime(eventDateTime);              // may be null
        event.setRegistrationDeadline(registrationDeadline); // may be null
        event.setTeamEvent(isTeam);
        event.setMinTeamSize(isTeam ? minTeamSize : 1);
        event.setMaxTeamSize(isTeam ? maxTeamSize : 1);
        event.setOrganizer(currentUser);

        eventRepository.save(event);

        return "redirect:/events";
    }

    @GetMapping("/events/{id}/participants")
    public String viewParticipants(@PathVariable Long id, Model model, HttpSession session) {

        User currentUser = (User) session.getAttribute("loggedInUser");

        if (currentUser == null) {
            return "redirect:/login";
        }

        Event event = eventRepository.findById(id).orElseThrow();

        if (event.getOrganizer() == null
                || !event.getOrganizer().getId().equals(currentUser.getId())) {
            return "redirect:/events";
        }

        List<Registration> participants = registrationRepository.findByEvent(event);

        model.addAttribute("event", event);
        model.addAttribute("participants", participants);
        model.addAttribute("currentUser", currentUser);

        return "participants";
    }

}