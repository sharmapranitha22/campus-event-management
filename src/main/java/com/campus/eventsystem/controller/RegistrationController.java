package com.campus.eventsystem.controller;

import com.campus.eventsystem.model.Event;
import com.campus.eventsystem.model.Registration;
import com.campus.eventsystem.model.TeamMember;
import com.campus.eventsystem.model.User;
import com.campus.eventsystem.repository.EventRepository;
import com.campus.eventsystem.repository.RegistrationRepository;
import com.campus.eventsystem.repository.TeamMemberRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class RegistrationController {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final TeamMemberRepository teamMemberRepository;

    public RegistrationController(RegistrationRepository registrationRepository,
                                  EventRepository eventRepository,
                                  TeamMemberRepository teamMemberRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    // ---------- Solo registration ----------
    @PostMapping("/events/{id}/register")
    public String registerForEvent(@PathVariable Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!"STUDENT".equals(currentUser.getRole())) {
            return "redirect:/events";
        }

        Event event = eventRepository.findById(id).orElseThrow();

        if (event.isTeamEvent()) {
            return "redirect:/events/" + id + "/register-team";
        }
        if (!event.isRegistrationOpen()) {
            redirectAttributes.addFlashAttribute("message", "Registration deadline is over for this event.");
            return "redirect:/events";
        }
        if (registrationRepository.existsByEventAndStudent(event, currentUser)) {
            redirectAttributes.addFlashAttribute("message", "You are already registered for this event.");
            return "redirect:/events";
        }
        if (registrationRepository.countByEvent(event) >= event.getMaxParticipants()) {
            redirectAttributes.addFlashAttribute("message", "Sorry, this event is full.");
            return "redirect:/events";
        }

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setStudent(currentUser);
        registration.setRegisteredAt(LocalDateTime.now());
        registration.setAttended(false);
        registrationRepository.save(registration);

        redirectAttributes.addFlashAttribute("message", "Registered successfully!");
        return "redirect:/events";
    }

    // ---------- Team registration ----------
    @GetMapping("/events/{id}/register-team")
    public String showTeamForm(@PathVariable Long id, Model model, HttpSession session) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!"STUDENT".equals(currentUser.getRole())) {
            return "redirect:/events";
        }

        Event event = eventRepository.findById(id).orElseThrow();
        if (!event.isTeamEvent()) {
            return "redirect:/events";
        }

        model.addAttribute("event", event);
        return "register-team";
    }

    @PostMapping("/events/{id}/register-team")
    public String registerTeam(@PathVariable Long id,
                               @RequestParam String teamName,
                               @RequestParam(required = false) List<String> memberNames,
                               @RequestParam(required = false) List<String> memberEmails,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!"STUDENT".equals(currentUser.getRole())) {
            return "redirect:/events";
        }

        Event event = eventRepository.findById(id).orElseThrow();
        if (!event.isTeamEvent()) {
            return "redirect:/events";
        }

        if (!event.isRegistrationOpen()) {
            redirectAttributes.addFlashAttribute("message", "Registration deadline is over for this event.");
            return "redirect:/events";
        }
        if (registrationRepository.existsByEventAndStudent(event, currentUser)) {
            redirectAttributes.addFlashAttribute("message", "You have already registered a team for this event.");
            return "redirect:/events";
        }
        if (registrationRepository.countByEvent(event) >= event.getMaxParticipants()) {
            redirectAttributes.addFlashAttribute("message", "Sorry, this event is full.");
            return "redirect:/events";
        }

        // Collect only the filled-in member rows
        List<TeamMember> members = new ArrayList<>();
        if (memberNames != null) {
            for (int i = 0; i < memberNames.size(); i++) {
                String name = memberNames.get(i);
                if (name != null && !name.trim().isEmpty()) {
                    TeamMember m = new TeamMember();
                    m.setName(name.trim());
                    if (memberEmails != null && i < memberEmails.size()) {
                        m.setEmail(memberEmails.get(i).trim());
                    }
                    members.add(m);
                }
            }
        }

        if (members.size() < event.getMinTeamSize() || members.size() > event.getMaxTeamSize()) {
            model.addAttribute("event", event);
            model.addAttribute("error", "Team must have between " + event.getMinTeamSize()
                    + " and " + event.getMaxTeamSize() + " members.");
            return "register-team";
        }

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setStudent(currentUser);
        registration.setRegisteredAt(LocalDateTime.now());
        registration.setTeamName(teamName.trim());
        registrationRepository.save(registration);

        for (TeamMember m : members) {
            m.setRegistration(registration);
            teamMemberRepository.save(m);
        }

        redirectAttributes.addFlashAttribute("message", "Team registered successfully!");
        return "redirect:/events";
    }

    // ---------- My registrations ----------
    @GetMapping("/my-events")
    public String myEvents(Model model, HttpSession session) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<Registration> myRegistrations = registrationRepository.findByStudent(currentUser);
        model.addAttribute("registrations", myRegistrations);
        return "my-events";
    }

    // ---------- Attendance: solo registration ----------
    @PostMapping("/registrations/{id}/mark-attended")
    public String markAttended(@PathVariable Long id, HttpSession session) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null || !"ORGANIZER".equals(currentUser.getRole())) {
            return "redirect:/events";
        }

        Registration registration = registrationRepository.findById(id).orElseThrow();
        Event event = registration.getEvent();

        if (event.getOrganizer() == null
                || !event.getOrganizer().getId().equals(currentUser.getId())) {
            return "redirect:/events";
        }

        registration.setAttended(true);
        registrationRepository.save(registration);

        return "redirect:/events/" + event.getId() + "/participants";
    }

    // ---------- Attendance: one team member ----------
    @PostMapping("/members/{id}/mark-attended")
    public String markMemberAttended(@PathVariable Long id, HttpSession session) {

        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null || !"ORGANIZER".equals(currentUser.getRole())) {
            return "redirect:/events";
        }

        TeamMember member = teamMemberRepository.findById(id).orElseThrow();
        Event event = member.getRegistration().getEvent();

        if (event.getOrganizer() == null
                || !event.getOrganizer().getId().equals(currentUser.getId())) {
            return "redirect:/events";
        }

        member.setAttended(true);
        teamMemberRepository.save(member);

        return "redirect:/events/" + event.getId() + "/participants";
    }

}