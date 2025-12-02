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

    // Constructeur pour création (id = -1)
    public Loisir(String nom, String description) {
        super(); // id = -1 par défaut
        this.nom = nom;
        this.description = description;
    }

    // Constructeur pour récupération depuis la BDD
    public Loisir(int id, String nom, String description) {
        super(id);
        this.nom = nom;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Sport : " + nom + " (" + description + ")";
    }

    // --- Méthodes de ClasseMiroir ---

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        // On prépare la requête pour insérer et récupérer la clé générée
        PreparedStatement pst = con.prepareStatement(
            "insert into loisir (nom, description) values (?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.nom);
        pst.setString(2, this.description);
        pst.executeUpdate();
        return pst;
    }

    // --- Méthodes statiques utilitaires ---

    public static List<Loisir> getAll(Connection con) throws SQLException {
        List<Loisir> res = new ArrayList<>();
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("select id, nom, description from loisir");
            while (rs.next()) {
                res.add(new Loisir(rs.getInt("id"), rs.getString("nom"), rs.getString("description")));
            }
        }
        return res;
    }
    
    // --- Getters et Setters ---
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
