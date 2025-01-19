package com.msal.controller;

import com.msal.model.UserProfile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import javax.servlet.http.HttpSession;

@Controller
public class HomeController {

    @GetMapping(value = {"/home", "/"})
    public String home(Model model, HttpSession session) {
        UserProfile userProfile = (UserProfile) session.getAttribute("userInfo");
        System.out.println("Home Controller - User Profile: " + userProfile); // Debug log

        if (userProfile == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("userProfile", userProfile);
        return "home";
    }

    @GetMapping(value = {"/index"})
    public String index(Model model, HttpSession session) {
        return "index";
    }


}
