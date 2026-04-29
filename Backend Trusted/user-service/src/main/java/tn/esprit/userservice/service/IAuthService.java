package tn.esprit.userservice.service;

import tn.esprit.userservice.dto.AuthResponse;
import tn.esprit.userservice.dto.LoginRequest;
import tn.esprit.userservice.dto.RegisterRequest;
import tn.esprit.userservice.dto.VerifyTwoFactorRequest;

public interface IAuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse verifyTwoFactor(VerifyTwoFactorRequest request);

    AuthResponse refreshToken(String refreshToken);
}