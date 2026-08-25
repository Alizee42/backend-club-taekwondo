package club.taekwondo.service.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;

@Service
public class FileUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml");

    private final Path uploadRoot;

    public FileUploadService(@Value("${upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Enregistre un fichier dans un sous-dossier spécifique (ex: "evenements", "photos", etc.).
     * Retourne le chemin relatif (ex: "evenements/image.jpg") à stocker côté base ou front.
     */

    public String uploadFile(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou invalide.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Le nom du fichier est invalide.");
        }

        String extension = extractExtension(originalFilename);
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Formats acceptés : "
                    + String.join(", ", ALLOWED_EXTENSIONS) + ".");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Type de fichier non autorisé (contenu détecté : " + contentType + ").");
        }

        // Crée le répertoire cible s’il n’existe pas
        Path uploadPath = uploadRoot.resolve(subFolder).normalize();
        if (!uploadPath.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Sous-dossier d'upload invalide.");
        }
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
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

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}

