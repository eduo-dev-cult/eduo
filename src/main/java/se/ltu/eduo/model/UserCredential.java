package se.ltu.eduo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_credential", schema = "eduo")
public class UserCredential {
    @Id
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "username", nullable = false, length = 8)
    private String username;

    @Column(name = "password", nullable = false, length = Integer.MAX_VALUE)
    private String password;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;


}