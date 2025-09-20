package ru.redgho7t.telegrambot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.redgho7t.telegrambot.service.HoroscopeService;

@RestController
@RequestMapping("/api/v1/get-horoscope")
public class HoroscopeController {

    private final HoroscopeService horoscopeService;

    @Autowired
    public HoroscopeController(HoroscopeService horoscopeService) {
        this.horoscopeService = horoscopeService;
    }

    @GetMapping("/daily")
    public ResponseEntity<String> getDailyHoroscope(
            @RequestParam(name = "Day", defaultValue = "today") String day,
            @RequestParam(name = "sign") String sign) {
        // day пока игнорируем, всегда today
        String result = horoscopeService.getHoroscope(sign);
        return ResponseEntity.ok(result);
    }
}
