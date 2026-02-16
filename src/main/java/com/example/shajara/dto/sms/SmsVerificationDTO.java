package com.example.shajara.dto.sms;

import jakarta.validation.constraints.NotBlank;

public class SmsVerificationDTO {
    @NotBlank(message = "phone required")//bo'shmasligini tekshiriberadi.
    private String phone;
    @NotBlank(message = "code required")//bo'shmasligini tekshiriberadi.
    private String code;

    @Override
    public String toString() { //dtoni log qilgan paytda string ko'rinishida qaytariberadi.
        return "SmsVerificationDTO{" +
                "phone='" + phone + '\'' +
                ", code='" + code + '\'' +
                '}';
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
