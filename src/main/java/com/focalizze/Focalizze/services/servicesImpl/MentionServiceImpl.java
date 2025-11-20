package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.Mention;
import com.focalizze.Focalizze.models.NotificationType;
import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.MentionRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.MentionService;
import com.focalizze.Focalizze.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MentionServiceImpl implements MentionService {

    private final UserRepository userRepository;
    private final MentionRepository mentionRepository;
    private final NotificationService notificationService;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    @Override
    @Transactional
    public void processMentions(Post post, User author) {
// 1. Extraemos todos los usernames mencionados del contenido del post.
        Set<String> mentionedUsernames = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(post.getContent());
        while (matcher.find()) {
            mentionedUsernames.add(matcher.group(1));
        }

        if (mentionedUsernames.isEmpty()) {
            return;
        }

        // 2. Buscamos en la base de datos todos los usuarios que coincidan.
        List<User> mentionedUsers = userRepository.findAllByUsernameIn(mentionedUsernames);

        // 3. Para cada usuario encontrado, creamos la mención y la notificación.
        for (User mentionedUser : mentionedUsers) {
            // Evitamos auto-menciones y auto-notificaciones
            if (mentionedUser.getId().equals(author.getId())) {
                continue;
            }

            // a) Crear y guardar la entidad Mention
            Mention mention = Mention.builder()
                    .mentionedUser(mentionedUser)
                    .post(post)
                    .build();
            mentionRepository.save(mention);

            // b) Crear y enviar la notificación
            notificationService.createAndSendNotification(
                    mentionedUser,
                    NotificationType.MENTION,
                    author,
                    post.getThread()
            );
        }
    }
}
