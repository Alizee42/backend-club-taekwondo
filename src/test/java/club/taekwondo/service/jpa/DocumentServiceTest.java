package club.taekwondo.service.jpa;

import club.taekwondo.dto.DocumentDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Document;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.DocumentRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private MembreRepository membreRepository;

    @InjectMocks
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDocumentsByUtilisateurId_usesFetchQueryAndMapsNestedRelations() {
        Document document = sampleDocument();
        when(documentRepository.findByUtilisateurIdWithFetch(9L)).thenReturn(List.of(document));

        List<DocumentDTO> result = documentService.getDocumentsByUtilisateurId(9L);

        assertEquals(1, result.size());
        DocumentDTO dto = result.get(0);
        assertEquals(2L, dto.getId());
        assertEquals(9L, dto.getUtilisateurId());
        assertEquals(5L, dto.getMembreId());
        assertEquals(5L, dto.getEnfantId());
        assertNotNull(dto.getUtilisateur());
        assertEquals(3L, dto.getUtilisateur().getClubId());
        assertNotNull(dto.getEnfant());
        assertEquals(9L, dto.getEnfant().getUtilisateurId());
        verify(documentRepository).findByUtilisateurIdWithFetch(9L);
    }

    @Test
    void getDocumentById_usesFetchQuery() {
        Document document = sampleDocument();
        when(documentRepository.findByIdWithFetch(2L)).thenReturn(Optional.of(document));

        Optional<DocumentDTO> result = documentService.getDocumentById(2L);

        assertEquals(Optional.of(2L), result.map(DocumentDTO::getId));
        verify(documentRepository).findByIdWithFetch(2L);
    }

    private Document sampleDocument() {
        Club club = new Club();
        club.setId(3L);
        club.setName("Club 3");

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(9L);
        utilisateur.setNom("Gueye");
        utilisateur.setPrenom("Alizee");
        utilisateur.setEmail("alizee.gueye@gmail.com");
        utilisateur.setTelephone("0682192787");
        utilisateur.setRole(Role.MEMBRE);
        utilisateur.setClub(club);

        Membre membre = new Membre();
        membre.setId(5L);
        membre.setNom("Gueye");
        membre.setPrenom("Enfant");
        membre.setNumeroLicence("TKD-123");
        membre.setDateNaissance(LocalDate.of(2015, 5, 4));
        membre.setEstAdulte(false);
        membre.setParent(utilisateur);

        Document document = new Document();
        document.setId(2L);
        document.setTypeDocument("licence");
        document.setNomDocument("licence.pdf");
        document.setCheminFichier("documents/licence.pdf");
        document.setDateDepot(LocalDateTime.of(2026, 4, 21, 9, 43));
        document.setStatus("en attente");
        document.setDescription("Document de licence");
        document.setUtilisateur(utilisateur);
        document.setMembre(membre);
        return document;
    }
}
