package club.taekwondo.dto;

import java.util.List;

public class AboutConfigDto {

    private String headingLine1;
    private String headingLine2;
    private String leadText;
    private String descriptionText;
    private String imagePath;
    private String foundedYear;
    private String badgeLabel;
    private List<String> chips;
    private String missionTitle;
    private String missionText;
    private String visionTitle;
    private String visionText;
    private String valuesTitle;
    private List<AboutValueDto> values;

    public String getHeadingLine1() { return headingLine1; }
    public void setHeadingLine1(String v) { this.headingLine1 = v; }
    public String getHeadingLine2() { return headingLine2; }
    public void setHeadingLine2(String v) { this.headingLine2 = v; }
    public String getLeadText() { return leadText; }
    public void setLeadText(String v) { this.leadText = v; }
    public String getDescriptionText() { return descriptionText; }
    public void setDescriptionText(String v) { this.descriptionText = v; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String v) { this.imagePath = v; }
    public String getFoundedYear() { return foundedYear; }
    public void setFoundedYear(String v) { this.foundedYear = v; }
    public String getBadgeLabel() { return badgeLabel; }
    public void setBadgeLabel(String v) { this.badgeLabel = v; }
    public List<String> getChips() { return chips; }
    public void setChips(List<String> v) { this.chips = v; }
    public String getMissionTitle() { return missionTitle; }
    public void setMissionTitle(String v) { this.missionTitle = v; }
    public String getMissionText() { return missionText; }
    public void setMissionText(String v) { this.missionText = v; }
    public String getVisionTitle() { return visionTitle; }
    public void setVisionTitle(String v) { this.visionTitle = v; }
    public String getVisionText() { return visionText; }
    public void setVisionText(String v) { this.visionText = v; }
    public String getValuesTitle() { return valuesTitle; }
    public void setValuesTitle(String v) { this.valuesTitle = v; }
    public List<AboutValueDto> getValues() { return values; }
    public void setValues(List<AboutValueDto> v) { this.values = v; }

    public static class AboutValueDto {
        private String bold;
        private String description;
        public String getBold() { return bold; }
        public void setBold(String v) { this.bold = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { this.description = v; }
    }
}
