package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Override
    public Optional<User> findUserByUserName(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findUserByUsernameOrEmail(String username, String email) {
        return userRepository.findByUsernameOrEmail(username,email);
    }

    @Override
    public boolean validateEmail(String email) {
        String regexStrict = "^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:(?!-)[A-Za-z0-9-]+(?<!-)\\.)+[A-Za-z]{2,}$";
        Pattern p = Pattern.compile(regexStrict);

        return p.matcher(email).matches();
    }

    @Override
    public boolean UserNameAvailable(String username) {
        return userRepository.findUserNameAvailable(username);
    }


    @Override
    @Transactional(readOnly = true)
    public UserDto getUserProfile(String username, User currentUser) {
        // 1. Buscamos al usuario del perfil.
        User profileUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        // 2. Calculamos si el 'currentUser' sigue al 'profileUser'.
        boolean isFollowing = false;
        if (currentUser != null) {
            isFollowing = followRepository.existsByUserFollowerAndUserFollowed(currentUser, profileUser);
        }

        // 3. Construimos y devolvemos el DTO.
        return new UserDto(
                profileUser.getId(),
                profileUser.getUsername(),
                profileUser.getDisplayName(),
                profileUser.getAvatarUrl(),
                isFollowing
        );
    }

    public UserDto mapToUserDto(User user) {
        if (user == null) {
            return null;
        }
        // Para el endpoint "/me", 'isFollowing' no aplica, así que siempre es false.
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                false
        );
    }
}
