package com.campus.eventsystem.repository;

import com.campus.eventsystem.model.Event;
import com.campus.eventsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByOrganizer(User organizer);

}