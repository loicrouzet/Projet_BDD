package fr.insa.toto.webui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Club;
import fr.insa.toto.model.Equipe;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Loisir;
import fr.insa.toto.model.Terrain;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.model.Utilisateur;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.vaadin.flow.server.VaadinSession;
import fr.insa.beuvron.utils.database.ConnectionPool;

@Route(value = "")
@PageTitle("Gestion Tournois")
public class VuePrincipale extends VerticalLayout {

    private Connection con;
    private Utilisateur currentUser = null; 
    private Grid<Tournoi> grid; 
    private boolean isModeEdition = false;
    private HorizontalLayout formAjoutLayout;

    public VuePrincipale() {
        try {
            this.con = ConnectionPool.getConnection();
        } catch (SQLException ex) {
            this.add(new H3("Erreur BDD : " + ex.getMessage()));
            return;
        }
        
        // Récupération de la session s'il y en a une
        Utilisateur sessionUser = (Utilisateur) VaadinSession.getCurrent().getAttribute("user");
        if (sessionUser != null) {
            this.currentUser = sessionUser;
            showMainApplication();
        } else {
            showLoginScreen();
        }
    }

    private void showLoginScreen() {
        this.removeAll();
        H1 title = new H1("Connexion Multisport");
        TextField userField = new TextField("Utilisateur");
        PasswordField passField = new PasswordField("Mot de passe");
        Button loginButton = new Button("Se connecter", e -> {
            try {
                Optional<Utilisateur> user = Utilisateur.login(this.con, userField.getValue(), passField.getValue());
                if (user.isPresent()) {
                    this.currentUser = user.get();
                    VaadinSession.getCurrent().setAttribute("user", this.currentUser);
                    Notification.show("Bienvenue " + this.currentUser.getSurnom());
                    showMainApplication();
                } else { Notification.show("Identifiants incorrects", 3000, Notification.Position.MIDDLE); }
            } catch (SQLException ex) { Notification.show("Erreur technique : " + ex.getMessage()); }
        });
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClickShortcut(Key.ENTER);
        
        Button registerLink = new Button("Pas de compte ? Créer un compte", e -> showRegisterScreen());
        registerLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        VerticalLayout loginLayout = new VerticalLayout(title, userField, passField, loginButton, registerLink);
        loginLayout.setAlignItems(Alignment.CENTER); loginLayout.setJustifyContentMode(JustifyContentMode.CENTER); loginLayout.setSizeFull();
        this.add(loginLayout);
    }

    private void showRegisterScreen() {
        this.removeAll();
        H1 title = new H1("Créer un compte");
        TextField userField = new TextField("Pseudo");
        PasswordField passField = new PasswordField("Mot de passe");
        ComboBox<Club> clubSelect = new ComboBox<>("Mon Club (Optionnel)");
        try { clubSelect.setItems(Club.getAll(this.con)); clubSelect.setItemLabelGenerator(Club::getNom); } catch (SQLException e) {}
        RadioButtonGroup<String> roleSelect = new RadioButtonGroup<>();
        roleSelect.setItems("Visiteur", "Administrateur");
        roleSelect.setValue("Visiteur");
        PasswordField adminKeyField = new PasswordField("Clé Administrateur");
        adminKeyField.setPlaceholder("Saisir la clé secrète");
        adminKeyField.setVisible(false);
        roleSelect.addValueChangeListener(e -> { adminKeyField.setVisible(e.getValue().equals("Administrateur")); });
        
        Button createButton = new Button("S'inscrire", e -> {
            try {
                if (Utilisateur.existeSurnom(this.con, userField.getValue())) { Notification.show("Pseudo pris !"); return; }
                int roleId = 0;
                if (roleSelect.getValue().equals("Administrateur")) {
                    if ("toto".equals(adminKeyField.getValue())) { roleId = 1; } else { Notification.show("Clé administrateur incorrecte !"); return; }
                }
                Integer idClub = clubSelect.getValue() != null ? clubSelect.getValue().getId() : null;
                new Utilisateur(userField.getValue(), passField.getValue(), roleId, idClub).saveInDB(this.con);
                Notification.show("Compte créé !"); showLoginScreen();
            } catch (SQLException ex) { Notification.show("Erreur: " + ex.getMessage()); }
        });
        createButton.addClickShortcut(Key.ENTER);
        
        Button cancelButton = new Button("Annuler", e -> showLoginScreen());
        VerticalLayout l = new VerticalLayout(title, userField, passField, clubSelect, roleSelect, adminKeyField, createButton, cancelButton);
        l.setAlignItems(Alignment.CENTER); l.setJustifyContentMode(JustifyContentMode.CENTER); l.setSizeFull();
        this.add(l);
    }

    private void showMainApplication() {
        this.removeAll();
        HorizontalLayout header = new HorizontalLayout(); header.setWidthFull(); header.setJustifyContentMode(JustifyContentMode.BETWEEN); header.setAlignItems(Alignment.CENTER);
        HorizontalLayout leftHeader = new HorizontalLayout();
        H3 welcome = new H3("Espace " + (currentUser.isAdmin() ? "Admin" : "Visiteur"));
        leftHeader.add(welcome);
        
        if (currentUser.isAdmin()) {
            Button gestionClubBtn = new Button("Gérer mon Club", new Icon(VaadinIcon.BUILDING));
            gestionClubBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
            gestionClubBtn.addClickListener(e -> openGestionClubDialog());
            if (currentUser.getIdClub() == null) {
                gestionClubBtn.setText("Créer mon Club");
                gestionClubBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            }
            leftHeader.add(gestionClubBtn);
            
            Button toggleModeBtn = new Button("Mode: Consultation", new Icon(VaadinIcon.EYE));
            toggleModeBtn.addClickListener(e -> {
                this.isModeEdition = !this.isModeEdition;
                if (this.isModeEdition) { toggleModeBtn.setText("Mode: Édition"); toggleModeBtn.setIcon(new Icon(VaadinIcon.EDIT)); toggleModeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                } else { toggleModeBtn.setText("Mode: Consultation"); toggleModeBtn.setIcon(new Icon(VaadinIcon.EYE)); toggleModeBtn.removeThemeVariants(ButtonVariant.LUMO_PRIMARY); }
                updateViewVisibility();
            });
            leftHeader.add(toggleModeBtn);
        }
        
        Button logoutBtn = new Button("Déconnexion", e -> { 
            this.currentUser = null; 
            VaadinSession.getCurrent().setAttribute("user", null);
            showLoginScreen(); 
        });
        header.add(leftHeader, logoutBtn);
        this.add(header);
        
        this.add(new H1("Liste des Tournois"));
        if (currentUser.isAdmin()) {
            this.formAjoutLayout = createFormulaireAjout();
            this.add(formAjoutLayout);
        }
        setupGrid();
        updateGrid();
        updateViewVisibility();
    }
    
    private HorizontalLayout createFormulaireAjout() {
        HorizontalLayout form = new HorizontalLayout(); form.setWidthFull(); form.setAlignItems(Alignment.BASELINE);
        TextField nomField = new TextField("Nom tournoi");
        DatePicker dateField = new DatePicker("Date");
        ComboBox<Loisir> sportSelect = new ComboBox<>("Sport");
        
        Button addButton = new Button("Ajouter Tournoi", e -> {
            try {
                if (currentUser.getIdClub() == null) {
                    Notification.show("Erreur : Vous devez d'abord créer ou rejoindre un club !", 5000, Notification.Position.MIDDLE);
                    return;
                }
                if (nomField.isEmpty() || dateField.isEmpty() || sportSelect.isEmpty()) {
                    Notification.show("Remplissez tous les champs");
                    return;
                }
                new Tournoi(nomField.getValue(), dateField.getValue(), sportSelect.getValue(), 
                            new Club(currentUser.getIdClub(), "temp")).saveInDB(this.con);
                
                Notification.show("Tournoi ajouté par votre club !");
                nomField.clear(); dateField.clear(); sportSelect.clear();
                updateGrid();
            } catch (SQLException ex) { Notification.show("Erreur : " + ex.getMessage()); }
        });
        addButton.addClickShortcut(Key.ENTER);
        
        refreshCombos(sportSelect);
        if (currentUser.getIdClub() == null) {
            addButton.setEnabled(false);
            addButton.setText("Créez votre club d'abord");
        }
        form.add(nomField, dateField, sportSelect, addButton);
        return form;
    }
    
    // --- SETUP GRID (MODIFIÉ POUR SÉCURITÉ CLUB) ---
    private void setupGrid() {
        this.grid = new Grid<>(Tournoi.class, false);
        this.grid.addColumn(Tournoi::getNom).setHeader("Nom").setSortable(true);
        this.grid.addColumn(Tournoi::getDateDebut).setHeader("Date").setSortable(true);
        this.grid.addColumn(t -> t.getLeLoisir().getNom()).setHeader("Sport").setSortable(true);
        this.grid.addColumn(t -> t.getLeClub().getNom()).setHeader("Club").setSortable(true);
        
        this.grid.addComponentColumn(tournoi -> {
            Button openBtn = new Button(new Icon(VaadinIcon.ARROW_RIGHT));
            openBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            openBtn.setTooltipText("Voir le Planning & Classement");
            openBtn.addClickListener(e -> {
                openBtn.getUI().ifPresent(ui -> ui.navigate(VueTournoi.class, tournoi.getId()));
            });
            return openBtn;
        }).setHeader("Accéder").setAutoWidth(true);

        if (currentUser.isAdmin() && isModeEdition) {
            // Bouton Modifier : Visible seulement si l'admin appartient au club du tournoi
            this.grid.addComponentColumn(tournoi -> {
                boolean canEdit = currentUser.getIdClub() != null && currentUser.getIdClub() == tournoi.getLeClub().getId();
                if (canEdit) {
                    Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
                    editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    editBtn.addClickListener(e -> openEditTournoiDialog(tournoi));
                    return editBtn;
                } else {
                    return new Span(); // Élément vide
                }
            }).setHeader("Modifier").setAutoWidth(true);

            // Bouton Équipes : Visible seulement si l'admin appartient au club du tournoi
            this.grid.addComponentColumn(tournoi -> {
                boolean canEdit = currentUser.getIdClub() != null && currentUser.getIdClub() == tournoi.getLeClub().getId();
                if (canEdit) {
                    Button teamBtn = new Button(new Icon(VaadinIcon.USERS));
                    teamBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    teamBtn.addClickListener(e -> openGestionEquipesDialog(tournoi));
                    return teamBtn;
                } else {
                    return new Span();
                }
            }).setHeader("Équipes").setAutoWidth(true);
        }
        this.grid.setWidthFull(); 
        this.add(this.grid);
    }
    
    // ... (Le reste des méthodes dialogs openGestionClubDialog, openCreateClubDialog, etc. reste inchangé) ...
    
    private void openGestionClubDialog() {
        if (currentUser.getIdClub() == null) {
            openCreateClubDialog();
        } else {
            Dialog d = new Dialog(); d.setWidth("800px"); d.setHeight("600px");
            try {
                Optional<Club> c = Club.getById(this.con, currentUser.getIdClub());
                if (c.isEmpty()) return;
                d.setHeaderTitle("Gestion du Club : " + c.get().getNom());
                VerticalLayout content = new VerticalLayout(); content.setSizeFull();
                Grid<Equipe> gridEquipes = new Grid<>(Equipe.class, false);
                gridEquipes.addColumn(Equipe::getNom).setHeader("Mes Équipes");
                gridEquipes.addComponentColumn(eq -> {
                    Button editName = new Button(new Icon(VaadinIcon.EDIT)); editName.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    editName.addClickListener(e -> {
                        Dialog editD = new Dialog(); TextField tf = new TextField("Nouveau nom"); tf.setValue(eq.getNom());
                        Button save = new Button("Sauver", ev -> { eq.setNom(tf.getValue()); try { eq.update(this.con); gridEquipes.getDataProvider().refreshItem(eq); editD.close(); } catch(SQLException ex) { Notification.show("Erreur"); } });
                        editD.add(new VerticalLayout(tf, save)); editD.open();
                    }); return editName;
                }).setHeader("Renommer");
                gridEquipes.addComponentColumn(eq -> { Button effectifBtn = new Button("Effectif", new Icon(VaadinIcon.GROUP)); effectifBtn.addClickListener(e -> openGestionJoueursDialog(eq)); return effectifBtn; }).setHeader("Effectif");
                gridEquipes.addComponentColumn(eq -> {
                    Button delBtn = new Button(new Icon(VaadinIcon.TRASH)); delBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    delBtn.addClickListener(e -> { try { eq.delete(this.con); gridEquipes.setItems(Equipe.getByClub(this.con, currentUser.getIdClub())); } catch(SQLException ex) { Notification.show("Erreur suppression"); } });
                    return delBtn;
                });
                gridEquipes.setItems(Equipe.getByClub(this.con, currentUser.getIdClub()));
                HorizontalLayout addLayout = new HorizontalLayout(); TextField newTeamField = new TextField("Nouvelle équipe");
                Button addBtn = new Button("Créer", e -> { if (newTeamField.isEmpty()) return; try { new Equipe(newTeamField.getValue(), currentUser.getIdClub()).saveInDB(this.con); gridEquipes.setItems(Equipe.getByClub(this.con, currentUser.getIdClub())); newTeamField.clear(); Notification.show("Équipe créée"); } catch(SQLException ex) { Notification.show("Erreur"); } });
                addLayout.add(newTeamField, addBtn); addLayout.setAlignItems(Alignment.BASELINE);
                content.add(gridEquipes, addLayout); d.add(content); d.open();
            } catch (SQLException ex) { Notification.show("Erreur chargement club"); }
        }
    }
    
    private void openCreateClubDialog() {
        Dialog dialog = new Dialog(); dialog.setHeaderTitle("Créer mon Club");
        VerticalLayout dialogLayout = new VerticalLayout();
        TextField nomClubField = new TextField("Nom du Club"); nomClubField.setWidthFull();
        dialogLayout.add(nomClubField, new H4("Terrains initiaux"));
        VerticalLayout terrainsContainer = new VerticalLayout(); terrainsContainer.setSpacing(false); terrainsContainer.setPadding(false);
        List<TerrainRow> terrainRows = new ArrayList<>();
        Button addTerrainBtn = new Button("Ajouter un terrain", new Icon(VaadinIcon.PLUS_CIRCLE));
        addTerrainBtn.addClickListener(e -> { TerrainRow row = new TerrainRow(); terrainRows.add(row); terrainsContainer.add(row.layout); });
        addTerrainBtn.click();
        Button saveBtn = new Button("Créer et Lier à mon compte", e -> {
            try {
                if (nomClubField.isEmpty()) { Notification.show("Le nom du club est requis"); return; }
                Club newClub = new Club(nomClubField.getValue()); 
                int newClubId = newClub.saveInDB(this.con);
                for (TerrainRow row : terrainRows) { if (!row.nomField.isEmpty()) { new Terrain(row.nomField.getValue(), row.isIndoor.getValue(), newClubId).saveInDB(this.con); } }
                try (PreparedStatement pst = this.con.prepareStatement("update utilisateur set id_club=? where id=?")) {
                    pst.setInt(1, newClubId);
                    pst.setInt(2, currentUser.getId());
                    pst.executeUpdate();
                }
                Notification.show("Club créé ! Veuillez vous reconnecter.");
                this.currentUser = null; showLoginScreen();
                dialog.close();
            } catch (SQLException ex) { Notification.show("Erreur : " + ex.getMessage()); }
        });
        dialogLayout.add(terrainsContainer, addTerrainBtn, saveBtn); dialog.add(dialogLayout); dialog.open();
    }
    
    private void openGestionJoueursDialog(Equipe eq) {
        Dialog d = new Dialog(); d.setHeaderTitle("Effectif : " + eq.getNom());
        VerticalLayout layout = new VerticalLayout();
        Grid<Joueur> gridJoueurs = new Grid<>(Joueur.class, false);
        gridJoueurs.addColumn(Joueur::getNom).setHeader("Nom"); gridJoueurs.addColumn(Joueur::getPrenom).setHeader("Prénom");
        gridJoueurs.addComponentColumn(j -> {
            Button del = new Button(new Icon(VaadinIcon.TRASH)); del.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            del.addClickListener(e -> { try { j.delete(this.con); gridJoueurs.setItems(Joueur.getByEquipe(this.con, eq.getId())); } catch(SQLException ex) { Notification.show("Erreur"); } });
            return del;
        });
        try { gridJoueurs.setItems(Joueur.getByEquipe(this.con, eq.getId())); } catch(SQLException ex) {}
        HorizontalLayout addLayout = new HorizontalLayout(); TextField nomF = new TextField("Nom"); TextField prenomF = new TextField("Prénom");
        Button addBtn = new Button("Ajouter", e -> {
           if(nomF.isEmpty()) return;
           try { new Joueur(nomF.getValue(), prenomF.getValue(), eq.getId()).saveInDB(this.con); gridJoueurs.setItems(Joueur.getByEquipe(this.con, eq.getId())); nomF.clear(); prenomF.clear(); } catch(SQLException ex) { Notification.show("Erreur ajout"); }
        });
        addLayout.add(nomF, prenomF, addBtn); addLayout.setAlignItems(Alignment.BASELINE);
        layout.add(gridJoueurs, addLayout); d.add(layout); d.open();
    }
    
    private void openGestionEquipesDialog(Tournoi t) {
        Dialog d = new Dialog(); d.setHeaderTitle("Équipes pour : " + t.getNom()); d.setWidth("800px");
        VerticalLayout layout = new VerticalLayout();
        Grid<Equipe> gridEquipes = new Grid<>(Equipe.class, false);
        Grid.Column<Equipe> nomCol = gridEquipes.addColumn(Equipe::getNom).setHeader("Équipe");
        nomCol.setTooltipGenerator(eq -> Joueur.getNomsJoueurs(this.con, eq.getId()));
        gridEquipes.addColumn(Equipe::getNomClub).setHeader("Club").setSortable(true);
        gridEquipes.addColumn(new ComponentRenderer<>(eq -> {
            Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH)); deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteBtn.addClickListener(e -> { try { eq.desinscrireDuTournoi(this.con, t.getId()); Notification.show("Équipe retirée"); gridEquipes.setItems(Equipe.getByTournoi(this.con, t.getId())); } catch (SQLException ex) { Notification.show("Erreur suppression"); } });
            return deleteBtn;
        })).setHeader("Retirer").setAutoWidth(true);
        Runnable refreshEquipes = () -> { try { gridEquipes.setItems(Equipe.getByTournoi(this.con, t.getId())); } catch (SQLException ex) { Notification.show("Err chargement équipes"); } };
        refreshEquipes.run();
        HorizontalLayout addLayout = new HorizontalLayout();
        ComboBox<Equipe> selectEquipe = new ComboBox<>("Choisir équipe existante");
        try { selectEquipe.setItems(Equipe.getAll(this.con)); selectEquipe.setItemLabelGenerator(Equipe::getNom); } catch(SQLException ex) {}
        Button addExistingBtn = new Button("Inscrire", e -> {
            if(selectEquipe.getValue() == null) return;
            try { selectEquipe.getValue().inscrireATournoi(this.con, t.getId()); refreshEquipes.run(); Notification.show("Équipe inscrite"); } catch (SQLException ex) { Notification.show("Déjà inscrit ou erreur"); }
        });
        addLayout.add(selectEquipe, addExistingBtn); addLayout.setAlignItems(Alignment.BASELINE);
        layout.add(gridEquipes, new H4("Ajouter une équipe inscrite"), addLayout, new Span("Pour créer une équipe, allez dans 'Gérer mon Club'."));
        d.add(layout); d.open();
    }
    
    private void refreshCombos(ComboBox<Loisir> sportSelect) {
        try { if(sportSelect != null) { sportSelect.setItems(Loisir.getAll(this.con)); sportSelect.setItemLabelGenerator(Loisir::getNom); } } catch (SQLException ex) { Notification.show("Erreur chargement listes"); }
    }
    
    private void updateViewVisibility() {
        if (currentUser.isAdmin()) {
            if (formAjoutLayout != null) formAjoutLayout.setVisible(isModeEdition);
            this.remove(grid); setupGrid(); updateGrid();
        }
    }
    
    private void updateGrid() { try { this.grid.setItems(Tournoi.getAll(this.con)); } catch (SQLException ex) {} }
    
    private void openEditTournoiDialog(Tournoi t) {
        Dialog d = new Dialog(); d.setHeaderTitle("Modifier Tournoi");
        TextField nom = new TextField("Nom"); nom.setValue(t.getNom());
        DatePicker date = new DatePicker("Date"); if(t.getDateDebut() != null) date.setValue(t.getDateDebut());
        Button save = new Button("Enregistrer", e -> { try { t.setNom(nom.getValue()); t.setDateDebut(date.getValue()); t.update(this.con); updateGrid(); d.close(); Notification.show("Tournoi modifié"); } catch (SQLException ex) { Notification.show("Erreur: " + ex.getMessage()); } });
        d.add(new VerticalLayout(nom, date, save)); d.open();
    }
    
    private static class TerrainRow {
        TextField nomField = new TextField(); Checkbox isIndoor = new Checkbox("Intérieur"); HorizontalLayout layout;
        public TerrainRow() { nomField.setPlaceholder("Nom du terrain/salle"); layout = new HorizontalLayout(nomField, isIndoor); layout.setAlignItems(Alignment.BASELINE); }
    }
}