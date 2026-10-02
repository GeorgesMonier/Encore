package com.encore.encoreapi.user;

public class LoginResponse {

    private String token;
    private String email;
    private String name;
    private boolean requiresTotp;

    public LoginResponse(String token, String email, String name) {
        this.token = token;
        this.email = email;
        this.name = name;
        this.requiresTotp = false;
    }

    public static LoginResponse requiresTotp() {
        LoginResponse response = new LoginResponse(null, null, null);
        response.requiresTotp = true;
        return response;
    }

    public String getToken() { return token; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public boolean isRequiresTotp() { return requiresTotp; }
}