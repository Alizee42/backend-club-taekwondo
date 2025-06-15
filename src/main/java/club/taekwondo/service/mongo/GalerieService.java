package club.taekwondo.service.mongo;

import club.taekwondo.dto.GalerieDTO;
import club.taekwondo.entity.mongo.Galerie;
import club.taekwondo.repository.mongo.GalerieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GalerieService {

    private final GalerieRepository galerieRepository;

    public GalerieService(GalerieRepository galerieRepository) {
        this.galerieRepository = galerieRepository;
    }

    public List<GalerieDTO> getAll() {
        return galerieRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<GalerieDTO> getById(String id) {
        return galerieRepository.findById(id).map(this::convertToDTO);
    }

    public GalerieDTO create(GalerieDTO galerieDTO) {
        Galerie galerie = convertToEntity(galerieDTO);
        return convertToDTO(galerieRepository.save(galerie));
    }

    public GalerieDTO update(String id, GalerieDTO galerieDTO) {
        return galerieRepository.findById(id).map(existing -> {
            existing.setTitre(galerieDTO.getTitre());
            existing.setImageUrl(galerieDTO.getImageUrl());
            existing.setDescription(galerieDTO.getDescription());
            existing.setDatePublication(galerieDTO.getDatePublication());
            return convertToDTO(galerieRepository.save(existing));
        }).orElse(null);
    }

    public void delete(String id) {
        galerieRepository.deleteById(id);
    }

    private GalerieDTO convertToDTO(Galerie galerie) {
        GalerieDTO galerieDTO = new GalerieDTO();
        galerieDTO.setId(galerie.getId());
        galerieDTO.setTitre(galerie.getTitre());
        galerieDTO.setImageUrl(galerie.getImageUrl());
        galerieDTO.setDescription(galerie.getDescription());
        galerieDTO.setDatePublication(galerie.getDatePublication());
        return galerieDTO;
    }

    private Galerie convertToEntity(GalerieDTO galerieDTO) {
        Galerie galerie = new Galerie();
        galerie.setId(galerieDTO.getId());
        galerie.setTitre(galerieDTO.getTitre());
        galerie.setImageUrl(galerieDTO.getImageUrl());
        galerie.setDescription(galerieDTO.getDescription());
        galerie.setDatePublication(galerieDTO.getDatePublication());
        return galerie;
    }
}
