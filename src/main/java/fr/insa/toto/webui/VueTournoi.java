package fr.insa.toto.webui;

import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import java.util.Arrays;
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
        header.setWidthFull(); 
        header.setJustifyContentMode(JustifyContentMode.BETWEEN); 
        header.setAlignItems(Alignment.CENTER);
        
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

        Button btnClassementGeneral = new Button("Classement", new Icon(VaadinIcon.TROPHY));
        btnClassementGeneral.addClickListener(e -> showGeneralRankingDialog());

        Button btnStats = new Button("Stats", new Icon(VaadinIcon.CHART));
        btnStats.addClickListener(e -> openStatisticsDialog());

        actionButtons.add(btnClassementGeneral, btnStats);

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
        loadRondes(); // Charge les onglets des rondes existantes
        
        rondeTabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == inscriptionTab) {
                showInscriptionContent();
            } else {
                Ronde selectedRonde = tabToRondeMap.get(event.getSelectedTab());
                if (selectedRonde != null) showRondeContent(selectedRonde);
            }
        });

        // Sélection par défaut
        if (rondeTabs.getComponentCount() > 1) {
            rondeTabs.setSelectedIndex(rondeTabs.getComponentCount() - 1); // Sélectionne la dernière ronde
        } else {
            rondeTabs.setSelectedTab(inscriptionTab);
            showInscriptionContent();
        }
    }
    
    // --- PARTIE 1 : INSCRIPTIONS ---
    
    private void showInscriptionContent() {
        rondeContent.removeAll();

        if (canEdit) {
            rondeContent.add(createInscriptionContent()); 
            rondeContent.add(new Hr());
        }

        H2 titre = new H2("Inscription des Joueurs");
        
        Grid<Joueur> availableGrid = new Grid<>(Joueur.class, false);
        availableGrid.addColumn(j -> j.getPrenom() + " " + j.getNom()).setHeader("Joueurs Disponibles");
        
        Grid<Joueur> inscritGrid = new Grid<>(Joueur.class, false);
        inscritGrid.addColumn(j -> j.getPrenom() + " " + j.getNom()).setHeader("Inscrits au Tournoi");
        
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
        
        Dialog d = new Dialog(); d.setHeaderTitle("Générer " + defaultName); d.setWidth("500px");

        LocalDateTime estimatedStart;
        try {
            List<Ronde> rondesExistantes = new ArrayList<>(tabToRondeMap.values());
            if (rondesExistantes.isEmpty()) {
                LocalDate dateBase = (tournoi.getDateDebut() != null) ? tournoi.getDateDebut() : LocalDate.now();
                estimatedStart = LocalDateTime.of(dateBase, tournoi.getHeureDebut());
            } else {
                Ronde lastRonde = rondesExistantes.get(rondesExistantes.size() - 1);
                List<Match> lastMatches = Match.getByRonde(con, lastRonde.getId());
                LocalDateTime maxTime = lastMatches.stream()
                    .map(Match::getDateHeure)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                estimatedStart = maxTime.plusMinutes(tournoi.getDureeMatch()).plusMinutes(tournoi.getTempsPause());
            }
        } catch (Exception e) { estimatedStart = LocalDateTime.now(); }

        DateTimePicker startPicker = new DateTimePicker("Début de la ronde");
        startPicker.setValue(estimatedStart);
        startPicker.setStep(java.time.Duration.ofMinutes(5));
        startPicker.setWidthFull();

        Button valider = new Button("Générer le planning", e -> {
            try {
                List<Joueur> pool = Joueur.getInscritsAuTournoi(con, tournoi.getId());
                if (pool.size() < nbParEquipe * 2) { 
                    Notification.show("Pas assez de joueurs inscrits !"); 
                    return; 
                }
                
                Collections.shuffle(pool);
                
                int numEquipe = 1;
                List<Equipe> nouvellesEquipes = new ArrayList<>();
                
                for (int i = 0; i < pool.size(); i += nbParEquipe) {
                    if (i + nbParEquipe <= pool.size()) {
                        Equipe eq = new Equipe("R" + nextNum + "-Eq" + numEquipe, tournoi.getId());
                        int idEq = eq.saveInDB(con); 
                        eq.setId(idEq);
                        nouvellesEquipes.add(eq);
                        
                        for (int j = 0; j < nbParEquipe; j++) {
                            Joueur joueur = pool.get(i + j);
                            joueur.setIdEquipe(idEq); 
                            joueur.update(con);
                            try (java.sql.Statement st = con.createStatement()) {
                                st.executeUpdate("INSERT INTO composition (id_equipe, id_joueur) VALUES (" + idEq + ", " + joueur.getId() + ")");
                            }
                        }
                        eq.inscrireATournoi(con, tournoi.getId());
                        numEquipe++;
                    }
                }
                
                Ronde ronde = new Ronde(defaultName, Ronde.TYPE_POULE, tournoi.getId());
                int idRonde = ronde.saveInDB(con);

                List<Terrain> terrainsDispo = tournoi.getTerrainsSelectionnes(con);
                int nbTerrains = Math.max(1, terrainsDispo.size());
                
                int dureeMatch = tournoi.getDureeMatch();
                int pause = tournoi.getTempsPause();
                int slotTotal = dureeMatch + pause;

                LocalDateTime cursorTime = startPicker.getValue();
                int matchsDansCeCreneau = 0;

                for (int i = 0; i < nouvellesEquipes.size() - 1; i += 2) {
                    Terrain t = (!terrainsDispo.isEmpty()) ? terrainsDispo.get(matchsDansCeCreneau % nbTerrains) : null;
                    String infoT = (t != null) ? " (" + t.getNom() + ")" : "";
                    
                    new Match(tournoi.getId(), idRonde, nouvellesEquipes.get(i), nouvellesEquipes.get(i+1), 
                              "M" + ((i/2)+1) + infoT, cursorTime).saveInDB(con);
                    
                    matchsDansCeCreneau++;
                    
                    if (matchsDansCeCreneau >= nbTerrains) {
                        cursorTime = cursorTime.plusMinutes(slotTotal);
                        matchsDansCeCreneau = 0;
                    }
                }
                
                d.close(); buildUI(); Notification.show("Ronde générée !");
            } catch (Exception ex) { 
                Notification.show("Erreur: " + ex.getMessage());
                ex.printStackTrace(); 
            }
        });
        valider.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        d.add(new Span("Configuration pour " + defaultName), startPicker, valider);
        d.open();
    }

    // --- PARTIE 3 : AFFICHAGE DES RONDES ---

    // Nouvelle méthode utilitaire pour la logique de couleur (Match ou Ronde)
    private String getStatusColor(boolean isFinished, LocalDateTime dateHeure) {
        if (isFinished) return "black"; // Terminé
        if (dateHeure == null || LocalDateTime.now().isBefore(dateHeure)) return "gray"; // À venir
        return "green"; // En cours
    }

    private void loadRondes() {
        tabToRondeMap.clear();
        try {
            List<Ronde> rondes = Ronde.getByTournoi(this.con, tournoi.getId());
            for (Ronde r : rondes) {
                // Logique pour la couleur de la Ronde
                List<Match> ms = Match.getByRonde(con, r.getId());
                String color = "gray"; 
                
                if (!ms.isEmpty()) {
                    boolean allFinished = ms.stream().allMatch(Match::isEstJoue);
                    if (allFinished) {
                        color = "black";
                    } else {
                        // On prend la date du premier match (ou le plus tôt)
                        LocalDateTime earliestStart = ms.stream()
                            .map(Match::getDateHeure)
                            .filter(Objects::nonNull)
                            .min(LocalDateTime::compareTo)
                            .orElse(LocalDateTime.MAX);
                        
                        color = getStatusColor(false, earliestStart);
                    }
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
            
            // 1. Colonne État avec la nouvelle logique de couleur
            gridMatchs.addComponentColumn(m -> createStatusDot(getStatusColor(m.isEstJoue(), m.getDateHeure())))
                      .setHeader("État").setAutoWidth(true).setFlexGrow(0);
                      
            gridMatchs.addColumn(m -> m.getLabel()).setHeader("Info");
            
            // 2. Colonnes Équipes cliquables (Modif demandée)
            gridMatchs.addComponentColumn(m -> createTeamLink(m.getEquipe1())).setHeader("Équipe 1");
            
            gridMatchs.addColumn(m -> m.getScore1() + " - " + m.getScore2()).setHeader("Score");
            
            gridMatchs.addComponentColumn(m -> createTeamLink(m.getEquipe2())).setHeader("Équipe 2");

            gridMatchs.addColumn(m -> m.getDateHeure() != null ? m.getDateHeure().toLocalTime().toString() : "-")
            .setHeader("Heure")
            .setSortable(true)
            .setAutoWidth(true);
            
            if (canEdit) {
                gridMatchs.addComponentColumn(m -> {
                    Button edit = new Button(new Icon(VaadinIcon.EDIT));
                    edit.addClickListener(e -> openScoreDialog(m));
                    return edit;
                });
            }
            gridMatchs.setItems(matchs);
            rondeContent.add(gridMatchs);

            rondeContent.add(new Hr(), new H3("Classement de la Ronde"));
            Grid<ClassementRow> rankGrid = createRankingGrid();
            rankGrid.setItems(calculateRanking(matchs));
            rondeContent.add(rankGrid);

        } catch(SQLException ex) { ex.printStackTrace(); }
    }

    // Méthode pour créer le bouton/lien vers l'équipe
    private Component createTeamLink(Equipe e) {
        if (e == null) return new Span("TBD");
        Button btn = new Button(e.getNom());
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        // Style pour ressembler à un lien
        btn.getStyle().set("text-decoration", "underline").set("color", "#007bff").set("cursor", "pointer");
        btn.addClickListener(event -> showTeamPlayersDialog(e));
        return btn;
    }

    // Nouvelle modale pour afficher les joueurs d'une équipe
    private void showTeamPlayersDialog(Equipe e) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Joueurs : " + e.getNom());
        d.setWidth("400px");

        Grid<Joueur> grid = new Grid<>(Joueur.class, false);
        grid.addColumn(Joueur::getNom).setHeader("Nom");
        grid.addColumn(Joueur::getPrenom).setHeader("Prénom");

        try {
            List<Joueur> players = new ArrayList<>();
            // Requête pour récupérer les joueurs liés à cette équipe
            String sql = "SELECT * FROM joueur WHERE id_equipe = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, e.getId());
                ResultSet rs = pst.executeQuery();
                while(rs.next()) {
                    // Construction manuelle rapide pour l'affichage
                    // On utilise le constructeur qui accepte l'ID pour être propre
                    // Note: id_club et id_utilisateur peuvent être null/0, on gère basiquement
                    int uId = rs.getInt("id_utilisateur");
                    Integer userId = rs.wasNull() ? null : uId;
                    
                    Joueur j = new Joueur(
                        rs.getInt("id"), 
                        rs.getString("nom"), 
                        rs.getString("prenom"), 
                        rs.getInt("id_equipe"), 
                        rs.getInt("id_club"), 
                        userId
                    );
                    players.add(j);
                }
            }
            if (players.isEmpty()) {
                d.add(new Span("Aucun joueur dans cette équipe."));
            } else {
                grid.setItems(players);
                d.add(grid);
            }
        } catch (SQLException ex) {
            d.add(new Span("Erreur lors de la récupération des joueurs : " + ex.getMessage()));
        }

        Button close = new Button("Fermer", event -> d.close());
        d.getFooter().add(close);
        d.open();
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
    d.setHeaderTitle("Profil : " + j.getPrenom() + " " + j.getNom());
    d.setWidth("700px");

    List<HistoryRow> rows = fetchPlayerHistory(j.getId());

    int total = rows.size();
    int victoires = (int) rows.stream().filter(r -> r.victoire).count();
    int ratio = total > 0 ? (victoires * 100) / total : 0;

    HorizontalLayout formeLayout = new HorizontalLayout();
    formeLayout.setAlignItems(Alignment.CENTER);
    formeLayout.add(new Span("Forme : "));
    
    for (int i = 0; i < Math.min(5, rows.size()); i++) { 
        HistoryRow r = rows.get(i); 
        Icon icon;
        if (r.victoire) { icon = VaadinIcon.CHECK_CIRCLE.create(); icon.setColor("green"); }
        else if (r.defaite) { icon = VaadinIcon.CLOSE_CIRCLE.create(); icon.setColor("red"); }
        else { icon = VaadinIcon.MINUS_CIRCLE.create(); icon.setColor("gray"); }
        formeLayout.add(icon);
    }

    VerticalLayout infos = new VerticalLayout();
    infos.setSpacing(false);
    infos.add(new H3(ratio + "% de victoires (" + victoires + "/" + total + ")"));
    infos.add(formeLayout);
    
    if(j.getNomClub() != null && !j.getNomClub().isEmpty()) infos.add(new Span("Club : " + j.getNomClub()));
    
    Grid<HistoryRow> hGrid = new Grid<>();
    hGrid.addColumn(row -> row.ronde).setHeader("Ronde").setAutoWidth(true);
    hGrid.addColumn(row -> row.monEquipe).setHeader("Mon Équipe");
    hGrid.addColumn(row -> row.adversaire).setHeader("Adversaire");
    hGrid.addColumn(row -> row.score).setHeader("Score").setAutoWidth(true);
    hGrid.addComponentColumn(row -> {
        String color = row.victoire ? "green" : (row.defaite ? "red" : "gray");
        String text = row.victoire ? "VICTOIRE" : (row.defaite ? "DÉFAITE" : "NUL");
        Span s = new Span(text); s.getStyle().set("color", color).set("font-weight", "bold");
        return s;
    }).setHeader("Résultat");

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
    
    private void openScoreDialog(Match m) {
        Dialog d = new Dialog(); d.setHeaderTitle("Gestion du Match");
        d.setWidth("600px");

        HorizontalLayout scores = new HorizontalLayout();
        scores.setAlignItems(Alignment.BASELINE);
        NumberField s1 = new NumberField(m.getEquipe1() != null ? m.getEquipe1().getNom() : "Eq 1"); 
        s1.setValue((double)m.getScore1());
        Span sep = new Span("-");
        NumberField s2 = new NumberField(m.getEquipe2() != null ? m.getEquipe2().getNom() : "Eq 2"); 
        s2.setValue((double)m.getScore2());
        scores.add(s1, sep, s2);

        DateTimePicker datePicker = new DateTimePicker("Horaire du match");
        datePicker.setValue(m.getDateHeure());
        datePicker.setWidthFull();

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
        
        // On garde une référence à la colonne PTS
        Grid.Column<ClassementRow> colPts = grid.addColumn(ClassementRow::getPts).setHeader("PTS").setSortable(true);
        
        grid.addColumn(ClassementRow::getJoues).setHeader("J");
        grid.addColumn(ClassementRow::getVictoires).setHeader("G");
        grid.addColumn(ClassementRow::getNuls).setHeader("N");
        grid.addColumn(ClassementRow::getDefaites).setHeader("P");
        grid.addColumn(ClassementRow::getDiff).setHeader("Diff");

        // Force l'affichage du tri visuel (Flèche vers le bas)
        grid.sort(Arrays.asList(new GridSortOrder<>(colPts, SortDirection.DESCENDING)));
        
        return grid;
    }

    private List<ClassementRow> calculateRanking(List<Match> matchesToProcess) {
        Map<Integer, ClassementRow> stats = new HashMap<>();
        try {
            List<Joueur> allInscrits = Joueur.getInscritsAuTournoi(con, tournoi.getId());
            for (Joueur j : allInscrits) stats.put(j.getId(), new ClassementRow(j));
            
            Map<Integer, List<Integer>> teamCompoCache = new HashMap<>();
            
            for (Match m : matchesToProcess) {
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
        
        H3 titreConfig = new H3("Configuration du Tournoi");
        
        TextField nomField = new TextField("Nom du tournoi");
        nomField.setValue(tournoi.getNom());
        
        TimePicker heureDebutField = new TimePicker("Heure de début des matchs");
        heureDebutField.setValue(tournoi.getHeureDebut());
        
        IntegerField dureeField = new IntegerField("Durée d'un match (minutes)");
        dureeField.setValue(tournoi.getDureeMatch());
        dureeField.setMin(10); dureeField.setStep(5);
        
        IntegerField pauseField = new IntegerField("Pause / Transition (min)");
        pauseField.setValue(tournoi.getTempsPause());
        pauseField.setHelperText("Temps pour changer de terrain");

        CheckboxGroup<Terrain> terrainsSelect = new CheckboxGroup<>();
        terrainsSelect.setLabel("Terrains à utiliser");
        terrainsSelect.setItemLabelGenerator(Terrain::getNom);
        terrainsSelect.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        
        try {
            List<Terrain> tousTerrains = Terrain.getByClub(con, tournoi.getLeClub().getId());
            tousTerrains.removeIf(Terrain::isSousConstruction);
            terrainsSelect.setItems(tousTerrains);
            
            List<Terrain> dejaChoisis = tournoi.getTerrainsSelectionnes(con);
            terrainsSelect.setValue(new HashSet<>(dejaChoisis));
            
        } catch (SQLException e) { Notification.show("Err chargement terrains: " + e.getMessage()); }

        Button btnSaveConfig = new Button("Sauvegarder Configuration", e -> {
            try {
                tournoi.setNom(nomField.getValue());
                tournoi.setHeureDebut(heureDebutField.getValue());
                tournoi.setDureeMatch(dureeField.getValue());
                tournoi.setTempsPause(pauseField.getValue());
                tournoi.updateConfig(con);
                tournoi.setTerrainsSelectionnes(con, new ArrayList<>(terrainsSelect.getValue())); 
                
                Notification.show("Configuration sauvegardée !");
            } catch (SQLException ex) { Notification.show("Erreur sauvegarde : " + ex.getMessage()); }
        });

        layout.add(titreConfig, nomField, new HorizontalLayout(heureDebutField, dureeField), terrainsSelect, btnSaveConfig);
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
        
        // Ajustement des couleurs standards si nécessaire
        if("gray".equals(color)) dot.getStyle().set("background-color", "#ccc");
        if("black".equals(color)) dot.getStyle().set("background-color", "#000");
        if("green".equals(color)) dot.getStyle().set("background-color", "#28a745");

        return dot;
    }
    
    private void openStatisticsDialog() {
    Dialog d = new Dialog(); d.setHeaderTitle("Statistiques du Tournoi");
    d.setWidth("800px");

    try {
        List<Match> allMatches = new ArrayList<>();
        List<Ronde> rondes = Ronde.getByTournoi(con, tournoi.getId());
        for (Ronde r : rondes) {
            allMatches.addAll(Match.getByRonde(con, r.getId()));
        }
        List<Match> playedMatches = allMatches.stream().filter(Match::isEstJoue).collect(Collectors.toList());

        if (playedMatches.isEmpty()) {
            d.add(new Span("Aucun match joué pour le moment. Revenez plus tard !"));
            d.open();
            return;
        }

        int totalButs = playedMatches.stream().mapToInt(m -> m.getScore1() + m.getScore2()).sum();
        double moyButs = (double) totalButs / playedMatches.size();
        
        Match maxScoreMatch = playedMatches.stream()
            .max(Comparator.comparingInt(m -> m.getScore1() + m.getScore2()))
            .orElse(null);

        Map<String, Long> scoreFreq = playedMatches.stream()
            .map(m -> {
                int min = Math.min(m.getScore1(), m.getScore2());
                int max = Math.max(m.getScore1(), m.getScore2());
                return max + "-" + min; 
            })
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            
        String topScore = scoreFreq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("-");

        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.setJustifyContentMode(JustifyContentMode.CENTER);
        
        cards.add(createStatCard("Matchs Joués", String.valueOf(playedMatches.size()), "blue"));
        cards.add(createStatCard("Buts / Match", String.format("%.2f", moyButs), "green"));
        cards.add(createStatCard("Score Fétiche", topScore, "orange"));
        
        VerticalLayout details = new VerticalLayout();
        details.add(new H3("Faits Marquants"));
        
        if (maxScoreMatch != null) {
            String eq1 = maxScoreMatch.getEquipe1() != null ? maxScoreMatch.getEquipe1().getNom() : "?";
            String eq2 = maxScoreMatch.getEquipe2() != null ? maxScoreMatch.getEquipe2().getNom() : "?";
            details.add(new Span("🔥 Match le plus chaud : " + eq1 + " vs " + eq2 + " (" + maxScoreMatch.getScore1() + "-" + maxScoreMatch.getScore2() + ")"));
        }
        
        details.add(new Span("⚽ Total des buts inscrits : " + totalButs));
        
        d.add(cards, new Hr(), details);
        d.getFooter().add(new Button("Fermer", e -> d.close()));
        d.open();

    } catch (SQLException ex) { Notification.show("Erreur calcul stats"); }
}

private VerticalLayout createStatCard(String title, String value, String color) {
    VerticalLayout card = new VerticalLayout();
    card.setAlignItems(Alignment.CENTER);
    card.setSpacing(false);
    card.getStyle().set("background-color", "#f5f5f5").set("border-radius", "10px").set("padding", "15px");
    card.setWidth("150px");
    
    Span valSpan = new Span(value);
    valSpan.getStyle().set("font-size", "24px").set("font-weight", "bold").set("color", color);
    
    card.add(valSpan, new Span(title));
    return card;
}
}