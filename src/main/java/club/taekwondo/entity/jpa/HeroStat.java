package club.taekwondo.entity.jpa;

import jakarta.persistence.Embeddable;

@Embeddable
public class HeroStat {

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
