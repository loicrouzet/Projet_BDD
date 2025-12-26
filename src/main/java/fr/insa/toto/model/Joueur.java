package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class Joueur extends ClasseMiroir {
    
    private String nom;
    private String prenom;
    private int idEquipe;
    private int idClub; // Nouveau

    public Joueur(String nom, String prenom, int idEquipe, int idClub) {
        super();
        this.nom = nom;
        this.prenom = prenom;
        this.idEquipe = idEquipe;
        this.idClub = idClub;
    }

    public Joueur(int id, String nom, String prenom, int idEquipe, int idClub) {
        super(id);
        this.nom = nom;
        this.prenom = prenom;
        this.idEquipe = idEquipe;
        this.idClub = idClub;
    }

    // Setters
    public void setIdEquipe(int idEquipe) { this.idEquipe = idEquipe; }
    
    // Update complet
    public void update(Connection con) throws SQLException {
        String sql = "update joueur set nom=?, prenom=?, id_equipe=?, id_club=? where id=?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, this.nom);
            pst.setString(2, this.prenom);
            if(this.idEquipe > 0) pst.setInt(3, this.idEquipe); else pst.setNull(3, Types.INTEGER);
            if(this.idClub > 0) pst.setInt(4, this.idClub); else pst.setNull(4, Types.INTEGER);
            pst.setInt(5, this.getId());
            pst.executeUpdate();
        }
    }

    @Override
    public String toString() { return prenom + " " + nom; }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into joueur (nom, prenom, id_equipe, id_club) values (?,?,?,?)", 
            Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, this.nom);
        pst.setString(2, this.prenom);
        if(this.idEquipe > 0) pst.setInt(3, this.idEquipe); else pst.setNull(3, Types.INTEGER);
        pst.setInt(4, this.idClub);
        pst.executeUpdate();
        return pst;
    }
    
    public void delete(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("delete from joueur where id=?")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }
    }

    // --- REQUETES ---

    public static List<Joueur> getByClub(Connection con, int idClub) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        String sql = "SELECT * FROM joueur WHERE id_club = ?"; 
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idClub);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                res.add(new Joueur(rs.getInt("id"), rs.getString("nom"), rs.getString("prenom"), rs.getInt("id_equipe"), idClub));
            }
        }
        return res;
    }
    
    public static List<Joueur> getInscritsAuTournoi(Connection con, int idTournoi) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        String sql = "SELECT j.* FROM joueur j JOIN inscription_joueur i ON j.id = i.id_joueur WHERE i.id_tournoi = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                res.add(new Joueur(rs.getInt("id"), rs.getString("nom"), rs.getString("prenom"), rs.getInt("id_equipe"), rs.getInt("id_club")));
            }
        }
        return res;
    }

    // Autres méthodes utilitaires...
    public void inscrireAuTournoi(Connection con, int idTournoi) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("insert into inscription_joueur (id_tournoi, id_joueur) values (?, ?)")) {
            pst.setInt(1, idTournoi); pst.setInt(2, this.getId()); pst.executeUpdate();
        }
    }
    public void desinscrireDuTournoi(Connection con, int idTournoi) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("delete from inscription_joueur where id_tournoi=? and id_joueur=?")) {
            pst.setInt(1, idTournoi); pst.setInt(2, this.getId()); pst.executeUpdate();
        }
    }
    
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
}