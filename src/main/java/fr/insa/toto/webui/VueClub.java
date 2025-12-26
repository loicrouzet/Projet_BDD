package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
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
        try {
            this.con = ConnectionSimpleSGBD.defaultCon();
            this.currentUser = (Utilisateur) VaadinSession.getCurrent().getAttribute("user");
            
            // Sécurité de base
            if (currentUser == null || !currentUser.isAdmin() || currentUser.getIdClub() == null) {
                this.add(new H3("Accès refusé ou aucun club lié."));
                return;
            }
            
            // Chargement du club
            Optional<Club> c = Club.getById(con, currentUser.getIdClub());
            if(c.isPresent()) {
                this.myClub = c.get();
                buildUI();
            } else {
                this.add(new H3("Club introuvable en base."));
            }
            
        } catch (SQLException ex) { this.add(new Span("Erreur : " + ex.getMessage())); }
    }

    private void buildUI() {
        this.removeAll();
        
        // --- En-tête ---
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull(); header.setAlignItems(Alignment.CENTER);
        Button back = new Button("Retour", new Icon(VaadinIcon.ARROW_LEFT));
        back.addClickListener(e -> back.getUI().ifPresent(ui -> ui.navigate(VuePrincipale.class)));
        header.add(back, new H1("Gestion : " + myClub.getNom()));
        this.add(header);

        // --- Onglets ---
        tabs = new Tabs();
        Tab tabInfos = new Tab("Informations & Logo");
        Tab tabTerrains = new Tab("Infrastructures");
        Tab tabMembres = new Tab("Licenciés & Adhérents");
        tabs.add(tabInfos, tabTerrains, tabMembres);
        
        content = new VerticalLayout();
        content.setSizeFull();

        tabs.addSelectedChangeListener(e -> {
            content.removeAll();
            if(e.getSelectedTab().equals(tabInfos)) showInfos();
            else if(e.getSelectedTab().equals(tabTerrains)) showInfrastructures();
            else if(e.getSelectedTab().equals(tabMembres)) showMembres();
        });

        this.add(tabs, content);
        tabs.setSelectedTab(tabInfos);
        showInfos(); // Vue par défaut
    }

    // --- ONGLET 1 : INFOS ---
    private void showInfos() {
        TextField nom = new TextField("Nom du club"); nom.setValue(myClub.getNom()); nom.setWidthFull();
        TextField tel = new TextField("Téléphone"); tel.setValue(myClub.getTelephone() != null ? myClub.getTelephone() : "");
        com.vaadin.flow.component.textfield.TextArea desc = new com.vaadin.flow.component.textfield.TextArea("Description");
        desc.setValue(myClub.getDescription() != null ? myClub.getDescription() : ""); desc.setWidthFull();
        
        // Gestion Logo
        TextField logoUrl = new TextField("Logo (URL ou Upload)");
        logoUrl.setValue(myClub.getLogoUrl() != null ? myClub.getLogoUrl() : "");
        logoUrl.setWidthFull();
        
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png");
        upload.setDropLabel(new Span("Glisser logo ici"));
        upload.addSucceededListener(event -> {
            try (InputStream in = buffer.getInputStream()) {
                String base64 = "data:" + event.getMIMEType() + ";base64," + Base64.getEncoder().encodeToString(IOUtils.toByteArray(in));
                logoUrl.setValue(base64);
                Notification.show("Logo reçu !");
            } catch (Exception ex) {}
        });

        Button save = new Button("Enregistrer les modifications", e -> {
            try {
                myClub.setNom(nom.getValue());
                myClub.setTelephone(tel.getValue());
                myClub.setDescription(desc.getValue());
                myClub.setLogoUrl(logoUrl.getValue());
                myClub.updateInfos(con);
                Notification.show("Sauvegardé !");
            } catch(SQLException ex) { Notification.show("Erreur BDD"); }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        content.add(new H3("Modifier les informations"), nom, new HorizontalLayout(logoUrl, upload), desc, tel, save);
    }

    // --- ONGLET 2 : INFRASTRUCTURES ---
    private void showInfrastructures() {
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
        
        // Formulaire ajout
        HorizontalLayout addLayout = new HorizontalLayout();
        addLayout.setAlignItems(Alignment.BASELINE);
        TextField nomT = new TextField("Nouveau terrain");
        Checkbox inT = new Checkbox("Intérieur ?");
        Button addBtn = new Button("Ajouter", e -> {
            try { 
                new Terrain(nomT.getValue(), inT.getValue(), myClub.getId()).saveInDB(con);
                showInfrastructures(); // Refresh
            } catch(SQLException ex) { Notification.show("Erreur ajout"); }
        });
        addLayout.add(nomT, inT, addBtn);
        
        content.add(new H3("Mes Terrains"), grid, addLayout);
    }

    // --- ONGLET 3 : MEMBRES (LICENCIÉS) ---
    private void showMembres() {
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
        
        // Formulaire ajout
        HorizontalLayout addLayout = new HorizontalLayout();
        addLayout.setAlignItems(Alignment.BASELINE);
        TextField nomJ = new TextField("Nom");
        TextField prenomJ = new TextField("Prénom");
        Button addBtn = new Button("Ajouter Licencié", e -> {
            if(nomJ.isEmpty() || prenomJ.isEmpty()) return;
            try {
                // Création du joueur rattaché au club (idEquipe = 0 pour l'instant)
                new Joueur(nomJ.getValue(), prenomJ.getValue(), 0, myClub.getId()).saveInDB(con);
                showMembres();
                nomJ.clear(); prenomJ.clear();
                Notification.show("Joueur ajouté !");
            } catch(SQLException ex) { Notification.show("Erreur ajout : " + ex.getMessage()); }
        });
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        addLayout.add(nomJ, prenomJ, addBtn);
        
        content.add(new H3("Gestion des Licenciés"), grid, addLayout);
    }
}
