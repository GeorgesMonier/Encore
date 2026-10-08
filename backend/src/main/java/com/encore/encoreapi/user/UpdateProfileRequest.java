package com.encore.encoreapi.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
