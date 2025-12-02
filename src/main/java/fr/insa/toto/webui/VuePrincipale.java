package fr.insa.toto.webui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.beuvron.vaadin.utils.dataGrid.ResultSetGrid;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Route(value = "")
@PageTitle("Gestion Tournois")
public class VuePrincipale extends VerticalLayout {

    public VuePrincipale() {
        this.add(new H1("Bienvenue sur l'application Multisport"));
        
        this.add(new H3("Liste des Tournois"));

        try {
            // 1. Récupération de la connexion (La même que celle utilisée dans GestionBDD)
            Connection con = ConnectionSimpleSGBD.defaultCon();
            
            // 2. Préparation de la requête pour afficher les tournois avec le nom du sport
            String query = "select t.id, t.nom as 'Nom Tournoi', t.date_debut as 'Date', l.nom as 'Sport' " +
                           "from tournoi t join loisir l on t.id_loisir = l.id";
            PreparedStatement pst = con.prepareStatement(query);

            // 3. Utilisation de ResultSetGrid pour afficher le résultat automatiquement
            ResultSetGrid grid = new ResultSetGrid(pst);
            grid.setWidthFull();
            
            this.add(grid);

        } catch (SQLException ex) {
            this.add(new H3("Erreur de connexion à la base de données : " + ex.getMessage()));
            ex.printStackTrace();
        }
    }
}
