package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "revoked_tokens", indexes = {
    @Index(name = "idx_revoked_token_key", columnList = "token_key"),
    @Index(name = "idx_revoked_token_expiry", columnList = "expires_at")
})
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_key", nullable = false, unique = true, length = 512)
    private String tokenKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RevokedToken() {}

    public RevokedToken(String tokenKey, Instant expiresAt) {
        this.tokenKey = tokenKey;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getTokenKey() { return tokenKey; }
    public void setTokenKey(String tokenKey) { this.tokenKey = tokenKey; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
