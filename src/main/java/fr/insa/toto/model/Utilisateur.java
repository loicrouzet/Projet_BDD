package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class Utilisateur extends ClasseMiroir {

    // Constantes pour les rôles
    public static final int ROLE_VISITEUR = 0;
    public static final int ROLE_ADMIN = 1;

    private String surnom;
    private String pass;
    private int role;

    // Constructeur création
    public Utilisateur(String surnom, String pass, int role) {
        super();
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
    }

    // Constructeur récupération
    public Utilisateur(int id, String surnom, String pass, int role) {
        super(id);
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
    }

    @Override
    public String toString() {
        return surnom + " (" + (role == ROLE_ADMIN ? "Admin" : "Visiteur") + ")";
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into utilisateur (surnom, pass, role) values (?,?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.surnom);
        pst.setString(2, this.pass);
        pst.setInt(3, this.role);
        pst.executeUpdate();
        return pst;
    }

    /**
     * Tente de connecter un utilisateur.
     * @return Un Optional contenant l'utilisateur si identifiants corrects, vide sinon.
     */
    public static Optional<Utilisateur> login(Connection con, String surnom, String pass) throws SQLException {
        String query = "select id, role from utilisateur where surnom = ? and pass = ?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, surnom);
            pst.setString(2, pass);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return Optional.of(new Utilisateur(rs.getInt("id"), surnom, pass, rs.getInt("role")));
            } else {
                return Optional.empty();
            }
        }
    }
    
    // Getters
    public int getRole() { return role; }
    public String getSurnom() { return surnom; }
    public boolean isAdmin() { return this.role == ROLE_ADMIN; }
}