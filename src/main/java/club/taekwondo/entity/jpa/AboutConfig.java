package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "about_config")
public class AboutConfig {

    @Id
    private Long id = 1L;

    private String headingLine1;
    private String headingLine2;
    private String leadText;
    private String descriptionText;
    private String imagePath;
    private String foundedYear;
    private String badgeLabel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "about_chips", joinColumns = @JoinColumn(name = "about_config_id"))
    @Column(name = "chip")
    @OrderColumn(name = "chip_order")
    private List<String> chips = new ArrayList<>();

    private String missionTitle;
    private String missionText;
    private String visionTitle;
    private String visionText;
    private String valuesTitle;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "about_values", joinColumns = @JoinColumn(name = "about_config_id"))
    @OrderColumn(name = "value_order")
    private List<AboutValue> values = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHeadingLine1() { return headingLine1; }
    public void setHeadingLine1(String headingLine1) { this.headingLine1 = headingLine1; }
    public String getHeadingLine2() { return headingLine2; }
    public void setHeadingLine2(String headingLine2) { this.headingLine2 = headingLine2; }
    public String getLeadText() { return leadText; }
    public void setLeadText(String leadText) { this.leadText = leadText; }
    public String getDescriptionText() { return descriptionText; }
    public void setDescriptionText(String descriptionText) { this.descriptionText = descriptionText; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getFoundedYear() { return foundedYear; }
    public void setFoundedYear(String foundedYear) { this.foundedYear = foundedYear; }
    public String getBadgeLabel() { return badgeLabel; }
    public void setBadgeLabel(String badgeLabel) { this.badgeLabel = badgeLabel; }
    public List<String> getChips() { return chips; }
    public void setChips(List<String> chips) { this.chips = chips; }
    public String getMissionTitle() { return missionTitle; }
    public void setMissionTitle(String missionTitle) { this.missionTitle = missionTitle; }
    public String getMissionText() { return missionText; }
    public void setMissionText(String missionText) { this.missionText = missionText; }
    public String getVisionTitle() { return visionTitle; }
    public void setVisionTitle(String visionTitle) { this.visionTitle = visionTitle; }
    public String getVisionText() { return visionText; }
    public void setVisionText(String visionText) { this.visionText = visionText; }
    public String getValuesTitle() { return valuesTitle; }
    public void setValuesTitle(String valuesTitle) { this.valuesTitle = valuesTitle; }
    public List<AboutValue> getValues() { return values; }
    public void setValues(List<AboutValue> values) { this.values = values; }
}
