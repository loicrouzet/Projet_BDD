/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.beuvron.utils.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Représente une entité de la base de donnée dans Java.
 * <p>
 * permet de gérer les identificateurs créés automatiquement par le sgbd </p>
 * <p>
 * Une ClasseMiroir e est crée avec un id égal à -1; cela signifie que l'objet
 * java n'a pour l'instant pas été sauvegardé dans la base de donnée.
 * <p>
 * <p>
 * la sauvegarde dans la base de donnée se fait grace à la méthode saveInDb qui
 * a deux étapes :
 * <ul>
 * <li> une étape spécifique à chaque sous-classe qui définie toutes les
 * colonnes sauf l'ID : methode saveSansId</li>
 * <li> cette methode saveSansId renvoit le Statement qui lui a permit de créer
 * l'objet</li>
 * <li> la méthode saveInDb reprend ce Statement pour retrouver l'ID créé
 * automatiquement</li>
 * </ul>
 * </p>
 * <p>
 * Cette classe abstraite peut servir de base aux différentes classes
 * représentant effectivement des tables d'entités dans la BdD.</p>
 * <p>
 * ATTENTION : lz PreparedStatement utilisé dans saveSansId devra être créé avec
 * l'option PreparedStatement.RETURN_GENERATED_KEYS
 * </p>
 * <p>
 * pour la gestion de l'égalité/identité, nous sommes stricts : si les objets ne
 * sont pas dans la base de donnée (id = -1) il sont incomparables. Un test
 * d'égalité est une erreur.
 * </p>
 * <p>
 * en cohérence, le hashcode d'un objet est simplement sont id. si son id est -1
 * c'est une errue
 * </p>
 *
 * @author francois
 */
public abstract class ClasseMiroir {

    private int id;

    /**
     * Constructeur typiquement utilisé lorsque l'on a retrouvé une entité déjà
     * existante dans la base de donnée.
     *
     * @param id
     */
    public ClasseMiroir(int id) {
        this.id = id;
    }

    /**
     * Constructeur typiquement utilisé lorsque l'on crée une nouvelle entité en
     * mémoire avant de la sauvegarder dans la base de donnée.
     */
    public ClasseMiroir() {
        this(-1);
    }

    public static class EntiteDejaSauvegardee extends SQLException {

        public EntiteDejaSauvegardee() {
            super("L'entité à déjà été sauvegardée (id != -1");
        }
    }

    public static class EntiteNonSauvegardee extends Error {

        public EntiteNonSauvegardee() {
            super("Une entité non sauvegardée ; pas de test d'égalité ; impossible à supprimer dans la BdD");
        }
    }

    /**
     * chaque classe miroir spécifique doit fournir cette méthode qui sauvegarde
     * tous les attributs dans la base de donnée sauf l'identificateur. Elle
     * renvoie le Statement qui a servi à la création pour pouvoir récupérer
     * l'identificateur généré.
     *
     * @param con
     * @return
     */
    protected abstract Statement saveSansId(Connection con) throws SQLException;

    /**
     * Sauvegarde une nouvelle entité et retourne la clé affecté automatiquement
     * par le SGBD.
     * <p>
     * la clé est également sauvegardée dans l'attribut id de la DatabaseEntity
     * </p>
     * <p>
     * cette methode est déclaré "final", elle ne peut donc pas être spécialisée
     * dans les sous-classes. En effet, elle ne s'occupe que de la gestion des
     * identificateurs. La sauvegarde effective doit être spécifiée dans les
     * sous-classes à l'aide de la méthode saveSansId
     * </p>
     *
     * @param con
     * @return la clé de la nouvelle entité dans la table de la BdD
     * @throws EntiteDejaSauvegardee si l'id de l'entité est différent de -1
     * @throws SQLException si autre problème avec la BdD
     */
    public final int saveInDB(Connection con) throws SQLException {
        if (this.id != -1) {
            throw new EntiteDejaSauvegardee();
        }
        Statement saveAllButId = this.saveSansId(con);
        try (ResultSet rid = saveAllButId.getGeneratedKeys()) {
            rid.next();
            this.id = rid.getInt(1);
            return this.id;
        }
    }

    /**
     * cette méthode doit être utilisée avec précaution pour signaler par
     * exemple que l'on a supprimé l'entité de la base de donnée.
     */
    protected void entiteSupprimee() {
        this.id = -1;
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        if (this.id != -1) {
            return this.id;
        } else {
            throw new EntiteNonSauvegardee();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        } else if (this.id == -1) {
            throw new EntiteNonSauvegardee();
        }
        ClasseMiroir other = (ClasseMiroir) obj;
        if (other.id != -1) {
            throw new EntiteNonSauvegardee();
        } else {
            return this.id == other.id;
        }
    }

}
