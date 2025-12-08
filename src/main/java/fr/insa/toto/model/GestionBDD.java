package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class GestionBDD {

    public static void creeSchema(Connection con) throws SQLException {
        con.setAutoCommit(true);

        try (Statement st = con.createStatement()) {
            // 1. Tables de base
            st.executeUpdate("create table loisir ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(50) not null unique,"
                    + " description varchar(255))");

            st.executeUpdate("create table club ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null unique)");

            // 2. Nouvelle table TERRAIN
            st.executeUpdate("create table terrain ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " est_interieur boolean not null,"
                    + " id_club integer not null,"
                    + " foreign key (id_club) references club(id))");

            st.executeUpdate("create table tournoi ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " date_debut date,"
                    + " id_loisir integer not null,"
                    + " id_club integer not null,"
                    + " foreign key (id_loisir) references loisir(id),"
                    + " foreign key (id_club) references club(id))");

            st.executeUpdate("create table utilisateur ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " surnom varchar(30) not null unique,"
                    + " pass varchar(20),"
                    + " role integer default 0)");
            
            // 3. Données de test (Utilisateurs et Loisirs seulement)
            st.executeUpdate("insert into utilisateur (surnom, pass, role) values ('toto', 'toto', 1)");
            st.executeUpdate("insert into utilisateur (surnom, pass, role) values ('invite', 'invite', 0)");
            
            // J'ai RETIRÉ la création automatique des clubs ici !
            
            // Loisirs
            Loisir foot = new Loisir("Football", "Collectif");
            foot.saveInDB(con);
            Loisir tennis = new Loisir("Tennis", "Individuel");
            tennis.saveInDB(con);

            System.out.println("Schéma mis à jour avec Terrains et sans Clubs par défaut !");
        }
    }

    public static void deleteSchema(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            // L'ordre est CRUCIAL pour éviter les erreurs de clé étrangère
            try { st.executeUpdate("drop table terrain"); } catch (SQLException ex) {} // <--- CETTE LIGNE EST ESSENTIELLE
            try { st.executeUpdate("drop table tournoi"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table club"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table loisir"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table utilisateur"); } catch (SQLException ex) {}
        }
    }

    public static void razBdd(Connection con) throws SQLException {
        deleteSchema(con);
        creeSchema(con);
    }

    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            // Cette méthode supprime tout, recrée les tables ET insère déjà les données de test (Foot et Tennis)
            razBdd(con);
            
            // SUPPRIMEZ OU COMMENTEZ LES LIGNES CI-DESSOUS CAR ELLES SONT DÉJÀ DANS creeSchema
            /*
            Loisir foot = new Loisir("Football", "Collectif");
            foot.saveInDB(con);
            Loisir tennis = new Loisir("Tennis", "Individuel");
            tennis.saveInDB(con);
            */
            
            System.out.println("Base de données réinitialisée avec succès !");
            
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }
}