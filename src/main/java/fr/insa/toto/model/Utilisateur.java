package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;

public class Utilisateur extends ClasseMiroir {

    public static final int ROLE_VISITEUR = 0;
    public static final int ROLE_ADMIN = 1;

    private String surnom;
    private String pass;
    private int role;
    private Integer idClub; // Peut être null

    public Utilisateur(String surnom, String pass, int role, Integer idClub) {
        super();
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
        this.idClub = idClub;
    }

    public Utilisateur(int id, String surnom, String pass, int role, Integer idClub) {
        super(id);
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
        this.idClub = idClub;
    }

    @Override
    public String toString() {
        return surnom;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into utilisateur (surnom, pass, role, id_club) values (?,?,?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.surnom);
        pst.setString(2, this.pass);
        pst.setInt(3, this.role);
        if (this.idClub == null) pst.setNull(4, Types.INTEGER);
        else pst.setInt(4, this.idClub);
        
        pst.executeUpdate();
        return pst;
    }

    public static Optional<Utilisateur> login(Connection con, String surnom, String pass) throws SQLException {
        String query = "select id, role, id_club from utilisateur where surnom = ? and pass = ?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, surnom);
            pst.setString(2, pass);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                // Gestion du null pour id_club
                int idClubVal = rs.getInt("id_club");
                Integer idClubObj = rs.wasNull() ? null : idClubVal;
                
                return Optional.of(new Utilisateur(rs.getInt("id"), surnom, pass, rs.getInt("role"), idClubObj));
            } else {
                return Optional.empty();
            }
        }
    }
    
    public static boolean existeSurnom(Connection con, String surnom) throws SQLException {
        String query = "select count(*) from utilisateur where surnom = ?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, surnom);
            ResultSet rs = pst.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
    
    public int getRole() { return role; }
    public String getSurnom() { return surnom; }
    public boolean isAdmin() { return this.role == ROLE_ADMIN; }
    public Integer getIdClub() { return idClub; }
}