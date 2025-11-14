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

import fr.insa.beuvron.utils.database.ResultSetUtils;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Permet facilement de visualier n'importe quel ResultSet dans une Grid vaadin.
 * <pre>
 * On fourni le PreparedStatement plutôt que directement le ResultSet : cela
 * permet si besoin de rafraichir la Grid en ré-évaluant le PreparedStatement.
 * </pre>
 * @author francois
 */
public class ResultSetGrid extends DataGrid {

    private PreparedStatement pst;
    
    /**
     * crée une Grid avec toutes les colonnes du ResultSet, et les noms de colonnes
     * comme entêtes.
     * <pre>
     * Si vous voulez de jolies Grid, utilisez plutôt le constructeur avec
     * une GridDescription, qui vous permet de préciser les colonnes et leur
     * forme.
     * </pre>
     * @param pst
     * @throws SQLException 
     */
    public ResultSetGrid(PreparedStatement pst) throws SQLException {
        super(executeStatement(pst));
        this.pst = pst;
    }

    /**
     * crée une Grid à partir d'un ResultSet, en précisant dans la GridDescription
     * les colonnes à afficher, et leur mise en forme.
     * @param pst
     * @param gridDes
     * @throws SQLException 
     */
    public ResultSetGrid(PreparedStatement pst, GridDescription gridDes) throws SQLException {
        super(gridDes);
        this.pst = pst;
        this.update();
    }

    private static ResultSetUtils.ResultSetAsLists executeStatement(PreparedStatement pst) throws SQLException {
        try (ResultSet res = pst.executeQuery()) {
            return ResultSetUtils.toLists(res);
        }
    }

    public void update() throws SQLException {
        var asLists = executeStatement(this.pst);
        this.setItems(asLists.getValues());
    }

}
