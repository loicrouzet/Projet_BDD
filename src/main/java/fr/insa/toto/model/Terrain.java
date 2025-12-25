package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Terrain extends ClasseMiroir {
    private String nom;
    private boolean estInterieur;
    private boolean sousConstruction;
    private int idClub;

    public Terrain(int id, String nom, boolean estInterieur, boolean sousConstruction, int idClub) {
        super(id);
        this.nom = nom;
        this.estInterieur = estInterieur;
        this.sousConstruction = sousConstruction;
        this.idClub = idClub;
    }

    public Terrain(String nom, boolean estInterieur, int idClub) {
        super();
        this.nom = nom;
        this.estInterieur = estInterieur;
        this.sousConstruction = false;
        this.idClub = idClub;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into terrain (nom, est_interieur, sous_construction, id_club) values (?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.nom);
        pst.setBoolean(2, this.estInterieur);
        pst.setBoolean(3, this.sousConstruction);
        pst.setInt(4, this.idClub);
        pst.executeUpdate();
        return pst;
    }

    // Cette méthode permet de sauvegarder les changements d'état
    public void updateEtat(Connection con) throws SQLException {
        String sql = "update terrain set nom=?, est_interieur=?, sous_construction = ? where id = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, this.nom);
            pst.setBoolean(2, this.estInterieur);
            pst.setBoolean(3, this.sousConstruction);
            pst.setInt(4, this.getId());
            pst.executeUpdate();
        }
    }

    public static List<Terrain> getByClub(Connection con, int idClub) throws SQLException {
        List<Terrain> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("select * from terrain where id_club = ?")) {
            pst.setInt(1, idClub);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                res.add(new Terrain(rs.getInt("id"), rs.getString("nom"), 
                        rs.getBoolean("est_interieur"), rs.getBoolean("sous_construction"), rs.getInt("id_club")));
            }
        }
        return res;
    }

    public void delete(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("delete from terrain where id = ?")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }
    }

    // Getters et Setters
    public String getNom() { return nom; }
    public boolean isEstInterieur() { return estInterieur; }
    public boolean isSousConstruction() { return sousConstruction; }
    public void setSousConstruction(boolean sousConstruction) { this.sousConstruction = sousConstruction; }
}