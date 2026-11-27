package com.example.sampleapi.user;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        User user = new User(request.name().strip(), email, request.age());
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse get(Long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = findUser(id);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }
        user.update(request.name().strip(), email, request.age());
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(findUser(id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
