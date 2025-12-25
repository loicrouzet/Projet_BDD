package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class GestionBDD {

    public static void creeSchema(Connection con) throws SQLException {
        con.setAutoCommit(true);

        try (Statement st = con.createStatement()) {
            // 1. Loisir
            st.executeUpdate("create table loisir ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(50) not null unique,"
                    + " description varchar(255))");

            // 2. Club (avec nouvelles colonnes)
            st.executeUpdate("create table club ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null unique,"
                    + " adresse varchar(255),"
                    + " effectif_manuel integer default 0)");

            // 3. Terrain
            st.executeUpdate("create table terrain ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " est_interieur boolean not null,"
                    + " id_club integer not null,"
                    + " foreign key (id_club) references club(id))");

            // 4. Tournoi
            st.executeUpdate("create table tournoi ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " date_debut date,"
                    + " id_loisir integer not null,"
                    + " id_club integer not null,"
                    + " pts_victoire integer default 3,"
                    + " pts_nul integer default 1,"
                    + " pts_defaite integer default 0," 
                    + " foreign key (id_loisir) references loisir(id),"
                    + " foreign key (id_club) references club(id))");

            // 5. Utilisateur (avec nouvelles colonnes)
            // Dans GestionBDD.java, remplacez la création de la table utilisateur par :
st.executeUpdate("create table utilisateur ("
    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
    + " surnom varchar(30) not null unique,"
    + " pass varchar(20),"
    + " role integer default 0,"
    + " id_club integer,"
    + " email varchar(100),"
    + " date_naissance date,"
    + " sexe varchar(20),"
    + " photo_url LONGTEXT,"
    + " infos_sup LONGTEXT," // <-- NOUVELLE COLONNE
    + " info_valide boolean default false,"
    + " nouvelles_infos_pendant boolean default false,"
    + " message_admin varchar(255),"
    + " foreign key (id_club) references club(id))");
            // 6. Equipe
            st.executeUpdate("create table equipe ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " id_club integer not null,"
                    + " foreign key (id_club) references club(id))");

            // 7. Joueur
            st.executeUpdate("create table joueur ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(50) not null,"
                    + " prenom varchar(50),"
                    + " id_equipe integer not null,"
                    + " foreign key (id_equipe) references equipe(id))");

            // 8. Inscription
            st.executeUpdate("create table inscription ("
                    + " id_tournoi integer not null,"
                    + " id_equipe integer not null,"
                    + " poule varchar(20)," 
                    + " primary key (id_tournoi, id_equipe),"
                    + " foreign key (id_tournoi) references tournoi(id),"
                    + " foreign key (id_equipe) references equipe(id))");
            
            // 9. Ronde
            st.executeUpdate("create table ronde ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " type_ronde integer not null," // 0=Basique, 1=Poule, 2=Phase Finale
                    + " id_tournoi integer not null,"
                    + " foreign key (id_tournoi) references tournoi(id))");

            // 10. Match
            st.executeUpdate("create table match_tournoi ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " id_tournoi integer not null,"
                    + " id_ronde integer,"
                    + " id_equipe1 integer,"
                    + " id_equipe2 integer,"
                    + " label varchar(50),"
                    + " score1 integer default 0,"
                    + " score2 integer default 0,"
                    + " est_joue boolean default false,"
                    + " date_heure timestamp,"
                    + " foreign key (id_tournoi) references tournoi(id),"
                    + " foreign key (id_ronde) references ronde(id),"
                    + " foreign key (id_equipe1) references equipe(id),"
                    + " foreign key (id_equipe2) references equipe(id))");
            
            // --- INSERTIONS INITIALES (UNE SEULE FOIS) ---
            st.executeUpdate("insert into utilisateur (surnom, pass, role) values ('toto', 'toto', 1)");
            st.executeUpdate("insert into utilisateur (surnom, pass, role) values ('invite', 'invite', 0)");
            
            String[][] sports = {
                {"Football", "Collectif"}, {"Tennis", "Individuel"}, {"Rugby", "Collectif"}, 
                {"Handball", "Collectif"}, {"Basketball", "Collectif"}, {"Ping Pong", "Individuel"}, 
                {"Volleyball", "Collectif"}, {"Badminton", "Individuel"}, {"Natation", "Individuel"}
            };
            for (String[] sport : sports) {
                try { st.executeUpdate("insert into loisir (nom, description) values ('" + sport[0] + "', '" + sport[1] + "')"); } catch (SQLException e) {}
            }

            System.out.println("Schéma initialisé avec succès !");
        }
    }

    public static void deleteSchema(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            String[] tables = {"match_tournoi", "ronde", "inscription", "joueur", "equipe", "utilisateur", "tournoi", "terrain", "club", "loisir"};
            for (String table : tables) {
                try { st.executeUpdate("drop table " + table); } catch (SQLException ex) {}
            }
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
            ex.printStackTrace();
        }
    }
}