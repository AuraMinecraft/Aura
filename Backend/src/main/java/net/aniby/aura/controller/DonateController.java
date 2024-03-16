package net.aniby.aura.controller;

import jakarta.servlet.http.HttpServletResponse;
import net.aniby.aura.service.donate.DonateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/donate")
public class DonateController {
    @Autowired
    DonateService donateService;

    @GetMapping(path = "/")
    public void twitch(HttpServletResponse response, @RequestParam String method, @RequestParam String discord, @RequestParam double amount) throws IOException {
        donateService.processRest(response, method, discord, amount);
    }
}
