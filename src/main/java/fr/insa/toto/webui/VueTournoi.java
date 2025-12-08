package fr.insa.toto.webui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Equipe;
import fr.insa.toto.model.Match;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.model.Utilisateur;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "tournoi")
public class VueTournoi extends VerticalLayout implements HasUrlParameter<Integer> {

    private Connection con;
    private Utilisateur currentUser;
    private Tournoi tournoi;
    
    // UI Elements pour gérer les onglets de rondes
    private Tabs rondeTabs;
    private VerticalLayout rondeContent;
    private Map<Tab, Ronde> tabToRondeMap = new HashMap<>();
    
    // Droit d'édition calculé (Admin du BON club)
    private boolean canEdit = false;

    public VueTournoi() {
        try {
            this.con = ConnectionSimpleSGBD.defaultCon();
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
                // --- VÉRIFICATION DES DROITS ---
                // On peut éditer seulement si on est ADMIN et qu'on appartient au CLUB du tournoi
                this.canEdit = currentUser != null 
                               && currentUser.isAdmin() 
                               && currentUser.getIdClub() != null
                               && currentUser.getIdClub() == tournoi.getLeClub().getId();
                               
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
        H1 title = new H1(tournoi.getNom());
        header.add(backBtn, title);
        this.add(header);

        // --- Configuration (Affiché uniquement si droits d'édition) ---
        if (canEdit) {
            HorizontalLayout adminActions = new HorizontalLayout();
            adminActions.setAlignItems(Alignment.BASELINE);
            
            Button addRondeBtn = new Button("Nouvelle Ronde", new Icon(VaadinIcon.PLUS));
            addRondeBtn.addClickListener(e -> openCreateRondeDialog());
            adminActions.add(addRondeBtn);
            
            Button configPointsBtn = new Button("Configurer Points", new Icon(VaadinIcon.COG));
            configPointsBtn.addClickListener(e -> openConfigPointsDialog());
            adminActions.add(configPointsBtn);
            
            this.add(adminActions);
        }

        // --- Système d'Onglets pour les Rondes ---
        rondeTabs = new Tabs();
        rondeContent = new VerticalLayout();
        rondeContent.setSizeFull();
        
        // Charger les rondes existantes
        loadRondes();
        
        rondeTabs.addSelectedChangeListener(event -> {
            Ronde selectedRonde = tabToRondeMap.get(event.getSelectedTab());
            if (selectedRonde != null) {
                showRondeContent(selectedRonde);
            }
        });

        this.add(rondeTabs, rondeContent);
    }
    
    private void loadRondes() {
        rondeTabs.removeAll();
        tabToRondeMap.clear();
        try {
            List<Ronde> rondes = Ronde.getByTournoi(this.con, tournoi.getId());
            for (Ronde r : rondes) {
                Tab tab = new Tab(r.getNom());
                rondeTabs.add(tab);
                tabToRondeMap.put(tab, r);
            }
            if (!rondes.isEmpty()) {
                rondeTabs.setSelectedIndex(0);
                showRondeContent(rondes.get(0));
            } else {
                rondeContent.removeAll();
                rondeContent.add(new Span("Aucune ronde créée pour ce tournoi."));
            }
        } catch (SQLException ex) { Notification.show("Erreur chargement rondes"); }
    }

    // --- AFFICHAGE DU CONTENU D'UNE RONDE ---
    private void showRondeContent(Ronde ronde) {
        rondeContent.removeAll();
        
        HorizontalLayout actions = new HorizontalLayout();
        actions.setAlignItems(Alignment.BASELINE);
        actions.add(new H2(ronde.getNom()));
        
        // Boutons d'action contextuels (Admin du club uniquement)
        if (canEdit) {
            if (ronde.getTypeRonde() == Ronde.TYPE_POULE) {
                Button manageGroups = new Button("Gérer les Poules", new Icon(VaadinIcon.USERS));
                manageGroups.addClickListener(e -> openManagePoolsDialog());
                actions.add(manageGroups);
            }
            
            // Pour tous les types sauf phase finale générée auto, on peut ajouter des matchs manuellement
            Button addMatch = new Button("Ajouter Match", new Icon(VaadinIcon.PLUS));
            addMatch.addClickListener(e -> openAddMatchDialog(ronde));
            actions.add(addMatch);
        }
        
        rondeContent.add(actions);

        try {
            List<Match> matchs = Match.getByRonde(this.con, ronde.getId());

            // --- CAS 1 : PHASE FINALE (ARBRE) ---
            if (ronde.getTypeRonde() == Ronde.TYPE_PHASE_FINALE) {
                rondeContent.add(createBracketDisplay(matchs));
            } 
            // --- CAS 2 : TABLEAU CLASSIQUE (POULES OU CHAMPIONNAT) ---
            else {
                Grid<Match> gridMatchs = new Grid<>(Match.class, false);
                gridMatchs.addColumn(m -> m.getLabel() != null && !m.getLabel().isEmpty() ? m.getLabel() : (m.getDateHeure() != null ? m.getDateHeure().toString().replace("T", " ") : "-")).setHeader("Info");
                gridMatchs.addColumn(m -> m.getEquipe1() != null ? m.getEquipe1().getNom() : "TBD").setHeader("Équipe 1");
                gridMatchs.addColumn(m -> m.isEstJoue() ? m.getScore1() + " - " + m.getScore2() : "vs").setHeader("Score").setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
                gridMatchs.addColumn(m -> m.getEquipe2() != null ? m.getEquipe2().getNom() : "TBD").setHeader("Équipe 2");
                
                if (canEdit) {
                    gridMatchs.addComponentColumn(m -> {
                        Button scoreBtn = new Button(new Icon(VaadinIcon.EDIT));
                        scoreBtn.addClickListener(e -> openScoreDialog(m, () -> showRondeContent(ronde)));
                        return scoreBtn;
                    });
                }
                gridMatchs.setItems(matchs);
                rondeContent.add(gridMatchs);
            }
            
            // --- CLASSEMENT (Sauf pour Phase Finale) ---
            if (ronde.getTypeRonde() != Ronde.TYPE_PHASE_FINALE) {
                rondeContent.add(new H2("Classement"));
                if (ronde.getTypeRonde() == Ronde.TYPE_POULE) {
                    displayPoolRankings(ronde);
                } else {
                    Grid<ClassementRow> rankingGrid = createRankingGrid();
                    rankingGrid.setItems(calculateRanking(ronde, null));
                    rondeContent.add(rankingGrid);
                }
            }
            
        } catch(SQLException ex) { Notification.show("Erreur chargement données"); }
    }
    
    // --- COMPOSANT ARBRE DE TOURNOI (BRACKET) ---
    private HorizontalLayout createBracketDisplay(List<Match> matchs) {
        HorizontalLayout bracketLayout = new HorizontalLayout();
        bracketLayout.setWidthFull();
        bracketLayout.setHeight("600px"); // Hauteur fixe pour le scrolling si besoin
        bracketLayout.getStyle().set("overflow-x", "auto");
        
        // Séparer les matchs par phase selon leur label
        List<Match> f16 = new ArrayList<>();
        List<Match> f8 = new ArrayList<>();
        List<Match> f4 = new ArrayList<>(); // Quarts
        List<Match> f2 = new ArrayList<>(); // Demis
        List<Match> f1 = new ArrayList<>(); // Finale
        
        for (Match m : matchs) {
            String lbl = m.getLabel().toLowerCase();
            if (lbl.contains("1/16")) f16.add(m);
            else if (lbl.contains("1/8")) f8.add(m);
            else if (lbl.contains("1/4") || lbl.contains("quart")) f4.add(m);
            else if (lbl.contains("1/2") || lbl.contains("demi")) f2.add(m);
            else if (lbl.contains("finale")) f1.add(m);
        }
        
        // Ajouter les colonnes si elles contiennent des matchs
        if (!f16.isEmpty()) bracketLayout.add(createBracketColumn("1/16èmes", f16));
        if (!f8.isEmpty()) bracketLayout.add(createBracketColumn("1/8èmes", f8));
        if (!f4.isEmpty()) bracketLayout.add(createBracketColumn("Quarts", f4));
        if (!f2.isEmpty()) bracketLayout.add(createBracketColumn("Demi-Finales", f2));
        if (!f1.isEmpty()) bracketLayout.add(createBracketColumn("Finale", f1));
        
        return bracketLayout;
    }
    
    private VerticalLayout createBracketColumn(String title, List<Match> phaseMatchs) {
        VerticalLayout col = new VerticalLayout();
        col.setAlignItems(Alignment.CENTER);
        col.setWidth("250px");
        // Espace les matchs équitablement pour centrer visuellement par rapport à la colonne précédente
        col.setJustifyContentMode(JustifyContentMode.EVENLY); 
        col.setHeightFull();
        
        col.add(new H4(title));
        
        for (Match m : phaseMatchs) {
            Div matchCard = new Div();
            matchCard.getStyle()
                .set("border", "1px solid #ccc")
                .set("border-radius", "5px")
                .set("padding", "10px")
                .set("width", "100%")
                .set("background-color", "#f9f9f9")
                .set("box-shadow", "2px 2px 5px rgba(0,0,0,0.1)");
            
            String eq1 = m.getEquipe1() != null ? m.getEquipe1().getNom() : "TBD";
            String eq2 = m.getEquipe2() != null ? m.getEquipe2().getNom() : "TBD";
            String score = m.isEstJoue() ? (m.getScore1() + " - " + m.getScore2()) : "vs";
            
            // Affichage compact
            Div t1 = new Div(new Span(eq1));
            t1.getStyle().set("font-weight", m.getScore1() > m.getScore2() && m.isEstJoue() ? "bold" : "normal");
            Div sc = new Div(new Span(score));
            sc.getStyle().set("text-align", "center").set("font-weight", "bold").set("margin", "5px 0");
            Div t2 = new Div(new Span(eq2));
            t2.getStyle().set("font-weight", m.getScore2() > m.getScore1() && m.isEstJoue() ? "bold" : "normal");
            
            matchCard.add(t1, sc, t2);
            
            // Si admin, clic pour modifier
            if (canEdit) {
                matchCard.getStyle().set("cursor", "pointer");
                matchCard.addClickListener(e -> openScoreDialog(m, () -> showRondeContent(tabToRondeMap.get(rondeTabs.getSelectedTab()))));
                matchCard.setTitle("Cliquez pour modifier");
            }
            
            col.add(matchCard);
        }
        return col;
    }
    
    // --- LOGIQUE POULES ---
    private void displayPoolRankings(Ronde ronde) {
        try {
            List<String> poules = new ArrayList<>();
            try(PreparedStatement pst = con.prepareStatement("select distinct poule from inscription where id_tournoi=? and poule is not null order by poule")) {
                pst.setInt(1, tournoi.getId());
                ResultSet rs = pst.executeQuery();
                while(rs.next()) poules.add(rs.getString(1));
            }
            
            if(poules.isEmpty()) {
                rondeContent.add(new Span("Aucune poule définie. Allez dans 'Gérer les Poules'."));
                Grid<ClassementRow> rankingGrid = createRankingGrid();
                rankingGrid.setItems(calculateRanking(ronde, null));
                rondeContent.add(rankingGrid);
            } else {
                HorizontalLayout poolsLayout = new HorizontalLayout();
                poolsLayout.setWidthFull();
                poolsLayout.setWrap(true); // Permet de passer à la ligne s'il y a beaucoup de poules
                
                for (String p : poules) {
                    VerticalLayout pLayout = new VerticalLayout();
                    pLayout.setWidth("400px"); // Largeur fixe par poule
                    pLayout.add(new H4("Groupe " + p));
                    Grid<ClassementRow> g = createRankingGrid();
                    g.setAllRowsVisible(true); // CORRECTION: setHeightByRows -> setAllRowsVisible
                    g.setItems(calculateRanking(ronde, p));
                    pLayout.add(g);
                    poolsLayout.add(pLayout);
                }
                rondeContent.add(poolsLayout);
            }
        } catch(SQLException ex) {}
    }

    private Grid<ClassementRow> createRankingGrid() {
        Grid<ClassementRow> grid = new Grid<>(ClassementRow.class, false);
        grid.addColumn(ClassementRow::getRang).setHeader("#").setWidth("40px").setFlexGrow(0);
        grid.addColumn(ClassementRow::getNomEquipe).setHeader("Équipe").setAutoWidth(true);
        grid.addColumn(ClassementRow::getPoints).setHeader("Pts");
        grid.addColumn(ClassementRow::getJoues).setHeader("J");
        grid.addColumn(ClassementRow::getGagnes).setHeader("G");
        grid.addColumn(ClassementRow::getNuls).setHeader("N");
        grid.addColumn(ClassementRow::getPerdus).setHeader("P");
        return grid;
    }

    private List<ClassementRow> calculateRanking(Ronde ronde, String filterPoule) {
        try {
            List<Match> matchs = Match.getByRonde(this.con, ronde.getId());
            String sqlEquipes = "select e.id, e.nom from equipe e join inscription i on e.id = i.id_equipe where i.id_tournoi = ?";
            if (filterPoule != null) sqlEquipes += " and i.poule = ?";
            
            Map<Integer, ClassementRow> stats = new HashMap<>();
            try (PreparedStatement pst = con.prepareStatement(sqlEquipes)) {
                pst.setInt(1, tournoi.getId());
                if (filterPoule != null) pst.setString(2, filterPoule);
                ResultSet rs = pst.executeQuery();
                while(rs.next()) stats.put(rs.getInt("id"), new ClassementRow(rs.getString("nom")));
            }
            
            for (Match m : matchs) {
                if (m.isEstJoue() && m.getEquipe1() != null && m.getEquipe2() != null) {
                    ClassementRow r1 = stats.get(m.getEquipe1().getId());
                    ClassementRow r2 = stats.get(m.getEquipe2().getId());
                    if (r1 != null && r2 != null) { 
                        r1.joues++; r2.joues++;
                        if (m.getScore1() > m.getScore2()) { r1.points += tournoi.getPtsVictoire(); r1.gagnes++; r2.points += tournoi.getPtsDefaite(); r2.perdus++; }
                        else if (m.getScore2() > m.getScore1()) { r2.points += tournoi.getPtsVictoire(); r2.gagnes++; r1.points += tournoi.getPtsDefaite(); r1.perdus++; }
                        else { r1.points += tournoi.getPtsNul(); r1.nuls++; r2.points += tournoi.getPtsNul(); r2.nuls++; }
                    }
                }
            }
            List<ClassementRow> rows = new ArrayList<>(stats.values());
            Collections.sort(rows, (a, b) -> {
                int pts = Integer.compare(b.points, a.points);
                if (pts != 0) return pts;
                return Integer.compare(b.gagnes - b.perdus, a.gagnes - a.perdus); // Diff de buts simplifiée
            });
            for (int i = 0; i < rows.size(); i++) rows.get(i).rang = i + 1;
            return rows;
        } catch (SQLException ex) { return new ArrayList<>(); }
    }

    // --- DIALOGUES CRÉATION ET CONFIGURATION ---
    
    private void openConfigPointsDialog() {
        Dialog d = new Dialog(); d.setHeaderTitle("Configuration des Points");
        NumberField ptsV = new NumberField("Victoire"); ptsV.setValue((double)tournoi.getPtsVictoire());
        NumberField ptsN = new NumberField("Nul"); ptsN.setValue((double)tournoi.getPtsNul());
        NumberField ptsD = new NumberField("Défaite"); ptsD.setValue((double)tournoi.getPtsDefaite());
        Button save = new Button("Sauvegarder", e -> {
           tournoi.setPtsVictoire(ptsV.getValue().intValue());
           tournoi.setPtsNul(ptsN.getValue().intValue());
           tournoi.setPtsDefaite(ptsD.getValue().intValue());
           try { 
               tournoi.update(this.con); 
               Notification.show("Config sauvegardée"); 
               loadRondes(); // Rafraichir l'affichage
               d.close();
           } catch(SQLException ex) { Notification.show("Erreur update"); }
        });
        save.addClickShortcut(Key.ENTER);
        d.add(new VerticalLayout(ptsV, ptsN, ptsD, save));
        d.open();
    }
    
    private void openCreateRondeDialog() {
        Dialog d = new Dialog(); d.setHeaderTitle("Nouvelle Ronde");
        TextField nom = new TextField("Nom");
        ComboBox<String> type = new ComboBox<>("Type");
        type.setItems("Basique (Championnat)", "Matchs de Poule", "Phase Finale (Arbre)");
        
        ComboBox<String> tailleArbre = new ComboBox<>("Démarrer en");
        tailleArbre.setItems("Finale (2 équipes)", "Demi-finales (4)", "Quarts (8)", "1/8èmes (16)", "1/16èmes (32)");
        tailleArbre.setVisible(false);
        type.addValueChangeListener(e -> tailleArbre.setVisible(e.getValue().equals("Phase Finale (Arbre)")));
        
        Button create = new Button("Créer", e -> {
            if (nom.isEmpty() || type.isEmpty()) return;
            int typeId = 0;
            if (type.getValue().contains("Poule")) typeId = 1;
            else if (type.getValue().contains("Finale")) typeId = 2;
            
            try {
                Ronde r = new Ronde(nom.getValue(), typeId, tournoi.getId());
                int rondeId = r.saveInDB(this.con);
                
                // Génération automatique des slots pour l'arbre
                if (typeId == 2 && !tailleArbre.isEmpty()) {
                    int nbMatchs = 1;
                    String labelStart = "Finale";
                    if (tailleArbre.getValue().contains("Demi")) { nbMatchs = 2; labelStart = "1/2 Finale"; }
                    else if (tailleArbre.getValue().contains("Quarts")) { nbMatchs = 4; labelStart = "1/4 Finale"; }
                    else if (tailleArbre.getValue().contains("1/8")) { nbMatchs = 8; labelStart = "1/8 Finale"; }
                    else if (tailleArbre.getValue().contains("1/16")) { nbMatchs = 16; labelStart = "1/16 Finale"; }
                    
                    for (int i = 1; i <= nbMatchs; i++) {
                        new Match(tournoi.getId(), rondeId, null, null, labelStart + " " + i, null).saveInDB(this.con);
                    }
                    Notification.show("Arbre généré avec " + nbMatchs + " matchs.");
                }
                
                loadRondes();
                d.close();
            } catch(SQLException ex) { Notification.show("Erreur création ronde"); }
        });
        create.addClickShortcut(Key.ENTER);
        d.add(new VerticalLayout(nom, type, tailleArbre, create));
        d.open();
    }
    
    private void openManagePoolsDialog() {
        Dialog d = new Dialog(); d.setHeaderTitle("Assignation aux Poules"); d.setWidth("500px");
        VerticalLayout layout = new VerticalLayout();
        try {
            List<Equipe> equipes = Equipe.getByTournoi(this.con, tournoi.getId());
            for (Equipe eq : equipes) {
                HorizontalLayout row = new HorizontalLayout();
                row.setAlignItems(Alignment.BASELINE);
                Span name = new Span(eq.getNom()); name.setWidth("150px");
                TextField pouleField = new TextField(); pouleField.setPlaceholder("ex: A"); pouleField.setWidth("80px");
                try(PreparedStatement pst = con.prepareStatement("select poule from inscription where id_tournoi=? and id_equipe=?")) {
                    pst.setInt(1, tournoi.getId()); pst.setInt(2, eq.getId());
                    ResultSet rs = pst.executeQuery();
                    if(rs.next()) pouleField.setValue(rs.getString(1) == null ? "" : rs.getString(1));
                }
                Button saveLine = new Button(new Icon(VaadinIcon.CHECK));
                saveLine.addClickListener(e -> {
                    try(PreparedStatement pst = con.prepareStatement("update inscription set poule=? where id_tournoi=? and id_equipe=?")) {
                        if(pouleField.isEmpty()) pst.setNull(1, java.sql.Types.VARCHAR);
                        else pst.setString(1, pouleField.getValue().toUpperCase());
                        pst.setInt(2, tournoi.getId()); pst.setInt(3, eq.getId());
                        pst.executeUpdate();
                        Notification.show("Poule mise à jour");
                    } catch(SQLException ex) {}
                });
                row.add(name, pouleField, saveLine); layout.add(row);
            }
        } catch(SQLException ex) {}
        Button close = new Button("Fermer", e -> { d.close(); loadRondes(); });
        layout.add(close); d.add(layout); d.open();
    }

    private void openAddMatchDialog(Ronde ronde) {
        Dialog d = new Dialog(); d.setHeaderTitle("Ajouter Match");
        ComboBox<Equipe> eq1 = new ComboBox<>("Équipe 1");
        ComboBox<Equipe> eq2 = new ComboBox<>("Équipe 2");
        TextField label = new TextField("Label (ex: Grp A)");
        DateTimePicker datePicker = new DateTimePicker("Date");
        try {
            List<Equipe> equipes = Equipe.getByTournoi(this.con, tournoi.getId());
            eq1.setItems(equipes); eq1.setItemLabelGenerator(Equipe::getNom);
            eq2.setItems(equipes); eq2.setItemLabelGenerator(Equipe::getNom);
        } catch (SQLException ex) {}
        Button save = new Button("Ajouter", e -> {
            try {
                new Match(tournoi.getId(), ronde.getId(), eq1.getValue(), eq2.getValue(), label.getValue(), datePicker.getValue()).saveInDB(this.con);
                showRondeContent(ronde);
                d.close();
            } catch(SQLException ex) { Notification.show("Erreur"); }
        });
        save.addClickShortcut(Key.ENTER);
        d.add(new VerticalLayout(eq1, eq2, label, datePicker, save)); d.open();
    }
    
    private void openScoreDialog(Match m, Runnable onSave) {
        Dialog d = new Dialog(); d.setHeaderTitle("Modifier Match");
        ComboBox<Equipe> eq1 = new ComboBox<>("Équipe 1");
        ComboBox<Equipe> eq2 = new ComboBox<>("Équipe 2");
        try {
            List<Equipe> equipes = Equipe.getByTournoi(this.con, tournoi.getId());
            eq1.setItems(equipes); eq1.setItemLabelGenerator(Equipe::getNom);
            eq2.setItems(equipes); eq2.setItemLabelGenerator(Equipe::getNom);
            if(m.getEquipe1() != null) eq1.setValue(equipes.stream().filter(e->e.getId()==m.getEquipe1().getId()).findFirst().orElse(null));
            if(m.getEquipe2() != null) eq2.setValue(equipes.stream().filter(e->e.getId()==m.getEquipe2().getId()).findFirst().orElse(null));
        } catch(SQLException ex) {}
        NumberField s1 = new NumberField("Score 1"); s1.setValue((double)m.getScore1());
        NumberField s2 = new NumberField("Score 2"); s2.setValue((double)m.getScore2());
        Button save = new Button("Sauvegarder", e -> {
            try {
                if (eq1.getValue() != null || eq2.getValue() != null) {
                    try(PreparedStatement pst = con.prepareStatement("update match_tournoi set id_equipe1=?, id_equipe2=? where id=?")) {
                        if(eq1.getValue() != null) pst.setInt(1, eq1.getValue().getId()); else pst.setNull(1, java.sql.Types.INTEGER);
                        if(eq2.getValue() != null) pst.setInt(2, eq2.getValue().getId()); else pst.setNull(2, java.sql.Types.INTEGER);
                        pst.setInt(3, m.getId());
                        pst.executeUpdate();
                    }
                }
                m.updateScore(this.con, s1.getValue().intValue(), s2.getValue().intValue(), true);
                onSave.run();
                d.close();
            } catch(SQLException ex) { Notification.show("Erreur"); }
        });
        save.addClickShortcut(Key.ENTER);
        d.add(new VerticalLayout(eq1, s1, eq2, s2, save)); d.open();
    }

    public static class ClassementRow {
        int rang; String nomEquipe; int points = 0; int joues = 0; int gagnes = 0; int nuls = 0; int perdus = 0;
        public ClassementRow(String nom) { this.nomEquipe = nom; }
        public int getRang() { return rang; }
        public String getNomEquipe() { return nomEquipe; }
        public int getPoints() { return points; }
        public int getJoues() { return joues; }
        public int getGagnes() { return gagnes; }
        public int getNuls() { return nuls; }
        public int getPerdus() { return perdus; }
    }
}