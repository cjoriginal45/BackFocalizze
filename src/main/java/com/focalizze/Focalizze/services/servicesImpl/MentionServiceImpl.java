package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.Mention;
import com.focalizze.Focalizze.models.NotificationType;
import com.focalizze.Focalizze.models.Post;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
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

/**
 * Implementation of the {@link MentionService} interface.
 * Parses and processes user mentions (@username) in posts.
 * <p>
 * Implementación de la interfaz {@link MentionService}.
 * Analiza y procesa menciones de usuario (@username) en publicaciones.
 */
@Service
@RequiredArgsConstructor
public class MentionServiceImpl implements MentionService {

    private final UserRepository userRepository;
    private final MentionRepository mentionRepository;
    private final NotificationService notificationService;
    private final BlockRepository blockRepository;

    // Compiled regex pattern for efficiency / Patrón regex compilado para eficiencia
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    /**
     * Extracts mentions from a post, verifies users, checks blocks, and creates notifications.
     * <p>
     * Extrae menciones de un post, verifica usuarios, comprueba bloqueos y crea notificaciones.
     *
     * @param post   The post containing content.
     *               El post que contiene el contenido.
     * @param author The author of the post.
     *               El autor del post.
     */
    @Override
    @Transactional
    public void processMentions(Post post, User author) {
        // 1. Extract usernames from content / Extraer nombres de usuario del contenido
        Set<String> mentionedUsernames = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(post.getContent());
        while (matcher.find()) {
            mentionedUsernames.add(matcher.group(1));
        }

        if (mentionedUsernames.isEmpty()) {
            return;
        }

        // 2. Batch fetch valid users / Obtener usuarios válidos en lote
        List<User> mentionedUsers = userRepository.findAllByUsernameIn(mentionedUsernames);

        // 3. Process each user / Procesar cada usuario
        for (User mentionedUser : mentionedUsers) {
            // Prevent self-mention / Prevenir auto-mención
            if (mentionedUser.getId().equals(author.getId())) {
                continue;
            }

            // Check block status (bidirectional) / Comprobar estado de bloqueo (bidireccional)
            boolean isBlocked = blockRepository.existsByBlockerAndBlocked(author, mentionedUser) ||
                    blockRepository.existsByBlockerAndBlocked(mentionedUser, author);

            if (isBlocked) {
                continue; // Skip if blocked / Saltar si está bloqueado
            }

            // a) Save Mention entity / Guardar entidad Mención
            Mention mention = Mention.builder()
                    .mentionedUser(mentionedUser)
                    .post(post)
                    .build();
            mentionRepository.save(mention);

            // b) Trigger Notification / Disparar Notificación
            notificationService.createAndSendNotification(
                    mentionedUser,
                    NotificationType.MENTION,
                    author,
                    post.getThread()
            );
        }
    }
}
