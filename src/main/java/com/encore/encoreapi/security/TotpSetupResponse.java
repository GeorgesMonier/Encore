package com.encore.encoreapi.security;

public class TotpSetupResponse {

    private String qrCodeImage;
    private String secret;

    public TotpSetupResponse(String qrCodeImage, String secret) {
        this.qrCodeImage = qrCodeImage;
        this.secret = secret;
    }

    public String getQrCodeImage() { return qrCodeImage; }
    public String getSecret() { return secret; }
}