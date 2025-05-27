package club.taekwondo.service.common;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileUploadService {

    private final String uploadDir = "uploads"; // Répertoire de téléversement

    public String uploadFile(MultipartFile file) throws IOException {
        // Vérifiez si le fichier est nul ou vide
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou invalide.");
        }

        // Créez le répertoire de téléversement s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Générez un nom de fichier unique en cas de doublon
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Le nom du fichier est invalide.");
        }

        String uniqueFilename = generateUniqueFilename(uploadPath, originalFilename);

        // Sauvegardez le fichier
        Path filePath = uploadPath.resolve(uniqueFilename);
        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            throw new IOException("Erreur lors de la sauvegarde du fichier : " + uniqueFilename, e);
        }

        return filePath.toString();
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

        // Ajoutez un suffixe au nom du fichier jusqu'à ce qu'il soit unique
        while (Files.exists(filePath)) {
            String newFilename = filename + "_" + counter + extension;
            filePath = uploadPath.resolve(newFilename);
            counter++;
        }

        return filePath.getFileName().toString();
    }
}