package com.encore.encoreapi.security;

import jakarta.validation.constraints.NotBlank;

public class TotpVerifyRequest {

    @NotBlank
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}