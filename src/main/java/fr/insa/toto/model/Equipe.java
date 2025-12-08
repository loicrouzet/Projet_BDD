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
    private int idClub;
    private String nomClub; // Champ utilitaire

    public Equipe(String nom, int idClub) {
        super();
        this.nom = nom;
        this.idClub = idClub;
        this.nomClub = "";
    }

    public Equipe(int id, String nom, int idClub, String nomClub) {
        super(id);
        this.nom = nom;
        this.idClub = idClub;
        this.nomClub = nomClub;
    }
    
    @Override
    public String toString() {
        return nom + " (" + nomClub + ")";
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement("insert into equipe (nom, id_club) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, this.nom);
        pst.setInt(2, this.idClub);
        pst.executeUpdate();
        return pst;
    }
    
    // NOUVELLE MÉTHODE : Mise à jour
    public void update(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("update equipe set nom=? where id=?")) {
            pst.setString(1, this.nom);
            pst.setInt(2, this.getId());
            pst.executeUpdate();
        }
    }
    
    // NOUVELLE MÉTHODE : Suppression complète (cascade manuelle)
    public void delete(Connection con) throws SQLException {
        // 1. Supprimer les inscriptions aux tournois
        try (PreparedStatement pst = con.prepareStatement("delete from inscription where id_equipe=?")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }
        // 2. Supprimer les joueurs
        try (PreparedStatement pst = con.prepareStatement("delete from joueur where id_equipe=?")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }
        // 3. Supprimer l'équipe
        try (PreparedStatement pst = con.prepareStatement("delete from equipe where id=?")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }
    }
    
    // NOUVELLE MÉTHODE : Récupérer les équipes d'un club
    public static List<Equipe> getByClub(Connection con, int idClub) throws SQLException {
        List<Equipe> res = new ArrayList<>();
        String sql = "select e.id, e.nom from equipe e where e.id_club = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idClub);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                res.add(new Equipe(
                    rs.getInt("id"), 
                    rs.getString("nom"), 
                    idClub,
                    "" // On connait déjà le club
                ));
            }
        }
        return res;
    }
    
    public static List<Equipe> getAll(Connection con) throws SQLException {
        List<Equipe> res = new ArrayList<>();
        String sql = "select e.id, e.nom, e.id_club, c.nom as nom_club from equipe e join club c on e.id_club = c.id";
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                res.add(new Equipe(rs.getInt("id"), rs.getString("nom"), rs.getInt("id_club"), rs.getString("nom_club")));
            }
        }
        return res;
    }
    
    public void inscrireATournoi(Connection con, int idTournoi) throws SQLException {
        if (this.getId() == -1) throw new Error("Sauvegardez l'équipe d'abord");
        String sql = "insert into inscription (id_tournoi, id_equipe) values (?, ?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            pst.setInt(2, this.getId());
            pst.executeUpdate();
        }
    }
    
    public void desinscrireDuTournoi(Connection con, int idTournoi) throws SQLException {
         String sql = "delete from inscription where id_tournoi=? and id_equipe=?";
         try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            pst.setInt(2, this.getId());
            pst.executeUpdate();
        }
    }
    
    public static List<Equipe> getByTournoi(Connection con, int idTournoi) throws SQLException {
        List<Equipe> res = new ArrayList<>();
        String sql = "select e.id, e.nom, e.id_club, c.nom as nom_club from equipe e join inscription i on e.id = i.id_equipe join club c on e.id_club = c.id where i.id_tournoi = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                res.add(new Equipe(rs.getInt("id"), rs.getString("nom"), rs.getInt("id_club"), rs.getString("nom_club")));
            }
        }
        return res;
    }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getIdClub() { return idClub; }
    public String getNomClub() { return nomClub; }
}
