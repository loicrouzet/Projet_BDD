package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Joueur extends ClasseMiroir {
    
    private String nom;
    private String prenom;
    private int idEquipe;

    public Joueur(String nom, String prenom, int idEquipe) {
        super();
        this.nom = nom;
        this.prenom = prenom;
        this.idEquipe = idEquipe;
    }

    public Joueur(int id, String nom, String prenom, int idEquipe) {
        super(id);
        this.nom = nom;
        this.prenom = prenom;
        this.idEquipe = idEquipe;
    }

    @Override
    public String toString() {
        return prenom + " " + nom;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into joueur (nom, prenom, id_equipe) values (?,?,?)", 
            Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, this.nom);
        pst.setString(2, this.prenom);
        pst.setInt(3, this.idEquipe);
        pst.executeUpdate();
        return pst;
    }
    
    // NOUVELLE MÉTHODE : Suppression
    public void delete(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("delete from joueur where id=?")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }
    }
    
    public static List<Joueur> getByEquipe(Connection con, int idEquipe) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("select id, nom, prenom from joueur where id_equipe = ?")) {
            pst.setInt(1, idEquipe);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                res.add(new Joueur(rs.getInt("id"), rs.getString("nom"), rs.getString("prenom"), idEquipe));
            }
        }
        return res;
    }
    
    public static String getNomsJoueurs(Connection con, int idEquipe) {
        try {
            return getByEquipe(con, idEquipe).stream()
                    .map(Joueur::toString)
                    .collect(Collectors.joining(", "));
        } catch (SQLException ex) {
            return "Erreur chargement joueurs";
        }
    }
    
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
}