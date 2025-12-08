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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Club;
import fr.insa.toto.model.Loisir;
import fr.insa.toto.model.Terrain;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.model.Utilisateur;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Route(value = "")
@PageTitle("Gestion Tournois")
public class VuePrincipale extends VerticalLayout {

    private Connection con;
    private Utilisateur currentUser = null; 
    private Grid<Tournoi> grid; 
    
    // On garde une référence à la ComboBox des clubs pour pouvoir la rafraîchir
    private ComboBox<Club> clubSelectMain; 

    public VuePrincipale() {
        try {
            this.con = ConnectionSimpleSGBD.defaultCon();
        } catch (SQLException ex) {
            this.add(new H3("Erreur BDD : " + ex.getMessage()));
            return;
        }
        showLoginScreen();
    }

    // ... (Méthodes showLoginScreen et showRegisterScreen inchangées) ...
    // Pour la clarté, je ne les répète pas ici, gardez celles de l'étape précédente.
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
        RadioButtonGroup<String> roleSelect = new RadioButtonGroup<>();
        roleSelect.setItems("Visiteur", "Administrateur");
        roleSelect.setValue("Visiteur");
        Button createButton = new Button("S'inscrire", e -> {
            try {
                if (Utilisateur.existeSurnom(this.con, userField.getValue())) { Notification.show("Pseudo pris !"); return; }
                int roleId = roleSelect.getValue().equals("Administrateur") ? 1 : 0;
                new Utilisateur(userField.getValue(), passField.getValue(), roleId).saveInDB(this.con);
                Notification.show("Compte créé !"); showLoginScreen();
            } catch (SQLException ex) { Notification.show("Erreur: " + ex.getMessage()); }
        });
        Button cancelButton = new Button("Annuler", e -> showLoginScreen());
        VerticalLayout l = new VerticalLayout(title, userField, passField, roleSelect, createButton, cancelButton);
        l.setAlignItems(Alignment.CENTER); l.setJustifyContentMode(JustifyContentMode.CENTER); l.setSizeFull();
        this.add(l);
    }

    // --- APPLICATION PRINCIPALE ---

    private void showMainApplication() {
        this.removeAll();
        
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        H3 welcome = new H3("Espace " + (currentUser.isAdmin() ? "Admin" : "Visiteur"));
        Button logoutBtn = new Button("Déconnexion", e -> { this.currentUser = null; showLoginScreen(); });
        header.add(welcome, logoutBtn);
        this.add(header);

        this.add(new H1("Liste des Tournois"));

        if (currentUser.isAdmin()) {
            this.add(createFormulaireAjout());
        }

        setupGrid();
        updateGrid();
    }

    private HorizontalLayout createFormulaireAjout() {
        HorizontalLayout form = new HorizontalLayout();
        form.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        
        TextField nomField = new TextField("Nom tournoi");
        DatePicker dateField = new DatePicker("Date");
        ComboBox<Loisir> sportSelect = new ComboBox<>("Sport");
        this.clubSelectMain = new ComboBox<>("Club"); // Stocké dans l'attribut de classe
        
        // Bouton "+" pour créer un nouveau club
        Button newClubBtn = new Button(new Icon(VaadinIcon.PLUS));
        newClubBtn.addClickListener(e -> openCreateClubDialog());
        newClubBtn.setTooltipText("Créer un nouveau club");

        refreshCombos(sportSelect); // Chargement initial des données

        Button addButton = new Button("Ajouter Tournoi", e -> {
            try {
                if (nomField.isEmpty() || dateField.isEmpty() || sportSelect.isEmpty() || clubSelectMain.isEmpty()) return;
                new Tournoi(nomField.getValue(), dateField.getValue(), sportSelect.getValue(), clubSelectMain.getValue()).saveInDB(this.con);
                Notification.show("Tournoi ajouté !");
                nomField.clear(); dateField.clear(); sportSelect.clear(); clubSelectMain.clear();
                updateGrid();
            } catch (SQLException ex) { Notification.show("Erreur : " + ex.getMessage()); }
        });

        // On met le bouton "+" juste à côté du sélecteur de club
        HorizontalLayout clubLayout = new HorizontalLayout(clubSelectMain, newClubBtn);
        clubLayout.setAlignItems(Alignment.BASELINE);

        form.add(nomField, dateField, sportSelect, clubLayout, addButton);
        return form;
    }

    private void refreshCombos(ComboBox<Loisir> sportSelect) {
        try {
            sportSelect.setItems(Loisir.getAll(this.con));
            sportSelect.setItemLabelGenerator(Loisir::getNom);
            
            // Mise à jour de la liste des clubs
            clubSelectMain.setItems(Club.getAll(this.con));
            clubSelectMain.setItemLabelGenerator(Club::getNom);
        } catch (SQLException ex) { Notification.show("Erreur chargement listes"); }
    }

    // --- DIALOGUE DE CRÉATION DE CLUB ---
    private void openCreateClubDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nouveau Club & Terrains");

        VerticalLayout dialogLayout = new VerticalLayout();
        
        // 1. Info Club
        TextField nomClubField = new TextField("Nom du Club");
        nomClubField.setPlaceholder("Ex: Tennis Club de Lyon");
        nomClubField.setWidthFull();

        // 2. Gestion des Terrains
        dialogLayout.add(nomClubField, new H4("Terrains disponibles"));
        
        VerticalLayout terrainsContainer = new VerticalLayout();
        terrainsContainer.setSpacing(false);
        terrainsContainer.setPadding(false);
        
        // Liste temporaire pour stocker les composants graphiques des terrains en cours d'ajout
        List<TerrainRow> terrainRows = new ArrayList<>();

        Button addTerrainBtn = new Button("Ajouter un terrain", new Icon(VaadinIcon.PLUS_CIRCLE));
        addTerrainBtn.addClickListener(e -> {
            TerrainRow row = new TerrainRow();
            terrainRows.add(row);
            terrainsContainer.add(row.layout);
        });
        // On ajoute un premier terrain par défaut
        addTerrainBtn.click();

        // 3. Actions
        Button saveBtn = new Button("Sauvegarder tout", e -> {
            try {
                if (nomClubField.isEmpty()) { Notification.show("Le nom du club est requis"); return; }

                // A. Sauvegarde du Club
                Club newClub = new Club(nomClubField.getValue());
                newClub.saveInDB(this.con); // Le club récupère son ID ici

                // B. Sauvegarde des Terrains liés au Club
                int countTerrains = 0;
                for (TerrainRow row : terrainRows) {
                    if (!row.nomField.isEmpty()) {
                        Terrain t = new Terrain(row.nomField.getValue(), row.isIndoor.getValue(), newClub.getId());
                        t.saveInDB(this.con);
                        countTerrains++;
                    }
                }

                Notification.show("Club créé avec " + countTerrains + " terrains !");
                refreshCombos(new ComboBox<>()); // Astuce pour rafraîchir la liste principale
                dialog.close();

            } catch (SQLException ex) {
                Notification.show("Erreur sauvegarde : " + ex.getMessage());
            }
        });

        dialogLayout.add(terrainsContainer, addTerrainBtn, saveBtn);
        dialog.add(dialogLayout);
        dialog.open();
    }

    // Petite classe interne pour gérer une ligne de saisie de terrain
    private static class TerrainRow {
        TextField nomField = new TextField();
        Checkbox isIndoor = new Checkbox("Intérieur");
        HorizontalLayout layout;

        public TerrainRow() {
            nomField.setPlaceholder("Nom du terrain/salle");
            layout = new HorizontalLayout(nomField, isIndoor);
            layout.setAlignItems(Alignment.BASELINE);
        }
    }

    // ... (Méthodes setupGrid, updateGrid, openEditDialog inchangées) ...
    private void setupGrid() {
        this.grid = new Grid<>(Tournoi.class, false);
        this.grid.addColumn(Tournoi::getNom).setHeader("Nom");
        this.grid.addColumn(Tournoi::getDateDebut).setHeader("Date");
        this.grid.addColumn(t -> t.getLeLoisir().getNom()).setHeader("Sport");
        this.grid.addColumn(t -> t.getLeClub().getNom()).setHeader("Club Organisateur");
        this.grid.setWidthFull();
        this.add(this.grid);
    }
    
    private void updateGrid() {
        try { this.grid.setItems(Tournoi.getAll(this.con)); } catch (SQLException ex) {}
    }
}