package fr.insa.toto.webui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Club;
import fr.insa.toto.model.GestionBDD;
import fr.insa.toto.model.Loisir;
import fr.insa.toto.model.Terrain;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.model.Utilisateur;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;

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
            this.con = ConnectionSimpleSGBD.defaultCon();
            // Réparation auto du schéma si besoin
            try { GestionBDD.creeSchema(this.con); } catch (Exception e) {}
        } catch (SQLException ex) {
            this.add(new H3("Erreur BDD : " + ex.getMessage()));
            return;
        }
        
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
        TextField userField = new TextField("Identifiant de connexion");
        PasswordField passField = new PasswordField("Mot de passe");
        Button loginButton = new Button("Se connecter", e -> {
            try {
                Optional<Utilisateur> user = Utilisateur.login(this.con, userField.getValue(), passField.getValue());
                if (user.isPresent()) {
                    this.currentUser = user.get();
                    VaadinSession.getCurrent().setAttribute("user", this.currentUser);
                    Notification.show("Bienvenue " + this.currentUser.getSurnom());
                    showMainApplication();
                } else { Notification.show("Identifiants incorrects"); }
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
        
        TextField idField = new TextField("Identifiant (Connexion)"); idField.setRequired(true);
        TextField prenomField = new TextField("Prénom"); prenomField.setRequired(true);
        TextField nomField = new TextField("Nom"); nomField.setRequired(true);
        TextField surnomField = new TextField("Surnom (Facultatif)");
        PasswordField passField = new PasswordField("Mot de passe");

        ComboBox<Club> clubSelect = new ComboBox<>("Mon Club (Optionnel)");
        try { clubSelect.setItems(Club.getAll(this.con)); clubSelect.setItemLabelGenerator(Club::getNom); } catch (SQLException e) {}
        
        RadioButtonGroup<String> roleSelect = new RadioButtonGroup<>();
        roleSelect.setItems("Visiteur", "Administrateur");
        roleSelect.setValue("Visiteur");
        PasswordField adminKeyField = new PasswordField("Clé Administrateur");
        adminKeyField.setVisible(false);
        roleSelect.addValueChangeListener(e -> adminKeyField.setVisible(e.getValue().equals("Administrateur")));

        Button createButton = new Button("S'inscrire", e -> {
            try {
                if (idField.isEmpty() || nomField.isEmpty() || prenomField.isEmpty() || passField.isEmpty()) {
                    Notification.show("Champs obligatoires manquants !"); return;
                }
                if (Utilisateur.existeIdentifiant(this.con, idField.getValue())) {
                    Notification.show("Identifiant déjà utilisé !"); return;
                }
                int roleId = 0;
                if (roleSelect.getValue().equals("Administrateur")) {
                    if ("toto".equals(adminKeyField.getValue())) roleId = 1; 
                    else { Notification.show("Clé admin incorrecte !"); return; }
                }
                Integer idClub = clubSelect.getValue() != null ? clubSelect.getValue().getId() : null;
                new Utilisateur(idField.getValue(), surnomField.getValue(), nomField.getValue(), 
                               prenomField.getValue(), passField.getValue(), roleId, idClub).saveInDB(this.con);
                Notification.show("Compte créé !");
                showLoginScreen();
            } catch (SQLException ex) { Notification.show("Erreur: " + ex.getMessage()); }
        });

        Button cancelButton = new Button("Annuler", e -> showLoginScreen());
        VerticalLayout l = new VerticalLayout(title, idField, prenomField, nomField, surnomField, passField, clubSelect, roleSelect, adminKeyField, createButton, cancelButton);
        l.setAlignItems(Alignment.CENTER);
        this.add(l);
    }

    private void showMainApplication() {
        this.removeAll();
        HorizontalLayout header = new HorizontalLayout(); 
        header.setWidthFull(); header.setJustifyContentMode(JustifyContentMode.BETWEEN); header.setAlignItems(Alignment.CENTER);
        
        HorizontalLayout leftHeader = new HorizontalLayout();
        leftHeader.setAlignItems(Alignment.CENTER);

        // --- Avatar et Pseudo ---
        HorizontalLayout userArea = new HorizontalLayout();
        userArea.setAlignItems(Alignment.CENTER);
        com.vaadin.flow.component.Component avatarDisplay = createSmallAvatar(currentUser);
        avatarDisplay.getElement().addEventListener("click", e -> openProfileDialog());
        userArea.add(avatarDisplay, new Span(currentUser.getSurnom()));
        leftHeader.add(userArea);

        // --- RESTAURATION : Boutons spécifiques Admin ---
        if (currentUser.isAdmin()) {
            Button gestionClubBtn = new Button("Gérer mon Club", new Icon(VaadinIcon.BUILDING));
            gestionClubBtn.addClickListener(e -> gestionClubBtn.getUI().ifPresent(ui -> ui.navigate(VueClub.class)));
            Button toggleModeBtn = new Button(isModeEdition ? "Mode: Édition" : "Mode: Consultation", 
                    new Icon(isModeEdition ? VaadinIcon.EDIT : VaadinIcon.EYE));
            toggleModeBtn.addClickListener(e -> {
                this.isModeEdition = !this.isModeEdition;
                showMainApplication();
            });

            Button notifyBtn = new Button(new Icon(VaadinIcon.ENVELOPE));
            if (checkPendingValidations()) notifyBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            notifyBtn.addClickListener(e -> openValidationInbox());
            
            leftHeader.add(gestionClubBtn, toggleModeBtn, notifyBtn);
        }

        // --- Bouton Liste Membres ---
        Button userListBtn = new Button(new Icon(VaadinIcon.USERS));
        userListBtn.setTooltipText("Voir les membres");
        userListBtn.addClickListener(e -> openUserListDrawer());
        leftHeader.add(userListBtn);

        // --- Bouton Déconnexion  ---
        Button logoutBtn = new Button("Déconnexion", e -> { 
            VaadinSession.getCurrent().setAttribute("user", null);
            showLoginScreen(); 
        });
        if (currentUser.isAdmin()) {
            header.add(leftHeader, logoutBtn);
        } else {
            header.add(leftHeader, logoutBtn);
        }
        
        this.add(header);
        
        // --- Bannières Club ---
        if (currentUser.getIdClub() != null) {
            try {
                Optional<Club> clubOpt = Club.getById(this.con, currentUser.getIdClub());
                if (clubOpt.isPresent()) {
                    Club c = clubOpt.get();
                    HorizontalLayout clubBanner = new HorizontalLayout();
                    clubBanner.setWidthFull(); clubBanner.setAlignItems(Alignment.CENTER);
                    clubBanner.getStyle().set("background", "#f8f9fa").set("padding", "20px")
                              .set("border-radius", "12px").set("border", "1px solid #ddd");

                    if (c.getLogoUrl() != null && !c.getLogoUrl().isEmpty()) {
                        com.vaadin.flow.component.html.Image logo = new com.vaadin.flow.component.html.Image(c.getLogoUrl(), "Logo");
                        logo.setHeight("80px");
                        clubBanner.add(logo);
                    }
                    VerticalLayout clubDetails = new VerticalLayout(new H3(c.getNom()));
                    if (c.getDescription() != null) clubDetails.add(new Span(c.getDescription()));
                    
                    // Contact
                    HorizontalLayout contactLine = new HorizontalLayout();
                    if (c.getTelephone() != null) contactLine.add(new Icon(VaadinIcon.PHONE), new Anchor("tel:" + c.getTelephone(), c.getTelephone()));
                    clubDetails.add(contactLine);
                    clubBanner.add(clubDetails);
                    this.add(clubBanner);
                }
            } catch (SQLException e) { }
        }

        this.add(new H1("Liste des Tournois"));
        
        if (currentUser.isAdmin() && isModeEdition) {
            this.formAjoutLayout = createFormulaireAjout();
            this.add(formAjoutLayout);
        }
        
        setupGrid();
        updateGrid();
    }

    // --- DIALOGUES UTILISATEUR & VALIDATION ---
    
    private void openProfileDialog() {
        Dialog d = new Dialog(); d.setHeaderTitle("Mon Profil");
        VerticalLayout layout = new VerticalLayout();
        TextField photoUrlField = new TextField("Lien URL Photo (ou upload)");
        photoUrlField.setValue(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl() : "");
        
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png");
        upload.addSucceededListener(event -> {
            try (InputStream inputStream = buffer.getInputStream()) {
                String base64 = "data:" + event.getMIMEType() + ";base64," + 
                                Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
                photoUrlField.setValue(base64);
                Notification.show("Image chargée !");
            } catch (Exception ex) { Notification.show("Erreur lecture fichier"); }
        });

        DatePicker birthDate = new DatePicker("Date de naissance", currentUser.getDateNaissance());
        TextField emailField = new TextField("Email", currentUser.getEmail() != null ? currentUser.getEmail() : "");
        com.vaadin.flow.component.textfield.TextArea infosSupField = new com.vaadin.flow.component.textfield.TextArea("Infos additionnelles");
        infosSupField.setValue(currentUser.getInfosSup() != null ? currentUser.getInfosSup() : "");

        Button submitBtn = new Button("Mettre à jour", e -> {
            try {
                String sql = "update utilisateur set photo_url=?, date_naissance=?, email=?, infos_sup=?, info_valide=false, nouvelles_infos_pendant=true where id=?";
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, photoUrlField.getValue());
                    pst.setDate(2, birthDate.getValue() != null ? java.sql.Date.valueOf(birthDate.getValue()) : null);
                    pst.setString(3, emailField.getValue());
                    pst.setString(4, infosSupField.getValue());
                    pst.setInt(5, currentUser.getId());
                    pst.executeUpdate();
                }
                Notification.show("Profil mis à jour (en attente de validation admin)");
                d.close();
            } catch (SQLException ex) { Notification.show("Erreur BDD"); }
        });
        layout.add(photoUrlField, upload, birthDate, emailField, infosSupField, submitBtn);
        d.add(layout); d.open();
    }
    
    private boolean checkPendingValidations() {
        try (PreparedStatement pst = con.prepareStatement("select count(*) from utilisateur where nouvelles_infos_pendant = true")) {
            var rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { }
        return false;
    }
    
    private void openValidationInbox() {
        Dialog d = new Dialog(); d.setHeaderTitle("Demandes de validation"); d.setWidth("800px");
        Grid<Utilisateur> pendingGrid = new Grid<>(Utilisateur.class, false);
        pendingGrid.addColumn(Utilisateur::getSurnom).setHeader("Joueur");
        pendingGrid.addColumn(Utilisateur::getEmail).setHeader("Email");
        pendingGrid.addComponentColumn(u -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button ok = new Button(new Icon(VaadinIcon.CHECK));
            ok.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            ok.addClickListener(e -> {
                try { u.confirmInfos(this.con, true, "Validé"); showMainApplication(); d.close(); } catch(SQLException ex){}
            });
            Button ko = new Button(new Icon(VaadinIcon.CLOSE));
            ko.addThemeVariants(ButtonVariant.LUMO_ERROR);
            ko.addClickListener(e -> {
                 try { u.confirmInfos(this.con, false, "Refusé"); showMainApplication(); d.close(); } catch(SQLException ex){}
            });
            actions.add(ok, ko); return actions;
        }).setHeader("Actions");
        
        try { 
            pendingGrid.setItems(Utilisateur.getAllUsers(this.con).stream().filter(u -> !u.isInfoValide() && u.getId() != currentUser.getId()).toList());
        } catch (SQLException ex) {}
        d.add(pendingGrid); d.open();
    }

    // --- GESTION CLUB & TERRAINS (RESTAURÉ) ---
    
    private void openGestionClubDialog() {
        if (currentUser.getIdClub() == null) { openCreateClubDialog(); return; }
        try {
            Optional<Club> clubOpt = Club.getById(this.con, currentUser.getIdClub());
            if (clubOpt.isEmpty()) return;
            Club club = clubOpt.get();

            Dialog d = new Dialog(); d.setHeaderTitle("Configuration du Club");
            VerticalLayout mainLayout = new VerticalLayout();

            TextField logoUrlField = new TextField("URL Logo");
            logoUrlField.setValue(club.getLogoUrl() != null ? club.getLogoUrl() : "");
            
            MemoryBuffer buffer = new MemoryBuffer();
            Upload upload = new Upload(buffer);
            upload.setAcceptedFileTypes("image/jpeg", "image/png");
            upload.addSucceededListener(event -> {
                try (InputStream in = buffer.getInputStream()) {
                    String base64 = "data:" + event.getMIMEType() + ";base64," + Base64.getEncoder().encodeToString(IOUtils.toByteArray(in));
                    logoUrlField.setValue(base64);
                } catch (Exception ex) {}
            });

            com.vaadin.flow.component.textfield.TextArea descArea = new com.vaadin.flow.component.textfield.TextArea("Description");
            descArea.setValue(club.getDescription() != null ? club.getDescription() : "");
            TextField telField = new TextField("Téléphone");
            telField.setValue(club.getTelephone() != null ? club.getTelephone() : "");

            Button saveBtn = new Button("Enregistrer", ev -> {
                try {
                    club.setLogoUrl(logoUrlField.getValue());
                    club.setDescription(descArea.getValue());
                    club.setTelephone(telField.getValue());
                    club.updateInfos(this.con);
                    Notification.show("Infos Club mises à jour"); d.close(); showMainApplication();
                } catch (SQLException e) { Notification.show("Erreur BDD"); }
            });
            
            mainLayout.add(logoUrlField, upload, descArea, telField, saveBtn);
            d.add(mainLayout); d.open();
        } catch (SQLException ex) {}
    }
    
    private void openCreateClubDialog() {
        Dialog dialog = new Dialog(); dialog.setHeaderTitle("Créer mon Club");
        VerticalLayout dialogLayout = new VerticalLayout();
        TextField nomClubField = new TextField("Nom du Club");
        
        VerticalLayout terrainsContainer = new VerticalLayout();
        List<TerrainRow> terrainRows = new ArrayList<>();
        Button addTerrainBtn = new Button("Ajouter un terrain initial", new Icon(VaadinIcon.PLUS_CIRCLE));
        addTerrainBtn.addClickListener(e -> {
            TerrainRow row = new TerrainRow(() -> {}); 
            row.btnSuppr.addClickListener(ev -> { terrainsContainer.remove(row.layout); terrainRows.remove(row); });
            terrainRows.add(row); terrainsContainer.add(row.layout);
        });
        addTerrainBtn.click(); // Un par défaut

        Button saveBtn = new Button("Créer et Lier", e -> {
            try {
                if (nomClubField.isEmpty()) return;
                Club newClub = new Club(nomClubField.getValue()); 
                int newClubId = newClub.saveInDB(this.con);
                for (TerrainRow row : terrainRows) { 
                    if (!row.nomField.isEmpty()) new Terrain(row.nomField.getValue(), row.isIndoor.getValue(), newClubId).saveInDB(this.con); 
                }
                try (PreparedStatement pst = this.con.prepareStatement("update utilisateur set id_club=? where id=?")) {
                    pst.setInt(1, newClubId); pst.setInt(2, currentUser.getId()); pst.executeUpdate();
                }
                Notification.show("Club créé ! Reconnexion requise.");
                this.currentUser = null; showLoginScreen(); dialog.close();
            } catch (SQLException ex) { Notification.show("Erreur : " + ex.getMessage()); }
        });
        dialogLayout.add(nomClubField, new H4("Terrains"), terrainsContainer, addTerrainBtn, saveBtn); 
        dialog.add(dialogLayout); dialog.open();
    }
    
    private void openGestionTerrainsDialog() {
        if (currentUser.getIdClub() == null) return;
        Dialog d = new Dialog(); d.setHeaderTitle("Infrastructures"); d.setWidth("600px");
        Grid<Terrain> tGrid = new Grid<>(Terrain.class, false);
        tGrid.addColumn(Terrain::getNom).setHeader("Nom");
        tGrid.addColumn(t -> t.isEstInterieur() ? "Intérieur" : "Extérieur");
        tGrid.addComponentColumn(t -> {
            Button del = new Button(new Icon(VaadinIcon.TRASH));
            del.addThemeVariants(ButtonVariant.LUMO_ERROR);
            del.addClickListener(e -> {
                try { t.delete(this.con); tGrid.setItems(Terrain.getByClub(this.con, currentUser.getIdClub())); } catch(SQLException ex){}
            });
            return del;
        });
        try { tGrid.setItems(Terrain.getByClub(this.con, currentUser.getIdClub())); } catch (SQLException e) {}
        
        HorizontalLayout addL = new HorizontalLayout();
        TextField nom = new TextField("Nouveau terrain");
        Checkbox in = new Checkbox("Int.");
        Button add = new Button(new Icon(VaadinIcon.PLUS), e -> {
             try { new Terrain(nom.getValue(), in.getValue(), currentUser.getIdClub()).saveInDB(this.con); 
                   tGrid.setItems(Terrain.getByClub(this.con, currentUser.getIdClub())); nom.clear(); } catch(SQLException ex){}
        });
        addL.add(nom, in, add);
        d.add(tGrid, addL); d.open();
    }

    // --- FORMULAIRE ET GRILLE TOURNOIS ---

    private HorizontalLayout createFormulaireAjout() {
        HorizontalLayout form = new HorizontalLayout(); form.setWidthFull(); form.setAlignItems(Alignment.BASELINE);
        TextField nomField = new TextField("Nom tournoi");
        DatePicker dateField = new DatePicker("Date");
        ComboBox<Loisir> sportSelect = new ComboBox<>("Sport");
        Button addButton = new Button("Ajouter Tournoi", e -> {
            try {
                if (currentUser.getIdClub() == null) { Notification.show("Créez un club d'abord"); return; }
                new Tournoi(nomField.getValue(), dateField.getValue(), sportSelect.getValue(), 
                            new Club(currentUser.getIdClub(), "temp")).saveInDB(this.con);
                updateGrid(); Notification.show("Tournoi créé");
            } catch (SQLException ex) { Notification.show("Erreur BDD"); }
        });
        try { sportSelect.setItems(Loisir.getAll(this.con)); sportSelect.setItemLabelGenerator(Loisir::getNom); } catch (SQLException e) {}
        form.add(nomField, dateField, sportSelect, addButton);
        return form;
    }
    
    private void setupGrid() {
        this.grid = new Grid<>(Tournoi.class, false);
        this.grid.addColumn(Tournoi::getNom).setHeader("Nom").setSortable(true);
        this.grid.addColumn(Tournoi::getDateDebut).setHeader("Date").setSortable(true);
        this.grid.addColumn(t -> t.getLeLoisir().getNom()).setHeader("Sport");
        this.grid.addComponentColumn(tournoi -> {
            Button openBtn = new Button(new Icon(VaadinIcon.ARROW_RIGHT));
            openBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            openBtn.addClickListener(e -> openBtn.getUI().ifPresent(ui -> ui.navigate(VueTournoi.class, tournoi.getId())));
            return openBtn;
        }).setHeader("Accéder");

        if (currentUser.isAdmin() && isModeEdition) {
            this.grid.addComponentColumn(tournoi -> {
                boolean canEdit = currentUser.getIdClub() != null && currentUser.getIdClub() == tournoi.getLeClub().getId();
                if (canEdit) {
                    Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
                    editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    editBtn.addClickListener(e -> openEditTournoiDialog(tournoi));
                    return editBtn;
                }
                return new Span();
            }).setHeader("Édition");
        }
        this.add(this.grid);
    }
    
    private void updateGrid() { try { this.grid.setItems(Tournoi.getAll(this.con)); } catch (SQLException ex) {} }
    
    private void openEditTournoiDialog(Tournoi t) {
        Dialog d = new Dialog(); d.setHeaderTitle("Modifier Tournoi");
        TextField nom = new TextField("Nom"); nom.setValue(t.getNom());
        DatePicker date = new DatePicker("Date"); if(t.getDateDebut() != null) date.setValue(t.getDateDebut());
        Button save = new Button("Enregistrer", e -> { try { t.setNom(nom.getValue()); t.setDateDebut(date.getValue()); t.update(this.con); updateGrid(); d.close(); } catch (SQLException ex) {} });
        d.add(new VerticalLayout(nom, date, save)); d.open();
    }
    
    // --- UTILITAIRES ---

    private void openUserListDrawer() {
        Dialog drawer = new Dialog(); drawer.setHeaderTitle("Membres"); drawer.setWidth("300px");
        VerticalLayout l = new VerticalLayout();
        try { for (Utilisateur u : Utilisateur.getAllUsers(this.con)) {
            HorizontalLayout row = new HorizontalLayout(createSmallAvatar(u), new Span(u.getSurnom()));
            row.setAlignItems(Alignment.CENTER);
            row.addClickListener(e -> showPublicProfile(u));
            l.add(row);
        }} catch(SQLException ex){}
        drawer.add(l); drawer.open();
    }
    
    private void showPublicProfile(Utilisateur u) {
        Dialog d = new Dialog(); d.setHeaderTitle("Profil de " + u.getSurnom());
        VerticalLayout v = new VerticalLayout(createSmallAvatar(u), new H4(u.getPrenom() + " " + u.getNom()));
        if(u.isInfoValide() || currentUser.isAdmin()) v.add(new Span(u.getEmail()), new Span(u.getInfosSup()));
        d.add(v); d.open();
    }

    private com.vaadin.flow.component.Component createSmallAvatar(Utilisateur u) {
        if (u != null && u.getPhotoUrl() != null && !u.getPhotoUrl().isEmpty()) {
            com.vaadin.flow.component.html.Image img = new com.vaadin.flow.component.html.Image(u.getPhotoUrl(), "");
            img.setWidth("40px"); img.setHeight("40px"); img.getStyle().set("border-radius", "50%").set("object-fit", "cover");
            return img;
        }
        Span s = new Span(u != null ? u.getInitiales() : "?");
        s.getStyle().set("background", "#007bff").set("color", "white").set("border-radius", "50%").set("width", "40px").set("height", "40px").set("display", "flex").set("align-items", "center").set("justify-content", "center");
        return s;
    }
    
    private static class TerrainRow {
        TextField nomField = new TextField(); Checkbox isIndoor = new Checkbox("Int."); Button btnSuppr = new Button(new Icon(VaadinIcon.TRASH));
        HorizontalLayout layout = new HorizontalLayout(nomField, isIndoor, btnSuppr);
        public TerrainRow(Runnable onRemove) { 
            nomField.setPlaceholder("Nom terrain"); 
            btnSuppr.addThemeVariants(ButtonVariant.LUMO_ERROR); 
            layout.setAlignItems(Alignment.BASELINE); 
        }
    }
}