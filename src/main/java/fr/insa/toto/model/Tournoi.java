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
    private Loisir leLoisir;
    private Club leClub; // Nouveau champ

    public Tournoi(String nom, LocalDate dateDebut, Loisir leLoisir, Club leClub) {
        super();
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.leLoisir = leLoisir;
        this.leClub = leClub;
    }

    public Tournoi(int id, String nom, LocalDate dateDebut, Loisir leLoisir, Club leClub) {
        super(id);
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.leLoisir = leLoisir;
        this.leClub = leClub;
    }

    @Override
    public String toString() {
        return nom;
    }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into tournoi (nom, date_debut, id_loisir, id_club) values (?,?,?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.nom);
        pst.setDate(2, Date.valueOf(this.dateDebut));
        pst.setInt(3, this.leLoisir.getId());
        pst.setInt(4, this.leClub.getId());
        pst.executeUpdate();
        return pst;
    }
    
    // Nouvelle méthode pour mettre à jour un tournoi existant
    public void update(Connection con) throws SQLException {
        if (this.getId() == -1) {
            throw new Error("Impossible de mettre à jour un objet non sauvegardé (utilisez saveInDB)");
        }
        String query = "update tournoi set nom=?, date_debut=?, id_loisir=?, id_club=? where id=?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, this.nom);
            pst.setDate(2, Date.valueOf(this.dateDebut));
            pst.setInt(3, this.leLoisir.getId());
            pst.setInt(4, this.leClub.getId());
            pst.setInt(5, this.getId());
            pst.executeUpdate();
        }
    }
    
    public static List<Tournoi> getAll(Connection con) throws SQLException {
        List<Tournoi> res = new ArrayList<>();
        // Double jointure pour avoir le sport ET le club
        String query = "select t.id as t_id, t.nom as t_nom, t.date_debut, " +
                       "l.id as l_id, l.nom as l_nom, l.description, " +
                       "c.id as c_id, c.nom as c_nom " +
                       "from tournoi t " +
                       "join loisir l on t.id_loisir = l.id " +
                       "join club c on t.id_club = c.id";
                       
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                Loisir l = new Loisir(rs.getInt("l_id"), rs.getString("l_nom"), rs.getString("description"));
                Club c = new Club(rs.getInt("c_id"), rs.getString("c_nom"));
                LocalDate date = rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null;
                
                res.add(new Tournoi(rs.getInt("t_id"), rs.getString("t_nom"), date, l, c));
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
    public void setLeLoisir(Loisir leLoisir) { this.leLoisir = leLoisir; }
    public Club getLeClub() { return leClub; }
    public void setLeClub(Club leClub) { this.leClub = leClub; }
}