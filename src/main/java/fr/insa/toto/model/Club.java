package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Club extends ClasseMiroir {
    
    private String nom;
    private String adresse;
    private int effectifManuel;
    private String logoUrl, description, email, telephone, instagram;

    public Club(String nom) {
        super();
        this.nom = nom;
    }

    public Club(int id, String nom) {
        super(id);
        this.nom = nom;
    }

    @Override
    public String toString() { return nom; }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into club (nom) values (?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.nom);
        pst.executeUpdate();
        return pst;
    }

    public static List<Club> getAll(Connection con) throws SQLException {
        List<Club> res = new ArrayList<>();
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("select id, nom from club");
            while (rs.next()) {
                res.add(new Club(rs.getInt("id"), rs.getString("nom")));
            }
        }
        return res;
    }

    public static Optional<Club> getById(Connection con, int id) throws SQLException {
        String query = "select * from club where id = ?";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Club c = new Club(rs.getInt("id"), rs.getString("nom"));
                c.setAdresse(rs.getString("adresse"));
                c.setEffectifManuel(rs.getInt("effectif_manuel"));
                c.setLogoUrl(rs.getString("logo_url"));
                c.setDescription(rs.getString("description"));
                c.setEmail(rs.getString("email"));
                c.setTelephone(rs.getString("telephone"));
                c.setInstagram(rs.getString("instagram"));
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    public void updateInfos(Connection con) throws SQLException {
        String sql = "update club set adresse=?, effectif_manuel=?, logo_url=?, description=?, email=?, telephone=?, instagram=? where id=?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, this.adresse);
            pst.setInt(2, this.effectifManuel);
            pst.setString(3, this.logoUrl);
            pst.setString(4, this.description);
            pst.setString(5, this.email);
            pst.setString(6, this.telephone);
            pst.setString(7, this.instagram);
            pst.setInt(8, this.getId());
            pst.executeUpdate();
        }
    }

    // Getters et Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public int getEffectifManuel() { return effectifManuel; }
    public void setEffectifManuel(int effectifManuel) { this.effectifManuel = effectifManuel; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }
}