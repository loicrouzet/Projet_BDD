package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class GestionBDD {

    public static void creeSchema(Connection con) throws SQLException {
        con.setAutoCommit(true);

        try (Statement st = con.createStatement()) {
            
            // --- 1. TABLES INDÉPENDANTES ---
            st.executeUpdate("create table loisir ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(50) not null unique,"
                    + " nb_joueurs_equipe integer default 1,"
                    + " description varchar(255))");

            st.executeUpdate("create table club ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null unique,"
                    + " adresse varchar(255),"
                    + " logo_url LONGTEXT,"
                    + " description LONGTEXT,"
                    + " email varchar(100),"
                    + " telephone varchar(20),"
                    + " annee_creation integer,"   
                    + " instagram varchar(100),"
                    + " facebook varchar(100),"    
                    + " twitter varchar(100),"     
                    + " effectif_manuel integer default 0)");

            // --- 2. UTILISATEUR & TERRAIN ---
            st.executeUpdate("create table utilisateur ("
                + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                + " identifiant varchar(30) not null unique,"
                + " surnom varchar(30),"
                + " nom varchar(50) not null,"
                + " prenom varchar(50) not null,"
                + " pass varchar(20),"
                + " role integer default 0,"
                + " id_club integer,"
                + " email varchar(100),"
                + " date_naissance date,"
                + " photo_url LONGTEXT,"
                + " infos_sup LONGTEXT,"
                + " info_valide boolean default false,"
                + " nouvelles_infos_pendant boolean default false,"
                + " message_admin varchar(255),"
                + " foreign key (id_club) references club(id))");

            st.executeUpdate("create table terrain ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " est_interieur boolean not null,"
                    + " sous_construction boolean default false,"
                    + " id_club integer not null,"
                    + " foreign key (id_club) references club(id))");

            // --- 3. TOURNOI & ÉQUIPE ---
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

            st.executeUpdate("create table equipe ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " id_tournoi integer not null,"
                    + " foreign key (id_tournoi) references tournoi(id))");

            // --- 4. JOUEUR ---
            st.executeUpdate("create table joueur ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(50) not null,"
                    + " prenom varchar(50),"
                    + " date_naissance date,"       
                    + " instagram varchar(100),"    
                    + " facebook varchar(100),"     
                    + " twitter varchar(100),"      
                    + " id_equipe integer,"
                    + " id_club integer,"
                    + " id_utilisateur integer," 
                    + " foreign key (id_equipe) references equipe(id),"
                    + " foreign key (id_club) references club(id),"
                    + " foreign key (id_utilisateur) references utilisateur(id))");

            // --- 5. TABLES DE JEU ---
             st.executeUpdate("create table inscription ("
                    + " id_tournoi integer not null,"
                    + " id_equipe integer not null,"
                    + " poule varchar(20)," 
                    + " primary key (id_tournoi, id_equipe),"
                    + " foreign key (id_tournoi) references tournoi(id),"
                    + " foreign key (id_equipe) references equipe(id))");
            
            st.executeUpdate("create table ronde ("
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null,"
                    + " type_ronde integer not null,"
                    + " id_tournoi integer not null,"
                    + " foreign key (id_tournoi) references tournoi(id))");

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

            st.executeUpdate("create table inscription_joueur ("
                    + " id_tournoi integer not null,"
                    + " id_joueur integer not null,"
                    + " primary key (id_tournoi, id_joueur),"
                    + " foreign key (id_tournoi) references tournoi(id),"
                    + " foreign key (id_joueur) references joueur(id))");

            // DONNÉES DE BASE
            st.executeUpdate("insert into utilisateur (identifiant, surnom, nom, prenom, pass, role) values ('toto', 'Le Boss', 'Admin', 'Toto', 'toto', 1)");
            st.executeUpdate("insert into utilisateur (identifiant, surnom, nom, prenom, pass, role) values ('invite', 'Visiteur', 'User', 'Invite', 'invite', 0)");
            
            String[][] sports = {{"Football", "Collectif", "11"}, {"Tennis", "Individuel", "1"}, {"Rugby", "Collectif", "15"}, {"Handball", "Collectif", "7"}, {"Basketball", "Collectif", "5"}, {"Ping Pong", "Individuel", "1"}, {"Volleyball", "Collectif", "6"}, {"Badminton", "Individuel", "1"}, {"Natation", "Individuel", "1"}};
            for (String[] sport : sports) { try { st.executeUpdate("insert into loisir (nom, description, nb_joueurs_equipe) values ('" + sport[0] + "', '" + sport[1] + "', " + sport[2] + ")"); } catch (SQLException e) { } }
            
            // 1. Création des 4 Clubs
            // Comme on vient de recréer la table, ils auront les IDs 1, 2, 3 et 4
            String[] nomsClubs = {"Paris Saint-Germain", "Olympique de Marseille", "Olympique Lyonnais", "RC Lens"};
            for (String nc : nomsClubs) {
                // On échappe les apostrophes simples par sécurité (ex: d'Artagnan -> d''Artagnan)
                st.executeUpdate("insert into club (nom) values ('" + nc.replace("'", "''") + "')");
            }

            // 2. Création de 50 Joueurs répartis
            String[] prenoms = {"Jean", "Pierre", "Paul", "Jacques", "Lucas", "Léo", "Louis", "Gabriel"};
            
            for (int i = 0; i < 50; i++) {
                String nomJoueur = "Joueur" + (i + 1);
                String prenomJoueur = prenoms[i % prenoms.length];
                
                // Répartition cyclique : Joueur 1 -> Club 1, Joueur 2 -> Club 2, ..., Joueur 5 -> Club 1
                int idClub = (i % 4) + 1; 

                // Insertion SQL directe
                st.executeUpdate("insert into joueur (nom, prenom, id_club) values ('" 
                        + nomJoueur + "', '" 
                        + prenomJoueur + "', " 
                        + idClub + ")");
            }
            
            System.out.println("Schéma complet créé.");
        }
    }

    public static void deleteSchema(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            String[] tables = {"inscription_joueur", "match_tournoi", "ronde", "inscription", "joueur", "equipe", "utilisateur", "tournoi", "terrain", "club", "loisir"};
            for (String table : tables) { try { st.executeUpdate("drop table " + table); } catch (SQLException ex) {} }
        }
    }
    public static void razBdd(Connection con) throws SQLException { deleteSchema(con); creeSchema(con); }
}