package fr.insa.toto.webui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.beuvron.vaadin.utils.dataGrid.ResultSetGrid;
import fr.insa.toto.model.Loisir;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.model.Utilisateur;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Route(value = "")
@PageTitle("Gestion Tournois")
public class VuePrincipale extends VerticalLayout {

    private Connection con;
    private Utilisateur currentUser = null; // L'utilisateur connecté
    
    // Composants d'interface
    private VerticalLayout mainContent;
    private ResultSetGrid grid;

    public VuePrincipale() {
        try {
            this.con = ConnectionSimpleSGBD.defaultCon();
        } catch (SQLException ex) {
            this.add(new H3("Erreur BDD : " + ex.getMessage()));
            return;
        }

        // Au démarrage, on affiche le login
        showLoginScreen();
    }

    /**
     * Affiche l'écran de connexion
     */
    private void showLoginScreen() {
        this.removeAll();
        
        H1 title = new H1("Connexion");
        TextField userField = new TextField("Utilisateur");
        PasswordField passField = new PasswordField("Mot de passe");
        
        Button loginButton = new Button("Se connecter");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClickShortcut(Key.ENTER);
        
        loginButton.addClickListener(e -> {
            try {
                Optional<Utilisateur> user = Utilisateur.login(this.con, userField.getValue(), passField.getValue());
                if (user.isPresent()) {
                    this.currentUser = user.get();
                    Notification.show("Bienvenue " + this.currentUser.getSurnom());
                    showMainApplication();
                } else {
                    Notification.show("Identifiants incorrects (Essayez toto/toto ou invite/invite)", 3000, Notification.Position.MIDDLE);
                }
            } catch (SQLException ex) {
                Notification.show("Erreur technique : " + ex.getMessage());
            }
        });

        VerticalLayout loginLayout = new VerticalLayout(title, userField, passField, loginButton);
        loginLayout.setAlignItems(Alignment.CENTER);
        loginLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        loginLayout.setSizeFull();
        
        this.add(loginLayout);
    }

    /**
     * Affiche l'application principale une fois connecté
     */
    private void showMainApplication() {
        this.removeAll();
        
        // Entête avec bouton déconnexion
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        
        H3 welcome = new H3("Espace " + (currentUser.isAdmin() ? "Administrateur" : "Visiteur"));
        Button logoutBtn = new Button("Déconnexion", e -> {
            this.currentUser = null;
            showLoginScreen();
        });
        header.add(welcome, logoutBtn);
        this.add(header);

        // Contenu principal
        this.add(new H1("Liste des Tournois"));

        // SI ADMIN : On affiche le formulaire d'ajout
        if (currentUser.isAdmin()) {
            this.add(createFormulaireAjout());
        }

        // Pour tout le monde : La liste
        setupGrid();
    }

    private HorizontalLayout createFormulaireAjout() {
        HorizontalLayout form = new HorizontalLayout();
        form.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        
        TextField nomField = new TextField("Nom du tournoi");
        DatePicker dateField = new DatePicker("Date de début");
        ComboBox<Loisir> sportSelect = new ComboBox<>("Sport");
        
        try {
            List<Loisir> sports = Loisir.getAll(this.con);
            sportSelect.setItems(sports);
            sportSelect.setItemLabelGenerator(Loisir::getNom);
        } catch (SQLException ex) {
            Notification.show("Erreur chargement sports");
        }

        Button addButton = new Button("Ajouter", e -> {
            try {
                if (nomField.isEmpty() || dateField.isEmpty() || sportSelect.isEmpty()) return;
                
                new Tournoi(nomField.getValue(), dateField.getValue(), sportSelect.getValue()).saveInDB(this.con);
                
                Notification.show("Tournoi ajouté !");
                nomField.clear(); dateField.clear(); sportSelect.clear();
                this.grid.update(); // Rafraîchir la grille
            } catch (SQLException ex) {
                Notification.show("Erreur : " + ex.getMessage());
            }
        });

        form.add(nomField, dateField, sportSelect, addButton);
        return form;
    }

    private void setupGrid() {
        try {
            String query = "select t.id, t.nom as 'Nom', t.date_debut as 'Date', l.nom as 'Sport' " +
                           "from tournoi t join loisir l on t.id_loisir = l.id order by t.id desc";
            this.grid = new ResultSetGrid(this.con.prepareStatement(query));
            this.grid.setWidthFull();
            this.add(this.grid);
        } catch (SQLException ex) {
            this.add(new H3("Erreur grille : " + ex.getMessage()));
        }
    }
}