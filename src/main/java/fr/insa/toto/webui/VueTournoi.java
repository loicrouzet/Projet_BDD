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
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
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
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Match;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.model.Utilisateur;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    
    private Tabs rondeTabs;
    private VerticalLayout rondeContent;
    private Tab inscriptionTab;
    private Map<Tab, Ronde> tabToRondeMap = new HashMap<>();
    
    private boolean canEdit = false;

    public VueTournoi() {
        try {
            this.con = ConnectionSimpleSGBD.defaultCon();
            this.currentUser = (Utilisateur) VaadinSession.getCurrent().getAttribute("user");
        } catch (SQLException ex) { this.add(new Span("Erreur BDD")); }
    }

    @Override
    public void setParameter(BeforeEvent event, Integer tournoiId) {
        try {
            Optional<Tournoi> t = Tournoi.getById(this.con, tournoiId);
            if (t.isPresent()) {
                this.tournoi = t.get();
                this.canEdit = currentUser != null && currentUser.isAdmin() 
                               && currentUser.getIdClub() != null
                               && currentUser.getIdClub() == tournoi.getLeClub().getId();
                buildUI();
            } else { this.add(new H1("Tournoi introuvable")); }
        } catch (SQLException ex) { Notification.show("Erreur : " + ex.getMessage()); }
    }

    private void buildUI() {
        this.removeAll();

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull(); header.setJustifyContentMode(JustifyContentMode.BETWEEN); header.setAlignItems(Alignment.CENTER);
        Button backBtn = new Button("Retour", new Icon(VaadinIcon.ARROW_LEFT));
        backBtn.addClickListener(e -> backBtn.getUI().ifPresent(ui -> ui.navigate(VuePrincipale.class)));
        header.add(backBtn, new H1(tournoi.getNom()));
        this.add(header);

        rondeTabs = new Tabs();
        rondeContent = new VerticalLayout();
        rondeContent.setSizeFull();
        
        // ONGLET 1 : Inscriptions
        inscriptionTab = new Tab("1. Inscriptions & Équipes");
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

        this.add(rondeTabs, rondeContent);
        rondeTabs.setSelectedTab(inscriptionTab);
        showInscriptionContent();
    }
    
    // --- GESTION DES INSCRIPTIONS ET GENERATION ---
    
    private void showInscriptionContent() {
        rondeContent.removeAll();
        H2 titre = new H2("Inscription des Joueurs");
        
        // Grille des joueurs DISPONIBLES (tous les joueurs de la BDD)
        Grid<Joueur> availableGrid = new Grid<>(Joueur.class, false);
        availableGrid.addColumn(j -> j.getPrenom() + " " + j.getNom()).setHeader("Joueurs Disponibles");
        
        // Grille des joueurs INSCRITS au tournoi
        Grid<Joueur> inscritGrid = new Grid<>(Joueur.class, false);
        inscritGrid.addColumn(j -> j.getPrenom() + " " + j.getNom()).setHeader("Inscrits au Tournoi");
        
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
        
        // ZONE DE GÉNÉRATION (Visible pour admin)
        if (canEdit) {
            VerticalLayout genZone = new VerticalLayout();
            genZone.setAlignItems(Alignment.CENTER);
            
            int nbJoueursReq = tournoi.getLeLoisir().getNbJoueursEquipe();
            Span infoSport = new Span("Sport : " + tournoi.getLeLoisir().getNom() + " (Format: Équipes de " + nbJoueursReq + " joueurs)");
            infoSport.getStyle().set("font-weight", "bold").set("color", "blue");
            
            Button genererEquipesBtn = new Button("Générer les Équipes et Créer une Ronde", new Icon(VaadinIcon.MAGIC));
            genererEquipesBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            genererEquipesBtn.addClickListener(e -> openGenerationDialog(nbJoueursReq));

            genZone.add(new Hr(), infoSport, genererEquipesBtn);
            rondeContent.add(genZone);
        }
    }

    private void updateGrids(Grid<Joueur> g1, Grid<Joueur> g2) {
        try {
            List<Joueur> all = Joueur.getByClub(con, 0); // Utilise la méthode modifiée qui renvoie tout le monde
            List<Joueur> inscrits = Joueur.getInscritsAuTournoi(con, tournoi.getId());
            List<Integer> idsInscrits = inscrits.stream().map(Joueur::getId).toList();
            
            List<Joueur> dispos = all.stream().filter(j -> !idsInscrits.contains(j.getId())).toList();
            
            g1.setItems(dispos);
            g2.setItems(inscrits);
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void openGenerationDialog(int nbParEquipe) {
        Dialog d = new Dialog(); d.setHeaderTitle("Génération Aléatoire");
        TextField nomRonde = new TextField("Nom de la première ronde"); nomRonde.setValue("Phase de Poules");
        
        Button valider = new Button("Mélanger et Générer", e -> {
            try {
                List<Joueur> pool = Joueur.getInscritsAuTournoi(con, tournoi.getId());
                if (pool.size() < nbParEquipe * 2) {
                    Notification.show("Pas assez de joueurs inscrits !"); return;
                }
                
                Collections.shuffle(pool); // MÉLANGE
                
                int numEquipe = 1;
                List<Equipe> nouvellesEquipes = new ArrayList<>();
                
                for (int i = 0; i < pool.size(); i += nbParEquipe) {
                    if (i + nbParEquipe <= pool.size()) {
                        Equipe eq = new Equipe("Eq-" + numEquipe, tournoi.getId());
                        int idEq = eq.saveInDB(con);
                        nouvellesEquipes.add(eq);
                        
                        for (int j = 0; j < nbParEquipe; j++) {
                            Joueur joueur = pool.get(i + j);
                            joueur.setIdEquipe(idEq);
                            joueur.update(con);
                        }
                        
                        // IMPORTANT : Inscription table de liaison pour les matchs
                        eq.inscrireATournoi(con, tournoi.getId());
                        numEquipe++;
                    }
                }
                
                // Création auto de la ronde
                Ronde ronde = new Ronde(nomRonde.getValue(), Ronde.TYPE_POULE, tournoi.getId());
                ronde.saveInDB(con);

                Notification.show(nouvellesEquipes.size() + " équipes générées !");
                d.close();
                buildUI(); 
            } catch (SQLException ex) { Notification.show("Erreur : " + ex.getMessage()); }
        });
        
        d.add(new VerticalLayout(new Span("Création d'équipes de " + nbParEquipe + " joueurs."), nomRonde, valider));
        d.open();
    }

    // --- LOGIQUE EXISTANTE POUR LES RONDES ---
    
    private void loadRondes() {
        // Nettoyage des onglets sauf Inscription
        while (rondeTabs.getComponentCount() > 1) { rondeTabs.remove(rondeTabs.getComponentAt(1)); }
        tabToRondeMap.clear();
        
        try {
            List<Ronde> rondes = Ronde.getByTournoi(this.con, tournoi.getId());
            for (Ronde r : rondes) {
                Tab tab = new Tab(r.getNom());
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
            Button addMatch = new Button("Ajouter Match", new Icon(VaadinIcon.PLUS));
            addMatch.addClickListener(e -> openAddMatchDialog(ronde));
            actions.add(addMatch);
        }
        rondeContent.add(actions);

        // Affichage des matchs
        try {
            List<Match> matchs = Match.getByRonde(this.con, ronde.getId());
            Grid<Match> gridMatchs = new Grid<>(Match.class, false);
            gridMatchs.addColumn(m -> m.getLabel()).setHeader("Info");
            gridMatchs.addColumn(m -> m.getEquipe1() != null ? m.getEquipe1().getNom() : "TBD").setHeader("Équipe 1");
            gridMatchs.addColumn(m -> m.isEstJoue() ? m.getScore1() + " - " + m.getScore2() : "vs").setHeader("Score");
            gridMatchs.addColumn(m -> m.getEquipe2() != null ? m.getEquipe2().getNom() : "TBD").setHeader("Équipe 2");
            
            if (canEdit) {
                gridMatchs.addComponentColumn(m -> {
                    Button edit = new Button(new Icon(VaadinIcon.EDIT));
                    edit.addClickListener(e -> openScoreDialog(m, () -> showRondeContent(ronde)));
                    return edit;
                });
            }
            gridMatchs.setItems(matchs);
            rondeContent.add(gridMatchs);
        } catch(SQLException ex) {}
    }

    // (Gardez les méthodes openAddMatchDialog et openScoreDialog du fichier original ici)
    // Pour la concision de la réponse, je mets juste les stubs, mais copiez le contenu précédent ici :
    
    private void openAddMatchDialog(Ronde ronde) {
        Dialog d = new Dialog(); d.setHeaderTitle("Ajouter Match");
        ComboBox<Equipe> eq1 = new ComboBox<>("Équipe 1");
        ComboBox<Equipe> eq2 = new ComboBox<>("Équipe 2");
        TextField label = new TextField("Label");
        DateTimePicker datePicker = new DateTimePicker("Date");
        try {
            List<Equipe> equipes = Equipe.getByTournoi(this.con, tournoi.getId());
            eq1.setItems(equipes); eq1.setItemLabelGenerator(Equipe::getNom);
            eq2.setItems(equipes); eq2.setItemLabelGenerator(Equipe::getNom);
        } catch (SQLException ex) {}
        Button save = new Button("Ajouter", e -> {
            try {
                new Match(tournoi.getId(), ronde.getId(), eq1.getValue(), eq2.getValue(), label.getValue(), datePicker.getValue()).saveInDB(this.con);
                showRondeContent(ronde); d.close();
            } catch(SQLException ex) { Notification.show("Erreur"); }
        });
        d.add(new VerticalLayout(eq1, eq2, label, datePicker, save)); d.open();
    }
    
    private void openScoreDialog(Match m, Runnable onSave) {
        Dialog d = new Dialog(); d.setHeaderTitle("Score");
        NumberField s1 = new NumberField("Score 1"); s1.setValue((double)m.getScore1());
        NumberField s2 = new NumberField("Score 2"); s2.setValue((double)m.getScore2());
        Button save = new Button("Sauvegarder", e -> {
            try {
                m.updateScore(this.con, s1.getValue().intValue(), s2.getValue().intValue(), true);
                onSave.run(); d.close();
            } catch(SQLException ex) {}
        });
        d.add(new VerticalLayout(s1, s2, save)); d.open();
    }
}