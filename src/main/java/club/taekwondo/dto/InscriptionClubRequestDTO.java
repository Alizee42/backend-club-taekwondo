package club.taekwondo.dto;

import java.util.List;

/**
 * Payload d'inscription complete au club (auto-inscription publique) : cree
 * l'utilisateur et son/ses membre(s) associe(s) en une seule operation
 * transactionnelle, pour eviter un utilisateur orphelin sans membre si la
 * creation du membre echoue. A ne pas confondre avec InscriptionRequestDTO,
 * qui sert a inscrire un parent/ses enfants a un evenement.
 */
public class InscriptionClubRequestDTO {

    private UtilisateurDTO utilisateur;
    private List<MembreDTO> membres;

    public UtilisateurDTO getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(UtilisateurDTO utilisateur) {
        this.utilisateur = utilisateur;
    }

    public List<MembreDTO> getMembres() {
        return membres;
    }

    public void setMembres(List<MembreDTO> membres) {
        this.membres = membres;
    }
}
