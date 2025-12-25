/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
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