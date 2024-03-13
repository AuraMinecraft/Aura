package net.aniby.aura.controller;

import net.aniby.aura.service.YooMoneyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/yoomoney")
public class YooMoneyController {
    @Autowired
    YooMoneyService yooMoneyService;

    @PostMapping(path = "/payment_notifications", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public void paymentNotifications(@RequestParam Map<String, String> body) throws IOException, IllegalAccessException {
        yooMoneyService.processNotification(body);
    }
}
