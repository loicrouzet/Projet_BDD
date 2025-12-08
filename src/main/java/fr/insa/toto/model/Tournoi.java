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
import java.util.Optional;

public class Tournoi extends ClasseMiroir {
    
    private String nom;
    private LocalDate dateDebut;
    private Loisir leLoisir;
    private Club leClub;
    
    // Nouveaux champs
    private int ptsVictoire = 3;
    private int ptsNul = 1;
    private int ptsDefaite = 0;

    public Tournoi(String nom, LocalDate dateDebut, Loisir leLoisir, Club leClub) {
        super();
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.leLoisir = leLoisir;
        this.leClub = leClub;
    }

    public Tournoi(int id, String nom, LocalDate dateDebut, Loisir leLoisir, Club leClub, int pv, int pn, int pd) {
        super(id);
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.leLoisir = leLoisir;
        this.leClub = leClub;
        this.ptsVictoire = pv;
        this.ptsNul = pn;
        this.ptsDefaite = pd;
    }

    @Override
    public String toString() { return nom; }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into tournoi (nom, date_debut, id_loisir, id_club, pts_victoire, pts_nul, pts_defaite) values (?,?,?,?,?,?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.nom);
        pst.setDate(2, Date.valueOf(this.dateDebut));
        pst.setInt(3, this.leLoisir.getId());
        pst.setInt(4, this.leClub.getId());
        pst.setInt(5, this.ptsVictoire);
        pst.setInt(6, this.ptsNul);
        pst.setInt(7, this.ptsDefaite);
        pst.executeUpdate();
        return pst;
    }
    
    public void update(Connection con) throws SQLException {
        String query = "update tournoi set nom=?, date_debut=?, id_loisir=?, id_club=?, pts_victoire=?, pts_nul=?, pts_defaite=? where id=?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, this.nom);
            pst.setDate(2, Date.valueOf(this.dateDebut));
            pst.setInt(3, this.leLoisir.getId());
            pst.setInt(4, this.leClub.getId());
            pst.setInt(5, this.ptsVictoire);
            pst.setInt(6, this.ptsNul);
            pst.setInt(7, this.ptsDefaite);
            pst.setInt(8, this.getId());
            pst.executeUpdate();
        }
    }
    
    // Pour récupérer un tournoi spécifique par ID (utile pour la nouvelle vue)
    public static Optional<Tournoi> getById(Connection con, int id) throws SQLException {
        String query = "select t.*, l.id as l_id, l.nom as l_nom, l.description, c.id as c_id, c.nom as c_nom " +
                       "from tournoi t join loisir l on t.id_loisir = l.id join club c on t.id_club = c.id where t.id = ?";
        try(PreparedStatement pst = con.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if(rs.next()) {
                Loisir l = new Loisir(rs.getInt("l_id"), rs.getString("l_nom"), rs.getString("description"));
                Club c = new Club(rs.getInt("c_id"), rs.getString("c_nom"));
                LocalDate date = rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null;
                return Optional.of(new Tournoi(rs.getInt("id"), rs.getString("nom"), date, l, c, 
                        rs.getInt("pts_victoire"), rs.getInt("pts_nul"), rs.getInt("pts_defaite")));
            }
        }
        return Optional.empty();
    }
    
    public static List<Tournoi> getAll(Connection con) throws SQLException {
        List<Tournoi> res = new ArrayList<>();
        String query = "select t.*, l.id as l_id, l.nom as l_nom, l.description, c.id as c_id, c.nom as c_nom from tournoi t join loisir l on t.id_loisir = l.id join club c on t.id_club = c.id";
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                Loisir l = new Loisir(rs.getInt("l_id"), rs.getString("l_nom"), rs.getString("description"));
                Club c = new Club(rs.getInt("c_id"), rs.getString("c_nom"));
                LocalDate date = rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null;
                res.add(new Tournoi(rs.getInt("id"), rs.getString("nom"), date, l, c, rs.getInt("pts_victoire"), rs.getInt("pts_nul"), rs.getInt("pts_defaite")));
            }
        }
        return res;
    }

    // Getters & Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public Loisir getLeLoisir() { return leLoisir; }
    public Club getLeClub() { return leClub; }
    public int getPtsVictoire() { return ptsVictoire; }
    public void setPtsVictoire(int ptsVictoire) { this.ptsVictoire = ptsVictoire; }
    public int getPtsNul() { return ptsNul; }
    public void setPtsNul(int ptsNul) { this.ptsNul = ptsNul; }
    public int getPtsDefaite() { return ptsDefaite; }
    public void setPtsDefaite(int ptsDefaite) { this.ptsDefaite = ptsDefaite; }
}