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

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import fr.insa.beuvron.utils.database.ResultSetUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author francois
 */
public class DataGrid extends Grid<List<Object>> {

    private GridDescription gridDes;

    public DataGrid(List<List<Object>> datas, GridDescription gridDes) {
        this.gridDes = gridDes;
        for (var colDes : gridDes.getColumns()) {
            Column<List<Object>> col;
            if (colDes.getRenderFromRow().isPresent()) {
                col = this.addColumn(new ComponentRenderer<>(colDes.getRenderFromRow().get()));
            } else {
                col = this.addColumn((source) -> colDes.getToObjectFromRow().get().apply(source));
            }
            if (colDes.getHeaderCompo().isPresent()) {
                col.setHeader(colDes.getHeaderCompo().get());
            } else if (colDes.getHeaderString().isPresent()) {
                col.setHeader(colDes.getHeaderString().get());
            }
            col.setVisible(colDes.isVisible());
            col.setAutoWidth(colDes.isAutoWidth());
        }
        this.setItems(datas);
        if (gridDes.isColumnBorders()) {
            this.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        }
    }
    
    /**
     * Crée une DataGrid avec une liste vide de lignes.
     * <pre>
     * Les données de la DataGrid pourront être fixées par la suite avec la
     * méthode setItems de Grid.
     * </pre>
     * @param gridDes 
     */
    public DataGrid(GridDescription gridDes) {
        this(new ArrayList<>(),gridDes);
    }
    
    /**
     * un constructeur spécifiquement destinée à la sous-classe ResultSetGrid.
     * @param dataEtHeaders 
     */
    protected DataGrid(ResultSetUtils.ResultSetAsLists dataEtHeaders) {
        this(dataEtHeaders.getValues(),GridDescription.simpleGridDes(dataEtHeaders.getColumnNames()));
    }

    public static DataGrid simpleGrid(List<List<Object>> datas, List<String> headers) {
        return new DataGrid(datas, GridDescription.simpleGridDes(headers));
    }

}
