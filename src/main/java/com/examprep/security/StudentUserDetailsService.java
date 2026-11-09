package com.examprep.security;

import com.examprep.user.domain.UserAccount;
import com.examprep.user.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public StudentUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username == null ? "" : username.trim().toLowerCase();
        UserAccount account = userAccountRepository
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for this email address"));
        return StudentPrincipal.from(account);
    }
}
