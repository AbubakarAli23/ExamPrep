package com.examprep.user.service;

import com.examprep.user.domain.UserAccount;
import com.examprep.user.dto.RegisterUserRequest;

public interface UserRegistrationService {

    UserAccount register(RegisterUserRequest request);
}
