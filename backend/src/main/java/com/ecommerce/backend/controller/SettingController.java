package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.SettingService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/delivery-fee")
    public BigDecimal getDeliveryFee() {
        return settingService.getDeliveryFee();
    }

    @PutMapping("/delivery-fee")
    public BigDecimal updateDeliveryFee(@RequestBody BigDecimal fee) {
        return settingService.updateDeliveryFee(fee);
    }
}
