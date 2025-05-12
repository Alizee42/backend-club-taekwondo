package club.taekwondo.service.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileUploadService {

    @Value("${upload.dir}")
    private String uploadDir;

    public String uploadFile(MultipartFile file) throws IOException {
        // Crée le dossier s'il n'existe pas
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            boolean dirCreated = uploadDirectory.mkdirs();
            if (dirCreated) {
                System.out.println("Le dossier 'uploads' a été créé avec succès.");
            } else {
                System.out.println("Échec de la création du dossier 'uploads'.");
            }
        }

        // Nettoie le nom du fichier (par sécurité)
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("Le fichier n’a pas de nom.");
        }

        // Crée le chemin où le fichier sera stocké
        Path filePath = Paths.get(uploadDir, fileName);
        System.out.println("Le fichier sera stocké à : " + filePath.toString());

        // Copie le fichier dans le répertoire de stockage
        try {
            Files.copy(file.getInputStream(), filePath);
            System.out.println("Fichier téléchargé avec succès.");
        } catch (IOException e) {
            System.out.println("Erreur lors du téléchargement du fichier : " + e.getMessage());
            throw new IOException("Erreur lors du téléchargement du fichier.", e);
        }

        // Retourne une URL ou un chemin relatif que le front peut utiliser
        String fileUrl = "/uploads/" + fileName;
        System.out.println("URL du fichier téléchargé : " + fileUrl);
        return fileUrl;
    }
}
