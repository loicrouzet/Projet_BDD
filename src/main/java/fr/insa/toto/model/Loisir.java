package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Loisir extends ClasseMiroir {
    
    private String nom;
    private String description;
    private int nbJoueursEquipe; // Ajouté

    public Loisir(String nom, String description, int nbJoueursEquipe) {
        super();
        this.nom = nom;
        this.description = description;
        this.nbJoueursEquipe = nbJoueursEquipe;
    }

    public Loisir(int id, String nom, String description, int nbJoueursEquipe) {
        super(id);
        this.nom = nom;
        this.description = description;
        this.nbJoueursEquipe = nbJoueursEquipe;
    }

    @Override
    public String toString() { return nom; }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into loisir (nom, description, nb_joueurs_equipe) values (?,?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.nom);
        pst.setString(2, this.description);
        pst.setInt(3, this.nbJoueursEquipe);
        pst.executeUpdate();
        return pst;
    }

    public static List<Loisir> getAll(Connection con) throws SQLException {
        List<Loisir> res = new ArrayList<>();
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("select * from loisir");
            while (rs.next()) {
                res.add(new Loisir(rs.getInt("id"), rs.getString("nom"), 
                                   rs.getString("description"), rs.getInt("nb_joueurs_equipe")));
            }
        }
        return res;
    }
    
    public String getNom() { return nom; }
    public int getNbJoueursEquipe() { return nbJoueursEquipe; }
    public String getDescription() { return description; }
}