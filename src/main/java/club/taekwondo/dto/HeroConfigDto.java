package club.taekwondo.dto;

import java.util.List;

public class HeroConfigDto {

    private String videoPath;
    private String eyebrowText;
    private String identityStrong;
    private String identityMid;
    private List<String> slogans;
    private List<HeroStatDto> stats;

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
    public List<HeroStatDto> getStats() { return stats; }
    public void setStats(List<HeroStatDto> stats) { this.stats = stats; }

    public static class HeroStatDto {
        private String value;
        private String icon;
        private String label;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}
