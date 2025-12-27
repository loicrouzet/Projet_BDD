package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Tournoi extends ClasseMiroir {
    
    private String nom;
    private String code;
    private String pass;
    private LocalDate dateDebut;
    private Loisir leLoisir;
    private Club leClub;
    
    // Points
    private int ptsVictoire = 3;
    private int ptsNul = 1;
    private int ptsDefaite = 0;
    
    // Configuration Temps
    private LocalTime heureDebut;
    private int dureeMatch; // en minutes
    private int tempsPause; // en minutes (nouveau champ)

    // Constructeur de création (nouveau tournoi)
    public Tournoi(String nom, LocalDate dateDebut, Loisir leLoisir, Club leClub) {
        super();
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.leLoisir = leLoisir;
        this.leClub = leClub;
        
        // Valeurs par défaut
        this.heureDebut = LocalTime.of(9, 0);
        this.dureeMatch = 60;
        this.tempsPause = 10;
    }

    // Constructeur complet (lecture depuis BDD)
    public Tournoi(int id, String nom, LocalDate dateDebut, Loisir leLoisir, Club leClub, int pv, int pn, int pd) {
        super(id);
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.leLoisir = leLoisir;
        this.leClub = leClub;
        this.ptsVictoire = pv;
        this.ptsNul = pn;
        this.ptsDefaite = pd;
        
        // Valeurs par défaut (seront écrasées par les setters si présentes en base)
        this.heureDebut = LocalTime.of(9, 0);
        this.dureeMatch = 60;
        this.tempsPause = 10;
    }
    
    // Constructeur utilitaire pour les updates de config (temporaire)
    public Tournoi(int id, String nom, String code, String pass, int idClub, LocalTime heureDebut, int dureeMatch, int tempsPause) {
        super(id);
        this.nom = nom;
        this.code = code;
        this.pass = pass;
        this.leClub = new Club(idClub, "ClubTemp"); 
        this.heureDebut = heureDebut != null ? heureDebut : LocalTime.of(9, 0);
        this.dureeMatch = dureeMatch > 0 ? dureeMatch : 60;
        this.tempsPause = tempsPause >= 0 ? tempsPause : 10;
    }

    @Override
    public String toString() { return nom; }

    @Override
    protected Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement pst = con.prepareStatement(
            "insert into tournoi (nom, date_debut, id_loisir, id_club, pts_victoire, pts_nul, pts_defaite, code, pass, heure_debut, duree_match, temps_pause) values (?,?,?,?,?,?,?,?,?,?,?,?)", 
            Statement.RETURN_GENERATED_KEYS
        );
        pst.setString(1, this.nom);
        pst.setDate(2, Date.valueOf(this.dateDebut));
        pst.setInt(3, this.leLoisir.getId());
        pst.setInt(4, this.leClub.getId());
        pst.setInt(5, this.ptsVictoire);
        pst.setInt(6, this.ptsNul);
        pst.setInt(7, this.ptsDefaite);
        pst.setString(8, this.code);
        pst.setString(9, this.pass);
        pst.setTime(10, this.heureDebut != null ? Time.valueOf(this.heureDebut) : null);
        pst.setInt(11, this.dureeMatch);
        pst.setInt(12, this.tempsPause);
        
        pst.executeUpdate();
        return pst;
    }
    
    public void update(Connection con) throws SQLException {
        // Mise à jour standard des infos générales
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
    
    // Méthode spécifique pour mettre à jour la configuration avancée (Utilisée dans VueTournoi)
    public void updateConfig(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "UPDATE tournoi SET nom=?, code=?, pass=?, heure_debut=?, duree_match=?, temps_pause=? WHERE id=?")) {
            pst.setString(1, this.nom);
            pst.setString(2, this.code);
            pst.setString(3, this.pass);
            pst.setTime(4, this.heureDebut != null ? Time.valueOf(this.heureDebut) : null);
            pst.setInt(5, this.dureeMatch);
            pst.setInt(6, this.tempsPause);
            pst.setInt(7, this.getId());
            pst.executeUpdate();
        }
    }
    
    public static Optional<Tournoi> getById(Connection con, int id) throws SQLException {
        String query = "select t.*, l.id as l_id, l.nom as l_nom, l.description, l.nb_joueurs_equipe, c.id as c_id, c.nom as c_nom " +
                       "from tournoi t join loisir l on t.id_loisir = l.id join club c on t.id_club = c.id where t.id = ?";
        try(PreparedStatement pst = con.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if(rs.next()) {
                Loisir l = new Loisir(rs.getInt("l_id"), rs.getString("l_nom"), rs.getString("description"), rs.getInt("nb_joueurs_equipe"));
                Club c = new Club(rs.getInt("c_id"), rs.getString("c_nom"));
                LocalDate date = rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null;
                
                Tournoi t = new Tournoi(rs.getInt("id"), rs.getString("nom"), date, l, c, 
                        rs.getInt("pts_victoire"), rs.getInt("pts_nul"), rs.getInt("pts_defaite"));
                
                // Remplissage des champs supplémentaires (Config)
                t.setCode(rs.getString("code"));
                t.setPass(rs.getString("pass"));
                if (rs.getTime("heure_debut") != null) t.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
                t.setDureeMatch(rs.getInt("duree_match"));
                t.setTempsPause(rs.getInt("temps_pause"));
                
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
    
    public static List<Tournoi> getAll(Connection con) throws SQLException {
        List<Tournoi> res = new ArrayList<>();
        String query = "select t.*, l.id as l_id, l.nom as l_nom, l.description, l.nb_joueurs_equipe, c.id as c_id, c.nom as c_nom " +
                       "from tournoi t join loisir l on t.id_loisir = l.id join club c on t.id_club = c.id";
        try (Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                Loisir l = new Loisir(rs.getInt("l_id"), rs.getString("l_nom"), rs.getString("description"), rs.getInt("nb_joueurs_equipe"));
                Club c = new Club(rs.getInt("c_id"), rs.getString("c_nom"));
                LocalDate date = rs.getDate("date_debut") != null ? rs.getDate("date_debut").toLocalDate() : null;
                
                Tournoi t = new Tournoi(rs.getInt("id"), rs.getString("nom"), date, l, c, 
                        rs.getInt("pts_victoire"), rs.getInt("pts_nul"), rs.getInt("pts_defaite"));
                
                // Champs optionnels
                t.setCode(rs.getString("code"));
                t.setPass(rs.getString("pass"));
                if (rs.getTime("heure_debut") != null) t.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
                t.setDureeMatch(rs.getInt("duree_match"));
                t.setTempsPause(rs.getInt("temps_pause"));

                res.add(t);
            }
        }
        return res;
    }

    // --- GESTION DES TERRAINS LIES ---
    
    public List<Terrain> getTerrainsSelectionnes(Connection con) throws SQLException {
        List<Terrain> res = new ArrayList<>();
        String sql = "SELECT t.* FROM terrain t " +
                     "JOIN tournoi_terrain tt ON t.id = tt.id_terrain " +
                     "WHERE tt.id_tournoi = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, this.getId());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                res.add(new Terrain(
                    rs.getInt("id"), 
                    rs.getString("nom"), 
                    rs.getBoolean("est_interieur"),  
                    rs.getBoolean("sous_construction"), 
                    rs.getInt("id_club")
                ));
            }
        }
        return res;
    }

    public void setTerrainsSelectionnes(Connection con, List<Terrain> terrains) throws SQLException {
        // 1. Supprimer les anciennes liaisons
        try (PreparedStatement pstDel = con.prepareStatement("DELETE FROM tournoi_terrain WHERE id_tournoi=?")) {
            pstDel.setInt(1, this.getId());
            pstDel.executeUpdate();
        }
        // 2. Insérer les nouvelles
        if (terrains != null && !terrains.isEmpty()) {
            try (PreparedStatement pstIns = con.prepareStatement("INSERT INTO tournoi_terrain (id_tournoi, id_terrain) VALUES (?,?)")) {
                for (Terrain t : terrains) {
                    pstIns.setInt(1, this.getId());
                    pstIns.setInt(2, t.getId());
                    pstIns.addBatch();
                }
                pstIns.executeBatch();
            }
        }
    }

    // --- GETTERS & SETTERS ---

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getPass() { return pass; }
    public void setPass(String pass) { this.pass = pass; }
    
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
    
    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }
    
    public int getDureeMatch() { return dureeMatch; }
    public void setDureeMatch(int dureeMatch) { this.dureeMatch = dureeMatch; }
    
    public int getTempsPause() { return tempsPause; }
    public void setTempsPause(int tempsPause) { this.tempsPause = tempsPause; }
}