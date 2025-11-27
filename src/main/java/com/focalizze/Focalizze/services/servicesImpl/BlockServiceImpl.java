package com.focalizze.Focalizze.services.servicesImpl;

import com.focalizze.Focalizze.models.Block;
import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.BlockRepository;
import com.focalizze.Focalizze.repository.UserRepository;
import com.focalizze.Focalizze.services.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class BlockServiceImpl implements BlockService {

    private final UserRepository userRepository;
    private final BlockRepository blockRepository;

    @Override
    public boolean toggleBlock(String usernameToToggle) {
        // 1. Obtener al usuario actual (el que está bloqueando)
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        // 2. Obtener al usuario objetivo (el que será bloqueado)
        User userToToggle = userRepository.findByUsername(usernameToToggle)
                .orElseThrow(() -> new RuntimeException("Usuario a bloquear no encontrado: " + usernameToToggle));

        // No se puede bloquear a uno mismo
        if (currentUser.equals(userToToggle)) {
            throw new IllegalArgumentException("No puedes bloquearte a ti mismo.");
        }

        // 3. Comprobar si ya existe una relación de bloqueo
        Optional<Block> existingBlock = blockRepository.findByBlockerAndBlocked(currentUser, userToToggle);

        if (existingBlock.isPresent()) {
            // Si ya está bloqueado, lo desbloqueamos (eliminando la fila)
            blockRepository.delete(existingBlock.get());
            return false;
        } else {
            // Si no está bloqueado, creamos la relación de bloqueo
            Block newBlock = Block.builder()
                    .blocker(currentUser)
                    .blocked(userToToggle)
                    .createdAt(LocalDateTime.now())
                    .build();
            blockRepository.save(newBlock);
            return true;
        }
    }

}
