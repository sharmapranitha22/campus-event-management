package com.campus.eventsystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String venue;
    private LocalDateTime eventDateTime;
    private LocalDateTime registrationDeadline;
    private int maxParticipants;

    // Team settings (for events like hackathons)
    private boolean teamEvent;
    private int minTeamSize;
    private int maxTeamSize;

    @ManyToOne
    private User organizer;

    // Not stored in the database; filled in by the controller for display
    @Transient
    private int registeredCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public LocalDateTime getEventDateTime() { return eventDateTime; }
    public void setEventDateTime(LocalDateTime eventDateTime) { this.eventDateTime = eventDateTime; }

    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public boolean isTeamEvent() { return teamEvent; }
    public void setTeamEvent(boolean teamEvent) { this.teamEvent = teamEvent; }

    public int getMinTeamSize() { return minTeamSize; }
    public void setMinTeamSize(int minTeamSize) { this.minTeamSize = minTeamSize; }

    public int getMaxTeamSize() { return maxTeamSize; }
    public void setMaxTeamSize(int maxTeamSize) { this.maxTeamSize = maxTeamSize; }

    public User getOrganizer() { return organizer; }
    public void setOrganizer(User organizer) { this.organizer = organizer; }

    public int getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(int registeredCount) { this.registeredCount = registeredCount; }

    public boolean isRegistrationOpen() {
        // No deadline set means registration never closes
        return registrationDeadline == null || LocalDateTime.now().isBefore(registrationDeadline);
    }

    public boolean isFull() {
        return registeredCount >= maxParticipants;
    }
}