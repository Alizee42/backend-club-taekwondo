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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Test
    void getDocumentsByUtilisateurId_idInvalide_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> documentService.getDocumentsByUtilisateurId(0L));
        assertThrows(IllegalArgumentException.class, () -> documentService.getDocumentsByUtilisateurId(-1L));
        assertThrows(IllegalArgumentException.class, () -> documentService.getDocumentsByUtilisateurId(null));
    }

    @Test
    void getDocumentsByStatus_statutVide_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> documentService.getDocumentsByStatus(null));
        assertThrows(IllegalArgumentException.class, () -> documentService.getDocumentsByStatus(""));
    }

    @Test
    void getDocumentsByClubId_filtreParClubDeLUtilisateur() {
        Document document = sampleDocument(); // utilisateur rattache au club id=3
        when(documentRepository.findAllWithUtilisateurAndMembre()).thenReturn(List.of(document));

        List<DocumentDTO> resultatsClub3 = documentService.getDocumentsByClubId(3L);
        List<DocumentDTO> resultatsAutreClub = documentService.getDocumentsByClubId(999L);

        assertEquals(1, resultatsClub3.size());
        assertTrue(resultatsAutreClub.isEmpty());
    }

    @Test
    void createDocument_documentActifDejaExistantMemeTypeMemeMembre_leveIllegalArgumentException() {
        Document existant = sampleDocument();
        existant.setStatus("en attente");
        when(documentRepository.findByUtilisateurId(9L)).thenReturn(List.of(existant));

        DocumentDTO dto = new DocumentDTO();
        dto.setUtilisateurId(9L);
        dto.setMembreId(5L);
        dto.setTypeDocument("licence");

        assertThrows(IllegalArgumentException.class, () -> documentService.createDocument(dto));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void createDocument_documentPrecedentRefuse_autoriseUnNouveauDepot() {
        Document refuse = sampleDocument();
        refuse.setStatus("refusé");
        when(documentRepository.findByUtilisateurId(9L)).thenReturn(List.of(refuse));
        when(utilisateurRepository.findById(9L)).thenReturn(Optional.of(refuse.getUtilisateur()));
        when(membreRepository.findById(5L)).thenReturn(Optional.of(refuse.getMembre()));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DocumentDTO dto = new DocumentDTO();
        dto.setUtilisateurId(9L);
        dto.setMembreId(5L);
        dto.setTypeDocument("licence");
        dto.setNomDocument("nouvelle-licence.pdf");

        DocumentDTO created = documentService.createDocument(dto);

        assertNotNull(created);
        verify(documentRepository).save(any());
    }

    @Test
    void createDocument_membreNAppartientPasAUtilisateur_leveIllegalArgumentException() {
        Document ref = sampleDocument();
        Utilisateur autreParent = new Utilisateur();
        autreParent.setId(42L);
        when(documentRepository.findByUtilisateurId(42L)).thenReturn(List.of());
        when(utilisateurRepository.findById(42L)).thenReturn(Optional.of(autreParent));
        when(membreRepository.findById(5L)).thenReturn(Optional.of(ref.getMembre())); // membre rattache au parent 9L

        DocumentDTO dto = new DocumentDTO();
        dto.setUtilisateurId(42L);
        dto.setMembreId(5L);
        dto.setTypeDocument("licence");

        assertThrows(IllegalArgumentException.class, () -> documentService.createDocument(dto));
    }

    @Test
    void createDocument_sansUtilisateurValide_leveIllegalArgumentException() {
        DocumentDTO dto = new DocumentDTO();
        dto.setTypeDocument("licence");

        assertThrows(IllegalArgumentException.class, () -> documentService.createDocument(dto));
    }

    @Test
    void createDocument_utilisateurIntrouvable_leveIllegalArgumentException() {
        when(documentRepository.findByUtilisateurId(99L)).thenReturn(List.of());
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());

        DocumentDTO dto = new DocumentDTO();
        dto.setUtilisateurId(99L);
        dto.setTypeDocument("licence");

        assertThrows(IllegalArgumentException.class, () -> documentService.createDocument(dto));
    }

    @Test
    void updateDocumentStatus_versRefuse_persisteLeMotif() {
        Document document = sampleDocument();
        when(documentRepository.findById(2L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentService.updateDocumentStatus(2L, "refusé", "Photo illisible");

        assertEquals("refusé", document.getStatus());
        assertEquals("Photo illisible", document.getMotifRefus());
    }

    @Test
    void updateDocumentStatus_versApprouve_effaceLeMotifRefus() {
        Document document = sampleDocument();
        document.setMotifRefus("Ancien motif");
        when(documentRepository.findById(2L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentService.updateDocumentStatus(2L, "approuvé");

        assertEquals("approuvé", document.getStatus());
        assertNull(document.getMotifRefus());
    }

    @Test
    void updateDocumentStatus_documentIntrouvable_leveIllegalArgumentException() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> documentService.updateDocumentStatus(999L, "approuvé"));
    }

    @Test
    void replaceDocumentFile_remplaceLeFichierEtResetLeStatut() {
        Document document = sampleDocument();
        document.setStatus("refusé");
        document.setMotifRefus("Ancien motif");
        when(documentRepository.findById(2L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DocumentDTO updated = documentService.replaceDocumentFile(2L, "documents/nouveau.pdf", "nouveau.pdf");

        assertEquals("documents/nouveau.pdf", updated.getCheminFichier());
        assertEquals("en attente", updated.getStatus());
        assertNull(updated.getMotifRefus());
    }

    @Test
    void replaceDocumentFile_documentIntrouvable_leveIllegalArgumentException() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> documentService.replaceDocumentFile(999L, "x.pdf", "x.pdf"));
    }

    @Test
    void updateDocument_documentIntrouvable_leveIllegalArgumentException() {
        when(documentRepository.existsById(999L)).thenReturn(false);

        DocumentDTO dto = new DocumentDTO();
        dto.setUtilisateurId(9L);
        dto.setTypeDocument("licence");

        assertThrows(IllegalArgumentException.class, () -> documentService.updateDocument(999L, dto));
    }

    @Test
    void updateDocument_idInvalide_leveIllegalArgumentException() {
        DocumentDTO dto = new DocumentDTO();
        assertThrows(IllegalArgumentException.class, () -> documentService.updateDocument(0L, dto));
    }

    @Test
    void deleteDocument_documentIntrouvable_leveIllegalArgumentException() {
        when(documentRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> documentService.deleteDocument(999L));
        verify(documentRepository, never()).deleteById(any());
    }

    @Test
    void deleteDocument_existant_appelleDeleteById() {
        when(documentRepository.existsById(2L)).thenReturn(true);

        documentService.deleteDocument(2L);

        verify(documentRepository).deleteById(2L);
    }

    @Test
    void getDocumentById_idInvalide_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> documentService.getDocumentById(-1L));
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
