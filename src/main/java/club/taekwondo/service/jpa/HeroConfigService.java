package club.taekwondo.service.jpa;

import club.taekwondo.dto.HeroConfigDto;
import club.taekwondo.entity.jpa.HeroConfig;
import club.taekwondo.entity.jpa.HeroStat;
import club.taekwondo.repository.jpa.HeroConfigRepository;
import club.taekwondo.service.common.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HeroConfigService {

    @Autowired
    private HeroConfigRepository repo;

    @Autowired
    private FileUploadService fileUploadService;

    public HeroConfigDto get() {
        return toDto(repo.findById(1L).orElseGet(HeroConfig::new));
    }

    public HeroConfigDto update(HeroConfigDto dto) {
        HeroConfig config = repo.findById(1L).orElseGet(() -> {
            HeroConfig c = new HeroConfig();
            c.setId(1L);
            return c;
        });
        applyDto(config, dto);
        return toDto(repo.save(config));
    }

    public HeroConfigDto uploadVideo(MultipartFile file) throws IOException {
        String path = fileUploadService.uploadFile(file, "hero");
        HeroConfig config = repo.findById(1L).orElseGet(() -> {
            HeroConfig c = new HeroConfig();
            c.setId(1L);
            return c;
        });
        config.setVideoPath(path);
        return toDto(repo.save(config));
    }

    private HeroConfigDto toDto(HeroConfig config) {
        HeroConfigDto dto = new HeroConfigDto();
        dto.setVideoPath(config.getVideoPath());
        dto.setEyebrowText(config.getEyebrowText());
        dto.setIdentityStrong(config.getIdentityStrong());
        dto.setIdentityMid(config.getIdentityMid());
        dto.setSlogans(config.getSlogans());
        dto.setStats(config.getStats().stream().map(s -> {
            HeroConfigDto.HeroStatDto sd = new HeroConfigDto.HeroStatDto();
            sd.setValue(s.getValue());
            sd.setIcon(s.getIcon());
            sd.setLabel(s.getLabel());
            return sd;
        }).collect(Collectors.toList()));
        return dto;
    }

    private void applyDto(HeroConfig config, HeroConfigDto dto) {
        if (dto.getVideoPath() != null) config.setVideoPath(dto.getVideoPath());
        if (dto.getEyebrowText() != null) config.setEyebrowText(dto.getEyebrowText());
        if (dto.getIdentityStrong() != null) config.setIdentityStrong(dto.getIdentityStrong());
        if (dto.getIdentityMid() != null) config.setIdentityMid(dto.getIdentityMid());
        if (dto.getSlogans() != null) {
            config.getSlogans().clear();
            config.getSlogans().addAll(dto.getSlogans());
        }
        if (dto.getStats() != null) {
            List<HeroStat> stats = dto.getStats().stream().map(sd -> {
                HeroStat s = new HeroStat();
                s.setValue(sd.getValue());
                s.setIcon(sd.getIcon());
                s.setLabel(sd.getLabel());
                return s;
            }).collect(Collectors.toList());
            config.getStats().clear();
            config.getStats().addAll(stats);
        }
    }
}
