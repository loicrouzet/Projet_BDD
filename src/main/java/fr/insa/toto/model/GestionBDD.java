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
    // On désactive l'auto-commit pour gérer les transactions si besoin, 
    // mais ici on fait simple commande par commande.
    con.setAutoCommit(true);

    try (Statement st = con.createStatement()) {
        // 1. Table LOISIR (Le sport)
        st.executeUpdate("create table loisir ("
                + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                + " nom varchar(50) not null unique,"
                + " description varchar(255)"
                + ")");

        // 2. Table CLUB
        st.executeUpdate("create table club ("
                + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                + " nom varchar(100) not null unique"
                + ")");

        // 3. Table TOURNOI (Lié à un Loisir)
        st.executeUpdate("create table tournoi ("
                + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                + " nom varchar(100) not null,"
                + " date_debut date,"
                + " id_loisir integer not null,"
                + " foreign key (id_loisir) references loisir(id)"
                + ")");
                
        // 4. Table UTILISATEUR (Déjà présente dans ton code, je la laisse)
        st.executeUpdate("create table utilisateur ("
                + " id integer not null primary key,"
                + " surnom varchar(30) not null unique,"
                + " pass varchar(20)"
                + ")");
                
        System.out.println("Schema multisport cree avec succes !");
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
