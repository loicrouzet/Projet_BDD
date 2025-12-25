package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Utilisateur extends ClasseMiroir {
    public static final int ROLE_VISITEUR = 0;
    public static final int ROLE_ADMIN = 1;

    private String identifiant, surnom, nom, prenom, pass, email, messageAdmin, infosSup, photoUrl, sexe;
    private int role;
    private Integer idClub;
    private boolean infoValide, nouvellesInfosPendant;
    private java.time.LocalDate dateNaissance;

    // Constructeur complet
    public Utilisateur(int id, String identifiant, String surnom, String nom, String prenom, String pass, int role, Integer idClub, String email, boolean infoValide, boolean nouvellesInfosPendant, String messageAdmin) {
        super(id);
        this.identifiant = identifiant;
        this.surnom = surnom;
        this.nom = nom;
        this.prenom = prenom;
        this.pass = pass;
        this.role = role;
        this.idClub = idClub;
        this.email = email;
        this.infoValide = infoValide;
        this.nouvellesInfosPendant = nouvellesInfosPendant;
        this.messageAdmin = messageAdmin;
    }

    // Constructeur pour inscription
    public Utilisateur(String identifiant, String surnom, String nom, String prenom, String pass, int role, Integer idClub) {
        super();
        this.identifiant = identifiant;
        this.surnom = surnom;
        this.nom = nom;
        this.prenom = prenom;
        this.pass = pass;
        this.role = role;
        this.idClub = idClub;
    }

    // MÉTHODE POUR LES INITIALES (Ahmed Elzalaki -> AE)
    public String getInitiales() {
        String p = (prenom != null && !prenom.isEmpty()) ? prenom.substring(0, 1).toUpperCase() : "";
        String n = (nom != null && !nom.isEmpty()) ? nom.substring(0, 1).toUpperCase() : "";
        return p + n;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into utilisateur (identifiant, surnom, nom, prenom, pass, role, id_club, email, info_valide, nouvelles_infos_pendant, date_naissance) values (?,?,?,?,?,?,?,?,?,?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.identifiant);
        pst.setString(2, this.surnom);
        pst.setString(3, this.nom);
        pst.setString(4, this.prenom);
        pst.setString(5, this.pass);
        pst.setInt(6, this.role);
        if (this.idClub == null) pst.setNull(7, Types.INTEGER); else pst.setInt(7, this.idClub);
        pst.setString(8, this.email);
        pst.setBoolean(9, false);
        pst.setBoolean(10, false);
        pst.setDate(11, this.dateNaissance != null ? Date.valueOf(this.dateNaissance) : null);
        pst.executeUpdate();
        return pst;
    }

    public static Optional<Utilisateur> login(Connection con, String identifiant, String pass) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("select * from utilisateur where identifiant = ? and pass = ?")) {
            pst.setString(1, identifiant); pst.setString(2, pass);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return Optional.of(mapResultSetToUtilisateur(rs));
        }
        return Optional.empty();
    }

    private static Utilisateur mapResultSetToUtilisateur(ResultSet rs) throws SQLException {
        int idClubVal = rs.getInt("id_club");
        Integer idClubObj = rs.wasNull() ? null : idClubVal;
        Utilisateur u = new Utilisateur(rs.getInt("id"), rs.getString("identifiant"), rs.getString("surnom"),
                rs.getString("nom"), rs.getString("prenom"), rs.getString("pass"), 
                rs.getInt("role"), idClubObj, rs.getString("email"), 
                rs.getBoolean("info_valide"), rs.getBoolean("nouvelles_infos_pendant"), rs.getString("message_admin"));
        
        Date d = rs.getDate("date_naissance");
        if (d != null) u.setDateNaissance(d.toLocalDate());
        u.setPhotoUrl(rs.getString("photo_url"));
        u.setInfosSup(rs.getString("infos_sup"));
        return u;
    }

    public static boolean existeIdentifiant(Connection con, String identifiant) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("select count(*) from utilisateur where identifiant = ?")) {
            pst.setString(1, identifiant);
            ResultSet rs = pst.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // Getters et Setters
    public String getIdentifiant() { return identifiant; }
    public String getSurnom() { return (surnom == null || surnom.isEmpty()) ? prenom : surnom; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public boolean isAdmin() { return role == ROLE_ADMIN; }
    public Integer getIdClub() { return idClub; }
    public String getEmail() { return email; }
    public String getPhotoUrl() { return photoUrl; }
    public boolean isInfoValide() { return infoValide; }
    public java.time.LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(java.time.LocalDate d) { this.dateNaissance = d; }
    public void setPhotoUrl(String url) { this.photoUrl = url; }
    public void setInfosSup(String info) { this.infosSup = info; }
    public String getInfosSup() { return infosSup; }
    public void setEmail(String email) { this.email = email; }
    public static List<Utilisateur> getAllUsers(Connection con) throws SQLException {
        List<Utilisateur> users = new ArrayList<>();
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT * FROM utilisateur ORDER BY nom");
            while (rs.next()) users.add(mapResultSetToUtilisateur(rs));
        }
        return users;
    }
    // --- MÉTHODE À AJOUTER POUR VALIDER UN PROFIL ---
    public void confirmInfos(Connection con, boolean estValide, String message) throws SQLException {
        String sql = "update utilisateur set info_valide = ?, nouvelles_infos_pendant = false, message_admin = ? where id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBoolean(1, estValide);
            if (message == null) pst.setNull(2, java.sql.Types.VARCHAR); else pst.setString(2, message);
            pst.setInt(3, this.getId());
            pst.executeUpdate();
            this.infoValide = estValide; // Mise à jour locale pour l'affichage immédiat
        }
    }
    
    // Ajoute aussi ces setters pour éviter d'autres erreurs "find symbol"
    public void setNom(String nom) { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setSurnom(String surnom) { this.surnom = surnom; }
    
}