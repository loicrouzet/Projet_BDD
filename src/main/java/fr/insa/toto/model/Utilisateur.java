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
        PreparedStatement pst = con.prepareStatement(
            "insert into utilisateur (surnom, pass, role, id_club, email, age, info_valide, nouvelles_infos_pendant, message_admin) values (?,?,?,?,?,?,?,?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.surnom);
        pst.setString(2, this.pass);
        pst.setInt(3, this.role);
        if (this.idClub == null) pst.setNull(4, java.sql.Types.INTEGER); else pst.setInt(4, this.idClub);
        pst.setString(5, this.email);
        if (this.age == null) pst.setNull(6, java.sql.Types.INTEGER); else pst.setInt(6, this.age);
        pst.setBoolean(7, false);
        pst.setBoolean(8, false);
        pst.setNull(9, java.sql.Types.VARCHAR);
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
        int ageVal = rs.getInt("age");
        Integer ageObj = rs.wasNull() ? null : ageVal;
        return new Utilisateur(rs.getInt("id"), rs.getString("surnom"), rs.getString("pass"), 
                rs.getInt("role"), idClubObj, rs.getString("email"), ageObj, 
                rs.getBoolean("info_valide"), rs.getBoolean("nouvelles_infos_pendant"), 
                rs.getString("message_admin"));
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
}