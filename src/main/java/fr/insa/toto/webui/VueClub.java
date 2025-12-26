package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
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
                this.add(new H3("Accès refusé. Vous devez être administrateur."));
                return;
            }
            if (currentUser.getIdClub() == null) {
                showCreationUI();
            } else {
                chargerClubEtAfficherInterface();
            }
            
        } catch (SQLException ex) { this.add(new Span("Erreur BDD : " + ex.getMessage())); }
    }

    private void showCreationUI() {
        this.removeAll(); this.setAlignItems(Alignment.CENTER);
        H2 title = new H2("Bienvenue ! Créez votre Club");
        TextField nomClubField = new TextField("Nom du Club");
        nomClubField.setPlaceholder("Ex: FC Rœschwoog");
        
        Button createBtn = new Button("Créer", e -> creerNouveauClub(nomClubField.getValue()));
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        this.add(title, nomClubField, createBtn);
    }
    
    private void creerNouveauClub(String nom) {
        if(nom == null || nom.trim().isEmpty()) { Notification.show("Nom obligatoire"); return; }
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
            Notification.show("Club créé !");
            chargerClubEtAfficherInterface();
        } catch(SQLException ex) { Notification.show("Erreur création (Nom déjà pris ?)"); }
    }

    private void chargerClubEtAfficherInterface() throws SQLException {
        Optional<Club> c = Club.getById(con, currentUser.getIdClub());
        if(c.isPresent()) { this.myClub = c.get(); buildMainUI(); } else { this.add(new H3("Erreur critique : Club introuvable.")); }
    }

    private void buildMainUI() {
        this.removeAll();
        HorizontalLayout header = new HorizontalLayout(
            new Button("Retour", new Icon(VaadinIcon.ARROW_LEFT), e -> e.getSource().getUI().ifPresent(ui -> ui.navigate(VuePrincipale.class))),
            new H1(myClub.getNom())
        );
        header.setAlignItems(Alignment.CENTER);
        
        tabs = new Tabs();
        Tab tabInfos = new Tab("Informations");
        Tab tabTerrains = new Tab("Infrastructures");
        Tab tabMembres = new Tab("Membres");
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

    private void showInfos() {
        HorizontalLayout mainLayout = new HorizontalLayout(); mainLayout.setSizeFull();
        VerticalLayout formLayout = new VerticalLayout(); formLayout.setWidth("50%");
        
        TextField nom = new TextField("Nom"); nom.setValue(myClub.getNom()); nom.setWidthFull();
        IntegerField annee = new IntegerField("Année Création"); annee.setValue(myClub.getAnneeCreation() > 0 ? myClub.getAnneeCreation() : null); annee.setWidthFull();
        com.vaadin.flow.component.textfield.TextArea desc = new com.vaadin.flow.component.textfield.TextArea("Description");
        desc.setValue(myClub.getDescription() != null ? myClub.getDescription() : ""); desc.setWidthFull();

        EmailField email = new EmailField("Email"); email.setValue(myClub.getEmail() != null ? myClub.getEmail() : ""); email.setWidthFull();
        TextField tel = new TextField("Téléphone"); tel.setValue(myClub.getTelephone() != null ? myClub.getTelephone() : ""); tel.setWidthFull();
        TextField adresse = new TextField("Adresse"); adresse.setValue(myClub.getAdresse() != null ? myClub.getAdresse() : ""); adresse.setWidthFull();
        
        TextField insta = new TextField("Instagram"); insta.setValue(myClub.getInstagram() != null ? myClub.getInstagram() : ""); insta.setPrefixComponent(new Icon(VaadinIcon.CAMERA)); insta.setWidthFull();
        TextField fb = new TextField("Facebook"); fb.setValue(myClub.getFacebook() != null ? myClub.getFacebook() : ""); fb.setPrefixComponent(new Icon(VaadinIcon.THUMBS_UP)); fb.setWidthFull();
        TextField tw = new TextField("Twitter (X)"); tw.setValue(myClub.getTwitter() != null ? myClub.getTwitter() : ""); tw.setPrefixComponent(new Icon(VaadinIcon.PAPERPLANE)); tw.setWidthFull();

        VerticalLayout visualLayout = new VerticalLayout(); visualLayout.setWidth("50%");
        TextField logoUrl = new TextField("Logo (URL)"); logoUrl.setValue(myClub.getLogoUrl() != null ? myClub.getLogoUrl() : ""); logoUrl.setWidthFull();
        
        MemoryBuffer buffer = new MemoryBuffer(); Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png");
        upload.setDropLabel(new Span("Glisser logo ici"));
        upload.addSucceededListener(event -> {
            try (InputStream in = buffer.getInputStream()) {
                String base64 = "data:" + event.getMIMEType() + ";base64," + Base64.getEncoder().encodeToString(IOUtils.toByteArray(in));
                logoUrl.setValue(base64); Notification.show("Logo reçu !");
            } catch (Exception ex) {}
        });
        
        Button save = new Button("Enregistrer tout", e -> {
            try {
                myClub.setNom(nom.getValue());
                myClub.setAnneeCreation(annee.getValue() != null ? annee.getValue() : 0);
                myClub.setDescription(desc.getValue());
                myClub.setEmail(email.getValue()); 
                myClub.setTelephone(tel.getValue());
                myClub.setAdresse(adresse.getValue());
                myClub.setInstagram(insta.getValue());
                myClub.setFacebook(fb.getValue());
                myClub.setTwitter(tw.getValue());
                myClub.setLogoUrl(logoUrl.getValue());
                myClub.updateInfos(con);
                Notification.show("Informations sauvegardées !").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch(SQLException ex) { Notification.show("Erreur BDD : " + ex.getMessage()); }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY); save.setWidthFull();

        formLayout.add(new H3("Détails Club"), nom, annee, desc, email, tel, adresse, new H3("Réseaux Sociaux"), insta, fb, tw, save);
        visualLayout.add(new H3("Visuel"), logoUrl, upload);
        mainLayout.add(formLayout, visualLayout);
        content.add(mainLayout);
    }

    private void showInfrastructures() {
        content.removeAll();
        Grid<Terrain> grid = new Grid<>(Terrain.class, false);
        grid.addColumn(Terrain::getNom).setHeader("Terrain");
        grid.addColumn(t -> t.isEstInterieur() ? "Intérieur" : "Extérieur").setHeader("Type");
        grid.addComponentColumn(t -> {
            Button del = new Button(new Icon(VaadinIcon.TRASH));
            del.addThemeVariants(ButtonVariant.LUMO_ERROR);
            del.addClickListener(e -> { try { t.delete(con); showInfrastructures(); } catch(SQLException ex) {} });
            return del;
        });
        try { grid.setItems(Terrain.getByClub(con, myClub.getId())); } catch(SQLException e) {}
        HorizontalLayout addLayout = new HorizontalLayout();
        TextField nomT = new TextField("Nouveau terrain"); Checkbox inT = new Checkbox("Intérieur ?");
        Button addBtn = new Button("Ajouter", e -> {
            try { new Terrain(nomT.getValue(), inT.getValue(), myClub.getId()).saveInDB(con); showInfrastructures(); } catch(SQLException ex) {}
        });
        addLayout.add(nomT, inT, addBtn);
        content.add(new H3("Mes Terrains"), grid, addLayout);
    }

    private void showMembres() {
        content.removeAll();
        Grid<Joueur> grid = new Grid<>(Joueur.class, false);
        grid.addColumn(Joueur::getNom).setHeader("Nom");
        grid.addColumn(Joueur::getPrenom).setHeader("Prénom");
        grid.addColumn(j -> j.getIdUtilisateur() != null ? "✅ Compte lié" : "❌ En attente").setHeader("Statut Compte");
        
        grid.addComponentColumn(j -> {
            Button del = new Button(new Icon(VaadinIcon.TRASH));
            del.addThemeVariants(ButtonVariant.LUMO_ERROR);
            del.addClickListener(e -> { try { j.delete(con); showMembres(); } catch(SQLException ex) {} });
            return del;
        });
        
        try { grid.setItems(Joueur.getByClub(con, myClub.getId())); } catch(SQLException e) {}
        
        HorizontalLayout addLayout = new HorizontalLayout();
        TextField nomJ = new TextField("Nom"); TextField prenomJ = new TextField("Prénom");
        Button addBtn = new Button("Ajouter Licencié", e -> {
            if(nomJ.isEmpty()) return;
            try { 
                new Joueur(nomJ.getValue(), prenomJ.getValue(), myClub.getId()).saveInDB(con); 
                showMembres(); 
                Notification.show("Licencié ajouté. Il peut maintenant créer son compte.");
            } catch(SQLException ex) { Notification.show("Erreur ajout"); }
        });
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        addLayout.add(nomJ, prenomJ, addBtn);
        content.add(new H3("Gestion des Licenciés"), new Span("Ajoutez les noms ici. Les joueurs compléteront leur profil eux-mêmes."), grid, addLayout);
    }
}