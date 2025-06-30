package club.taekwondo.service.common;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileUploadService {

    private final String uploadDir = "uploads"; // Dossier racine

    public String uploadFile(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou invalide.");
        }

        // Répertoire cible : uploads/avis, uploads/photos, etc.
        Path uploadPath = Paths.get(uploadDir, subFolder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Le nom du fichier est invalide.");
        }

        String uniqueFilename = generateUniqueFilename(uploadPath, originalFilename);
        Path filePath = uploadPath.resolve(uniqueFilename);

        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            throw new IOException("Erreur lors de la sauvegarde du fichier : " + uniqueFilename, e);
        }

        // On retourne le chemin relatif pour Angular (ex : "avis/image.jpg")
        return subFolder + "/" + uniqueFilename;
    }

    private String generateUniqueFilename(Path uploadPath, String originalFilename) {
        String filename = originalFilename;
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex > 0) {
            filename = originalFilename.substring(0, dotIndex);
            extension = originalFilename.substring(dotIndex);
        }

        Path filePath = uploadPath.resolve(originalFilename);
        int counter = 1;

        while (Files.exists(filePath)) {
            String newFilename = filename + "_" + counter + extension;
            filePath = uploadPath.resolve(newFilename);
            counter++;
        }

        return filePath.getFileName().toString();
    }
}
