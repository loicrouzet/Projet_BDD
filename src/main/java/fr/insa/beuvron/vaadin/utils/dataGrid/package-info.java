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
/**
 * définition d'une Grid vaadin générique permettant d'afficher tout ensemble
 * de donnée représenté par une liste de liste d'Object.
 * <pre>
 * <p> vaadin propose un grand nombre de classes pour définir des sources
 * de données associées aux tables. Elle permettent par exemple de ne charger
 * que les données actuellement affichées, de filtrer ...
 * </p>
 * <p> Malheuresusement, ces classes sont plutôt adaptées à des sources de
 * données Spring ou JPA, et pas vraiment prévues pour des sources plus 
 * "basiques" comme celles retrouvées en utilisant jdbc.
 * </p>
 * <p> Dans ce package, une ligne de donnée est simplement une {@code List<Object>}
 * et les données de la table complète est donc une {@code List<List<Object>>>}.
 * L'avantage est que l'on peut mettre tout type d'objet dans la table.
 * L'inconvénient est que l'on perd la vérification statique des types, et que l'on
 * devra utiliser des cast qui ne seront testés qu'à l'exécution.
 * </p>
 * <p> Une des utilisations est l'affichage sous forme de Grid vaadin d'un
 * ResultSet jdbc quelconque. En fait on donnera plutôt un PreparedStatement
 * qui après exécution doit fournir un ResultSet : cela permet d'exécuter de
 * nouveau le PreparedStatement si l'on veut rafraichir les données.
 * </p>
 * </pre>
 */
package fr.insa.beuvron.vaadin.utils.dataGrid;
