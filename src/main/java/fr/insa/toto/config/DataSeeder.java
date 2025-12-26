package fr.insa.toto.config;

import fr.insa.toto.model.Club;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ClubRepository clubRepository;
    private final PlayerRepository playerRepository;

    // Injection des repositories via le constructeur
    public DataSeeder(ClubRepository clubRepository, PlayerRepository playerRepository) {
        this.clubRepository = clubRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // On vérifie si la base est vide pour ne pas créer des doublons à chaque redémarrage
        if (clubRepository.count() == 0) {
            loadData();
        }
    }

    private void loadData() {
        // 1. Création des 4 Clubs
        Club c1 = new Club("Paris Saint-Germain");
        Club c2 = new Club("Olympique de Marseille");
        Club c3 = new Club("Olympique Lyonnais");
        Club c4 = new Club("RC Lens");

        // On sauvegarde les clubs d'abord pour qu'ils aient un ID
        List<Club> clubs = Arrays.asList(c1, c2, c3, c4);
        clubRepository.saveAll(clubs);

        System.out.println("--- 4 Clubs créés ---");

        // 2. Création de 50 Joueurs
        String[] positions = {"Gardien", "Défenseur", "Milieu", "Attaquant"};

        for (int i = 0; i < 50; i++) {
            Player p = new Player();
            
            // Génération d'un nom simple : "Joueur 1", "Joueur 2", etc.
            p.setFirstName("Joueur");
            p.setLastName(String.valueOf(i + 1));
            
            // Assignation d'une position au hasard (juste pour l'exemple)
            p.setPosition(positions[i % positions.length]);

            // --- LOGIQUE DE RÉPARTITION ---
            // On récupère un club dans la liste en utilisant le reste de la division par 4
            // i=0 -> club 0, i=1 -> club 1, i=2 -> club 2, i=3 -> club 3, i=4 -> club 0...
            Club assignedClub = clubs.get(i % 4);
            
            p.setClub(assignedClub);

            playerRepository.save(p);
        }

        System.out.println("--- 50 Joueurs créés et répartis dans les clubs ---");
    }
}