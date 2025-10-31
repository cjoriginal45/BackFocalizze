package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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

    private Integer dailyThreadsRemaining;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private Integer followingCount;

    private Integer followersCount;

    private Integer dailyInteractionsRemaining;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy="user")
    private List<SavedThreads> savedThreads;

    @OneToMany(mappedBy="user")
    private List<ThreadClass> threads;

    @OneToMany(mappedBy="user")
    private List<NotificationClass> notifications;

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

    @ManyToMany
    @JoinTable(
            name = "categories_users",
            joinColumns = @JoinColumn(name = "user_id"),           // FK a User
            inverseJoinColumns = @JoinColumn(name = "category_id") // FK a Category
    )
    private Set<CategoryClass> categories;

    @OneToMany(mappedBy="userFollower")
    private List<Follow> following;

    @OneToMany(mappedBy="userFollowed")
    private List<Follow> followers;

    public User(){
        this.savedThreads = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.categories = new HashSet<>();
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
        this.likes = new ArrayList<>();
        this.threads = new ArrayList<>();
        this.notifications = new ArrayList<>();
        this.reportsMade = new ArrayList<>();
        this.reportsReceived = new ArrayList<>();
        this.interactions = new ArrayList<>();
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
                ", categories=" + categories +
                ", following=" + following +
                ", followers=" + followers +
                '}';
    }
}
