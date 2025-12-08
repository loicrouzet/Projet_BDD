/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is ecole of CoursBeuvron.

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


import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 *
 * @author francois
 */
public class GestionBDD {

    public static void creeSchema(Connection con) throws SQLException {
        con.setAutoCommit(true);

        try (Statement st = con.createStatement()) {
            // 1. Tables existantes (Loisir, Club, Tournoi)
            // ... (Tu peux garder ton code précédent pour ces 3 tables) ...
            st.executeUpdate("create table loisir ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(50) not null unique,"
                    + " description varchar(255))");

            st.executeUpdate("create table club ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null unique)");

            st.executeUpdate("create table tournoi ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " date_debut date,"
                    + " id_loisir integer not null,"
                    + " foreign key (id_loisir) references loisir(id))");

            // 2. Table UTILISATEUR mise à jour (avec colonne 'role')
            st.executeUpdate("create table utilisateur ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " surnom varchar(30) not null unique,"
                    + " pass varchar(20),"
                    + " role integer default 0" // 0: Visiteur, 1: Admin
                    + ")");
            
            // 3. Création des utilisateurs par défaut
            // Admin (toto / toto)
            st.executeUpdate("insert into utilisateur (surnom, pass, role) values ('toto', 'toto', 1)");
            // Visiteur (invite / invite)
            st.executeUpdate("insert into utilisateur (surnom, pass, role) values ('invite', 'invite', 0)");

            System.out.println("Schema multisport mis a jour avec gestion des roles !");
        }
    }

public static void deleteSchema(Connection con) throws SQLException {
    try (Statement st = con.createStatement()) {
        // L'ordre est important à cause des clés étrangères
        try { st.executeUpdate("drop table tournoi"); } catch (SQLException ex) {}
        try { st.executeUpdate("drop table club"); } catch (SQLException ex) {}
        try { st.executeUpdate("drop table loisir"); } catch (SQLException ex) {}
        try { st.executeUpdate("drop table utilisateur"); } catch (SQLException ex) {}
    }
}

    /**
     *
     * @param con
     * @throws SQLException
     */
    public static void razBdd(Connection con) throws SQLException {
        deleteSchema(con);
        creeSchema(con);
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
                    // Exemple d'utilisation à mettre dans un main ou un test
        GestionBDD.razBdd(con); // Attention, ça efface tout !

// 1. Créer des sports
        Loisir foot = new Loisir("Football", "Sport collectif 11 vs 11");
        foot.saveInDB(con); // Donne un ID à foot

        Loisir tennis = new Loisir("Tennis", "Raquette et balle jaune");
        tennis.saveInDB(con);

// 2. Créer un tournoi
        Tournoi t1 = new Tournoi("Grand Chelem Insalien", LocalDate.now(), tennis);
        t1.saveInDB(con);

        System.out.println("Tournoi cree avec l'ID : " + t1.getId());
            
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }

}
