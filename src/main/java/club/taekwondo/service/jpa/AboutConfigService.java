package club.taekwondo.service.jpa;

import club.taekwondo.dto.AboutConfigDto;
import club.taekwondo.entity.jpa.AboutConfig;
import club.taekwondo.entity.jpa.AboutValue;
import club.taekwondo.repository.jpa.AboutConfigRepository;
import club.taekwondo.service.common.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AboutConfigService {

    @Autowired
    private AboutConfigRepository repo;

    @Autowired
    private FileUploadService fileUploadService;

    public AboutConfigDto get() {
        return toDto(repo.findById(1L).orElseGet(AboutConfig::new));
    }

    public AboutConfigDto update(AboutConfigDto dto) {
        AboutConfig config = repo.findById(1L).orElseGet(() -> {
            AboutConfig c = new AboutConfig(); c.setId(1L); return c;
        });
        applyDto(config, dto);
        return toDto(repo.save(config));
    }

    public AboutConfigDto uploadImage(MultipartFile file) throws IOException {
        String path = fileUploadService.uploadFile(file, "about");
        AboutConfig config = repo.findById(1L).orElseGet(() -> {
            AboutConfig c = new AboutConfig(); c.setId(1L); return c;
        });
        config.setImagePath(path);
        return toDto(repo.save(config));
    }

    private AboutConfigDto toDto(AboutConfig c) {
        AboutConfigDto dto = new AboutConfigDto();
        dto.setHeadingLine1(c.getHeadingLine1());
        dto.setHeadingLine2(c.getHeadingLine2());
        dto.setLeadText(c.getLeadText());
        dto.setDescriptionText(c.getDescriptionText());
        dto.setImagePath(c.getImagePath());
        dto.setFoundedYear(c.getFoundedYear());
        dto.setBadgeLabel(c.getBadgeLabel());
        dto.setChips(c.getChips());
        dto.setMissionTitle(c.getMissionTitle());
        dto.setMissionText(c.getMissionText());
        dto.setVisionTitle(c.getVisionTitle());
        dto.setVisionText(c.getVisionText());
        dto.setValuesTitle(c.getValuesTitle());
        dto.setValues(c.getValues().stream().map(v -> {
            AboutConfigDto.AboutValueDto vd = new AboutConfigDto.AboutValueDto();
            vd.setBold(v.getBold());
            vd.setDescription(v.getDescription());
            return vd;
        }).collect(Collectors.toList()));
        return dto;
    }

    private void applyDto(AboutConfig c, AboutConfigDto dto) {
        if (dto.getHeadingLine1() != null) c.setHeadingLine1(dto.getHeadingLine1());
        if (dto.getHeadingLine2() != null) c.setHeadingLine2(dto.getHeadingLine2());
        if (dto.getLeadText() != null) c.setLeadText(dto.getLeadText());
        if (dto.getDescriptionText() != null) c.setDescriptionText(dto.getDescriptionText());
        if (dto.getImagePath() != null) c.setImagePath(dto.getImagePath());
        if (dto.getFoundedYear() != null) c.setFoundedYear(dto.getFoundedYear());
        if (dto.getBadgeLabel() != null) c.setBadgeLabel(dto.getBadgeLabel());
        if (dto.getChips() != null) {
            c.getChips().clear();
            c.getChips().addAll(dto.getChips());
        }
        if (dto.getMissionTitle() != null) c.setMissionTitle(dto.getMissionTitle());
        if (dto.getMissionText() != null) c.setMissionText(dto.getMissionText());
        if (dto.getVisionTitle() != null) c.setVisionTitle(dto.getVisionTitle());
        if (dto.getVisionText() != null) c.setVisionText(dto.getVisionText());
        if (dto.getValuesTitle() != null) c.setValuesTitle(dto.getValuesTitle());
        if (dto.getValues() != null) {
            List<AboutValue> vals = dto.getValues().stream().map(vd -> {
                AboutValue v = new AboutValue();
                v.setBold(vd.getBold());
                v.setDescription(vd.getDescription());
                return v;
            }).collect(Collectors.toList());
            c.getValues().clear();
            c.getValues().addAll(vals);
        }
    }
}
