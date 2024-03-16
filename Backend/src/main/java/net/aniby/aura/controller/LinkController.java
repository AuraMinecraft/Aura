package net.aniby.aura.controller;

import jakarta.servlet.http.HttpServletResponse;
import net.aniby.aura.service.rest.LinkRest;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/link")
public class LinkController {
    @Autowired
    LinkRest linkRest;

    @GetMapping(path = "/auth")
    public void auth(HttpServletResponse response, @RequestParam String code, @RequestParam String id) throws IOException {
        linkRest.auth(response, code, id);
    }

    @GetMapping(path = "/twitch")
    public void twitch(HttpServletResponse response, @RequestParam String code, @RequestParam String scope, @RequestParam String state)
            throws IOException, URISyntaxException, ParseException, InterruptedException {
        linkRest.twitch(response, code, state);
    }
}
