package com.examprep.user.service;

import com.examprep.common.exception.DuplicateEmailException;
import com.examprep.user.domain.UserAccount;
import com.examprep.user.dto.RegisterUserRequest;
import com.examprep.user.repository.UserAccountRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService implements UserRegistrationService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserAccount register(RegisterUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("An account already exists for this email address");
        }
        UserAccount account = new UserAccount(
                UUID.randomUUID(),
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()));
        return userAccountRepository.save(account);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
