package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.dto.UpdateThemeDto;
import com.focalizze.Focalizze.dto.UserDto;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.FollowRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;

    @Value("${app.default-avatar-url}") // Inyecta el valor desde application.properties
    private String defaultAvatarUrl;

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

        boolean isFollowing = false;
        boolean isBlocked = false;

        if (currentUser != null && !currentUser.getId().equals(profileUser.getId())) {
            isFollowing = followRepository.existsByUserFollowerAndUserFollowed(currentUser, profileUser);
            isBlocked = blockRepository.existsByBlockerAndBlocked(currentUser, profileUser);
        }

        // 3. Construimos y devolvemos el DTO.
        return new UserDto(
                profileUser.getId(),
                profileUser.getUsername(),
                profileUser.getDisplayName(),
                profileUser.getAvatarUrl(defaultAvatarUrl),
                profileUser.getCalculatedThreadCount(),
                isFollowing,
                profileUser.getFollowingCount(),
                profileUser.getFollowersCount(),
                isBlocked,
                profileUser.getRole().name(),
                profileUser.isTwoFactorEnabled(),
                profileUser.getBackgroundType(),
                profileUser.getBackgroundValue()
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
                user.getAvatarUrl(defaultAvatarUrl),
                user.getCalculatedThreadCount(), false,
                user.getFollowingCount(),
                user.getFollowersCount(),
                false,
                user.getRole().name(),
                user.isTwoFactorEnabled(),
                user.getBackgroundType(),
                user.getBackgroundValue()
        );
    }

    @Override
    public void updateThemePreferences(String username, UpdateThemeDto dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setBackgroundType(dto.backgroundType());
        user.setBackgroundValue(dto.backgroundValue());

        userRepository.save(user);
    }

}
