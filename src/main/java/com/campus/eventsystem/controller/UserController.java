package com.campus.eventsystem.controller;

import com.campus.eventsystem.model.User;
import com.campus.eventsystem.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String role,
                               Model model) {

        String cleanEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(cleanEmail)) {
            model.addAttribute("error", "An account with this email already exists. Please log in instead.");
            return "register";
        }

        if (!"STUDENT".equals(role) && !"ORGANIZER".equals(role)) {
            model.addAttribute("error", "Invalid role selected.");
            return "register";
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        userRepository.save(user);

        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            HttpSession session,
                            Model model) {

        Optional<User> foundUser = userRepository.findByEmail(email.trim().toLowerCase());

        if (foundUser.isEmpty()
                || !passwordEncoder.matches(password, foundUser.get().getPassword())) {
            model.addAttribute("error", "Invalid email or password.");
            return "login";
        }

        session.setAttribute("loggedInUser", foundUser.get());

        return "redirect:/events";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/events";
    }

}