package fr.insa.toto.webui;

import java.time.format.DateTimeFormatter;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.html.Image;
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
import fr.insa.toto.model.*;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;

@Route(value = "")
@PageTitle("Multisport")
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
        this.setSizeFull();      
        this.setPadding(false);  
        this.setSpacing(false);  

        H1 title = new H1("Connexion Multisport");
        
        TextField userField = new TextField("Identifiant");
        userField.setWidthFull();
        userField.setPrefixComponent(new Icon(VaadinIcon.USER));

        PasswordField passField = new PasswordField("Mot de passe");
        passField.setWidthFull();
        passField.setPrefixComponent(new Icon(VaadinIcon.LOCK));

        Button loginButton = new Button("Se connecter", e -> {
            try {
                Optional<Utilisateur> user = Utilisateur.login(this.con, userField.getValue(), passField.getValue());
                if (user.isPresent()) {
                    this.currentUser = user.get();
                    VaadinSession.getCurrent().setAttribute("user", this.currentUser);
                    Notification.show("Bienvenue " + this.currentUser.getSurnom());
                    showMainApplication();
                } else {
                    Notification.show("Identifiants incorrects").addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
                }
            } catch (SQLException ex) { Notification.show("Erreur technique : " + ex.getMessage()); }
        });
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.addClickShortcut(Key.ENTER);

        Button registerLink = new Button("Pas de compte ? Créer un compte", e -> showRegisterScreen());
        registerLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        registerLink.getStyle().set("color", "black");
        registerLink.setWidthFull();

        VerticalLayout formContainer = new VerticalLayout(title, userField, passField, loginButton, new Hr(), registerLink);
        formContainer.addClassName("login-form-container");
        formContainer.setAlignItems(Alignment.CENTER);
        formContainer.setSpacing(true);

        VerticalLayout loginLayout = new VerticalLayout(formContainer);
        loginLayout.addClassName("login-screen");
        loginLayout.setSizeFull(); 
        loginLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        loginLayout.setAlignItems(Alignment.CENTER);

        this.add(loginLayout);
    }

    private void showRegisterScreen() {
        this.removeAll();
        this.setSizeFull();      
        this.setPadding(false);  
        this.setSpacing(false);  
        
        H1 title = new H1("Créer un compte");
        
        TextField idField = new TextField("Identifiant (Connexion)"); idField.setRequired(true); idField.setWidthFull();
        TextField prenomField = new TextField("Prénom"); prenomField.setRequired(true); prenomField.setWidthFull();
        TextField nomField = new TextField("Nom"); nomField.setRequired(true); nomField.setWidthFull();
        TextField surnomField = new TextField("Surnom (Facultatif)"); surnomField.setWidthFull();
        PasswordField passField = new PasswordField("Mot de passe"); passField.setWidthFull();

        ComboBox<Club> clubSelect = new ComboBox<>("Mon Club (Optionnel)");
        clubSelect.setWidthFull();
        try { clubSelect.setItems(Club.getAll(this.con)); clubSelect.setItemLabelGenerator(Club::getNom); } catch (SQLException e) {}
        
        RadioButtonGroup<String> roleSelect = new RadioButtonGroup<>();
        roleSelect.setItems("Visiteur", "Administrateur");
        roleSelect.setValue("Visiteur");
        roleSelect.addClassName("white-text-radio");
        
        PasswordField adminKeyField = new PasswordField("Clé Administrateur");
        adminKeyField.setVisible(false);
        adminKeyField.setWidthFull();
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
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.setWidthFull();

        Button cancelButton = new Button("Annuler", e -> showLoginScreen());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.getStyle().set("color", "black");
        cancelButton.setWidthFull();

        VerticalLayout formContainer = new VerticalLayout(title, idField, prenomField, nomField, surnomField, passField, clubSelect, roleSelect, adminKeyField, createButton, cancelButton);
        formContainer.addClassName("login-form-container");
        formContainer.setAlignItems(Alignment.CENTER);
        formContainer.setSpacing(false); 

        VerticalLayout registerLayout = new VerticalLayout(formContainer);
        registerLayout.addClassName("login-screen");
        registerLayout.setSizeFull();
        registerLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        registerLayout.setAlignItems(Alignment.CENTER);
        
        this.add(registerLayout);
    }

    private void showMainApplication() {
        this.removeAll();
        this.setPadding(true);  
        this.setSpacing(true);  
        this.setSizeUndefined();     
        this.setWidthFull();
        
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

        // BOUTONS CLASSEMENT ET MEMBRES
        Button globalRankBtn = new Button("Classement Général", new Icon(VaadinIcon.TROPHY));
        globalRankBtn.addClickListener(e -> showGlobalRanking());
        leftHeader.add(globalRankBtn);

        Button userListBtn = new Button("Membres", new Icon(VaadinIcon.USERS));
        userListBtn.addClickListener(e -> openUserListDrawer());
        leftHeader.add(userListBtn);
        
        Button logoutBtn = new Button("Déconnexion", e -> { 
            VaadinSession.getCurrent().setAttribute("user", null);
            showLoginScreen(); 
        });
        
        Button helpBtn = new Button("Aide", new Icon(VaadinIcon.QUESTION_CIRCLE_O));
        helpBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        helpBtn.addClickListener(e -> helpBtn.getUI().ifPresent(ui -> ui.getPage().open("aide.pdf", "_blank")));
        
        HorizontalLayout rightHeader = new HorizontalLayout(helpBtn, logoutBtn);
        rightHeader.setAlignItems(Alignment.CENTER);

        header.add(leftHeader, rightHeader);
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

        // Conteneur pour le Titre et le Filtre alignés sur la même ligne
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.BASELINE); // Aligne le texte du titre avec le bas du champ filtre
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN); // Écarte les éléments

        H1 titreSection = new H1("Liste des Tournois");
        
        // Création du Filtre
        this.filterStatus = new ComboBox<>();
        this.filterStatus.setItems("Tous", "À venir", "En cours", "Terminés");
        this.filterStatus.setValue("Tous"); // Valeur par défaut
        this.filterStatus.setPlaceholder("Filtrer par statut...");
        this.filterStatus.addValueChangeListener(e -> updateGrid()); // Déclenche le tri au changement
        this.filterStatus.setWidth("200px");

        toolbar.add(titreSection, this.filterStatus);
        this.add(toolbar);
        
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
        joueursLayout.add(new H3("Annuaire des Joueurs"));
        
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
    
    // METHODE UTILITAIRE 
    private String getValidUrl(String input, String baseUrl) {
        if (input == null || input.isEmpty()) return "";
        if (input.startsWith("http")) return input;
        return baseUrl + input;
    }
    
    // CLASSEMENT GÉNÉRAL
    private void showGlobalRanking() {
        Dialog d = new Dialog(); 
        d.setHeaderTitle("Classement Général (Tous Tournois)"); 
        d.setWidth("600px");
        
        Grid<Map.Entry<String, Integer>> grid = new Grid<>();
        grid.addColumn(Map.Entry::getKey).setHeader("Joueur");
        grid.addColumn(Map.Entry::getValue).setHeader("Victoires Totales").setSortable(true);
        
        Map<String, Integer> globalStats = new HashMap<>();
        try {
            // Calcul simple : Nombre de matchs gagnés (score équipe 1 vs 2)
            String sql = "SELECT j.nom, j.prenom, COUNT(*) as victoires FROM match_tournoi m " +
                         "JOIN composition c1 ON m.id_equipe1 = c1.id_equipe " +
                         "JOIN joueur j ON c1.id_joueur = j.id " +
                         "WHERE m.est_joue = true AND m.score1 > m.score2 " +
                         "GROUP BY j.id " +
                         "UNION ALL " +
                         "SELECT j.nom, j.prenom, COUNT(*) as victoires FROM match_tournoi m " +
                         "JOIN composition c2 ON m.id_equipe2 = c2.id_equipe " +
                         "JOIN joueur j ON c2.id_joueur = j.id " +
                         "WHERE m.est_joue = true AND m.score2 > m.score1 " +
                         "GROUP BY j.id";
            
            PreparedStatement pst = con.prepareStatement(sql);
            java.sql.ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                String nom = rs.getString("prenom") + " " + rs.getString("nom");
                globalStats.put(nom, globalStats.getOrDefault(nom, 0) + rs.getInt("victoires"));
            }
            grid.setItems(globalStats.entrySet());
        } catch (Exception e) {}
        
        d.add(grid);
        d.getFooter().add(new Button("Fermer", e -> d.close()));
        d.open();
    }
    
    // TIROIR MEMBRES
    private void openUserListDrawer() {
        Dialog drawer = new Dialog(); drawer.setHeaderTitle("Membres"); drawer.setWidth("350px");
        VerticalLayout l = new VerticalLayout();
        try { 
            for (Utilisateur u : Utilisateur.getAllUsers(this.con)) {
                String label = u.getPrenom() + " " + u.getNom();
                if (u.getSurnom() != null && !u.getSurnom().isEmpty() && !u.getSurnom().equals(u.getIdentifiant())) {
                    label += " (" + u.getSurnom() + ")";
                }
                
                HorizontalLayout row = new HorizontalLayout(createSmallAvatar(u), new Span(label));
                row.setAlignItems(Alignment.CENTER);
                row.getStyle().set("cursor", "pointer").set("padding", "5px").set("border-bottom", "1px solid #eee");
                row.addClickListener(e -> showPublicProfile(u));
                l.add(row);
            }
        } catch(SQLException ex){ l.add(new Span("Erreur chargement membres")); }
        drawer.add(l); 
        drawer.getFooter().add(new Button("Fermer", e -> drawer.close()));
        drawer.open();
    }
    
    // PROFIL PUBLIC
    private void showPublicProfile(Utilisateur u) {
        Dialog d = new Dialog(); d.setHeaderTitle("Profil de " + u.getSurnom()); d.setWidth("400px");
        VerticalLayout v = new VerticalLayout();
        v.setAlignItems(Alignment.CENTER);
        
        if (u.getPhotoUrl() != null && !u.getPhotoUrl().isEmpty()) {
            Image img = new Image(u.getPhotoUrl(), "Avatar");
            img.setWidth("100px"); img.setHeight("100px"); img.getStyle().set("border-radius", "50%").set("object-fit", "cover");
            v.add(img);
        }
        v.add(new H3(u.getPrenom() + " " + u.getNom()));
        
        if (u.getEmail() != null && !u.getEmail().isEmpty()) v.add(new Span("Email : " + u.getEmail()));
        if (u.getInfosSup() != null && !u.getInfosSup().isEmpty()) v.add(new Span("Bio : " + u.getInfosSup()));
        
        v.add(new Hr());

        try {
            Optional<Joueur> optJ = Joueur.getByUtilisateurId(con, u.getId());
            if (optJ.isPresent()) {
                Joueur j = optJ.get();
                if (j.getNomClub() != null) {
                    HorizontalLayout clubLayout = new HorizontalLayout();
                    clubLayout.setAlignItems(Alignment.CENTER);
                    if (j.getClubLogoUrl() != null && !j.getClubLogoUrl().isEmpty()) {
                        Image clubLogo = new Image(j.getClubLogoUrl(), "Logo");
                        clubLogo.setWidth("30px");
                        clubLayout.add(clubLogo);
                    }
                    clubLayout.add(new Span("Club : " + j.getNomClub()));
                    v.add(clubLayout);
                }
                
                HorizontalLayout socials = new HorizontalLayout();
                if(j.getInstagram() != null && !j.getInstagram().isEmpty()) { 
                    Anchor a = new Anchor(getValidUrl(j.getInstagram(), "https://instagram.com/"), new Icon(VaadinIcon.CAMERA)); 
                    a.setTarget("_blank"); socials.add(a); 
                }
                if(j.getFacebook() != null && !j.getFacebook().isEmpty()) { 
                    Anchor a = new Anchor(getValidUrl(j.getFacebook(), "https://facebook.com/"), new Icon(VaadinIcon.THUMBS_UP)); 
                    a.setTarget("_blank"); socials.add(a); 
                }
                if(j.getTwitter() != null && !j.getTwitter().isEmpty()) { 
                    Anchor a = new Anchor(getValidUrl(j.getTwitter(), "https://twitter.com/"), new Icon(VaadinIcon.PAPERPLANE)); 
                    a.setTarget("_blank"); socials.add(a); 
                }
                
                if (socials.getComponentCount() > 0) v.add(new H4("Réseaux Sociaux"), socials);
            }
        } catch (SQLException e) { v.add(new Span("Erreur détails joueur.")); }

        Button close = new Button("Fermer", e -> d.close());
        v.add(new Hr(), close);
        d.add(v); d.open();
    }

    // EDITION DE PROFIL
    private void openProfileDialog() {
        Dialog d = new Dialog(); d.setHeaderTitle("Mon Profil & Fiche Joueur");
        d.setWidth("600px");
        
        VerticalLayout layout = new VerticalLayout();
        
        TextField nomField = new TextField("Nom"); 
        nomField.setValue(currentUser.getNom() != null ? currentUser.getNom() : "");
        
        TextField prenomField = new TextField("Prénom"); 
        prenomField.setValue(currentUser.getPrenom() != null ? currentUser.getPrenom() : "");
        
        TextField surnomField = new TextField("Surnom"); 
        surnomField.setValue(currentUser.getSurnom() != null ? currentUser.getSurnom() : "");
        
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

        DatePicker birthDate = new DatePicker("Date de naissance"); birthDate.setWidthFull();
        TextField insta = new TextField("Instagram"); insta.setPrefixComponent(new Icon(VaadinIcon.CAMERA)); insta.setWidthFull();
        TextField fb = new TextField("Facebook"); fb.setPrefixComponent(new Icon(VaadinIcon.THUMBS_UP)); fb.setWidthFull();
        TextField tw = new TextField("Twitter"); tw.setPrefixComponent(new Icon(VaadinIcon.PAPERPLANE)); tw.setWidthFull();
        
        Joueur tempJ = null;
        try {
             Optional<Joueur> opt = Joueur.getByUtilisateurId(con, currentUser.getId());
             if (opt.isPresent()) tempJ = opt.get();
        } catch (SQLException e) { }
        
        final Joueur linkedJoueur = tempJ;

        if (linkedJoueur != null) {
            if (linkedJoueur.getDateNaissance() != null) birthDate.setValue(linkedJoueur.getDateNaissance());
            if (linkedJoueur.getInstagram() != null) insta.setValue(linkedJoueur.getInstagram());
            if (linkedJoueur.getFacebook() != null) fb.setValue(linkedJoueur.getFacebook());
            if (linkedJoueur.getTwitter() != null) tw.setValue(linkedJoueur.getTwitter());
        } else {
            layout.add(new Span("⚠️ Aucune fiche joueur liée."));
        }

        Button submitBtn = new Button("Enregistrer mon profil", e -> {
            try {
                try (PreparedStatement pst = con.prepareStatement("update utilisateur set photo_url=?, email=?, infos_sup=?, nom=?, prenom=?, surnom=? where id=?")) {
                    pst.setString(1, photoUrlField.getValue());
                    pst.setString(2, emailField.getValue());
                    pst.setString(3, infosSupField.getValue());
                    pst.setString(4, nomField.getValue());
                    pst.setString(5, prenomField.getValue());
                    pst.setString(6, surnomField.getValue());
                    pst.setInt(7, currentUser.getId());
                    pst.executeUpdate();
                }
                
                currentUser.setPhotoUrl(photoUrlField.getValue());
                currentUser.setEmail(emailField.getValue());
                currentUser.setInfosSup(infosSupField.getValue());
                currentUser.setNom(nomField.getValue());
                currentUser.setPrenom(prenomField.getValue());
                currentUser.setSurnom(surnomField.getValue());

                if (linkedJoueur != null) {
                    try (PreparedStatement pst = con.prepareStatement("update joueur set date_naissance=?, instagram=?, facebook=?, twitter=?, nom=?, prenom=? where id_utilisateur=?")) {
                        pst.setDate(1, birthDate.getValue() != null ? java.sql.Date.valueOf(birthDate.getValue()) : null);
                        pst.setString(2, insta.getValue());
                        pst.setString(3, fb.getValue());
                        pst.setString(4, tw.getValue());
                        pst.setString(5, nomField.getValue());
                        pst.setString(6, prenomField.getValue());
                        pst.setInt(7, currentUser.getId());
                        pst.executeUpdate();
                    }
                    if(birthDate.getValue() != null) currentUser.setDateNaissance(birthDate.getValue());
                }

                Notification.show("Profil sauvegardé !"); 
                d.close(); 
                showMainApplication(); 
            } catch (SQLException ex) { Notification.show("Erreur BDD : " + ex.getMessage()); }
        });
        
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitBtn.setWidthFull();

        layout.add(new H4("Mon Compte"), nomField, prenomField, surnomField, photoUrlField, upload, emailField, infosSupField, new Hr(), new H4("Ma Fiche Joueur"), birthDate, insta, fb, tw, new Hr(), submitBtn);
        d.add(layout); d.open();
    }

    private void openJoueurDetailsDialog(Joueur j) {
        Dialog d = new Dialog(); d.setHeaderTitle("Fiche Joueur");
        VerticalLayout l = new VerticalLayout(); l.setAlignItems(Alignment.CENTER);
        
        if (j.getPhotoUrl() != null && !j.getPhotoUrl().isEmpty()) {
            Image img = new Image(j.getPhotoUrl(), "Avatar");
            img.setWidth("100px"); img.setHeight("100px"); img.getStyle().set("border-radius", "50%").set("object-fit", "cover");
            l.add(img);
        }
        l.add(new H3(j.getPrenom() + " " + j.getNom()));
        if(j.getDateNaissance() != null) l.add(new Span("Né(e) le : " + j.getDateNaissance().toString()));
        l.add(new Span("Club : " + j.getNomClub()));
        
        HorizontalLayout socials = new HorizontalLayout();
        if(j.getInstagram() != null && !j.getInstagram().isEmpty()) { Anchor a = new Anchor(getValidUrl(j.getInstagram(), "https://instagram.com/"), new Icon(VaadinIcon.CAMERA)); a.setTarget("_blank"); socials.add(a); }
        if(j.getFacebook() != null && !j.getFacebook().isEmpty()) { Anchor a = new Anchor(getValidUrl(j.getFacebook(), "https://facebook.com/"), new Icon(VaadinIcon.THUMBS_UP)); a.setTarget("_blank"); socials.add(a); }
        if(j.getTwitter() != null && !j.getTwitter().isEmpty()) { Anchor a = new Anchor(getValidUrl(j.getTwitter(), "https://twitter.com/"), new Icon(VaadinIcon.PAPERPLANE)); a.setTarget("_blank"); socials.add(a); }
        
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
            if(c.getLogoUrl() != null && !c.getLogoUrl().isEmpty()) { Image img = new Image(c.getLogoUrl(), "Logo"); img.setHeight("100px"); content.add(img); }
            content.add(new H4("A propos"));
            if(c.getAnneeCreation() > 0) content.add(new Span("Créé en : " + c.getAnneeCreation()));
            if(c.getDescription() != null) content.add(new Span(c.getDescription()));
            content.add(new H4("Contact & Réseaux"));
            HorizontalLayout socials = new HorizontalLayout();
            if(c.getInstagram() != null && !c.getInstagram().isEmpty()) { Anchor a = new Anchor(getValidUrl(c.getInstagram(), "https://instagram.com/"), new Icon(VaadinIcon.CAMERA)); a.setTarget("_blank"); socials.add(a); }
            if(c.getFacebook() != null && !c.getFacebook().isEmpty()) { Anchor a = new Anchor(getValidUrl(c.getFacebook(), "https://facebook.com/"), new Icon(VaadinIcon.THUMBS_UP)); a.setTarget("_blank"); socials.add(a); }
            if(c.getTwitter() != null && !c.getTwitter().isEmpty()) { Anchor a = new Anchor(getValidUrl(c.getTwitter(), "https://twitter.com/"), new Icon(VaadinIcon.PAPERPLANE)); a.setTarget("_blank"); socials.add(a); }
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

        // Colonne Statut avec Badges
        this.grid.addComponentColumn(tournoi -> {
            LocalDate date = tournoi.getDateDebut();
            LocalDate now = LocalDate.now();
            Span badge = new Span();
            
            if (date == null) {
                badge.setText("Inconnu");
            } else if (date.isAfter(now)) {
                badge.setText("À venir");
                // Style Vaadin "badge"
                badge.getElement().getThemeList().add("badge"); 
            } else if (date.isEqual(now)) {
                badge.setText("En cours");
                // Style Vaadin "badge success" (Vert)
                badge.getElement().getThemeList().add("badge success");
            } else {
                badge.setText("Terminé");
                // Style Vaadin "badge contrast" (Gris/Noir)
                badge.getElement().getThemeList().add("badge contrast");
            }
            return badge;
        }).setHeader("Statut").setSortable(true).setAutoWidth(true).setFlexGrow(0);

        this.grid.addColumn(Tournoi::getNom).setHeader("Nom").setSortable(true);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        this.grid.addColumn(tournoi -> 
                tournoi.getDateDebut() != null ? tournoi.getDateDebut().format(formatter) : ""
            )
            .setHeader("Date")
            .setComparator(Tournoi::getDateDebut) 
            .setSortable(true);
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

    private ComboBox<String> filterStatus;
    
    private void updateGrid() {
        try {
            List<Tournoi> allTournois = Tournoi.getAll(this.con);
            
            String filterValue = (filterStatus != null) ? filterStatus.getValue() : "Tous";
            
            if (filterValue == null || filterValue.equals("Tous")) {
                // Pas de filtre, on affiche tout
                this.grid.setItems(allTournois);
            } else {
                // 3. Filtrage en mémoire (Java Stream)
                LocalDate now = LocalDate.now();
                
                List<Tournoi> filteredList = allTournois.stream().filter(t -> {
                    LocalDate d = t.getDateDebut();
                    if (d == null) return false;
                    
                    if (filterValue.equals("À venir")) {
                        return d.isAfter(now);
                    } else if (filterValue.equals("En cours")) {
                        return d.isEqual(now);
                    } else if (filterValue.equals("Terminés")) {
                        return d.isBefore(now);
                    }
                    return true;
                }).collect(java.util.stream.Collectors.toList());
                
                this.grid.setItems(filteredList);
            }
        } catch (SQLException ex) {
            Notification.show("Erreur lors du chargement des tournois");
        }
    }
    
    private void openEditTournoiDialog(Tournoi t) {
        Dialog d = new Dialog(); d.setHeaderTitle("Modifier Tournoi");
        TextField nom = new TextField("Nom"); nom.setValue(t.getNom());
        DatePicker date = new DatePicker("Date"); if(t.getDateDebut() != null) date.setValue(t.getDateDebut());
        Button save = new Button("Enregistrer", e -> { try { t.setNom(nom.getValue()); t.setDateDebut(date.getValue()); t.update(this.con); updateGrid(); d.close(); } catch (SQLException ex) {} });
        d.add(new VerticalLayout(nom, date, save)); d.open();
    }

    private com.vaadin.flow.component.Component createSmallAvatar(Utilisateur u) {
        if (u != null && u.getPhotoUrl() != null && !u.getPhotoUrl().isEmpty()) {
            Image img = new Image(u.getPhotoUrl(), "");
            img.setWidth("40px"); img.setHeight("40px"); img.getStyle().set("border-radius", "50%").set("object-fit", "cover");
            return img;
        }
        Span s = new Span(u != null ? u.getInitiales() : "?");
        s.getStyle().set("background", "#007bff").set("color", "white").set("border-radius", "50%").set("width", "40px").set("height", "40px").set("display", "flex").set("align-items", "center").set("justify-content", "center");
        return s;
    }
}