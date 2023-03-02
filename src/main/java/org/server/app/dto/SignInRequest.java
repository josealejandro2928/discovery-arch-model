package org.server.app.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class SignInRequest {
    @NonNull
    private String email;
    @NonNull
    private String name;
    @NonNull
    private String lastName;
    @NonNull
    private String password;
    @NonNull
    private String confirmPassword;

    public SignInRequest(){

    }
}
