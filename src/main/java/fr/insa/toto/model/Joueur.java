package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Joueur extends ClasseMiroir {
    
    private String nom;
    private String prenom;
    private int idEquipe;
    private int idClub;
    private Integer idUtilisateur;
    
    // Nouveaux attributs persistants
    private LocalDate dateNaissance;
    private String instagram;
    private String facebook;
    private String twitter;

    // Champs transients (Affichage)
    private String nomClub; 
    private String photoUrl;
    private String email;

    public Joueur(String nom, String prenom, int idClub) {
        super();
        this.nom = nom; this.prenom = prenom; this.idClub = idClub;
        this.idEquipe = -1; this.idUtilisateur = null;
    }

    public Joueur(int id, String nom, String prenom, int idEquipe, int idClub, Integer idUtilisateur) {
        super(id);
        this.nom = nom; this.prenom = prenom; this.idEquipe = idEquipe; 
        this.idClub = idClub; this.idUtilisateur = idUtilisateur;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into joueur (nom, prenom, id_equipe, id_club, id_utilisateur, date_naissance, instagram, facebook, twitter) values (?,?,?,?,?,?,?,?,?)", 
            Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, this.nom);
        pst.setString(2, this.prenom);
        if(this.idEquipe > 0) pst.setInt(3, this.idEquipe); else pst.setNull(3, Types.INTEGER);
        pst.setInt(4, this.idClub);
        if(this.idUtilisateur != null && this.idUtilisateur > 0) pst.setInt(5, this.idUtilisateur); else pst.setNull(5, Types.INTEGER);
        
        pst.setDate(6, this.dateNaissance != null ? Date.valueOf(this.dateNaissance) : null);
        pst.setString(7, this.instagram);
        pst.setString(8, this.facebook);
        pst.setString(9, this.twitter);
        
        pst.executeUpdate();
        return pst;
    }

    public void update(Connection con) throws SQLException {
        String sql = "update joueur set nom=?, prenom=?, id_equipe=?, id_club=?, id_utilisateur=?, date_naissance=?, instagram=?, facebook=?, twitter=? where id=?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, this.nom);
            pst.setString(2, this.prenom);
            if(this.idEquipe > 0) pst.setInt(3, this.idEquipe); else pst.setNull(3, Types.INTEGER);
            if(this.idClub > 0) pst.setInt(4, this.idClub); else pst.setNull(4, Types.INTEGER);
            if(this.idUtilisateur != null && this.idUtilisateur > 0) pst.setInt(5, this.idUtilisateur); else pst.setNull(5, Types.INTEGER);
            
            pst.setDate(6, this.dateNaissance != null ? Date.valueOf(this.dateNaissance) : null);
            pst.setString(7, this.instagram);
            pst.setString(8, this.facebook);
            pst.setString(9, this.twitter);
            
            pst.setInt(10, this.getId());
            pst.executeUpdate();
        }
    }

    private static Joueur map(ResultSet rs) throws SQLException {
        int idU = rs.getInt("id_utilisateur");
        Joueur j = new Joueur(
            rs.getInt("id"), rs.getString("nom"), rs.getString("prenom"), 
            rs.getInt("id_equipe"), rs.getInt("id_club"), rs.wasNull() ? null : idU
        );
        Date d = rs.getDate("date_naissance");
        if(d != null) j.setDateNaissance(d.toLocalDate());
        j.setInstagram(rs.getString("instagram"));
        j.setFacebook(rs.getString("facebook"));
        j.setTwitter(rs.getString("twitter"));
        return j;
    }

    public static List<Joueur> getAll(Connection con) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        String sql = "SELECT j.*, c.nom AS nom_club, u.photo_url, u.email " +
                     "FROM joueur j " +
                     "LEFT JOIN club c ON j.id_club = c.id " +
                     "LEFT JOIN utilisateur u ON j.id_utilisateur = u.id"; 
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Joueur j = map(rs);
                j.setNomClub(rs.getString("nom_club"));
                j.setPhotoUrl(rs.getString("photo_url"));
                j.setEmail(rs.getString("email"));
                res.add(j);
            }
        }
        return res;
    }

    public static List<Joueur> getByClub(Connection con, int idClub) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        String sql = "SELECT * FROM joueur WHERE id_club = ?"; 
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idClub); ResultSet rs = pst.executeQuery();
            while (rs.next()) res.add(map(rs));
        }
        return res;
    }
    
    // NOUVELLE MÉTHODE POUR LE PROFIL
    public static Optional<Joueur> getByUtilisateurId(Connection con, int idUtilisateur) throws SQLException {
        String sql = "SELECT j.*, c.nom AS nom_club, u.photo_url, u.email " +
                     "FROM joueur j " +
                     "LEFT JOIN club c ON j.id_club = c.id " +
                     "LEFT JOIN utilisateur u ON j.id_utilisateur = u.id " +
                     "WHERE j.id_utilisateur = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idUtilisateur);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Joueur j = map(rs);
                j.setNomClub(rs.getString("nom_club"));
                j.setPhotoUrl(rs.getString("photo_url"));
                j.setEmail(rs.getString("email"));
                return Optional.of(j);
            }
        }
        return Optional.empty();
    }
    
    public static Optional<Joueur> findByNomPrenomClub(Connection con, String nom, String prenom, int idClub) throws SQLException {
        String sql = "SELECT * FROM joueur WHERE lower(nom)=lower(?) AND lower(prenom)=lower(?) AND id_club=?";
        try(PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, nom); pst.setString(2, prenom); pst.setInt(3, idClub);
            ResultSet rs = pst.executeQuery();
            if(rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }
    
    public void delete(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("delete from inscription_joueur where id_joueur=?")) {
            pst.setInt(1, this.getId()); pst.executeUpdate();
        }
        try (PreparedStatement pst = con.prepareStatement("delete from joueur where id=?")) {
            pst.setInt(1, this.getId()); pst.executeUpdate();
        }
    }
    
    public static List<Joueur> getInscritsAuTournoi(Connection con, int idTournoi) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        String sql = "SELECT j.* FROM joueur j JOIN inscription_joueur i ON j.id = i.id_joueur WHERE i.id_tournoi = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi); ResultSet rs = pst.executeQuery();
            while (rs.next()) res.add(map(rs));
        }
        return res;
    }
    public void inscrireAuTournoi(Connection con, int idTournoi) throws SQLException {
         try (PreparedStatement pst = con.prepareStatement("insert into inscription_joueur (id_tournoi, id_joueur) values (?, ?)")) {
            pst.setInt(1, idTournoi); pst.setInt(2, this.getId()); pst.executeUpdate();
        }
    }
    public void desinscrireDuTournoi(Connection con, int idTournoi) throws SQLException {
         try (PreparedStatement pst = con.prepareStatement("delete from inscription_joueur where id_tournoi=? and id_joueur=?")) {
            pst.setInt(1, idTournoi); pst.setInt(2, this.getId()); pst.executeUpdate();
        }
    }

    // Getters & Setters
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public int getIdEquipe() { return idEquipe; }
    public void setIdEquipe(int idEquipe) { this.idEquipe = idEquipe; }
    public int getIdClub() { return idClub; }
    public Integer getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(Integer idUtilisateur) { this.idUtilisateur = idUtilisateur; }
    
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }
    public String getFacebook() { return facebook; }
    public void setFacebook(String facebook) { this.facebook = facebook; }
    public String getTwitter() { return twitter; }
    public void setTwitter(String twitter) { this.twitter = twitter; }

    public String getNomClub() { return nomClub != null ? nomClub : "Inconnu"; }
    public void setNomClub(String nomClub) { this.nomClub = nomClub; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    @Override public String toString() { return prenom + " " + nom; }
}