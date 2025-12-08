package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Equipe;
import fr.insa.toto.model.Match;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.model.Utilisateur;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Route(value = "tournoi")
public class VueTournoi extends VerticalLayout implements HasUrlParameter<Integer> {

    private Connection con;
    private Utilisateur currentUser;
    private Tournoi tournoi;
    
    private Grid<Match> gridMatchs;
    private Grid<ClassementRow> gridClassement;

    public VueTournoi() {
        try {
            this.con = ConnectionSimpleSGBD.defaultCon();
            // Récupération de l'utilisateur depuis la session
            this.currentUser = (Utilisateur) VaadinSession.getCurrent().getAttribute("user");
        } catch (SQLException ex) {
            this.add(new Span("Erreur BDD"));
        }
    }

    @Override
    public void setParameter(BeforeEvent event, Integer tournoiId) {
        try {
            Optional<Tournoi> t = Tournoi.getById(this.con, tournoiId);
            if (t.isPresent()) {
                this.tournoi = t.get();
                buildUI();
            } else {
                this.add(new H1("Tournoi introuvable"));
            }
        } catch (SQLException ex) {
            Notification.show("Erreur : " + ex.getMessage());
        }
    }

    private void buildUI() {
        this.removeAll();
        
        // --- En-tête ---
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        
        Button backBtn = new Button("Retour", new Icon(VaadinIcon.ARROW_LEFT));
        backBtn.addClickListener(e -> backBtn.getUI().ifPresent(ui -> ui.navigate(VuePrincipale.class)));
        
        H1 title = new H1(tournoi.getNom() + " (" + tournoi.getLeLoisir().getNom() + ")");
        header.add(backBtn, title);
        this.add(header);

        // --- Configuration (Admin uniquement) ---
        boolean isAdmin = currentUser != null && currentUser.isAdmin();
        
        if (isAdmin) {
            HorizontalLayout configLayout = new HorizontalLayout();
            configLayout.setAlignItems(Alignment.BASELINE);
            NumberField ptsV = new NumberField("Pts Victoire"); ptsV.setValue((double)tournoi.getPtsVictoire());
            NumberField ptsN = new NumberField("Pts Nul"); ptsN.setValue((double)tournoi.getPtsNul());
            NumberField ptsD = new NumberField("Pts Défaite"); ptsD.setValue((double)tournoi.getPtsDefaite());
            
            Button saveConfig = new Button("Sauvegarder Points", e -> {
               tournoi.setPtsVictoire(ptsV.getValue().intValue());
               tournoi.setPtsNul(ptsN.getValue().intValue());
               tournoi.setPtsDefaite(ptsD.getValue().intValue());
               try { 
                   tournoi.update(this.con); 
                   Notification.show("Configuration sauvegardée"); 
                   updateClassement(); // Recalculer le classement avec les nouveaux barèmes
               } catch(SQLException ex) { Notification.show("Erreur update"); }
            });
            configLayout.add(ptsV, ptsN, ptsD, saveConfig);
            this.add(new H2("Configuration"), configLayout);
        }

        // --- Planning des Matchs ---
        HorizontalLayout planningHeader = new HorizontalLayout();
        planningHeader.setAlignItems(Alignment.CENTER);
        planningHeader.add(new H2("Planning des Matchs"));
        
        if (isAdmin) {
            Button addMatchBtn = new Button("Nouveau Match", new Icon(VaadinIcon.PLUS));
            addMatchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addMatchBtn.addClickListener(e -> openAddMatchDialog());
            planningHeader.add(addMatchBtn);
        }
        this.add(planningHeader);

        setupGridMatchs(isAdmin);
        this.add(gridMatchs);

        // --- Classement ---
        HorizontalLayout rankHeader = new HorizontalLayout();
        rankHeader.setAlignItems(Alignment.CENTER);
        rankHeader.add(new H2("Classement en Direct"));
        Button refreshRank = new Button(new Icon(VaadinIcon.REFRESH));
        refreshRank.addClickListener(e -> updateClassement());
        rankHeader.add(refreshRank);
        
        this.add(rankHeader);
        setupGridClassement();
        this.add(gridClassement);
        
        // Chargement initial
        updateMatchs();
        updateClassement();
    }

    private void setupGridMatchs(boolean isAdmin) {
        this.gridMatchs = new Grid<>(Match.class, false);
        this.gridMatchs.addColumn(m -> m.getDateHeure() != null ? m.getDateHeure().toString().replace("T", " ") : "Non planifié").setHeader("Horaire").setSortable(true);
        this.gridMatchs.addColumn(m -> m.getEquipe1().getNom()).setHeader("Équipe 1");
        
        // Colonne Score centrale
        this.gridMatchs.addColumn(m -> {
            if (m.isEstJoue()) return m.getScore1() + " - " + m.getScore2();
            return "vs";
        }).setHeader("Score").setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
        
        this.gridMatchs.addColumn(m -> m.getEquipe2().getNom()).setHeader("Équipe 2");
        
        if (isAdmin) {
            this.gridMatchs.addComponentColumn(m -> {
                Button scoreBtn = new Button("Saisir Score", new Icon(VaadinIcon.EDIT));
                scoreBtn.addClickListener(e -> openScoreDialog(m));
                if (m.isEstJoue()) {
                    scoreBtn.setText("Modifier");
                    scoreBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                } else {
                    scoreBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                }
                return scoreBtn;
            });
        }
    }
    
    private void setupGridClassement() {
        this.gridClassement = new Grid<>(ClassementRow.class, false);
        this.gridClassement.addColumn(ClassementRow::getRang).setHeader("#").setWidth("50px").setFlexGrow(0);
        this.gridClassement.addColumn(ClassementRow::getNomEquipe).setHeader("Équipe");
        this.gridClassement.addColumn(ClassementRow::getPoints).setHeader("Points").setSortable(true);
        this.gridClassement.addColumn(ClassementRow::getJoues).setHeader("Joués");
        this.gridClassement.addColumn(ClassementRow::getGagnes).setHeader("G");
        this.gridClassement.addColumn(ClassementRow::getNuls).setHeader("N");
        this.gridClassement.addColumn(ClassementRow::getPerdus).setHeader("P");
    }

    private void updateMatchs() {
        try {
            gridMatchs.setItems(Match.getByTournoi(this.con, tournoi.getId()));
        } catch (SQLException ex) { Notification.show("Erreur chargement matchs"); }
    }

    // --- ALGORITHME DE CLASSEMENT ---
    private void updateClassement() {
        try {
            List<Match> matchs = Match.getByTournoi(this.con, tournoi.getId());
            List<Equipe> equipes = Equipe.getByTournoi(this.con, tournoi.getId());
            
            Map<Integer, ClassementRow> stats = new HashMap<>();
            
            // Init stats pour chaque équipe inscrite
            for (Equipe eq : equipes) {
                stats.put(eq.getId(), new ClassementRow(eq.getNom()));
            }
            
            // Calcul des points
            for (Match m : matchs) {
                if (m.isEstJoue()) {
                    ClassementRow r1 = stats.get(m.getEquipe1().getId());
                    ClassementRow r2 = stats.get(m.getEquipe2().getId());
                    
                    if (r1 != null && r2 != null) { // Sécurité si une équipe a été supprimée
                        r1.joues++; r2.joues++;
                        
                        if (m.getScore1() > m.getScore2()) {
                            r1.points += tournoi.getPtsVictoire(); r1.gagnes++;
                            r2.points += tournoi.getPtsDefaite(); r2.perdus++;
                        } else if (m.getScore2() > m.getScore1()) {
                            r2.points += tournoi.getPtsVictoire(); r2.gagnes++;
                            r1.points += tournoi.getPtsDefaite(); r1.perdus++;
                        } else {
                            r1.points += tournoi.getPtsNul(); r1.nuls++;
                            r2.points += tournoi.getPtsNul(); r2.nuls++;
                        }
                    }
                }
            }
            
            List<ClassementRow> rows = new ArrayList<>(stats.values());
            // Tri par points décroissant
            Collections.sort(rows, (a, b) -> Integer.compare(b.points, a.points));
            
            // Assigner les rangs
            for (int i = 0; i < rows.size(); i++) {
                rows.get(i).rang = i + 1;
            }
            
            gridClassement.setItems(rows);
            
        } catch (SQLException ex) { Notification.show("Erreur calcul classement"); }
    }

    // --- DIALOGUES ---
    
    private void openAddMatchDialog() {
        Dialog d = new Dialog();
        d.setHeaderTitle("Créer un Match");
        
        ComboBox<Equipe> eq1 = new ComboBox<>("Équipe 1");
        ComboBox<Equipe> eq2 = new ComboBox<>("Équipe 2");
        DateTimePicker datePicker = new DateTimePicker("Date et Heure");
        
        try {
            List<Equipe> equipes = Equipe.getByTournoi(this.con, tournoi.getId());
            eq1.setItems(equipes); eq1.setItemLabelGenerator(Equipe::getNom);
            eq2.setItems(equipes); eq2.setItemLabelGenerator(Equipe::getNom);
        } catch (SQLException ex) {}
        
        Button save = new Button("Créer", e -> {
            if (eq1.isEmpty() || eq2.isEmpty() || datePicker.isEmpty()) return;
            if (eq1.getValue().getId() == eq2.getValue().getId()) {
                Notification.show("Impossible de faire jouer une équipe contre elle-même");
                return;
            }
            try {
                new Match(tournoi.getId(), eq1.getValue(), eq2.getValue(), datePicker.getValue()).saveInDB(this.con);
                updateMatchs();
                d.close();
                Notification.show("Match planifié");
            } catch (SQLException ex) { Notification.show("Erreur : " + ex.getMessage()); }
        });
        
        d.add(new VerticalLayout(eq1, eq2, datePicker, save));
        d.open();
    }
    
    private void openScoreDialog(Match m) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Résultat : " + m.getEquipe1().getNom() + " vs " + m.getEquipe2().getNom());
        
        NumberField s1 = new NumberField("Score " + m.getEquipe1().getNom());
        s1.setValue((double)m.getScore1());
        NumberField s2 = new NumberField("Score " + m.getEquipe2().getNom());
        s2.setValue((double)m.getScore2());
        
        Button save = new Button("Valider Résultat", e -> {
            try {
                m.updateScore(this.con, s1.getValue().intValue(), s2.getValue().intValue(), true);
                updateMatchs();
                updateClassement(); // Mise à jour auto du classement
                d.close();
                Notification.show("Score enregistré");
            } catch (SQLException ex) { Notification.show("Erreur : " + ex.getMessage()); }
        });
        
        d.add(new VerticalLayout(s1, s2, save));
        d.open();
    }

    // --- CLASSE INTERNE POUR LE CLASSEMENT ---
    public static class ClassementRow {
        int rang;
        String nomEquipe;
        int points = 0;
        int joues = 0;
        int gagnes = 0;
        int nuls = 0;
        int perdus = 0;

        public ClassementRow(String nom) { this.nomEquipe = nom; }
        
        // Getters pour la Grid
        public int getRang() { return rang; }
        public String getNomEquipe() { return nomEquipe; }
        public int getPoints() { return points; }
        public int getJoues() { return joues; }
        public int getGagnes() { return gagnes; }
        public int getNuls() { return nuls; }
        public int getPerdus() { return perdus; }
    }
}
