package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hero_config")
public class HeroConfig {

    @Id
    private Long id = 1L;

    private String videoPath;
    private String eyebrowText;
    private String identityStrong;
    private String identityMid;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hero_slogans", joinColumns = @JoinColumn(name = "hero_config_id"))
    @Column(name = "slogan")
    @OrderColumn(name = "slogan_order")
    private List<String> slogans = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hero_stats", joinColumns = @JoinColumn(name = "hero_config_id"))
    @OrderColumn(name = "stat_order")
    private List<HeroStat> stats = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
    public String getEyebrowText() { return eyebrowText; }
    public void setEyebrowText(String eyebrowText) { this.eyebrowText = eyebrowText; }
    public String getIdentityStrong() { return identityStrong; }
    public void setIdentityStrong(String identityStrong) { this.identityStrong = identityStrong; }
    public String getIdentityMid() { return identityMid; }
    public void setIdentityMid(String identityMid) { this.identityMid = identityMid; }
    public List<String> getSlogans() { return slogans; }
    public void setSlogans(List<String> slogans) { this.slogans = slogans; }
    public List<HeroStat> getStats() { return stats; }
    public void setStats(List<HeroStat> stats) { this.stats = stats; }
}
