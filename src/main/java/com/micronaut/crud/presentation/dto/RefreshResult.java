package com.micronaut.crud.presentation.dto;

import java.util.UUID;

public class RefreshResult {
    private final UUID userID;
    private final  String newRefreshToken;

    public RefreshResult(UUID userId, String newRefreshToken){
        this.userID = userId;
        this.newRefreshToken  = newRefreshToken;
    }

    public UUID getUserID() {return userID;}

    public String getNewRefreshToken() {
        return newRefreshToken;
    }
}
