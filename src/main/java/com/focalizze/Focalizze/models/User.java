package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Entity representing a system user.
 * Implements Spring Security's UserDetails for authentication.
 * <p>
 * Entidad que representa un usuario del sistema.
 * Implementa UserDetails de Spring Security para la autenticación.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_tbl")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // --- Credentials & Profile / Credenciales y Perfil ---
    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String displayName;

    private String biography;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private LocalDateTime createdAt;


    // --- Security & 2FA / Seguridad y 2FA ---
    @Column(nullable = false)
    @Builder.Default
    private boolean isTwoFactorEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer tokenVersion = 0; // For logout / Para cierre de sesión

    private String twoFactorCode;
    private LocalDateTime twoFactorCodeExpiry;

    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;


    // --- Moderation / Moderación ---
    private boolean isBanned = false;
    private LocalDateTime banExpiresAt;
    private String banReason;
    private LocalDateTime suspensionEndsAt;

    // --- Limits & Counters / Límites y Contadores ---
    private Integer dailyThreadsRemaining;
    private Integer dailyInteractionsRemaining;
    private Integer followingCount;
    private Integer followersCount;
    @Formula("(SELECT count(*) FROM thread_tbl t WHERE t.user_id = id AND t.is_published = true AND t.is_deleted = false)")
    private Integer calculatedThreadCount;

    // --- Theme / Tema ---
    @Column(name = "background_type")
    @Builder.Default
    private String backgroundType = "default";

    @Column(name = "background_value")
    private String backgroundValue;


    // --- Relationships / Relaciones ---
    @OneToMany(mappedBy = "user")
    @Builder.Default
    @ToString.Exclude
    private List<SavedThreads> savedThreads = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    @ToString.Exclude
    private List<ThreadClass> threads = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    @ToString.Exclude
    private List<NotificationClass> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "triggerUser")
    @Builder.Default
    @ToString.Exclude
    private List<NotificationClass> notificationsTrigger = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    @ToString.Exclude
    private List<CommentClass> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    @ToString.Exclude
    private List<Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "userReporter")
    @Builder.Default
    @ToString.Exclude
    private List<Report> reportsMade = new ArrayList<>();

    @OneToMany(mappedBy = "userReported")
    @Builder.Default
    @ToString.Exclude
    private List<Report> reportsReceived = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    @ToString.Exclude
    private List<InteractionLog> interactions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private Set<CategoryFollow> followedCategories = new HashSet<>();

    @OneToMany(mappedBy = "userFollower")
    @Builder.Default
    @ToString.Exclude
    private List<Follow> following = new ArrayList<>();

    @OneToMany(mappedBy = "userFollowed")
    @Builder.Default
    @ToString.Exclude
    private List<Follow> followers = new ArrayList<>();

    @OneToMany(mappedBy = "mentionedUser")
    @Builder.Default
    @ToString.Exclude
    private List<Mention> mentions = new ArrayList<>();

    @OneToMany(mappedBy = "blocker")
    @Builder.Default
    @ToString.Exclude
    private List<Block> blockedUser = new ArrayList<>();

    @OneToMany(mappedBy = "blocked")
    @Builder.Default
    @ToString.Exclude
    private List<Block> blockerUser = new ArrayList<>();


    // --- Helper Methods / Métodos Auxiliares ---
    public String getAvatarUrl(String defaultUrl) {
        return (this.avatarUrl == null || this.avatarUrl.isBlank()) ? defaultUrl : this.avatarUrl;
    }

    public boolean isSuspended() {
        return suspensionEndsAt != null && suspensionEndsAt.isAfter(LocalDateTime.now());
    }


    // --- UserDetails Implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.getRole().name()));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 1. Not banned = Not locked
        if (!this.isBanned) {
            return true;
        }
        // 2. Banned permanently
        if (this.banExpiresAt == null) {
            return false;
        }
        // 3. Banned temporarily: Check if time has passed
        return LocalDateTime.now().isAfter(this.banExpiresAt);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
