package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class Utilisateur extends ClasseMiroir {
    public static final int ROLE_VISITEUR = 0;
    public static final int ROLE_ADMIN = 1;

    private String surnom, pass, email, messageAdmin;
    private int role;
    private Integer idClub, age;
    private boolean infoValide, nouvellesInfosPendant;

    // Constructeur complet (utilisé par mapResultSetToUtilisateur)
    public Utilisateur(int id, String surnom, String pass, int role, Integer idClub, String email, Integer age, boolean infoValide, boolean nouvellesInfosPendant, String messageAdmin) {
        super(id);
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
        this.idClub = idClub;
        this.email = email;
        this.age = age;
        this.infoValide = infoValide;
        this.nouvellesInfosPendant = nouvellesInfosPendant;
        this.messageAdmin = messageAdmin;
    }

    // Constructeur pour création de compte
    public Utilisateur(String surnom, String pass, int role, Integer idClub) {
        super();
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
        this.idClub = idClub;
    }

    @Override

protected Statement saveSansId(Connection con) throws SQLException {
    // On retire 'age' de la liste des colonnes (index 6 précédemment)
    PreparedStatement pst = con.prepareStatement(
        "insert into utilisateur (surnom, pass, role, id_club, email, info_valide, nouvelles_infos_pendant, message_admin, date_naissance, sexe, photo_url) values (?,?,?,?,?,?,?,?,?,?,?)", 
        Statement.RETURN_GENERATED_KEYS
    );
    pst.setString(1, this.surnom);
    pst.setString(2, this.pass);
    pst.setInt(3, this.role);
    if (this.idClub == null) pst.setNull(4, java.sql.Types.INTEGER); else pst.setInt(4, this.idClub);
    pst.setString(5, this.email);
    pst.setBoolean(6, false); // info_valide
    pst.setBoolean(7, false); // nouvelles_infos_pendant
    pst.setNull(8, java.sql.Types.VARCHAR); // message_admin
    pst.setDate(9, this.dateNaissance != null ? Date.valueOf(this.dateNaissance) : null);
    pst.setString(10, this.sexe);
    pst.setString(11, this.photoUrl);
    
    pst.executeUpdate();
    return pst;
}

    public static Optional<Utilisateur> login(Connection con, String surnom, String pass) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("select * from utilisateur where surnom = ? and pass = ?")) {
            pst.setString(1, surnom); pst.setString(2, pass);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToUtilisateur(rs));
            }
        }
        return Optional.empty();
    }

    public static List<Utilisateur> getPendingValidations(Connection con) throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("select * from utilisateur where nouvelles_infos_pendant = true")) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) { list.add(mapResultSetToUtilisateur(rs)); }
        }
        return list;
    }

private static Utilisateur mapResultSetToUtilisateur(ResultSet rs) throws SQLException {
    int idClubVal = rs.getInt("id_club");
    Integer idClubObj = rs.wasNull() ? null : idClubVal;
    
    // On passe 'null' pour le paramètre 'age' du constructeur car la colonne n'existe plus
    Utilisateur u = new Utilisateur(rs.getInt("id"), rs.getString("surnom"), rs.getString("pass"), 
            rs.getInt("role"), idClubObj, rs.getString("email"), null, 
            rs.getBoolean("info_valide"), rs.getBoolean("nouvelles_infos_pendant"), 
            rs.getString("message_admin"));
            
    // Chargement des colonnes optionnelles
    Date d = rs.getDate("date_naissance");
    if (d != null) u.setDateNaissance(d.toLocalDate());
    u.setSexe(rs.getString("sexe"));
    u.setPhotoUrl(rs.getString("photo_url"));
    
    return u;
}
    // Getters nécessaires pour VuePrincipale
    public int getRole() { return role; }
    public String getSurnom() { return surnom; }
    public boolean isAdmin() { return role == ROLE_ADMIN; }
    public Integer getIdClub() { return idClub; }
    public String getEmail() { return email; }
    public Integer getAge() { return age; }
    public String getMessageAdmin() { return messageAdmin; }
    public boolean isInfoValide() { return infoValide; }
    // À ajouter dans Utilisateur.java
public static boolean existeSurnom(Connection con, String surnom) throws SQLException {
    String query = "select count(*) from utilisateur where surnom = ?";
    try (PreparedStatement pst = con.prepareStatement(query)) {
        pst.setString(1, surnom);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
    }
    return false;
}
// Ajoutez ces attributs dans la classe Utilisateur
private java.time.LocalDate dateNaissance;
private String sexe;
private String photoUrl;

// Mettez à jour vos getters et setters
public java.time.LocalDate getDateNaissance() { return dateNaissance; }
public void setDateNaissance(java.time.LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
public String getSexe() { return sexe; }
public void setSexe(String sexe) { this.sexe = sexe; }
public String getPhotoUrl() { return photoUrl; }
public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

}