package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Utilisateur extends ClasseMiroir {

    private String identifiant;
    private String surnom;
    private String nom;
    private String prenom;
    private String pass;
    private int role; // 0 = Visiteur, 1 = Admin
    private Integer idClub; // Peut être null
    
    // Champs profil étendu
    private String email;
    private LocalDate dateNaissance;
    private String photoUrl;
    private String infosSup;
    private boolean infoValide;
    private boolean nouvellesInfosPendant;
    private String messageAdmin;

    // --- CONSTRUCTEURS ---
    
    public Utilisateur(String identifiant, String surnom, String nom, String prenom, String pass, int role, Integer idClub) {
        this.identifiant = identifiant;
        this.surnom = (surnom == null || surnom.isEmpty()) ? identifiant : surnom;
        this.nom = nom;
        this.prenom = prenom;
        this.pass = pass;
        this.role = role;
        this.idClub = idClub;
    }

    // Constructeur complet depuis la BDD
    public Utilisateur(int id, String identifiant, String surnom, String nom, String prenom, String pass, int role, Integer idClub) {
        super(id);
        this.identifiant = identifiant;
        this.surnom = surnom;
        this.nom = nom;
        this.prenom = prenom;
        this.pass = pass;
        this.role = role;
        this.idClub = idClub;
    }

    // --- LOGIQUE METIER & BDD ---

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        String sql = "INSERT INTO utilisateur (identifiant, surnom, nom, prenom, pass, role, id_club, email, date_naissance, photo_url, infos_sup, info_valide, nouvelles_infos_pendant) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, identifiant);
        pst.setString(2, surnom);
        pst.setString(3, nom);
        pst.setString(4, prenom);
        pst.setString(5, pass);
        pst.setInt(6, role);
        
        if (idClub != null) pst.setInt(7, idClub); else pst.setNull(7, Types.INTEGER);
        
        pst.setString(8, email);
        pst.setDate(9, dateNaissance != null ? Date.valueOf(dateNaissance) : null);
        pst.setString(10, photoUrl);
        pst.setString(11, infosSup);
        pst.setBoolean(12, infoValide);
        pst.setBoolean(13, nouvellesInfosPendant);
        
        pst.executeUpdate();
        return pst;
    }

    public static Optional<Utilisateur> login(Connection con, String identifiant, String pass) throws SQLException {
        String query = "SELECT * FROM utilisateur WHERE identifiant = ? AND pass = ?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, identifiant);
            pst.setString(2, pass);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Utilisateur u = mapResultSetToUser(rs);
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }
    
    public static boolean existeIdentifiant(Connection con, String identifiant) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("SELECT 1 FROM utilisateur WHERE identifiant = ?")) {
            pst.setString(1, identifiant);
            return pst.executeQuery().next();
        }
    }
    
    public static List<Utilisateur> getAllUsers(Connection con) throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT * FROM utilisateur");
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }
        return list;
    }

    private static Utilisateur mapResultSetToUser(ResultSet rs) throws SQLException {
        int idClubVal = rs.getInt("id_club");
        Integer idClubFinal = rs.wasNull() ? null : idClubVal;
        
        Utilisateur u = new Utilisateur(
            rs.getInt("id"),
            rs.getString("identifiant"),
            rs.getString("surnom"),
            rs.getString("nom"),
            rs.getString("prenom"),
            rs.getString("pass"),
            rs.getInt("role"),
            idClubFinal
        );
        // Mapping des champs optionnels
        u.setEmail(rs.getString("email"));
        if (rs.getDate("date_naissance") != null) u.setDateNaissance(rs.getDate("date_naissance").toLocalDate());
        u.setPhotoUrl(rs.getString("photo_url"));
        u.setInfosSup(rs.getString("infos_sup"));
        u.setInfoValide(rs.getBoolean("info_valide"));
        u.setNouvellesInfosPendant(rs.getBoolean("nouvelles_infos_pendant"));
        u.setMessageAdmin(rs.getString("message_admin"));
        return u;
    }
    
    // Validation Admin
    public void confirmInfos(Connection con, boolean valider, String message) throws SQLException {
        String sql = "UPDATE utilisateur SET info_valide = ?, nouvelles_infos_pendant = false, message_admin = ? WHERE id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setBoolean(1, valider);
            pst.setString(2, message);
            pst.setInt(3, this.getId());
            pst.executeUpdate();
            
            this.infoValide = valider;
            this.nouvellesInfosPendant = false;
            this.messageAdmin = message;
        }
    }

    // --- GETTERS & SETTERS (C'est ici que setIdClub a été ajouté) ---

    public Integer getIdClub() { return idClub; }
    
    // LA VOICI : La méthode manquante !
    public void setIdClub(Integer idClub) { this.idClub = idClub; }

    public boolean isAdmin() { return role == 1; }
    
    public String getIdentifiant() { return identifiant; }
    public String getSurnom() { return surnom; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getInitiales() {
        String i1 = (prenom != null && !prenom.isEmpty()) ? prenom.substring(0, 1) : "";
        String i2 = (nom != null && !nom.isEmpty()) ? nom.substring(0, 1) : "";
        return (i1 + i2).toUpperCase();
    }
    
    // Getters/Setters pour le profil
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    
    public String getInfosSup() { return infosSup; }
    public void setInfosSup(String infosSup) { this.infosSup = infosSup; }
    
    public boolean isInfoValide() { return infoValide; }
    public void setInfoValide(boolean infoValide) { this.infoValide = infoValide; }
    
    public boolean isNouvellesInfosPendant() { return nouvellesInfosPendant; }
    public void setNouvellesInfosPendant(boolean p) { this.nouvellesInfosPendant = p; }
    
    public String getMessageAdmin() { return messageAdmin; }
    public void setMessageAdmin(String msg) { this.messageAdmin = msg; }
}