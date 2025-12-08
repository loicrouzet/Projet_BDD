package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Terrain extends ClasseMiroir {
    
    private String nom;
    private boolean estInterieur; // true = Intérieur (Salle), false = Extérieur
    private int idClub;

    public Terrain(String nom, boolean estInterieur, int idClub) {
        super();
        this.nom = nom;
        this.estInterieur = estInterieur;
        this.idClub = idClub;
    }

    public Terrain(int id, String nom, boolean estInterieur, int idClub) {
        super(id);
        this.nom = nom;
        this.estInterieur = estInterieur;
        this.idClub = idClub;
    }

    @Override
    public String toString() {
        return nom + " (" + (estInterieur ? "Intérieur" : "Extérieur") + ")";
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into terrain (nom, est_interieur, id_club) values (?,?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.nom);
        pst.setBoolean(2, this.estInterieur);
        pst.setInt(3, this.idClub);
        pst.executeUpdate();
        return pst;
    }
    
    // Récupérer les terrains d'un club spécifique
    public static List<Terrain> getByClub(Connection con, int idClub) throws SQLException {
        List<Terrain> res = new ArrayList<>();
        String query = "select id, nom, est_interieur from terrain where id_club = ?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setInt(1, idClub);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                res.add(new Terrain(rs.getInt("id"), rs.getString("nom"), rs.getBoolean("est_interieur"), idClub));
            }
        }
        return res;
    }

    // Getters
    public String getNom() { return nom; }
    public boolean isEstInterieur() { return estInterieur; }
}
