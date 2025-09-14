package club.taekwondo.service.common;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class FileUploadService {

    private final String uploadDir = "uploads"; // Dossier racine

    /**
     * Enregistre un fichier dans un sous-dossier spécifique (ex: "evenements", "photos", etc.).
     * Retourne le chemin relatif (ex: "evenements/image.jpg") à stocker côté base ou front.
     */
    
    public String uploadFile(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou invalide.");
        }

        // Crée le répertoire cible s’il n’existe pas
        Path uploadPath = Paths.get(uploadDir, subFolder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Nom de fichier original
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Le nom du fichier est invalide.");
        }

        // Génère un nom unique
        String uniqueFilename = generateUniqueFilename(uploadPath, originalFilename);
        Path filePath = uploadPath.resolve(uniqueFilename);

        // Copie le fichier
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Retourne un chemin relatif exploitable depuis Angular
        return subFolder + "/" + uniqueFilename;
    }

    /**
     * Génère un nom de fichier unique en ajoutant un timestamp si nécessaire.
     */
    private String generateUniqueFilename(Path uploadPath, String originalFilename) {
        String name = originalFilename;
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex > 0) {
            name = originalFilename.substring(0, dotIndex);
            extension = originalFilename.substring(dotIndex);
        }

        String baseName = System.currentTimeMillis() + "_" + name;
        String uniqueFilename = baseName + extension;
        Path candidatePath = uploadPath.resolve(uniqueFilename);
        int counter = 1;

        while (Files.exists(candidatePath)) {
            uniqueFilename = baseName + "_" + counter + extension;
            candidatePath = uploadPath.resolve(uniqueFilename);
            counter++;
        }

        return uniqueFilename;
    }
}

