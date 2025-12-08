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
                    + " role integer default 0,"
                    + " id_club integer,"
                    + " foreign key (id_club) references club(id))");

            st.executeUpdate("create table equipe ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " id_club integer not null,"
                    + " foreign key (id_club) references club(id))");

            st.executeUpdate("create table joueur ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(50) not null,"
                    + " prenom varchar(50),"
                    + " id_equipe integer not null,"
                    + " foreign key (id_equipe) references equipe(id))");

            st.executeUpdate("create table inscription ("
                    + " id_tournoi integer not null,"
                    + " id_equipe integer not null,"
                    + " primary key (id_tournoi, id_equipe),"
                    + " foreign key (id_tournoi) references tournoi(id),"
                    + " foreign key (id_equipe) references equipe(id))");
            
            // --- Données de test ---
            
            // Utilisateurs (toto est admin mais n'a pas encore de club)
            st.executeUpdate("insert into utilisateur (surnom, pass, role) values ('toto', 'toto', 1)");
            st.executeUpdate("insert into utilisateur (surnom, pass, role) values ('invite', 'invite', 0)");
            
            // Loisirs (Sports) UNIQUEMENT
            String[][] sports = {
                {"Football", "Collectif"}, {"Tennis", "Individuel"}, {"Rugby", "Collectif"}, 
                {"Handball", "Collectif"}, {"Basketball", "Collectif"}, {"Ping Pong", "Individuel"}, 
                {"Volleyball", "Collectif"}, {"Badminton", "Individuel"}, {"Natation", "Individuel"}, 
                {"Athlétisme", "Individuel"}, {"Cyclisme", "Individuel"}, {"Boxe", "Combat"}, 
                {"Judo", "Combat"}, {"Golf", "Individuel"}, {"Escalade", "Individuel"}
            };
            for (String[] sport : sports) {
                try { st.executeUpdate("insert into loisir (nom, description) values ('" + sport[0] + "', '" + sport[1] + "')"); } catch (SQLException e) {}
            }
            
            // PLUS AUCUN CLUB NI EQUIPE NI JOUEUR PRÉDÉFINI

            System.out.println("Schéma mis à jour (Sans clubs/équipes prédéfinis) !");
        }
    }

    public static void deleteSchema(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            try { st.executeUpdate("drop table inscription"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table joueur"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table equipe"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table terrain"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table tournoi"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table utilisateur"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table club"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table loisir"); } catch (SQLException ex) {}
        }
    }

    public static void razBdd(Connection con) throws SQLException {
        deleteSchema(con);
        creeSchema(con);
    }
    
    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            razBdd(con);
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }
}