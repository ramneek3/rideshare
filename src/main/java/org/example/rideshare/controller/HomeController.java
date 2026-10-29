package org.example.rideshare.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class HomeController {

    // The API has no UI of its own; send browsers to the interactive docs
    @GetMapping("/")
    public String home() {
        return "redirect:/swagger-ui.html";
    }
}
