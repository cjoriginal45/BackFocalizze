package com.focalizze.Focalizze.services;

import com.focalizze.Focalizze.dto.RegisterRequest;
import com.focalizze.Focalizze.dto.RegisterResponse;

public interface AuthService {
    RegisterResponse registerUser(RegisterRequest registerRequest);
}
