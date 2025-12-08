package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Match extends ClasseMiroir {
    
    private int idTournoi;
    private Equipe equipe1;
    private Equipe equipe2;
    private int score1;
    private int score2;
    private boolean estJoue;
    private LocalDateTime dateHeure;

    // Constructeur création
    public Match(int idTournoi, Equipe equipe1, Equipe equipe2, LocalDateTime dateHeure) {
        super();
        this.idTournoi = idTournoi;
        this.equipe1 = equipe1;
        this.equipe2 = equipe2;
        this.score1 = 0;
        this.score2 = 0;
        this.estJoue = false;
        this.dateHeure = dateHeure;
    }

    // Constructeur complet (lecture BDD)
    public Match(int id, int idTournoi, Equipe equipe1, Equipe equipe2, int score1, int score2, boolean estJoue, LocalDateTime dateHeure) {
        super(id);
        this.idTournoi = idTournoi;
        this.equipe1 = equipe1;
        this.equipe2 = equipe2;
        this.score1 = score1;
        this.score2 = score2;
        this.estJoue = estJoue;
        this.dateHeure = dateHeure;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into match_tournoi (id_tournoi, id_equipe1, id_equipe2, score1, score2, est_joue, date_heure) values (?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setInt(1, idTournoi);
        pst.setInt(2, equipe1.getId());
        pst.setInt(3, equipe2.getId());
        pst.setInt(4, score1);
        pst.setInt(5, score2);
        pst.setBoolean(6, estJoue);
        pst.setTimestamp(7, dateHeure != null ? Timestamp.valueOf(dateHeure) : null);
        pst.executeUpdate();
        return pst;
    }
    
    public void updateScore(Connection con, int s1, int s2, boolean fini) throws SQLException {
        try(PreparedStatement pst = con.prepareStatement("update match_tournoi set score1=?, score2=?, est_joue=? where id=?")) {
            pst.setInt(1, s1);
            pst.setInt(2, s2);
            pst.setBoolean(3, fini);
            pst.setInt(4, this.getId());
            pst.executeUpdate();
            this.score1 = s1;
            this.score2 = s2;
            this.estJoue = fini;
        }
    }
    
    public static List<Match> getByTournoi(Connection con, int idTournoi) throws SQLException {
        List<Match> res = new ArrayList<>();
        // On a besoin des noms des équipes pour l'affichage, donc on joint la table equipe 2 fois
        String sql = "select m.*, e1.nom as nom1, e1.id_club as club1, e2.nom as nom2, e2.id_club as club2 " +
                     "from match_tournoi m " +
                     "join equipe e1 on m.id_equipe1 = e1.id " +
                     "join equipe e2 on m.id_equipe2 = e2.id " +
                     "where m.id_tournoi = ? order by m.date_heure asc";
        
        try(PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                Equipe eq1 = new Equipe(rs.getInt("id_equipe1"), rs.getString("nom1"), rs.getInt("club1"), "");
                Equipe eq2 = new Equipe(rs.getInt("id_equipe2"), rs.getString("nom2"), rs.getInt("club2"), "");
                LocalDateTime dt = rs.getTimestamp("date_heure") != null ? rs.getTimestamp("date_heure").toLocalDateTime() : null;
                
                res.add(new Match(rs.getInt("id"), idTournoi, eq1, eq2, 
                        rs.getInt("score1"), rs.getInt("score2"), rs.getBoolean("est_joue"), dt));
            }
        }
        return res;
    }

    // Getters
    public Equipe getEquipe1() { return equipe1; }
    public Equipe getEquipe2() { return equipe2; }
    public int getScore1() { return score1; }
    public int getScore2() { return score2; }
    public boolean isEstJoue() { return estJoue; }
    public LocalDateTime getDateHeure() { return dateHeure; }
}
