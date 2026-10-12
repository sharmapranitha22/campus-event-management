package com.campus.eventsystem.repository;

import com.campus.eventsystem.model.Event;
import com.campus.eventsystem.model.Registration;
import com.campus.eventsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByStudent(User student);

    List<Registration> findByEvent(Event event);

    int countByEvent(Event event);

    boolean existsByEventAndStudent(Event event, User student);

}