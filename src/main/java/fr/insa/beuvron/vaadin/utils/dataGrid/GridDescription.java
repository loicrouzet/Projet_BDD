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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author francois
 */
public class GridDescription {

    private List<ColumnDescription> columns;
    private boolean columnBorders;

    public GridDescription(List<ColumnDescription> columns) {
        this.columns = columns;
        this.columnBorders = false;
    }

    public List<ColumnDescription> getColumns() {
        return columns;
    }

    /**
     * cas particulier ou l'on utiliser toString pour toutes les colonnes.
     * <pre>
     * <p> il doit y avoir autant de headers que de colonnes dans les données.
     * </p>
     * </pre>
     * @param headers
     * @return 
     */
    public static GridDescription simpleGridDes(List<String> headers) {
        List<ColumnDescription> res = new ArrayList<>(headers.size());
        for (int i = 0; i < headers.size(); i++) {
            res.add(new ColumnDescription()
                    .headerString(headers.get(i))
                    .colData(i));
        }
        return new GridDescription(res);
    }
    
    public GridDescription columnBorders(boolean borders) {
        this.columnBorders = borders;
        return this;
    }

    /**
     * @return the columnBorders
     */
    public boolean isColumnBorders() {
        return columnBorders;
    }

}
