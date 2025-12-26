package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Club extends ClasseMiroir {
    
    // Attributs existants
    private String nom;
    private String adresse;
    private int effectifManuel;
    private String logoUrl, description, email, telephone, instagram;

    // Constructeurs (Inchangés)
    public Club(String nom) { super(); this.nom = nom; }
    public Club(int id, String nom) { super(id); this.nom = nom; }

    @Override
    public String toString() { return nom; }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement("insert into club (nom) values (?)", Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, this.nom);
        pst.executeUpdate();
        return pst;
    }

    public static List<Club> getAll(Connection con) throws SQLException {
        List<Club> res = new ArrayList<>();
        try (Statement st = con.createStatement()) {
            // On change 'select id, nom' par 'select *' pour tout avoir
            ResultSet rs = st.executeQuery("select * from club");
            while (rs.next()) {
                Club c = new Club(rs.getInt("id"), rs.getString("nom"));
                // On remplit les infos pour l'affichage
                c.setAdresse(rs.getString("adresse"));
                c.setLogoUrl(rs.getString("logo_url"));
                c.setDescription(rs.getString("description"));
                c.setEmail(rs.getString("email"));
                c.setTelephone(rs.getString("telephone"));
                res.add(c);
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

    // --- MODIFICATION ICI : Ajout de email et adresse dans l'update ---
    public void updateInfos(Connection con) throws SQLException {
        String sql = "update club set logo_url=?, description=?, telephone=?, instagram=?, email=?, adresse=? where id=?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, this.logoUrl);
            pst.setString(2, this.description);
            pst.setString(3, this.telephone);
            pst.setString(4, this.instagram);
            pst.setString(5, this.email);   // Nouveau
            pst.setString(6, this.adresse); // Nouveau
            pst.setInt(7, this.getId());
            pst.executeUpdate();
        }
    }
    
    // Getters et Setters (Assurez-vous qu'ils sont tous là)
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    
    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }
    
    public int getEffectifManuel() { return effectifManuel; }
    public void setEffectifManuel(int effectifManuel) { this.effectifManuel = effectifManuel; }
}