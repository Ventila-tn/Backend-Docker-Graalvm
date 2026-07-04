package com.ecommerce.backend.service;

import com.ecommerce.backend.entity.Setting;
import com.ecommerce.backend.repository.SettingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SettingService {

    private static final String DELIVERY_FEE_KEY = "delivery_fee";
    private static final BigDecimal DEFAULT_DELIVERY_FEE = new BigDecimal("8.000");

    private final SettingRepository settingRepository;

    public SettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public BigDecimal getDeliveryFee() {
        return settingRepository.findById(DELIVERY_FEE_KEY)
                .map(s -> new BigDecimal(s.getValue()))
                .orElse(DEFAULT_DELIVERY_FEE);
    }

    public BigDecimal updateDeliveryFee(BigDecimal fee) {
        Setting setting = settingRepository.findById(DELIVERY_FEE_KEY)
                .orElse(new Setting(DELIVERY_FEE_KEY, fee.toPlainString()));
        setting.setValue(fee.toPlainString());
        settingRepository.save(setting);
        return fee;
    }
}
