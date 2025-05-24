package ma.ensah.plateforme_annotation.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginForm() {
            return "login";
    }

    // pour traitement post est gere par spring security
}


