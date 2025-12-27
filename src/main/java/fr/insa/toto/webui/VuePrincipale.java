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
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
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
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Loisir;
import fr.insa.toto.model.Terrain;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.model.Utilisateur;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
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
                
                Utilisateur newUser = new Utilisateur(idField.getValue(), surnomField.getValue(), nomField.getValue(), 
                               prenomField.getValue(), passField.getValue(), roleId, idClub);
                int newUserId = newUser.saveInDB(this.con);
                
                if (idClub != null && roleId == 0) { 
                    Optional<Joueur> existingJoueur = Joueur.findByNomPrenomClub(this.con, nomField.getValue(), prenomField.getValue(), idClub);
                    if (existingJoueur.isPresent()) {
                        Joueur j = existingJoueur.get();
                        if (j.getIdUtilisateur() == null) {
                            j.setIdUtilisateur(newUserId);
                            j.update(this.con);
                            Notification.show("Compte relié à votre fiche joueur existante !");
                        }
                    }
                }
                Notification.show("Compte créé avec succès !");
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

        HorizontalLayout userArea = new HorizontalLayout();
        userArea.setAlignItems(Alignment.CENTER);
        com.vaadin.flow.component.Component avatarDisplay = createSmallAvatar(currentUser);
        avatarDisplay.getElement().addEventListener("click", e -> openProfileDialog());
        userArea.add(avatarDisplay, new Span(currentUser.getSurnom()));
        leftHeader.add(userArea);

        if (currentUser.isAdmin()) {
            Button gestionClubBtn = new Button("Gérer mon Club", new Icon(VaadinIcon.BUILDING));
            gestionClubBtn.addClickListener(e -> gestionClubBtn.getUI().ifPresent(ui -> ui.navigate(VueClub.class)));
            Button toggleModeBtn = new Button(isModeEdition ? "Mode: Édition" : "Mode: Consultation", 
                    new Icon(isModeEdition ? VaadinIcon.EDIT : VaadinIcon.EYE));
            toggleModeBtn.addClickListener(e -> {
                this.isModeEdition = !this.isModeEdition;
                showMainApplication();
            });
            leftHeader.add(gestionClubBtn, toggleModeBtn);
        }

        Button userListBtn = new Button(new Icon(VaadinIcon.USERS));
        userListBtn.setTooltipText("Voir les membres");
        userListBtn.addClickListener(e -> openUserListDrawer());
        leftHeader.add(userListBtn);

        Button logoutBtn = new Button("Déconnexion", e -> { 
            VaadinSession.getCurrent().setAttribute("user", null);
            showLoginScreen(); 
        });
        
        header.add(leftHeader, logoutBtn);
        this.add(header);
        
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
        this.add(this.grid);
        
        HorizontalLayout bottomSection = new HorizontalLayout();
        bottomSection.setWidthFull();
        bottomSection.setSpacing(true);
        bottomSection.setPadding(true);
        
        VerticalLayout clubsLayout = new VerticalLayout();
        clubsLayout.setWidth("50%");
        clubsLayout.add(new H3("Annuaire des Clubs"));
        
        Grid<Club> clubGrid = new Grid<>(Club.class, false);
        clubGrid.addComponentColumn(c -> {
            if (c.getLogoUrl() != null && !c.getLogoUrl().isEmpty()) {
                com.vaadin.flow.component.html.Image img = new com.vaadin.flow.component.html.Image(c.getLogoUrl(), "Logo");
                img.setWidth("30px"); img.setHeight("30px"); img.getStyle().set("border-radius", "50%");
                return img;
            }
            return new Span("-");
        }).setHeader("Logo").setAutoWidth(true);
        
        clubGrid.addComponentColumn(c -> {
            Button b = new Button(c.getNom());
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            b.getStyle().set("font-weight", "bold").set("color", "#007bff");
            b.addClickListener(e -> openClubDetailsDialog(c));
            return b;
        }).setHeader("Nom").setSortable(true);
        clubGrid.addColumn(Club::getAdresse).setHeader("Localisation");
        try { clubGrid.setItems(Club.getAll(this.con)); } catch (SQLException e) {}
        clubsLayout.add(clubGrid);

        VerticalLayout joueursLayout = new VerticalLayout();
        joueursLayout.setWidth("50%");
        joueursLayout.add(new H3("Tous les Joueurs"));
        
        Grid<Joueur> joueurGrid = new Grid<>(Joueur.class, false);
        
        joueurGrid.addComponentColumn(j -> {
            if (j.getPhotoUrl() != null && !j.getPhotoUrl().isEmpty()) {
                com.vaadin.flow.component.html.Image img = new com.vaadin.flow.component.html.Image(j.getPhotoUrl(), "Avatar");
                img.setWidth("40px"); img.setHeight("40px"); 
                img.getStyle().set("border-radius", "50%").set("object-fit", "cover");
                return img;
            } else {
                Span s = new Span(j.getPrenom() != null && !j.getPrenom().isEmpty() ? j.getPrenom().substring(0,1) : "?");
                s.getStyle().set("background", "#ddd").set("border-radius", "50%")
                            .set("width", "40px").set("height", "40px")
                            .set("display", "flex").set("align-items", "center").set("justify-content", "center");
                return s;
            }
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);

        joueurGrid.addComponentColumn(j -> {
            Button b = new Button(j.getNom() + " " + j.getPrenom());
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            b.addClickListener(e -> openJoueurDetailsDialog(j));
            return b;
        }).setHeader("Joueur").setSortable(true);
        
        joueurGrid.addColumn(Joueur::getNomClub).setHeader("Club").setSortable(true);
        
        try { joueurGrid.setItems(Joueur.getAll(this.con)); } catch (SQLException e) {}
        joueursLayout.add(joueurGrid);

        bottomSection.add(clubsLayout, joueursLayout);
        this.add(new Hr(), bottomSection);
    }

    // --- CORRECTION DU PROBLÈME "linkedJoueur n'existe pas" ---
    private void openProfileDialog() {
        Dialog d = new Dialog(); d.setHeaderTitle("Mon Profil & Fiche Joueur");
        d.setWidth("600px");
        
        VerticalLayout layout = new VerticalLayout();
        
        // Champs Utilisateur
        TextField photoUrlField = new TextField("Avatar (URL ou Upload)");
        photoUrlField.setValue(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl() : "");
        photoUrlField.setWidthFull();
        
        MemoryBuffer buffer = new MemoryBuffer(); Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png");
        upload.addSucceededListener(event -> {
            try (InputStream inputStream = buffer.getInputStream()) {
                String base64 = "data:" + event.getMIMEType() + ";base64," + Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
                photoUrlField.setValue(base64); Notification.show("Image chargée !");
            } catch (Exception ex) {}
        });

        TextField emailField = new TextField("Email", currentUser.getEmail() != null ? currentUser.getEmail() : "");
        emailField.setWidthFull();
        com.vaadin.flow.component.textfield.TextArea infosSupField = new com.vaadin.flow.component.textfield.TextArea("Ma bio / Infos");
        infosSupField.setValue(currentUser.getInfosSup() != null ? currentUser.getInfosSup() : "");
        infosSupField.setWidthFull();

        // Champs Joueur
        DatePicker birthDate = new DatePicker("Date de naissance"); birthDate.setWidthFull();
        TextField insta = new TextField("Instagram"); insta.setPrefixComponent(new Icon(VaadinIcon.CAMERA)); insta.setWidthFull();
        TextField fb = new TextField("Facebook"); fb.setPrefixComponent(new Icon(VaadinIcon.THUMBS_UP)); fb.setWidthFull();
        TextField tw = new TextField("Twitter"); tw.setPrefixComponent(new Icon(VaadinIcon.PAPERPLANE)); tw.setWidthFull();
        
        // --- RECUPERATION DU JOUEUR ---
        Joueur tempJ = null;
        try {
             Optional<Joueur> opt = Joueur.getByUtilisateurId(con, currentUser.getId());
             if (opt.isPresent()) tempJ = opt.get();
        } catch (SQLException e) { }
        
        // Variable FINALE pour l'utiliser dans le bouton (C'est ça qui corrigeait l'erreur)
        final Joueur linkedJoueur = tempJ;

        // Pré-remplissage
        if (linkedJoueur != null) {
            if (linkedJoueur.getDateNaissance() != null) birthDate.setValue(linkedJoueur.getDateNaissance());
            if (linkedJoueur.getInstagram() != null) insta.setValue(linkedJoueur.getInstagram());
            if (linkedJoueur.getFacebook() != null) fb.setValue(linkedJoueur.getFacebook());
            if (linkedJoueur.getTwitter() != null) tw.setValue(linkedJoueur.getTwitter());
        } else {
            layout.add(new Span("⚠️ Vous n'êtes lié à aucune fiche joueur. Demandez à votre club de vous ajouter."));
        }

        Button submitBtn = new Button("Enregistrer mon profil", e -> {
            try {
                // 1. Update Utilisateur
                try (PreparedStatement pst = con.prepareStatement("update utilisateur set photo_url=?, email=?, infos_sup=? where id=?")) {
                    pst.setString(1, photoUrlField.getValue());
                    pst.setString(2, emailField.getValue());
                    pst.setString(3, infosSupField.getValue());
                    pst.setInt(4, currentUser.getId());
                    pst.executeUpdate();
                }
                
                currentUser.setPhotoUrl(photoUrlField.getValue());
                currentUser.setEmail(emailField.getValue());
                currentUser.setInfosSup(infosSupField.getValue());

                // 2. Update Joueur (Si existe)
                if (linkedJoueur != null) {
                    try (PreparedStatement pst = con.prepareStatement("update joueur set date_naissance=?, instagram=?, facebook=?, twitter=? where id_utilisateur=?")) {
                        pst.setDate(1, birthDate.getValue() != null ? java.sql.Date.valueOf(birthDate.getValue()) : null);
                        pst.setString(2, insta.getValue());
                        pst.setString(3, fb.getValue());
                        pst.setString(4, tw.getValue());
                        pst.setInt(5, currentUser.getId());
                        pst.executeUpdate();
                    }
                    if(birthDate.getValue() != null) currentUser.setDateNaissance(birthDate.getValue());
                }

                Notification.show("Profil sauvegardé avec succès !"); 
                d.close(); 
                showMainApplication(); 
            } catch (SQLException ex) { Notification.show("Erreur BDD : " + ex.getMessage()); }
        });
        
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitBtn.setWidthFull();

        layout.add(new H4("Mon Compte"), photoUrlField, upload, emailField, infosSupField, new Hr(), new H4("Ma Fiche Joueur"), birthDate, insta, fb, tw, new Hr(), submitBtn);
        d.add(layout); d.open();
    }

    private void openJoueurDetailsDialog(Joueur j) {
        Dialog d = new Dialog(); d.setHeaderTitle("Fiche Joueur");
        VerticalLayout l = new VerticalLayout(); l.setAlignItems(Alignment.CENTER);
        
        if (j.getPhotoUrl() != null && !j.getPhotoUrl().isEmpty()) {
            com.vaadin.flow.component.html.Image img = new com.vaadin.flow.component.html.Image(j.getPhotoUrl(), "Avatar");
            img.setWidth("100px"); img.setHeight("100px"); img.getStyle().set("border-radius", "50%").set("object-fit", "cover");
            l.add(img);
        }
        l.add(new H3(j.getPrenom() + " " + j.getNom()));
        if(j.getDateNaissance() != null) l.add(new Span("Né(e) le : " + j.getDateNaissance().toString()));
        l.add(new Span("Club : " + j.getNomClub()));
        
        HorizontalLayout socials = new HorizontalLayout();
        if(j.getInstagram() != null && !j.getInstagram().isEmpty()) { Anchor a = new Anchor(j.getInstagram(), new Icon(VaadinIcon.CAMERA)); a.setTarget("_blank"); socials.add(a); }
        if(j.getFacebook() != null && !j.getFacebook().isEmpty()) { Anchor a = new Anchor(j.getFacebook(), new Icon(VaadinIcon.THUMBS_UP)); a.setTarget("_blank"); socials.add(a); }
        if(j.getTwitter() != null && !j.getTwitter().isEmpty()) { Anchor a = new Anchor(j.getTwitter(), new Icon(VaadinIcon.PAPERPLANE)); a.setTarget("_blank"); socials.add(a); }
        if(socials.getComponentCount() > 0) l.add(socials);
        
        Button close = new Button("Fermer", e -> d.close());
        l.add(close); d.add(l); d.open();
    }

    private void openClubDetailsDialog(Club c) {
        Dialog d = new Dialog(); d.setHeaderTitle(c.getNom()); d.setWidth("800px"); d.setHeight("600px");
        Tabs tabs = new Tabs();
        Tab tabInfos = new Tab("Infos & Carte"); Tab tabTerrains = new Tab("Terrains"); Tab tabMembres = new Tab("Effectif");
        tabs.add(tabInfos, tabTerrains, tabMembres);
        VerticalLayout content = new VerticalLayout(); content.setSizeFull();
        tabs.addSelectedChangeListener(e -> updateContent(e.getSelectedTab(), c, content, tabInfos, tabTerrains, tabMembres));
        tabs.setSelectedTab(tabInfos); updateContent(tabInfos, c, content, tabInfos, tabTerrains, tabMembres);
        d.add(tabs, content); d.open();
    }
    
    private void updateContent(Tab selectedTab, Club c, VerticalLayout content, Tab tabInfos, Tab tabTerrains, Tab tabMembres) {
        content.removeAll();
        if (selectedTab.equals(tabInfos)) {
            if(c.getLogoUrl() != null && !c.getLogoUrl().isEmpty()) { com.vaadin.flow.component.html.Image img = new com.vaadin.flow.component.html.Image(c.getLogoUrl(), "Logo"); img.setHeight("100px"); content.add(img); }
            content.add(new H4("A propos"));
            if(c.getAnneeCreation() > 0) content.add(new Span("Créé en : " + c.getAnneeCreation()));
            if(c.getDescription() != null) content.add(new Span(c.getDescription()));
            content.add(new H4("Contact & Réseaux"));
            HorizontalLayout socials = new HorizontalLayout();
            if(c.getInstagram() != null && !c.getInstagram().isEmpty()) { Anchor a = new Anchor(c.getInstagram(), new Icon(VaadinIcon.CAMERA)); a.setTarget("_blank"); socials.add(a); }
            if(c.getFacebook() != null && !c.getFacebook().isEmpty()) { Anchor a = new Anchor(c.getFacebook(), new Icon(VaadinIcon.THUMBS_UP)); a.setTarget("_blank"); socials.add(a); }
            if(c.getTwitter() != null && !c.getTwitter().isEmpty()) { Anchor a = new Anchor(c.getTwitter(), new Icon(VaadinIcon.PAPERPLANE)); a.setTarget("_blank"); socials.add(a); }
            content.add(socials);
            if (c.getEmail() != null) content.add(new Span("Email: " + c.getEmail()));
            if (c.getTelephone() != null) content.add(new Span("Tél: " + c.getTelephone()));
            if (c.getAdresse() != null) {
                content.add(new Span("Adresse: " + c.getAdresse()));
                IFrame map = new IFrame(); map.setWidth("100%"); map.setHeight("300px"); map.getStyle().set("border", "1px solid #ddd");
                String encoded = URLEncoder.encode(c.getAdresse(), StandardCharsets.UTF_8);
                map.setSrc("https://maps.google.com/maps?q=" + encoded + "&t=&z=15&ie=UTF8&iwloc=&output=embed");
                content.add(map);
            }
        } else if (selectedTab.equals(tabTerrains)) {
            Grid<Terrain> g = new Grid<>(Terrain.class, false); g.addColumn(Terrain::getNom).setHeader("Terrain"); g.addColumn(t -> t.isEstInterieur() ? "Intérieur" : "Extérieur").setHeader("Type");
            try { g.setItems(Terrain.getByClub(this.con, c.getId())); } catch(SQLException ex){} content.add(g);
        } else if (selectedTab.equals(tabMembres)) {
            Grid<Joueur> g = new Grid<>(Joueur.class, false); g.addColumn(Joueur::getNom).setHeader("Nom"); g.addColumn(Joueur::getPrenom).setHeader("Prénom");
            try { g.setItems(Joueur.getByClub(this.con, c.getId())); } catch(SQLException ex){} content.add(g);
        }
    }
    
    private HorizontalLayout createFormulaireAjout() {
        HorizontalLayout form = new HorizontalLayout(); form.setWidthFull(); form.setAlignItems(Alignment.BASELINE);
        TextField nomField = new TextField("Nom tournoi");
        DatePicker dateField = new DatePicker("Date");
        ComboBox<Loisir> sportSelect = new ComboBox<>("Sport");
        Button addButton = new Button("Ajouter Tournoi", e -> {
            try {
                if (currentUser.getIdClub() == null) { Notification.show("Créez un club d'abord"); return; }
                new Tournoi(nomField.getValue(), dateField.getValue(), sportSelect.getValue(), new Club(currentUser.getIdClub(), "temp")).saveInDB(this.con);
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
    }
    
    private void updateGrid() { try { this.grid.setItems(Tournoi.getAll(this.con)); } catch (SQLException ex) {} }
    
    private void openEditTournoiDialog(Tournoi t) {
        Dialog d = new Dialog(); d.setHeaderTitle("Modifier Tournoi");
        TextField nom = new TextField("Nom"); nom.setValue(t.getNom());
        DatePicker date = new DatePicker("Date"); if(t.getDateDebut() != null) date.setValue(t.getDateDebut());
        Button save = new Button("Enregistrer", e -> { try { t.setNom(nom.getValue()); t.setDateDebut(date.getValue()); t.update(this.con); updateGrid(); d.close(); } catch (SQLException ex) {} });
        d.add(new VerticalLayout(nom, date, save)); d.open();
    }
    
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
        if (u.getEmail() != null) v.add(new Span("Email : " + u.getEmail()));
        if (u.getInfosSup() != null) v.add(new Span("Infos : " + u.getInfosSup()));
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
}