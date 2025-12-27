package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Match extends ClasseMiroir {
    
    private int idTournoi;
    private int idRonde;
    private Equipe equipe1;
    private Equipe equipe2;
    private String label;
    private int score1;
    private int score2;
    private boolean estJoue;
    private LocalDateTime dateHeure;

    public Match(int idTournoi, int idRonde, Equipe equipe1, Equipe equipe2, String label, LocalDateTime dateHeure) {
        super();
        this.idTournoi = idTournoi;
        this.idRonde = idRonde;
        this.equipe1 = equipe1;
        this.equipe2 = equipe2;
        this.label = label;
        this.score1 = 0;
        this.score2 = 0;
        this.estJoue = false;
        this.dateHeure = dateHeure;
    }

    public Match(int id, int idTournoi, int idRonde, Equipe equipe1, Equipe equipe2, String label, int score1, int score2, boolean estJoue, LocalDateTime dateHeure) {
        super(id);
        this.idTournoi = idTournoi;
        this.idRonde = idRonde;
        this.equipe1 = equipe1;
        this.equipe2 = equipe2;
        this.label = label;
        this.score1 = score1;
        this.score2 = score2;
        this.estJoue = estJoue;
        this.dateHeure = dateHeure;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into match_tournoi (id_tournoi, id_ronde, id_equipe1, id_equipe2, label, score1, score2, est_joue, date_heure) values (?,?,?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setInt(1, idTournoi);
        pst.setInt(2, idRonde);
        
        if (equipe1 != null) pst.setInt(3, equipe1.getId()); else pst.setNull(3, Types.INTEGER);
        if (equipe2 != null) pst.setInt(4, equipe2.getId()); else pst.setNull(4, Types.INTEGER);
        
        pst.setString(5, label);
        pst.setInt(6, score1);
        pst.setInt(7, score2);
        pst.setBoolean(8, estJoue);
        pst.setTimestamp(9, dateHeure != null ? Timestamp.valueOf(dateHeure) : null);
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
    
    // --- MODIFICATION ICI ---
    public static List<Match> getByRonde(Connection con, int idRonde) throws SQLException {
        List<Match> res = new ArrayList<>();
        // On récupère id_tournoi de l'équipe (anciennement id_club)
        String sql = "select m.*, " +
                     "e1.nom as nom1, e1.id_tournoi as t1, " +
                     "e2.nom as nom2, e2.id_tournoi as t2 " +
                     "from match_tournoi m " +
                     "left join equipe e1 on m.id_equipe1 = e1.id " +
                     "left join equipe e2 on m.id_equipe2 = e2.id " +
                     "where m.id_ronde = ? order by m.date_heure asc, m.id asc";
        
        try(PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idRonde);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                Equipe eq1 = null;
                if (rs.getObject("id_equipe1") != null) {
                    // Nouveau constructeur Equipe(id, nom, idTournoi)
                    eq1 = new Equipe(rs.getInt("id_equipe1"), rs.getString("nom1"), rs.getInt("t1"));
                }
                Equipe eq2 = null;
                if (rs.getObject("id_equipe2") != null) {
                    eq2 = new Equipe(rs.getInt("id_equipe2"), rs.getString("nom2"), rs.getInt("t2"));
                }
                LocalDateTime dt = rs.getTimestamp("date_heure") != null ? rs.getTimestamp("date_heure").toLocalDateTime() : null;
                
                res.add(new Match(rs.getInt("id"), rs.getInt("id_tournoi"), idRonde, eq1, eq2, 
                        rs.getString("label"), rs.getInt("score1"), rs.getInt("score2"), rs.getBoolean("est_joue"), dt));
            }
        }
        return res;
    }

    public Equipe getEquipe1() { return equipe1; }
    public Equipe getEquipe2() { return equipe2; }
    public int getScore1() { return score1; }
    public int getScore2() { return score2; }
    public boolean isEstJoue() { return estJoue; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    public String getLabel() { return label; }
    public void setEquipe1(Equipe e) { this.equipe1 = e; } 
    public void setEquipe2(Equipe e) { this.equipe2 = e; }
}