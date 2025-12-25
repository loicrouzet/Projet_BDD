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
import java.util.Base64;
import com.vaadin.flow.component.html.Hr;
import fr.insa.toto.model.GestionBDD;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import com.vaadin.flow.component.html.Anchor;



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
            
            // ==========================================
            // --- BLOC DE RÉPARATION FORCE (À METTRE ICI) ---
            try {
                // On tente de créer les tables sur le serveur distant
                GestionBDD.creeSchema(this.con);
                System.out.println("Schéma initialisé sur le serveur distant.");
            } catch (Exception e) {
                // Si les tables existent déjà, MySQL renvoie une erreur, on l'ignore.
                System.out.println("Le schéma existe déjà ou est déjà prêt.");
            }
            // ==========================================

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
        HorizontalLayout header = new HorizontalLayout(); 
        header.setWidthFull(); 
        header.setJustifyContentMode(JustifyContentMode.BETWEEN); 
        header.setAlignItems(Alignment.CENTER);
        
        HorizontalLayout leftHeader = new HorizontalLayout();
        leftHeader.setAlignItems(Alignment.CENTER);

        // --- Avatar et Pseudo ---
        HorizontalLayout userArea = new HorizontalLayout();
        userArea.setAlignItems(Alignment.CENTER);
        com.vaadin.flow.component.Component avatarDisplay = createSmallAvatar(currentUser);
        avatarDisplay.getElement().addEventListener("click", e -> openProfileDialog());
        userArea.add(avatarDisplay, new Span(currentUser.getSurnom()));
        leftHeader.add(userArea);

        // --- Boutons spécifiques Admin ---
        if (currentUser.isAdmin()) {
            Button gestionClubBtn = new Button("Gérer mon Club", new Icon(VaadinIcon.BUILDING));
            gestionClubBtn.addClickListener(e -> openGestionClubDialog());
            
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

        // --- BOUTON LISTE MEMBRES (Visible par tous) ---
        Button userListBtn = new Button(new Icon(VaadinIcon.USERS));
        userListBtn.setTooltipText("Voir les membres inscrits");
        userListBtn.addClickListener(e -> openUserListDrawer());
        leftHeader.add(userListBtn);

        // --- Bouton Déconnexion ---
        Button logoutBtn = new Button("Déconnexion", e -> { 
            VaadinSession.getCurrent().setAttribute("user", null);
            showLoginScreen(); 
        });
        
        header.add(leftHeader, logoutBtn);
        this.add(header, new H1("Liste des Tournois"));
        // --- SECTION INFOS DU CLUB (Visible par tous) ---
        if (currentUser.getIdClub() != null) {
            try {
                Optional<Club> clubOpt = Club.getById(this.con, currentUser.getIdClub());
                if (clubOpt.isPresent()) {
                    Club c = clubOpt.get();
                    HorizontalLayout clubBanner = new HorizontalLayout();
                    clubBanner.setWidthFull();
                    clubBanner.setAlignItems(Alignment.CENTER);
                    clubBanner.getStyle().set("background", "#f8f9fa").set("padding", "20px")
                              .set("border-radius", "12px").set("border", "1px solid #ddd");

                    // Affichage du Logo
                    if (c.getLogoUrl() != null && !c.getLogoUrl().isEmpty()) {
                        com.vaadin.flow.component.html.Image logo = new com.vaadin.flow.component.html.Image(c.getLogoUrl(), "Logo");
                        logo.setHeight("80px");
                        clubBanner.add(logo);
                    }

                    VerticalLayout clubDetails = new VerticalLayout();
                    clubDetails.setSpacing(false); clubDetails.setPadding(false);
                    clubDetails.add(new H3(c.getNom()));
                    if (c.getDescription() != null) clubDetails.add(new Span(c.getDescription()));

                    // Ligne de contact (Téléphone cliquable + Insta)
                    HorizontalLayout contactLine = new HorizontalLayout();
                    if (c.getTelephone() != null && !c.getTelephone().isEmpty()) {
                        Anchor tel = new Anchor("tel:" + c.getTelephone(), c.getTelephone());
                        contactLine.add(new Icon(VaadinIcon.PHONE), tel);
                    }
                    if (c.getInstagram() != null && !c.getInstagram().isEmpty()) {
                        contactLine.add(new Icon(VaadinIcon.GLOBE), new Span("@" + c.getInstagram()));
                    }
                    clubDetails.add(contactLine);
                    clubBanner.add(clubDetails);
                    this.add(clubBanner);
                }
            } catch (SQLException e) { /* Ignorer erreur */ }
        }
        
        if (currentUser.isAdmin() && isModeEdition) {
            this.formAjoutLayout = createFormulaireAjout();
            this.add(formAjoutLayout);
        }
        
        setupGrid();
        updateGrid();
    }
private void openProfileDialog() {
    Dialog d = new Dialog();
    d.setHeaderTitle("Mon Profil Personnel");
    VerticalLayout layout = new VerticalLayout();

    // Champ URL (on le garde au cas où)
    TextField photoUrlField = new TextField("Lien URL Photo (ou utilisez le bouton ci-dessous)");
    photoUrlField.setValue(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl() : "");
    photoUrlField.setWidthFull();

    // --- BLOC UPLOAD (POUR CHARGER DEPUIS LE PC) ---
    MemoryBuffer buffer = new MemoryBuffer();
    Upload upload = new Upload(buffer);
    upload.setAcceptedFileTypes("image/jpeg", "image/png");
    upload.setMaxFiles(1);
    
    upload.addSucceededListener(event -> {
        try (InputStream inputStream = buffer.getInputStream()) {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            String base64Image = "data:" + event.getMIMEType() + ";base64," + 
                                 Base64.getEncoder().encodeToString(bytes);
            photoUrlField.setValue(base64Image); // Met le contenu de l'image dans le champ texte
            Notification.show("Image chargée avec succès !");
        } catch (Exception ex) {
            Notification.show("Erreur lors de la lecture du fichier");
        }
    });
    // ----------------------------------------------

    DatePicker birthDate = new DatePicker("Date de naissance", currentUser.getDateNaissance());
    TextField emailField = new TextField("Email", currentUser.getEmail() != null ? currentUser.getEmail() : "");
    
    com.vaadin.flow.component.textfield.TextArea infosSupField = new com.vaadin.flow.component.textfield.TextArea("Informations additionnelles");
    infosSupField.setPlaceholder("Décrivez-vous...");
    infosSupField.setValue(currentUser.getInfosSup() != null ? currentUser.getInfosSup() : "");
    infosSupField.setWidthFull();

    Button submitBtn = new Button("Envoyer pour validation", e -> {
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
            Notification.show("Modifications envoyées à l'administrateur !");
            d.close();
        } catch (SQLException ex) { Notification.show("Erreur BDD"); }
    });
    submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    // Ajout des composants au dialogue
    layout.add(photoUrlField, new Span("OU"), upload, birthDate, emailField, infosSupField, submitBtn);
    d.add(layout);
    d.open();
}

private void openValidationInbox() {
        Dialog d = new Dialog();
        d.setHeaderTitle("Demandes de validation de profil");
        
        // --- TAILLE AGRANDIE ---
        d.setWidth("1000px"); // On donne beaucoup plus de place
        d.setHeight("600px");

        Grid<Utilisateur> pendingGrid = new Grid<>(Utilisateur.class, false);
        pendingGrid.addColumn(Utilisateur::getSurnom).setHeader("Joueur").setAutoWidth(true);
        pendingGrid.addColumn(Utilisateur::getEmail).setHeader("Nouvel Email").setAutoWidth(true);
        pendingGrid.addColumn(u -> u.getDateNaissance() != null ? u.getDateNaissance().toString() : "N/A").setHeader("Date Naissance");
        
        // Colonne pour le nouveau bloc d'infos additionnelles
        pendingGrid.addColumn(Utilisateur::getInfosSup).setHeader("Infos Additionnelles").setFlexGrow(2);

        pendingGrid.addComponentColumn(u -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            // Bouton de validation globale
            Button ok = new Button(new Icon(VaadinIcon.CHECK));
            ok.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
            ok.setTooltipText("Tout valider");
            ok.addClickListener(e -> {
                try {
                    confirmUserInfos(u.getId(), true, "Profil validé par l'admin");
                    Notification.show("Profil de " + u.getSurnom() + " validé !");
                    // Rafraîchissement de la grille dans la boîte aux lettres
                    pendingGrid.setItems(Utilisateur.getPendingValidations(this.con));
                    if (Utilisateur.getPendingValidations(this.con).isEmpty()) d.close();
                    showMainApplication();
                } catch (SQLException ex) { Notification.show("Erreur"); }
            });

            // Bouton de refus
            Button ko = new Button(new Icon(VaadinIcon.CLOSE));
            ko.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            ko.setTooltipText("Refuser");
            ko.addClickListener(e -> {
                Dialog reasonDialog = new Dialog();
                TextField reason = new TextField("Motif du refus");
                Button confirmKo = new Button("Confirmer le refus", ev -> {
                    try {
                        confirmUserInfos(u.getId(), false, reason.getValue());
                        Notification.show("Profil refusé");
                        reasonDialog.close();
                        pendingGrid.setItems(Utilisateur.getPendingValidations(this.con));
                        showMainApplication();
                    } catch (SQLException ex) { }
                });
                reasonDialog.add(new VerticalLayout(reason, confirmKo));
                reasonDialog.open();
            });

            actions.add(ok, ko);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        try { 
            List<Utilisateur> pending = Utilisateur.getPendingValidations(this.con);
            if(pending.isEmpty()) {
                d.add(new Span("Aucune demande en attente."));
            } else {
                pendingGrid.setItems(pending);
                d.add(pendingGrid);
            }
        } catch (SQLException ex) { }
        
        Button close = new Button("Fermer", e -> d.close());
        d.getFooter().add(close);
        d.open();
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
        if (currentUser.getIdClub() == null) { openCreateClubDialog(); return; }
        try {
            Optional<Club> clubOpt = Club.getById(this.con, currentUser.getIdClub());
            if (clubOpt.isEmpty()) return;
            Club club = clubOpt.get();

            Dialog d = new Dialog();
            d.setHeaderTitle("Paramètres du Club");
            d.setWidth("600px");

            VerticalLayout form = new VerticalLayout();
            
            // 1. Champ URL existant
            TextField logoField = new TextField("Lien URL du Logo");
            logoField.setValue(club.getLogoUrl() != null ? club.getLogoUrl() : "");
            logoField.setWidthFull();

            // 2. NOUVEAU : Bloc Upload pour charger depuis le PC
            MemoryBuffer buffer = new MemoryBuffer();
            Upload upload = new Upload(buffer);
            upload.setAcceptedFileTypes("image/jpeg", "image/png");
            upload.setMaxFiles(1);
            upload.setDropLabel(new Span("Déposez le logo ici (PNG/JPG)"));
            
            upload.addSucceededListener(event -> {
                try (InputStream inputStream = buffer.getInputStream()) {
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    String base64Image = "data:" + event.getMIMEType() + ";base64," + 
                                         Base64.getEncoder().encodeToString(bytes);
                    logoField.setValue(base64Image); // Remplit le champ URL avec l'image du PC
                    Notification.show("Logo chargé avec succès !");
                } catch (Exception ex) {
                    Notification.show("Erreur lors de la lecture du fichier");
                }
            });

            // 3. Autres champs
            com.vaadin.flow.component.textfield.TextArea descArea = new com.vaadin.flow.component.textfield.TextArea("Description du club");
            descArea.setValue(club.getDescription() != null ? club.getDescription() : "");
            descArea.setWidthFull();

            TextField telField = new TextField("Téléphone de contact");
            telField.setValue(club.getTelephone() != null ? club.getTelephone() : "");

            TextField instaField = new TextField("Instagram (pseudo)");
            instaField.setValue(club.getInstagram() != null ? club.getInstagram() : "");

            // 4. Bouton de sauvegarde
            Button saveBtn = new Button("Enregistrer tout", e -> {
                try {
                    club.setLogoUrl(logoField.getValue());
                    club.setDescription(descArea.getValue());
                    club.setTelephone(telField.getValue());
                    club.setInstagram(instaField.getValue());
                    club.updateInfos(this.con);
                    Notification.show("Club mis à jour !");
                    d.close();
                    showMainApplication(); // Pour rafraîchir la bannière
                } catch (SQLException ex) { Notification.show("Erreur BDD"); }
            });
            saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            // On ajoute tout au dialogue
            form.add(logoField, new Span("--- OU charger depuis le PC ---"), upload, 
                     descArea, telField, instaField, saveBtn);
            d.add(form);
            d.open();
        } catch (SQLException ex) { Notification.show("Erreur chargement"); }
    }
    
    private void openCreateClubDialog() {
        Dialog dialog = new Dialog(); dialog.setHeaderTitle("Créer mon Club");
        VerticalLayout dialogLayout = new VerticalLayout();
        TextField nomClubField = new TextField("Nom du Club"); nomClubField.setWidthFull();
        dialogLayout.add(nomClubField, new H4("Terrains initiaux"));
        VerticalLayout terrainsContainer = new VerticalLayout(); terrainsContainer.setSpacing(false); terrainsContainer.setPadding(false);
        List<TerrainRow> terrainRows = new ArrayList<>();
       Button addTerrainBtn = new Button("Ajouter un terrain", new Icon(VaadinIcon.PLUS_CIRCLE));
        addTerrainBtn.addClickListener(e -> {
            // Technique pour permettre à la ligne de se supprimer de la liste et de l'écran
            TerrainRow[] self = new TerrainRow[1];
            self[0] = new TerrainRow(() -> {
                terrainsContainer.remove(self[0].layout);
                terrainRows.remove(self[0]);
            });
            terrainRows.add(self[0]);
            terrainsContainer.add(self[0].layout);
        });

        // Simuler un clic pour avoir une ligne dès le départ
        addTerrainBtn.click();

        // Le bouton de sauvegarde reste identique, il bouclera sur terrainRows
        // qui ne contiendra que les lignes non supprimées.
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
    TextField nomField = new TextField();
    Checkbox isIndoor = new Checkbox("Intérieur");
    Button btnSuppr = new Button(new Icon(VaadinIcon.TRASH)); // Le bouton poubelle
    HorizontalLayout layout;

    public TerrainRow(Runnable onRemove) {
        nomField.setPlaceholder("Nom du terrain/salle");
        btnSuppr.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        
        // Action du bouton poubelle
        btnSuppr.addClickListener(e -> onRemove.run());

        layout = new HorizontalLayout(nomField, isIndoor, btnSuppr);
        layout.setAlignItems(Alignment.BASELINE);
    }
}
   // Vérifie s'il y a du travail pour l'admin
private boolean checkPendingValidations() {
    try (PreparedStatement pst = con.prepareStatement("select count(*) from utilisateur where nouvelles_infos_pendant = true")) {
        var rs = pst.executeQuery();
        if (rs.next()) return rs.getInt(1) > 0;
    } catch (SQLException e) { }
    return false;
}

// Valide officiellement les infos
private void confirmUserInfos(int userId, boolean estValide, String message) throws SQLException {
    String sql = "update utilisateur set info_valide = ?, nouvelles_infos_pendant = false, message_admin = ? where id = ?";
    try (PreparedStatement pst = con.prepareStatement(sql)) {
        pst.setBoolean(1, estValide);
        if (message == null) pst.setNull(2, java.sql.Types.VARCHAR); else pst.setString(2, message);
        pst.setInt(3, userId);
        pst.executeUpdate();
    }
}
private void updateUserPendingInfo(String email, int age) throws SQLException {
    String sql = "update utilisateur set email = ?, age = ?, nouvelles_infos_pendant = true where id = ?";
    try (PreparedStatement pst = con.prepareStatement(sql)) {
        pst.setString(1, email);
        pst.setInt(2, age);
        pst.setInt(3, currentUser.getId());
        pst.executeUpdate();
    }
}   
private void updateFullUserInfo(String photoUrl, java.time.LocalDate birthDate, String sexe, String email) throws SQLException {
    String sql = "update utilisateur set photo_url = ?, date_naissance = ?, sexe = ?, email = ?, nouvelles_infos_pendant = true where id = ?";
    try (PreparedStatement pst = con.prepareStatement(sql)) {
        pst.setString(1, photoUrl);
        pst.setDate(2, birthDate != null ? java.sql.Date.valueOf(birthDate) : null);
        pst.setString(3, sexe);
        pst.setString(4, email);
        pst.setInt(5, currentUser.getId());
        pst.executeUpdate();
        
        // Mise à jour de l'objet en mémoire
        currentUser.setPhotoUrl(photoUrl);
        currentUser.setDateNaissance(birthDate);
        currentUser.setSexe(sexe);
        currentUser.setEmail(email); // <--- Utilisera la méthode ajoutée à l'étape 1
    }
}
private void openUserListDrawer() {
        Dialog drawer = new Dialog();
        drawer.setHeaderTitle("Utilisateurs inscrits");
        drawer.setWidth("350px");
        drawer.setHeightFull();
        
        VerticalLayout layout = new VerticalLayout();
        try {
            List<Utilisateur> allUsers = Utilisateur.getAllUsers(this.con);
            layout.add(new H3("Total : " + allUsers.size() + " comptes"), new Hr());
            
            for (Utilisateur u : allUsers) {
                // 1. Déterminer l'affichage de l'email (Secret si pas validé)
                String affichageEmail;
                if (u.isInfoValide() || currentUser.isAdmin()) {
                    affichageEmail = (u.getEmail() != null) ? u.getEmail() : "Non renseigné";
                } else {
                    affichageEmail = "En attente de validation...";
                }

                HorizontalLayout row = new HorizontalLayout();
                row.setAlignItems(Alignment.CENTER);
                row.getStyle().set("cursor", "pointer").set("border-bottom", "1px solid #f0f0f0").set("width", "100%");
                
                // --- BLOC DE TEXTE (NOM + EMAIL + RÔLE) ---
                VerticalLayout textLayout = new VerticalLayout();
                textLayout.setSpacing(false);
                textLayout.setPadding(false);
                
                // Nom en gras
                Span nameSpan = new Span(u.getSurnom());
                nameSpan.getStyle().set("font-weight", "bold");
                
                // Email petit et gris
                Span emailSpan = new Span(affichageEmail);
                emailSpan.getStyle().set("font-size", "0.8em").set("color", "gray");

                // RÔLE en gris et italique
                String texteRole = u.isAdmin() ? "Administrateur" : "Utilisateur";
                Span roleSpan = new Span(texteRole);
                roleSpan.getStyle()
                    .set("font-size", "0.75em")
                    .set("color", "#808080")
                    .set("font-style", "italic");
                
                textLayout.add(nameSpan, emailSpan, roleSpan);
                // ------------------------------------------

                row.add(createSmallAvatar(u), textLayout);
                row.addClickListener(e -> showPublicProfile(u));
                layout.add(row);
            }
        } catch (SQLException ex) { 
            layout.add(new Span("Erreur de chargement des utilisateurs")); 
        }
        
        drawer.add(layout);
        drawer.open();
    }

    private com.vaadin.flow.component.Component createSmallAvatar(Utilisateur u) {
        if (u == null) return new Span("?");
        if (u.getPhotoUrl() != null && !u.getPhotoUrl().isEmpty()) {
            com.vaadin.flow.component.html.Image img = new com.vaadin.flow.component.html.Image(u.getPhotoUrl(), "");
            img.setWidth("40px"); img.setHeight("40px");
            img.getStyle().set("border-radius", "50%").set("object-fit", "cover").set("cursor", "pointer");
            return img;
        } else {
            String pseudo = (u.getSurnom() == null || u.getSurnom().isEmpty()) ? "?" : u.getSurnom();
            String initials = pseudo.substring(0, 1).toUpperCase();
            Span s = new Span(initials);
            s.getStyle().set("background-color", "#007bff").set("color", "white").set("border-radius", "50%")
             .set("width", "40px").set("height", "40px").set("display", "flex").set("align-items", "center")
             .set("justify-content", "center").set("font-weight", "bold").set("cursor", "pointer");
            return s;
        }
    }

private void showPublicProfile(Utilisateur u) {
        Dialog detail = new Dialog();
        detail.setHeaderTitle("Profil de " + u.getSurnom());
        VerticalLayout content = new VerticalLayout();
        content.setAlignItems(Alignment.CENTER);

        // Indicateur visuel pour l'admin si le profil n'est pas encore public
        if (!u.isInfoValide() && currentUser.isAdmin()) {
            Span warning = new Span("⚠️ EN ATTENTE DE VALIDATION (Voir Boîte aux lettres)");
            warning.getStyle().set("color", "orange").set("font-weight", "bold");
            content.add(warning);
        }

        content.add(createSmallAvatar(u));
        content.add(new H4("Informations"));
        
        // On n'affiche les infos privées que si c'est validé OU si on est admin
        if (u.isInfoValide() || currentUser.isAdmin()) {
            content.add(new Span("Email : " + (u.getEmail() != null ? u.getEmail() : "N/A")));
            content.add(new Span("Naissance : " + (u.getDateNaissance() != null ? u.getDateNaissance() : "N/A")));
            content.add(new Hr());
            content.add(new Span("Infos additionnelles :"));
            Span infos = new Span(u.getInfosSup() != null ? u.getInfosSup() : "Aucune");
            infos.getStyle().set("font-style", "italic").set("text-align", "center");
            content.add(infos);
        } else {
            content.add(new Span("Les informations de ce profil ne sont pas encore publiques."));
        }

        detail.add(content, new Button("Fermer", e -> detail.close()));
        detail.open();
    }
} // <--- DERNIÈRE ACCOLADE DU FICHIER