package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Equipe extends ClasseMiroir {
    
    private String nom;
    private int idTournoi;

    public Equipe(String nom, int idTournoi) {
        super();
        this.nom = nom;
        this.idTournoi = idTournoi;
    }

    public Equipe(int id, String nom, int idTournoi) {
        super(id);
        this.nom = nom;
        this.idTournoi = idTournoi;
    }

    @Override
    public String toString() { return nom; }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into equipe (nom, id_tournoi) values (?,?)", 
            Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, this.nom);
        pst.setInt(2, this.idTournoi);
        pst.executeUpdate();
        return pst;
    }

    // Méthode nécessaire pour lier l'équipe au tournoi dans la table 'inscription' (gestion des matchs/poules)
    public void inscrireATournoi(Connection con, int idTournoi) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("insert into inscription (id_tournoi, id_equipe) values (?,?)")) {
            pst.setInt(1, idTournoi);
            pst.setInt(2, this.getId());
            pst.executeUpdate();
        }
    }

    // Récupérer les équipes d'un tournoi
    public static List<Equipe> getByTournoi(Connection con, int idTournoi) throws SQLException {
        List<Equipe> res = new ArrayList<>();
        String query = "select * from equipe where id_tournoi = ?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setInt(1, idTournoi);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                res.add(new Equipe(rs.getInt("id"), rs.getString("nom"), idTournoi));
            }
        }
        return res;
    }
    
    public String getNom() { return nom; }
    public int getIdTournoi() { return idTournoi; }
}