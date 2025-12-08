package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Ronde extends ClasseMiroir {
    
    public static final int TYPE_BASIQUE = 0;
    public static final int TYPE_POULE = 1;
    public static final int TYPE_PHASE_FINALE = 2;
    
    private String nom;
    private int typeRonde;
    private int idTournoi;

    public Ronde(String nom, int typeRonde, int idTournoi) {
        super();
        this.nom = nom;
        this.typeRonde = typeRonde;
        this.idTournoi = idTournoi;
    }

    public Ronde(int id, String nom, int typeRonde, int idTournoi) {
        super(id);
        this.nom = nom;
        this.typeRonde = typeRonde;
        this.idTournoi = idTournoi;
    }
    
    @Override
    public String toString() { return nom; }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into ronde (nom, type_ronde, id_tournoi) values (?,?,?)", 
            Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, this.nom);
        pst.setInt(2, this.typeRonde);
        pst.setInt(3, this.idTournoi);
        pst.executeUpdate();
        return pst;
    }
    
    public static List<Ronde> getByTournoi(Connection con, int idTournoi) throws SQLException {
        List<Ronde> res = new ArrayList<>();
        String sql = "select * from ronde where id_tournoi = ? order by id";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                res.add(new Ronde(rs.getInt("id"), rs.getString("nom"), rs.getInt("type_ronde"), idTournoi));
            }
        }
        return res;
    }

    public String getNom() { return nom; }
    public int getTypeRonde() { return typeRonde; }
    public int getIdTournoi() { return idTournoi; }
}
