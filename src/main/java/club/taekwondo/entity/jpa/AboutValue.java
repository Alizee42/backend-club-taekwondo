package club.taekwondo.entity.jpa;

import jakarta.persistence.Embeddable;

@Embeddable
public class AboutValue {
    private String bold;
    private String description;

    public String getBold() { return bold; }
    public void setBold(String bold) { this.bold = bold; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
