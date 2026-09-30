package com.fashion.backend.config;

import com.fashion.backend.common.constant.GenderTarget;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter tự động chuyển đổi chuỗi query param (MALE, FEMALE, MEN, WOMEN, UNISEX,...)
 * sang Enum GenderTarget cho Spring Web MVC.
 */
@Component
public class StringToGenderTargetConverter implements Converter<String, GenderTarget> {

    @Override
    public GenderTarget convert(String source) {
        return GenderTarget.fromString(source);
    }
}
