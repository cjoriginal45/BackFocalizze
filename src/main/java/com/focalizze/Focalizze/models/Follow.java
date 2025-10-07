package com.focalizze.Focalizze.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Entity
@Table(name = "follow_tbl")
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //private CategoryClass category;  hay que analizar la relacion entre categoria y follow

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name="user_follower_id") //foreign key
    private User userFollower;

    @ManyToOne
    @JoinColumn(name="user_followed_id") //foreign key
    private User userFollowed;
}
