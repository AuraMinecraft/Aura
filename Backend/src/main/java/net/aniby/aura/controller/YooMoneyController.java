package net.aniby.aura.controller;

import net.aniby.aura.service.donate.DonateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/yoomoney")
public class YooMoneyController {
    @Autowired
    DonateService donateService;

    @PostMapping(path = "/payment_notifications", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public void paymentNotifications(@RequestParam Map<String, String> body) throws IOException, IllegalAccessException {
        donateService.processNotification(body);
    }
}
