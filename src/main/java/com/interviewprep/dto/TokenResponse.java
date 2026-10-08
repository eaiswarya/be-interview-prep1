package com.interviewprep.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
