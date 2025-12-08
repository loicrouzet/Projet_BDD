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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Tournoi extends ClasseMiroir {
    
    private String nom;
    private LocalDate dateDebut;
    private Loisir leLoisir; // Le sport concerné

    // Constructeur création
    public Tournoi(String nom, LocalDate dateDebut, Loisir leLoisir) {
        super();
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.leLoisir = leLoisir;
    }

    // Constructeur récupération
    public Tournoi(int id, String nom, LocalDate dateDebut, Loisir leLoisir) {
        super(id);
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.leLoisir = leLoisir;
    }

    @Override
    public String toString() {
        return nom + " (" + leLoisir.getNom() + ") - " + dateDebut;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        // Attention : le loisir doit avoir été sauvegardé AVANT (avoir un ID)
        if (this.leLoisir.getId() == -1) {
            throw new Error("Impossible de sauvegarder le tournoi : le loisir associé n'est pas sauvegardé.");
        }
        
        PreparedStatement pst = con.prepareStatement(
            "insert into tournoi (nom, date_debut, id_loisir) values (?,?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.nom);
        pst.setDate(2, Date.valueOf(this.dateDebut));
        pst.setInt(3, this.leLoisir.getId());
        pst.executeUpdate();
        return pst;
    }
    
    public static List<Tournoi> getAll(Connection con) throws SQLException {
        List<Tournoi> res = new ArrayList<>();
        // On fait une jointure pour récupérer les infos du tournoi ET du loisir en même temps
        String query = "select t.id as t_id, t.nom as t_nom, t.date_debut, " +
                       "l.id as l_id, l.nom as l_nom, l.description " +
                       "from tournoi t join loisir l on t.id_loisir = l.id";
                       
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                // On reconstruit l'objet Loisir
                Loisir l = new Loisir(rs.getInt("l_id"), rs.getString("l_nom"), rs.getString("description"));
                // On reconstruit le Tournoi
                LocalDate date = rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null;
                res.add(new Tournoi(rs.getInt("t_id"), rs.getString("t_nom"), date, l));
            }
        }
        return res;
    }

    // Getters
    public String getNom() { return nom; }
    public Loisir getLoisir() { return leLoisir; }
}
