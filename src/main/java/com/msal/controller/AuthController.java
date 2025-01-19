package com.msal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.msal4j.*;
import com.msal.model.UserProfile;
import com.msal.service.MsalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private MsalService msalService;

    @GetMapping("/login")
    public String loginRoot(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        return login(error, logout, model);
    }

    @GetMapping("/auth/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out.");
        }
        return "login";
    }

    @GetMapping("/auth/microsoft")
    public String microsoftLogin() throws Exception {
        String state = MsalService.generateState();
        String nonce = MsalService.generatePkce();

        String authUrl = msalService.getAuthorizationCodeUrl(state, nonce);
        return "redirect:" + authUrl;
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        request.getSession().setAttribute("message", "You have been successfully logged out.");

        return "redirect:/auth/login";
    }

}