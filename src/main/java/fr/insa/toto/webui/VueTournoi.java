package fr.insa.toto.webui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.textfield.TextArea;
import fr.insa.toto.model.Terrain;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Equipe;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Match;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.model.Utilisateur;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "tournoi")
public class VueTournoi extends VerticalLayout implements HasUrlParameter<Integer> {

    private Connection con;
    private Utilisateur currentUser;
    private Tournoi tournoi;
    
    private Tabs rondeTabs;
    private VerticalLayout rondeContent;
    private Tab inscriptionTab;
    private Map<Tab, Ronde> tabToRondeMap = new HashMap<>();
    
    private boolean canEdit = false;

    public VueTournoi() {
        try {
            this.con = ConnectionSimpleSGBD.defaultCon();
            this.currentUser = (Utilisateur) VaadinSession.getCurrent().getAttribute("user");
        } catch (SQLException ex) { this.add(new Span("Erreur connexion BDD")); }
    }

    @Override
    public void setParameter(BeforeEvent event, Integer tournoiId) {
        try {
            Optional<Tournoi> t = Tournoi.getById(this.con, tournoiId);
            if (t.isPresent()) {
                this.tournoi = t.get();
                // Vérification des droits d'édition
                this.canEdit = currentUser != null && currentUser.isAdmin() 
                               && currentUser.getIdClub() != null
                               && currentUser.getIdClub().equals(tournoi.getLeClub().getId());
                buildUI();
            } else { 
                this.add(new H1("Tournoi introuvable (ID: " + tournoiId + ")")); 
            }
        } catch (SQLException ex) { 
            Notification.show("Erreur de chargement : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void buildUI() {
        this.removeAll();

        // 1. En-tête
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull(); header.setJustifyContentMode(JustifyContentMode.BETWEEN); header.setAlignItems(Alignment.CENTER);
        
        Button backBtn = new Button("Retour", new Icon(VaadinIcon.ARROW_LEFT));
        backBtn.addClickListener(e -> backBtn.getUI().ifPresent(ui -> ui.navigate(VuePrincipale.class)));
        
        header.add(backBtn, new H1(tournoi.getNom()));
        this.add(header);

        // 2. Barre d'outils
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);

        rondeTabs = new Tabs();
        rondeTabs.getStyle().set("flex-grow", "1"); 
        
        HorizontalLayout actionButtons = new HorizontalLayout();
        actionButtons.setSpacing(true);

        Button btnClassementGeneral = new Button("Classement Général", new Icon(VaadinIcon.TROPHY));
        btnClassementGeneral.addClickListener(e -> showGeneralRankingDialog());
        actionButtons.add(btnClassementGeneral);

        if (canEdit) {
            Button btnNouvelleRonde = new Button("Nouvelle Ronde", new Icon(VaadinIcon.MAGIC));
            btnNouvelleRonde.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnNouvelleRonde.addClickListener(e -> openGenerationDialog(tournoi.getLeLoisir().getNbJoueursEquipe()));
            actionButtons.add(btnNouvelleRonde);
        }

        toolbar.add(rondeTabs, actionButtons);
        this.add(toolbar);

        // 3. Contenu
        rondeContent = new VerticalLayout();
        rondeContent.setSizeFull();
        this.add(rondeContent);
        
        inscriptionTab = new Tab("Inscriptions");
        rondeTabs.add(inscriptionTab);
        loadRondes();
        
        rondeTabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == inscriptionTab) {
                showInscriptionContent();
            } else {
                Ronde selectedRonde = tabToRondeMap.get(event.getSelectedTab());
                if (selectedRonde != null) showRondeContent(selectedRonde);
            }
        });

        if (rondeTabs.getComponentCount() > 1) {
            rondeTabs.setSelectedIndex(rondeTabs.getComponentCount() - 1);
        } else {
            rondeTabs.setSelectedTab(inscriptionTab);
            showInscriptionContent();
        }
    }
    
    // --- PARTIE 1 : INSCRIPTIONS ---
    
    private void showInscriptionContent() {
        rondeContent.removeAll();
        H2 titre = new H2("Inscription des Joueurs");
        
        Grid<Joueur> availableGrid = new Grid<>(Joueur.class, false);
        availableGrid.addColumn(j -> j.getPrenom() + " " + j.getNom()).setHeader("Joueurs Disponibles");
        
        Grid<Joueur> inscritGrid = new Grid<>(Joueur.class, false);
        inscritGrid.addColumn(j -> j.getPrenom() + " " + j.getNom()).setHeader("Inscrits au Tournoi");
        
        // Colonne Statut Admin
        inscritGrid.addComponentColumn(j -> {
            if (j.isUserAdmin()) {
                Span badge = new Span(new Icon(VaadinIcon.KEY), new Span(" Admin"));
                badge.getElement().getThemeList().add("badge success");
                badge.getStyle().set("color", "green").set("font-weight", "bold");
                return badge;
            }
            return new Span("");
        }).setHeader("Statut").setAutoWidth(true);
        
        if (canEdit) {
            availableGrid.addComponentColumn(j -> {
                Button btn = new Button(new Icon(VaadinIcon.ARROW_RIGHT));
                btn.addClickListener(e -> {
                    try { j.inscrireAuTournoi(con, tournoi.getId()); updateGrids(availableGrid, inscritGrid); } 
                    catch (SQLException ex) { Notification.show("Erreur inscription"); }
                });
                return btn;
            });
            inscritGrid.addComponentColumn(j -> {
                Button btn = new Button(new Icon(VaadinIcon.TRASH));
                btn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                btn.addClickListener(e -> {
                    try { j.desinscrireDuTournoi(con, tournoi.getId()); updateGrids(availableGrid, inscritGrid); } 
                    catch (SQLException ex) { Notification.show("Erreur désinscription"); }
                });
                return btn;
            });
        }
        HorizontalLayout grids = new HorizontalLayout(availableGrid, inscritGrid);
        grids.setSizeFull();
        updateGrids(availableGrid, inscritGrid);
        rondeContent.add(titre, grids);
    }

    private void updateGrids(Grid<Joueur> g1, Grid<Joueur> g2) {
        try {
            List<Joueur> all = Joueur.getAll(con); 
            List<Joueur> inscrits = Joueur.getInscritsAuTournoi(con, tournoi.getId());
            List<Integer> idsInscrits = inscrits.stream().map(Joueur::getId).collect(Collectors.toList());
            List<Joueur> dispos = all.stream().filter(j -> !idsInscrits.contains(j.getId())).collect(Collectors.toList());
            g1.setItems(dispos); g2.setItems(inscrits);
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    // --- PARTIE 2 : GÉNÉRATION DE RONDE ---

    private void openGenerationDialog(int nbParEquipe) {
    int nextNum = tabToRondeMap.size() + 1;
    String defaultName = "Ronde " + nextNum;
    
    // Création de la fenêtre de dialogue
    Dialog d = new Dialog(); 
    d.setHeaderTitle("Générer " + defaultName);
    d.setWidth("500px");

    VerticalLayout content = new VerticalLayout();
    content.add(new Span("Cette action va :"));
    content.add(new Html("<ul>" +
            "<li>Mélanger les joueurs inscrits</li>" +
            "<li>Créer des équipes de " + nbParEquipe + " joueurs</li>" +
            "<li>Générer les matchs en fonction des <b>terrains configurés</b></li>" +
            "</ul>"));

    Button valider = new Button("Mélanger et Générer", e -> {
        try {
            // 1. Récupération et vérification des joueurs
            List<Joueur> pool = Joueur.getInscritsAuTournoi(con, tournoi.getId());
            if (pool.size() < nbParEquipe * 2) { 
                Notification.show("Pas assez de joueurs inscrits pour faire un match !"); 
                return; 
            }
            
            // 2. Mélange aléatoire
            Collections.shuffle(pool); 
            
            // 3. Création des équipes
            int numEquipe = 1;
            List<Equipe> nouvellesEquipes = new ArrayList<>();
            
            // On boucle sur la liste des joueurs par paquet de 'nbParEquipe'
            for (int i = 0; i < pool.size(); i += nbParEquipe) {
                // On vérifie qu'il reste assez de joueurs pour faire une équipe complète
                if (i + nbParEquipe <= pool.size()) {
                    Equipe eq = new Equipe("R" + nextNum + "-Eq" + numEquipe, tournoi.getId());
                    int idEq = eq.saveInDB(con); 
                    eq.setId(idEq);
                    nouvellesEquipes.add(eq);
                    
                    // Affectation des joueurs à l'équipe
                    for (int j = 0; j < nbParEquipe; j++) {
                        Joueur joueur = pool.get(i + j);
                        joueur.setIdEquipe(idEq); 
                        joueur.update(con);
                        
                        // Sauvegarde dans la table de liaison composition
                        try (java.sql.Statement st = con.createStatement()) {
                            st.executeUpdate("INSERT INTO composition (id_equipe, id_joueur) VALUES (" + idEq + ", " + joueur.getId() + ")");
                        }
                    }
                    // Inscription de l'équipe au tournoi
                    eq.inscrireATournoi(con, tournoi.getId());
                    numEquipe++;
                }
            }
            
            // 4. Création de la Ronde
            Ronde ronde = new Ronde(defaultName, Ronde.TYPE_POULE, tournoi.getId());
            int idRonde = ronde.saveInDB(con);
            
            // =================================================================================
            // 5. PLANIFICATION DES MATCHS (Logique Infrastructure)
            // =================================================================================
            
            // A. Récupération de la configuration (Terrains, Heure, Durée)
            List<Terrain> terrainsDispo = tournoi.getTerrainsSelectionnes(con);
            
            // Sécurité : Si aucun terrain n'est configuré, on considère qu'il y en a 1 virtuel
            int nbTerrains = terrainsDispo.isEmpty() ? 1 : terrainsDispo.size();
            
            // Sécurité : Si l'heure n'est pas définie, on met 9h00 par défaut
            LocalTime startTime = tournoi.getHeureDebut() != null ? tournoi.getHeureDebut() : LocalTime.of(9, 0);
            
            // Sécurité : Durée minimale de 10 min
            int dureeMatch = tournoi.getDureeMatch() > 0 ? tournoi.getDureeMatch() : 60;

            // On initialise le curseur temps à la date du jour + l'heure de début configurée
            LocalDateTime cursorTime = LocalDateTime.of(LocalDate.now(), startTime);
            
            int matchsDansCeCreneau = 0; // Compteur pour savoir combien de matchs sont prévus sur cet horaire

            // On fait s'affronter les équipes 2 par 2 (0 vs 1, 2 vs 3, etc.)
            for (int i = 0; i < nouvellesEquipes.size() - 1; i += 2) {
                
                Equipe e1 = nouvellesEquipes.get(i);
                Equipe e2 = nouvellesEquipes.get(i+1);
                
                // (Optionnel) Ajout du nom du terrain dans le label du match pour plus de clarté
                String infoTerrain = "";
                if (!terrainsDispo.isEmpty()) {
                    // On cycle sur les terrains : match 0 -> terrain 0, match 1 -> terrain 1, match 2 -> terrain 0...
                    Terrain t = terrainsDispo.get(matchsDansCeCreneau % nbTerrains);
                    infoTerrain = " (" + t.getNom() + ")";
                }

                String labelMatch = "M" + ((i/2)+1) + infoTerrain;

                // Création et sauvegarde du match avec l'heure calculée
                new Match(tournoi.getId(), idRonde, e1, e2, labelMatch, cursorTime).saveInDB(con);
                
                matchsDansCeCreneau++;
                
                // Si on a rempli tous les terrains disponibles pour ce créneau horaire
                if (matchsDansCeCreneau >= nbTerrains) {
                    // On avance l'heure pour les prochains matchs
                    cursorTime = cursorTime.plusMinutes(dureeMatch);
                    // On remet le compteur de terrains à 0
                    matchsDansCeCreneau = 0;
                }
            }
            
            Notification.show(nouvellesEquipes.size() + " équipes générées. Planning optimisé sur " + nbTerrains + " terrain(s).");
            d.close(); 
            buildUI(); // Rafraichir l'affichage pour voir la nouvelle ronde
            
        } catch (SQLException ex) { 
            Notification.show("Erreur lors de la génération : " + ex.getMessage());
            ex.printStackTrace();
        }
    });
    
    valider.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    
    content.add(valider);
    d.add(content);
    d.open();
}

    // --- PARTIE 3 : AFFICHAGE DES RONDES ---

    private void loadRondes() {
        tabToRondeMap.clear();
        try {
            List<Ronde> rondes = Ronde.getByTournoi(this.con, tournoi.getId());
            for (Ronde r : rondes) {
                String color = "gray"; 
                List<Match> ms = Match.getByRonde(con, r.getId());
                if (!ms.isEmpty()) {
                    boolean allFinished = ms.stream().allMatch(Match::isEstJoue);
                    color = allFinished ? "black" : "green"; 
                }
                Tab tab = new Tab(new HorizontalLayout(createStatusDot(color), new Span(r.getNom())));
                rondeTabs.add(tab);
                tabToRondeMap.put(tab, r);
            }
        } catch (SQLException ex) { Notification.show("Erreur chargement rondes"); }
    }

    private void showRondeContent(Ronde ronde) {
        rondeContent.removeAll();
        HorizontalLayout actions = new HorizontalLayout(new H2(ronde.getNom()));
        actions.setAlignItems(Alignment.BASELINE);
        if (canEdit) {
            Button addMatch = new Button("Ajouter Match Manuel", new Icon(VaadinIcon.PLUS));
            addMatch.addClickListener(e -> openAddMatchDialog(ronde));
            actions.add(addMatch);
        }
        rondeContent.add(actions);

        try {
            List<Match> matchs = Match.getByRonde(this.con, ronde.getId());
            Grid<Match> gridMatchs = new Grid<>(Match.class, false);
            
            // Colonne Etat
            gridMatchs.addComponentColumn(m -> createStatusDot(m.isEstJoue() ? "black" : "green"))
                      .setHeader("État").setAutoWidth(true).setFlexGrow(0);
                      
            gridMatchs.addColumn(m -> m.getLabel()).setHeader("Info");
            gridMatchs.addColumn(m -> m.getEquipe1() != null ? m.getEquipe1().getNom() : "TBD").setHeader("Équipe 1");
            
            // --- CORRECTION ICI : Affichage inconditionnel du score ---
            gridMatchs.addColumn(m -> m.getScore1() + " - " + m.getScore2()).setHeader("Score");
            
            gridMatchs.addColumn(m -> m.getEquipe2() != null ? m.getEquipe2().getNom() : "TBD").setHeader("Équipe 2");
            
            if (canEdit) {
                gridMatchs.addComponentColumn(m -> {
                    Button edit = new Button(new Icon(VaadinIcon.EDIT));
                    edit.addClickListener(e -> openScoreDialog(m));
                    return edit;
                });
            }
            gridMatchs.setItems(matchs);
            rondeContent.add(gridMatchs);

            // Classement de la Ronde
            rondeContent.add(new Hr(), new H3("Classement de la Ronde"));
            Grid<ClassementRow> rankGrid = createRankingGrid();
            rankGrid.setItems(calculateRanking(matchs));
            rondeContent.add(rankGrid);

        } catch(SQLException ex) { ex.printStackTrace(); }
    }

    // --- PARTIE 4 : DIALOGUES ET MODALES ---

    private void showGeneralRankingDialog() {
        Dialog d = new Dialog(); d.setHeaderTitle("Classement Général (Cliquez pour détails)"); d.setWidth("800px");
        try {
            List<Match> allMatches = new ArrayList<>();
            for (Ronde r : Ronde.getByTournoi(con, tournoi.getId())) allMatches.addAll(Match.getByRonde(con, r.getId()));
            
            Grid<ClassementRow> grid = createRankingGrid();
            grid.setItems(calculateRanking(allMatches));
            grid.addItemClickListener(e -> showPlayerHistory(e.getItem().joueur));
            
            d.add(new Span("Cliquez sur un joueur pour voir ses matchs."), grid);
            d.getFooter().add(new Button("Fermer", e -> d.close()));
            d.open();
        } catch (SQLException ex) { Notification.show("Erreur calcul classement"); }
    }

    private void showPlayerHistory(Joueur j) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Historique : " + j.getPrenom() + " " + j.getNom());
        d.setWidth("700px");

        VerticalLayout infos = new VerticalLayout();
        infos.setSpacing(false);
        if(j.getNomClub() != null && !j.getNomClub().isEmpty()) infos.add(new Span("Club : " + j.getNomClub()));
        if(j.getEmail() != null && !j.getEmail().isEmpty()) infos.add(new Span("Email : " + j.getEmail()));
        if(j.isUserAdmin()) infos.add(new Span("Statut : Administrateur"));
        
        Grid<HistoryRow> hGrid = new Grid<>();
        hGrid.addColumn(row -> row.ronde).setHeader("Ronde");
        hGrid.addColumn(row -> row.monEquipe).setHeader("Mon Équipe");
        hGrid.addColumn(row -> row.adversaire).setHeader("Adversaire");
        hGrid.addColumn(row -> row.score).setHeader("Score");
        hGrid.addComponentColumn(row -> {
            String color = row.victoire ? "green" : (row.defaite ? "red" : "gray");
            String text = row.victoire ? "VICTOIRE" : (row.defaite ? "DÉFAITE" : "NUL");
            Span s = new Span(text); s.getStyle().set("color", color).set("font-weight", "bold");
            return s;
        }).setHeader("Résultat");

        List<HistoryRow> rows = fetchPlayerHistory(j.getId());
        hGrid.setItems(rows);
        
        d.add(infos, new Hr(), hGrid);
        d.getFooter().add(new Button("Fermer", e -> d.close()));
        d.open();
    }

    private void openAddMatchDialog(Ronde ronde) {
        Dialog d = new Dialog(); d.setHeaderTitle("Ajouter Match");
        ComboBox<Equipe> eq1 = new ComboBox<>("Équipe 1");
        ComboBox<Equipe> eq2 = new ComboBox<>("Équipe 2");
        TextField label = new TextField("Label");
        DateTimePicker datePicker = new DateTimePicker("Date");
        try {
            List<Equipe> allEquipes = Equipe.getByTournoi(this.con, tournoi.getId());
            List<Equipe> filteredEquipes = new ArrayList<>();
            
            if (ronde.getNom().startsWith("Ronde ")) {
                try {
                    String num = ronde.getNom().replace("Ronde ", "").trim();
                    String prefix = "R" + num + "-";
                    filteredEquipes = allEquipes.stream().filter(e -> e.getNom() != null && e.getNom().startsWith(prefix)).collect(Collectors.toList());
                } catch (Exception ignore) {}
            }
            if (filteredEquipes.isEmpty()) {
                 List<Match> matches = Match.getByRonde(this.con, ronde.getId());
                 filteredEquipes = matches.stream().flatMap(m -> Stream.of(m.getEquipe1(), m.getEquipe2())).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            }
            if (filteredEquipes.isEmpty()) filteredEquipes = allEquipes;
            
            eq1.setItems(filteredEquipes); eq1.setItemLabelGenerator(Equipe::getNom);
            eq2.setItems(filteredEquipes); eq2.setItemLabelGenerator(Equipe::getNom);
        } catch (SQLException ex) {}
        
        Button save = new Button("Ajouter", e -> {
            try {
                new Match(tournoi.getId(), ronde.getId(), eq1.getValue(), eq2.getValue(), label.getValue(), datePicker.getValue()).saveInDB(this.con);
                buildUI(); d.close();
            } catch(SQLException ex) { Notification.show("Erreur"); }
        });
        d.add(new VerticalLayout(eq1, eq2, label, datePicker, save)); d.open();
    }
    
    // Remplacer la méthode openScoreDialog
    private void openScoreDialog(Match m) {
        Dialog d = new Dialog(); d.setHeaderTitle("Gestion du Match");
        d.setWidth("600px");

        // Champs Scores
        HorizontalLayout scores = new HorizontalLayout();
        scores.setAlignItems(Alignment.BASELINE);
        NumberField s1 = new NumberField(m.getEquipe1() != null ? m.getEquipe1().getNom() : "Eq 1"); 
        s1.setValue((double)m.getScore1());
        Span sep = new Span("-");
        NumberField s2 = new NumberField(m.getEquipe2() != null ? m.getEquipe2().getNom() : "Eq 2"); 
        s2.setValue((double)m.getScore2());
        scores.add(s1, sep, s2);

        // Champs Date & Heure (Modification précise)
        DateTimePicker datePicker = new DateTimePicker("Horaire du match");
        datePicker.setValue(m.getDateHeure());
        datePicker.setWidthFull();

        // Champ Résumé (Notes de match)
        TextArea resumeField = new TextArea("Résumé / Notes / Arbitrage");
        resumeField.setValue(m.getResume() != null ? m.getResume() : "");
        resumeField.setWidthFull();
        resumeField.setMinHeight("150px");

        Checkbox fini = new Checkbox("Match terminé (Validation définitive)"); 
        fini.setValue(m.isEstJoue());
        
        Button save = new Button("Enregistrer", e -> {
            try {
                m.updateInfos(this.con, 
                    s1.getValue() != null ? s1.getValue().intValue() : 0, 
                    s2.getValue() != null ? s2.getValue().intValue() : 0, 
                    fini.getValue(),
                    resumeField.getValue(),
                    datePicker.getValue()
                );
                buildUI(); d.close();
                Notification.show("Match mis à jour");
            } catch(SQLException ex) { Notification.show("Erreur BDD: " + ex.getMessage()); }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setWidthFull();

        VerticalLayout layout = new VerticalLayout(scores, datePicker, resumeField, fini, save);
        d.add(layout); d.open();
    }

    // --- PARTIE 5 : CALCULS ET LOGIQUE MÉTIER ---

    private static class ClassementRow {
        Joueur joueur;
        int pts = 0, joues = 0, victoires = 0, nuls = 0, defaites = 0, diff = 0;
        public ClassementRow(Joueur j) { this.joueur = j; }
        public String getNomComplet() { return joueur.getPrenom() + " " + joueur.getNom(); }
        public int getPts() { return pts; }
        public int getJoues() { return joues; }
        public int getVictoires() { return victoires; }
        public int getNuls() { return nuls; }
        public int getDefaites() { return defaites; }
        public int getDiff() { return diff; }
    }
    
    private static class HistoryRow {
        String ronde, monEquipe, adversaire, score;
        boolean victoire, defaite, nul;
    }

    private Grid<ClassementRow> createRankingGrid() {
        Grid<ClassementRow> grid = new Grid<>();
        grid.addColumn(ClassementRow::getNomComplet).setHeader("Joueur").setAutoWidth(true);
        grid.addColumn(ClassementRow::getPts).setHeader("PTS").setSortable(true);
        grid.addColumn(ClassementRow::getJoues).setHeader("J");
        grid.addColumn(ClassementRow::getVictoires).setHeader("G");
        grid.addColumn(ClassementRow::getNuls).setHeader("N");
        grid.addColumn(ClassementRow::getDefaites).setHeader("P");
        grid.addColumn(ClassementRow::getDiff).setHeader("Diff");
        return grid;
    }

    private List<ClassementRow> calculateRanking(List<Match> matchesToProcess) {
        Map<Integer, ClassementRow> stats = new HashMap<>();
        try {
            List<Joueur> allInscrits = Joueur.getInscritsAuTournoi(con, tournoi.getId());
            for (Joueur j : allInscrits) stats.put(j.getId(), new ClassementRow(j));
            
            Map<Integer, List<Integer>> teamCompoCache = new HashMap<>();
            
            for (Match m : matchesToProcess) {
                // IMPORTANT : On ne compte les points QUE si le match est terminé
                if (!m.isEstJoue() || m.getEquipe1() == null || m.getEquipe2() == null) continue;
                
                List<Integer> idsE1 = getTeamMembers(m.getEquipe1().getId(), teamCompoCache);
                List<Integer> idsE2 = getTeamMembers(m.getEquipe2().getId(), teamCompoCache);
                
                List<Joueur> joueursE1 = allInscrits.stream().filter(j -> idsE1.contains(j.getId())).collect(Collectors.toList());
                List<Joueur> joueursE2 = allInscrits.stream().filter(j -> idsE2.contains(j.getId())).collect(Collectors.toList());
                
                int s1 = m.getScore1(), s2 = m.getScore2();
                int pts1 = (s1 > s2) ? tournoi.getPtsVictoire() : (s1 == s2) ? tournoi.getPtsNul() : tournoi.getPtsDefaite();
                int pts2 = (s2 > s1) ? tournoi.getPtsVictoire() : (s1 == s2) ? tournoi.getPtsNul() : tournoi.getPtsDefaite();
                
                updateTeamStats(stats, joueursE1, pts1, s1, s2);
                updateTeamStats(stats, joueursE2, pts2, s2, s1);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        List<ClassementRow> res = new ArrayList<>(stats.values());
        res.sort(Comparator.comparingInt(ClassementRow::getPts).reversed().thenComparingInt(ClassementRow::getDiff).reversed());
        return res;
    }

    private void updateTeamStats(Map<Integer, ClassementRow> stats, List<Joueur> joueurs, int points, int scorePour, int scoreContre) {
        for (Joueur j : joueurs) {
            ClassementRow row = stats.get(j.getId());
            if (row != null) {
                row.pts += points; row.joues++; row.diff += (scorePour - scoreContre);
                if (scorePour > scoreContre) row.victoires++;
                else if (scorePour == scoreContre) row.nuls++;
                else row.defaites++;
            }
        }
    }
    
    private Component createInscriptionContent() {
        VerticalLayout layout = new VerticalLayout();
        
        // 1. Configuration du Tournoi
        H3 titreConfig = new H3("Configuration du Tournoi");
        
        TextField nomField = new TextField("Nom du tournoi");
        nomField.setValue(tournoi.getNom());
        
        TimePicker heureDebutField = new TimePicker("Heure de début des matchs");
        heureDebutField.setValue(tournoi.getHeureDebut());
        
        IntegerField dureeField = new IntegerField("Durée d'un match (minutes)");
        dureeField.setValue(tournoi.getDureeMatch());
        dureeField.setMin(10); dureeField.setStep(5);

        // Sélection des terrains
        CheckboxGroup<Terrain> terrainsSelect = new CheckboxGroup<>();
        terrainsSelect.setLabel("Terrains à utiliser");
        terrainsSelect.setItemLabelGenerator(Terrain::getNom);
        terrainsSelect.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        
        // Charger les données
        try {
            // Tous les terrains du club (sauf en construction)
            List<Terrain> tousTerrains = Terrain.getByClub(con, tournoi.getLeClub().getId());
            tousTerrains.removeIf(Terrain::isSousConstruction);
            terrainsSelect.setItems(tousTerrains);
            
            // Pré-cocher ceux déjà configurés
            List<Terrain> dejaChoisis = tournoi.getTerrainsSelectionnes(con);
            // On doit mapper les IDs pour que la CheckboxGroup reconnaisse les objets (equals/hashCode importants dans Terrain)
            // Sinon on sélectionne par ID :
            terrainsSelect.setValue(new HashSet<>(dejaChoisis));
            
        } catch (SQLException e) { Notification.show("Err chargement terrains: " + e.getMessage()); }

        Button btnSaveConfig = new Button("Sauvegarder Configuration", e -> {
            try {
                tournoi.setNom(nomField.getValue());
                tournoi.setHeureDebut(heureDebutField.getValue());
                tournoi.setDureeMatch(dureeField.getValue());
                
                tournoi.updateConfig(con); // Sauvegarde infos simples
                tournoi.setTerrainsSelectionnes(con, new ArrayList<>(terrainsSelect.getValue())); // Sauvegarde terrains
                
                Notification.show("Configuration sauvegardée !");
            } catch (SQLException ex) { Notification.show("Erreur sauvegarde : " + ex.getMessage()); }
        });

        // ... (Le reste du code pour l'inscription des joueurs reste ici) ...
        
        layout.add(titreConfig, nomField, new HorizontalLayout(heureDebutField, dureeField), terrainsSelect, btnSaveConfig);
        // Ajouter ensuite la partie Inscription Joueurs en dessous...
        return layout;
    }
    
    private List<HistoryRow> fetchPlayerHistory(int idJoueur) {
        List<HistoryRow> res = new ArrayList<>();
        String sql = "SELECT m.score1, m.score2, r.nom as nom_ronde, e1.id as id1, e1.nom as nom1, e2.id as id2, e2.nom as nom2 " +
                     "FROM match_tournoi m " +
                     "JOIN ronde r ON m.id_ronde = r.id " +
                     "JOIN equipe e1 ON m.id_equipe1 = e1.id " +
                     "JOIN equipe e2 ON m.id_equipe2 = e2.id " +
                     "WHERE m.id_tournoi = ? AND m.est_joue = true " +
                     "ORDER BY m.date_heure DESC";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournoi.getId());
            ResultSet rs = pst.executeQuery();
            Map<Integer, List<Integer>> teamCompo = new HashMap<>(); 
            while(rs.next()) {
                int id1 = rs.getInt("id1");
                int id2 = rs.getInt("id2");
                boolean inEq1 = getTeamMembers(id1, teamCompo).contains(idJoueur);
                boolean inEq2 = getTeamMembers(id2, teamCompo).contains(idJoueur);
                
                if (!inEq1 && !inEq2) continue;
                
                HistoryRow row = new HistoryRow();
                row.ronde = rs.getString("nom_ronde");
                int s1 = rs.getInt("score1");
                int s2 = rs.getInt("score2");
                if (inEq1) {
                    row.monEquipe = rs.getString("nom1"); row.adversaire = rs.getString("nom2"); row.score = s1 + " - " + s2;
                    if (s1 > s2) row.victoire = true; else if (s1 < s2) row.defaite = true; else row.nul = true;
                } else {
                    row.monEquipe = rs.getString("nom2"); row.adversaire = rs.getString("nom1"); row.score = s2 + " - " + s1;
                    if (s2 > s1) row.victoire = true; else if (s2 < s1) row.defaite = true; else row.nul = true;
                }
                res.add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return res;
    }

    private List<Integer> getTeamMembers(int idEquipe, Map<Integer, List<Integer>> cache) {
        if (cache.containsKey(idEquipe)) return cache.get(idEquipe);
        List<Integer> ids = new ArrayList<>();
        try (java.sql.PreparedStatement pst = con.prepareStatement("SELECT id_joueur FROM composition WHERE id_equipe = ?")) {
            pst.setInt(1, idEquipe);
            java.sql.ResultSet rs = pst.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) { e.printStackTrace(); }
        cache.put(idEquipe, ids);
        return ids;
    }
    
    private Span createStatusDot(String color) {
        Span dot = new Span();
        dot.getStyle().set("width", "12px").set("height", "12px")
           .set("border-radius", "50%").set("display", "inline-block")
           .set("margin-right", "8px").set("background-color", color);
        if("gray".equals(color)) dot.getStyle().set("background-color", "#ccc");
        return dot;
    }
}