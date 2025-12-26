package fr.insa.toto.webui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.IFrame; // Pour la carte
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.EmailField; // Spécifique Email
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Club;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Terrain;
import fr.insa.toto.model.Utilisateur;
import java.io.InputStream;
import java.net.URLEncoder; // Pour encoder l'adresse URL
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;
import org.apache.commons.io.IOUtils;

@Route(value = "club")
public class VueClub extends VerticalLayout {

    private Connection con;
    private Utilisateur currentUser;
    private Club myClub;
    private Tabs tabs;
    private VerticalLayout content;

    public VueClub() {
        this.setSizeFull();
        try {
            this.con = ConnectionSimpleSGBD.defaultCon();
            this.currentUser = (Utilisateur) VaadinSession.getCurrent().getAttribute("user");
            
            if (currentUser == null || !currentUser.isAdmin()) {
                this.add(new H3("Accès refusé."));
                return;
            }
            if (currentUser.getIdClub() == null) { showCreationUI(); } 
            else { chargerClubEtAfficherInterface(); }
            
        } catch (SQLException ex) { this.add(new Span("Erreur BDD : " + ex.getMessage())); }
    }

    // --- (Méthodes showCreationUI et creerNouveauClub inchangées ici) ---
    // Copiez-les depuis votre code précédent si besoin, je me concentre sur la gestion ci-dessous.

    private void showCreationUI() {
        this.removeAll(); this.setAlignItems(Alignment.CENTER);
        H2 title = new H2("Bienvenue ! Créez votre Club");
        TextField nomClubField = new TextField("Nom du Club");
        Button createBtn = new Button("Créer", e -> creerNouveauClub(nomClubField.getValue()));
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        this.add(title, nomClubField, createBtn);
    }
    
    private void creerNouveauClub(String nom) {
        try {
            Club newClub = new Club(nom);
            int newId = newClub.saveInDB(con);
            newClub.setId(newId);
            this.myClub = newClub;
            try (PreparedStatement pst = con.prepareStatement("UPDATE utilisateur SET id_club = ? WHERE id = ?")) {
                pst.setInt(1, newId); pst.setInt(2, currentUser.getId()); pst.executeUpdate();
            }
            currentUser.setIdClub(newId);
            VaadinSession.getCurrent().setAttribute("user", currentUser);
            chargerClubEtAfficherInterface();
        } catch(SQLException ex) { Notification.show("Erreur création"); }
    }

    private void chargerClubEtAfficherInterface() throws SQLException {
        Optional<Club> c = Club.getById(con, currentUser.getIdClub());
        if(c.isPresent()) { this.myClub = c.get(); buildMainUI(); }
    }

    private void buildMainUI() {
        this.removeAll();
        HorizontalLayout header = new HorizontalLayout(
            new Button("Retour", new Icon(VaadinIcon.ARROW_LEFT), e -> e.getSource().getUI().ifPresent(ui -> ui.navigate(VuePrincipale.class))),
            new H1(myClub.getNom())
        );
        header.setAlignItems(Alignment.CENTER);
        
        tabs = new Tabs();
        Tab tabInfos = new Tab("Informations & Localisation"); // Renommé
        Tab tabTerrains = new Tab("Infrastructures");
        Tab tabMembres = new Tab("Licenciés & Adhérents");
        tabs.add(tabInfos, tabTerrains, tabMembres);
        
        content = new VerticalLayout();
        content.setSizeFull(); content.setPadding(true);

        tabs.addSelectedChangeListener(e -> {
            content.removeAll();
            if(e.getSelectedTab().equals(tabInfos)) showInfos();
            else if(e.getSelectedTab().equals(tabTerrains)) showInfrastructures();
            else if(e.getSelectedTab().equals(tabMembres)) showMembres();
        });

        this.add(header, tabs, content);
        tabs.setSelectedTab(tabInfos);
        showInfos();
    }

    // --- MODIFICATIONS PRINCIPALES ICI ---
    private void showInfos() {
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        
        // COLONNE GAUCHE : Formulaire
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setWidth("50%");
        
        TextField nom = new TextField("Nom du club"); nom.setValue(myClub.getNom()); nom.setWidthFull();
        com.vaadin.flow.component.textfield.TextArea desc = new com.vaadin.flow.component.textfield.TextArea("Description");
        desc.setValue(myClub.getDescription() != null ? myClub.getDescription() : ""); desc.setWidthFull();

        // Nouveaux champs
        EmailField email = new EmailField("Email de contact");
        email.setValue(myClub.getEmail() != null ? myClub.getEmail() : "");
        email.setWidthFull();
        
        TextField tel = new TextField("Téléphone"); 
        tel.setValue(myClub.getTelephone() != null ? myClub.getTelephone() : "");
        tel.setWidthFull();

        TextField adresse = new TextField("Adresse complète");
        adresse.setValue(myClub.getAdresse() != null ? myClub.getAdresse() : "");
        adresse.setPlaceholder("ex: 12 Rue de la Paix, Paris");
        adresse.setWidthFull();
        
        // COLONNE DROITE : Logo et Carte
        VerticalLayout visualLayout = new VerticalLayout();
        visualLayout.setWidth("50%");
        
        // Gestion Logo (inchangée)
        TextField logoUrl = new TextField("Logo (URL ou Upload)");
        logoUrl.setValue(myClub.getLogoUrl() != null ? myClub.getLogoUrl() : "");
        logoUrl.setWidthFull();
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png");
        upload.setDropLabel(new Span("Glisser logo"));
        upload.addSucceededListener(event -> {
            try (InputStream in = buffer.getInputStream()) {
                String base64 = "data:" + event.getMIMEType() + ";base64," + Base64.getEncoder().encodeToString(IOUtils.toByteArray(in));
                logoUrl.setValue(base64); Notification.show("Logo reçu");
            } catch (Exception ex) {}
        });

        // --- MINI CARTE ---
        Span mapTitle = new Span("Aperçu Localisation");
        mapTitle.getStyle().set("font-weight", "bold");
        
        IFrame mapFrame = new IFrame();
        mapFrame.setWidth("100%");
        mapFrame.setHeight("300px");
        mapFrame.getStyle().set("border", "1px solid #ddd").set("border-radius", "8px");
        
        // Fonction pour mettre à jour la carte
        Runnable updateMap = () -> {
            String adr = adresse.getValue();
            if (adr != null && !adr.isEmpty()) {
                String encoded = URLEncoder.encode(adr, StandardCharsets.UTF_8);
                // Utilisation de Google Maps Embed (ne nécessite pas toujours de clé pour l'affichage simple)
                mapFrame.setSrc("https://maps.google.com/maps?q=" + encoded + "&t=&z=15&ie=UTF8&iwloc=&output=embed");
            } else {
                mapFrame.setSrc(""); // Carte vide
            }
        };
        
        // Initialiser la carte
        updateMap.run();
        
        Button refreshMapBtn = new Button("Actualiser la carte", new Icon(VaadinIcon.MAP_MARKER));
        refreshMapBtn.addClickListener(e -> updateMap.run());
        refreshMapBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        visualLayout.add(new H3("Visuels"), logoUrl, upload, mapTitle, mapFrame, refreshMapBtn);

        // --- BOUTON SAUVEGARDER ---
        Button save = new Button("Enregistrer tout", e -> {
            try {
                myClub.setNom(nom.getValue());
                myClub.setDescription(desc.getValue());
                myClub.setEmail(email.getValue()); // Save Email
                myClub.setTelephone(tel.getValue());
                myClub.setAdresse(adresse.getValue()); // Save Adresse
                myClub.setLogoUrl(logoUrl.getValue());
                
                myClub.updateInfos(con);
                updateMap.run(); // Met à jour la carte si l'adresse a changé
                Notification.show("Informations sauvegardées !").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch(SQLException ex) { Notification.show("Erreur BDD"); }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setWidthFull();

        formLayout.add(new H3("Détails du Club"), nom, desc, email, tel, adresse, save);
        
        mainLayout.add(formLayout, visualLayout);
        content.add(mainLayout);
    }

// --- ONGLET 2 : INFRASTRUCTURES ---
    private void showInfrastructures() {
        content.removeAll();
        
        Grid<Terrain> grid = new Grid<>(Terrain.class, false);
        grid.addColumn(Terrain::getNom).setHeader("Terrain / Salle");
        grid.addColumn(t -> t.isEstInterieur() ? "Intérieur" : "Extérieur").setHeader("Type");
        grid.addComponentColumn(t -> {
            Button del = new Button(new Icon(VaadinIcon.TRASH));
            del.addThemeVariants(ButtonVariant.LUMO_ERROR);
            del.addClickListener(e -> {
               try { t.delete(con); showInfrastructures(); } catch(SQLException ex) { Notification.show("Impossible de supprimer"); }
            });
            return del;
        });
        
        try { grid.setItems(Terrain.getByClub(con, myClub.getId())); } catch(SQLException e) {}
        
        HorizontalLayout addLayout = new HorizontalLayout();
        addLayout.setAlignItems(Alignment.BASELINE);
        TextField nomT = new TextField("Nouveau terrain");
        Checkbox inT = new Checkbox("Intérieur ?");
        Button addBtn = new Button("Ajouter", e -> {
            try { 
                new Terrain(nomT.getValue(), inT.getValue(), myClub.getId()).saveInDB(con);
                showInfrastructures();
            } catch(SQLException ex) { Notification.show("Erreur ajout"); }
        });
        addLayout.add(nomT, inT, addBtn);
        
        content.add(new H3("Mes Terrains"), grid, addLayout);
    }

    // --- ONGLET 3 : MEMBRES (LICENCIÉS) ---
    private void showMembres() {
        content.removeAll();
        
        Grid<Joueur> grid = new Grid<>(Joueur.class, false);
        grid.addColumn(Joueur::getNom).setHeader("Nom");
        grid.addColumn(Joueur::getPrenom).setHeader("Prénom");
        
        grid.addComponentColumn(j -> {
            Button del = new Button(new Icon(VaadinIcon.TRASH));
            del.addThemeVariants(ButtonVariant.LUMO_ERROR);
            del.addClickListener(e -> {
               try { j.delete(con); showMembres(); } catch(SQLException ex) { Notification.show("Erreur suppression"); }
            });
            return del;
        });
        
        try { grid.setItems(Joueur.getByClub(con, myClub.getId())); } catch(SQLException e) {}
        
        HorizontalLayout addLayout = new HorizontalLayout();
        addLayout.setAlignItems(Alignment.BASELINE);
        TextField nomJ = new TextField("Nom");
        TextField prenomJ = new TextField("Prénom");
        
        Button addBtn = new Button("Ajouter Licencié", e -> {
            if(nomJ.isEmpty() || prenomJ.isEmpty()) return;
            try {
                // Utilise le constructeur corrigé (sans idEquipe)
                new Joueur(nomJ.getValue(), prenomJ.getValue(), myClub.getId()).saveInDB(con);
                showMembres();
                Notification.show("Joueur ajouté !");
            } catch(SQLException ex) { Notification.show("Erreur ajout : " + ex.getMessage()); }
        });
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        addLayout.add(nomJ, prenomJ, addBtn);
        
        content.add(new H3("Gestion des Licenciés"), grid, addLayout);
    }
}