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

@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(of = {"id", "username", "email"})
@Entity
@Table(name="user_tbl")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // CAMPO 1: Para la verificación en 2 pasos
    @Column(nullable = false)
    @Builder.Default
    private boolean isTwoFactorEnabled = false;

    // CAMPO 2: Para invalidar tokens (Logout masivo)
    @Column(nullable = false)
    @Builder.Default
    private Integer tokenVersion = 0;

    private String twoFactorCode; // El código de 6 dígitos

    private LocalDateTime twoFactorCodeExpiry; // Cuándo vence el código

    @Formula("(SELECT count(*) FROM thread_tbl t WHERE t.user_id = id AND t.is_published = true AND t.is_deleted = false)")
    private Integer calculatedThreadCount;

    /**
     * Este método devuelve la URL del avatar del usuario.
     * Si el usuario no ha subido un avatar (avatarUrl es null),
     * devuelve la URL del avatar por defecto definida en la configuración.
     * @param defaultUrl La URL por defecto inyectada desde application.properties.
     * @return Una URL de avatar válida.
     */
    public String getAvatarUrl(String defaultUrl) {
        if (this.avatarUrl == null || this.avatarUrl.isBlank()) {
            return defaultUrl;
        }
        return this.avatarUrl;
    }

    private Integer dailyThreadsRemaining;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private Integer followingCount;

    private Integer followersCount;

    private Integer dailyInteractionsRemaining;

    private LocalDateTime createdAt;

    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;

    @OneToMany(mappedBy="user")
    private List<SavedThreads> savedThreads;

    @OneToMany(mappedBy="user")
    private List<ThreadClass> threads;

    @OneToMany(mappedBy="user")
    private List<NotificationClass> notifications;

    @OneToMany(mappedBy="triggerUser")
    private List<NotificationClass> notificationsTrigger;

    @OneToMany(mappedBy="user")
    private List<CommentClass> comments;

    @OneToMany(mappedBy="user")
    private List<Like> likes;

    @OneToMany(mappedBy = "userReporter")
    private List<Report> reportsMade;

    @OneToMany(mappedBy = "userReported")
    private List<Report> reportsReceived;

    @OneToMany(mappedBy = "user")
    private List<InteractionLog> interactions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CategoryFollow> followedCategories;

    @OneToMany(mappedBy="userFollower")
    private List<Follow> following;

    @OneToMany(mappedBy="userFollowed")
    private List<Follow> followers;

    @OneToMany(mappedBy = "mentionedUser")
    private List<Mention> mentions;

    @OneToMany(mappedBy="blocker")
    private List<Block> blockedUser;

    @OneToMany(mappedBy="blocked")
    private List<Block> blockerUser;

    public User(){
        this.savedThreads = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
        this.likes = new ArrayList<>();
        this.threads = new ArrayList<>();
        this.notifications = new ArrayList<>();
        this.reportsMade = new ArrayList<>();
        this.reportsReceived = new ArrayList<>();
        this.interactions = new ArrayList<>();
        this.followedCategories = new HashSet<>();
        this.mentions = new ArrayList<>();
        this.blockedUser = new ArrayList<>();
        this.blockerUser = new ArrayList<>();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(this.getRole().name()));
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public User(Long id, String username, String email, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", displayName='" + displayName + '\'' +
                ", biography='" + biography + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", dailyThreadsRemaining=" + dailyThreadsRemaining +
                ", role=" + role +
                ", followingCount=" + followingCount +
                ", followersCount=" + followersCount +
                ", dailyInteractionsRemaining=" + dailyInteractionsRemaining +
                ", createdAt=" + createdAt +
                ", savedThreads=" + savedThreads +
                ", threads=" + threads +
                ", notifications=" + notifications +
                ", comments=" + comments +
                ", likes=" + likes +
                ", reportsMade=" + reportsMade +
                ", reportsReceived=" + reportsReceived +
                ", following=" + following +
                ", followers=" + followers +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        // Usa una constante para asegurar que el hash de objetos no persistidos no sea 0.
        return getClass().hashCode();
    }
}
