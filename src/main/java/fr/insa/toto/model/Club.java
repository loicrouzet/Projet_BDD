package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Club extends ClasseMiroir {
    
    private String nom;

    public Club(String nom) {
        super();
        this.nom = nom;
    }

    public Club(int id, String nom) {
        super(id);
        this.nom = nom;
    }

    @Override
    public String toString() {
        return nom;
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
    
    public static List<Club> getAll(Connection con) throws SQLException {
        List<Club> res = new ArrayList<>();
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("select id, nom from club");
            while (rs.next()) {
                res.add(new Club(rs.getInt("id"), rs.getString("nom")));
            }
        }
        return res;
    }
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
}