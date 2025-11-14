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
package fr.insa.beuvron.vaadin.utils.dataGrid;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableFunction;
import java.util.List;
import java.util.Optional;

/**
 * Description d'un colonne de la classe.
 * <pre>
 * <p> en vaadin, une colonne est calculée à partir d'une ligne des données.
 * Dans notre cas, une ligne est toujours une {@code List<Object>}.
 * </p>
 * <p> à partir de cette ligne complète, il nous faut définir :
 * <ul>
 *   <li> soit une fonction qui renvoie une objet : c'est l'objet qui sera
 *        affiché dans la colonne. Cela suppose que le type de l'objet est
 *        compatible avec vaadin. Par exemple String, Integer ...</li>
 *   <li> soit une fonction qui renvoie un Composant : c'est ce composant
 *        qui sera affiché dans chaque case de la colonne. Cela permet
 *        une mise en forme plus générale. On peut utiliser des LitRenderer
 *        si l'on veut plus d'efficacité, mais on ne s'en préoccupera pas ici</li>
 * </ul>
 * Donc dans le cas général, vaadin fourni toute une ligne de données pour
 * construire une colonne.
 * </p>
 * <p> Il est tout de même assez courant que la colonne affiché soit directement
 * calculée à partir d'une seule colonne des données. De plus, soit le type de la
 * colonne est compatible avec vaadin (ex. Integer), dans ce cas, on peut passer
 * l'objet lui-même, soit il faut le convertir en String.
 * C'est pourquoi nous avons cinq méthodes principales de définition de colonne :
 * <ul>
 *   <li> la plus générale : colonnes travaillant sur la ligne complète (colonnes calculée) :
 *   <ul>
 *     <li> colCalculatedObject pour une colonne affichant du texte </li>
 *     <li> colCalculatedCompo pour une colonne affichant un composant quelconque </li>
 *   </ul>
 *   </li>
 *   <li> colonnes travaillant sur une colonne de donnée :
 *   <ul>
 *     <li> la donnée est compatible avec vaadin (Integer, String ...)
 *     <ul>
 *       <li> colDataDirect affiche la donnée </li>
 *     </ul>
 *     </li>
 *     <li> la donnée n'est pas directement compatible avec vaaddin, ou nous
 *          voulons définir une affichage spécifique
 *     <ul>
 *       <li> colDataObject pour une colonne affichant du texte </li>
 *       <li> colDataCompo pour une colonne affichant un composant quelconque </li>
 *     </ul>
 *   </ul>
 *   </li>
 * </ul>
 * </p>
 * <p> pour les styles, voir https://vaadin.com/docs/latest/components/grid/styling
 * </p>
 * </pre>
 *
 * @author francois
 */
public class ColumnDescription {

    /**
     * indique si la colonne est effectivement affichée
     */
    private boolean visible;

    private boolean autoWidth;

    /**
     * indique le composant à utiliser comme entête de la colonne.
     * <pre>
     * <p>
     * Si headerCompo et headerString sont tous les deux présents, le headerCompo
     * est prioritaire.
     * </p>
     * <p> Si headerCompo et headerString sont tous les deux vides,
     * la colonne n'a pas d'entête.
     * </p>
     * </pre>
     */
    private Optional<Component> headerCompo;
    /**
     * Donne l'entête de la colonne comme String.
     * <pre>
     * <p>
     * Si headerCompo et headerString sont tous les deux présents, le headerCompo
     * est prioritaire.
     * </p>
     * <p> Si headerCompo et headerString sont tous les deux vides,
     * la colonne n'a pas d'entête.
     * </p>
     * </pre>
     */
    private Optional<String> headerString;

    /**
     * fonction qui construit l'objet à afficher pour une ligne donnée.
     * <pre>
     * <p>
     * Si toObjectFromRow et renderFromRow sont tous les deux présents, le renderFromRow
     * est prioritaire.
     * </p>
     * <p> On moins un headerCompo ou un headerString doit être présent.
     * </p>
     * </pre>
     */
    private Optional<SerializableFunction<List<Object>, Object>> toObjectFromRow;

    /**
     * fonction qui renvoie le composant qui doit s'afficher dans la colonne
     * pour une ligne donnée.
     * <pre>
     * <p>
     * Si toObjectFromRow et renderFromRow sont tous les deux présents, le headerCompo
     * est prioritaire.
     * </p>
     * <p> On moins un headerCompo ou un headerString doit être présent.
     * </p>
     * </pre>
     */
    private Optional<SerializableFunction<List<Object>, Component>> renderFromRow;

    /**
     * crée une colonne par défaut qui affiche simplement "undefined".
     * La description devra être complété par les méthodes de définition
     * qui peuvent être enchainée.
     */
    public ColumnDescription() {
        this.visible = true;
        this.headerString = Optional.empty();
        this.headerCompo = Optional.empty();
        this.toObjectFromRow = Optional.of((row) -> "undefined");
        this.renderFromRow = Optional.empty();
        this.autoWidth = true;
    }

    /**
     * défini si la colonne est visible (par défaut) ou non.
     * @param visible
     * @return 
     */
    public ColumnDescription visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * défini si la largeur de la colonne s'adapte automatiquement au contenu
     * (par défaut) ou non.
     * @param auto
     * @return 
     */
    public ColumnDescription autoWidth(boolean auto) {
        this.autoWidth = auto;
        return this;
    }

    /**
     * défini l'entete de la colonne comme du texte simple.
     * @param header
     * @return 
     */
    public ColumnDescription headerString(String header) {
        this.headerString = Optional.of(header);
        return this;
    }

    /**
     * défini l'entete de la colonne comme un composant quelconque.
     * @param compo
     * @return 
     */
    public ColumnDescription headerCompo(Component compo) {
        this.headerCompo = Optional.of(compo);
        return this;
    }

    /**
     * défini une fonction qui calcule la valeur affichée dans la colonne en
     * fonction de l'ensemble des données de la ligne (une {@code List<Object>}).
     * <pre>
     *   <p> la valeur retournée est affichée directement par vaadin. Pour les objets
     *       de type quelconque, vaadin appelle automatiquement toString.
     *   </p>
     * </pre>
     * @param rowToObj
     * @return 
     */
    public ColumnDescription colCalculatedObject(SerializableFunction<List<Object>, Object> rowToObj) {
        this.toObjectFromRow = Optional.of(rowToObj);
        return this;
    }

    /**
     * défini une fonction qui calcule un composant quelconque à afficher dans la colonne en
     * fonction de l'ensemble des données de la ligne (une {@code List<Object>}).
     * <pre>
     *   <p> l'objet retourné est affiché directement par vaadin. Pour les objets
     *       de type quelconque, vaadin appelle automatiquement toString.
     *   </p>
     * </pre>
     * @param rowToCompo
     * @return 
     */
    public ColumnDescription colCalculatedCompo(SerializableFunction<List<Object>, Component> rowToCompo) {
        this.renderFromRow = Optional.of(rowToCompo);
        return this;
    }

    /**
     * La colonne affiche simplement le contenu d'une colonne des données.
     * <pre>
     *   <p> la valeur retournée est affichée directement par vaadin. Pour les objets
     *       de type quelconque, vaadin appelle automatiquement toString.
     *   </p>
     * </pre>
     * @param dataColNum le numéro de la colonne dans les données.
     * @return 
     */
    public ColumnDescription colData(int dataColNum) {
        SerializableFunction<List<Object>, Object> combo
                = (List<Object> x1) -> x1.get(dataColNum);
        this.toObjectFromRow = Optional.of(combo);
        return this;
    }

    /**
     * défini une fonction qui calcule la valeur affichée dans la colonne en
     * fonction uniquement de la valeur d'une des colonnes de données (un {@code Object}).
     * <pre>
     *   <p> la valeur retournée est affichée directement par vaadin. Pour les objets
     *       de type quelconque, vaadin appelle automatiquement toString.
     *   </p>
     * </pre>
     * @param dataColNum le numéro de la colonne dans les données.
     * @return 
     */
    public ColumnDescription colDataObject(int dataColNum, SerializableFunction<Object, Object> dataToObject) {
        SerializableFunction<List<Object>, Object> combo
                = (List<Object> x1) -> dataToObject.compose((List<Object> x2) -> x2.get(dataColNum)).apply(x1);
        this.toObjectFromRow = Optional.of(combo);
        return this;
    }

    /**
     * défini une fonction qui calcule un composant quelconque à afficher dans la colonne en
     * fonction uniquement de la valeur d'une des colonnes de données (un {@code Object}).
     * <pre>
     *   <p> l'objet retourné est affiché directement par vaadin. Pour les objets
     *       de type quelconque, vaadin appelle automatiquement toString.
     *   </p>
     * </pre>
     * @param dataColNum le numéro de la colonne dans les données.
     * @return 
     */
    public ColumnDescription colDataCompo(int dataColNum, SerializableFunction<Object, Component> dataCompo) {
        SerializableFunction<List<Object>, Component> combo
                = (List<Object> x1) -> dataCompo.compose((List<Object> x2) -> x2.get(dataColNum)).apply(x1);
        this.renderFromRow = Optional.of(combo);
        return this;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @return the headerCompo
     */
    public Optional<Component> getHeaderCompo() {
        return headerCompo;
    }

    /**
     * @return the headerString
     */
    public Optional<String> getHeaderString() {
        return headerString;
    }

    /**
     * @return the toObjectFromRow
     */
    public Optional<SerializableFunction<List<Object>, Object>> getToObjectFromRow() {
        return toObjectFromRow;
    }

    /**
     * @return the renderFromRow
     */
    public Optional<SerializableFunction<List<Object>, Component>> getRenderFromRow() {
        return renderFromRow;
    }

    /**
     * @return the autoWidth
     */
    public boolean isAutoWidth() {
        return autoWidth;
    }

}
