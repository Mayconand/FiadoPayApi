package edu.ucsal.fiadopayapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class StatusController {
    @GetMapping("/fiadopay/health")
    public Map<String,String> health() {
        return Map.of("status","UP");
    }
}