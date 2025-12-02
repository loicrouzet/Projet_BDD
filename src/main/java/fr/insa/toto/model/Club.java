package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Club extends ClasseMiroir {
    
    private String nom;

    // Constructeur création
    public Club(String nom) {
        super();
        this.nom = nom;
    }

    // Constructeur récupération
    public Club(int id, String nom) {
        super(id);
        this.nom = nom;
    }

    @Override
    public String toString() {
        return "Club : " + nom;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into club (nom) values (?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.nom);
        pst.executeUpdate();
        return pst;
    }
    
    // Getters / Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
}