/*
    Copyright 2000-2014 Francois de Bertrand de Beuvron

    This file is part of UtilsBeuvron.

    UtilsBeuvron is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UtilsBeuvron is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UtilsBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.beuvron.utils.latex;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 *
 * @author francois
 */
public interface LatexProducer {
    
    /**
     * return the latex representation of this.
     * As a side effect, add all required includes in the collect parameter.
     * These includes will be taken care of in a {@link TopLevelLatex} or may
     * be simply ignored in a embeded context
     * @param out
     * @param collect 
     */
    public String toLatex(TopLevelIncludes collect,LatexMode mode);
    
    /**
     * par defaut, on ignore completement les includes
     * @return le code latex sans les includes.
     */
    public default String toLatex() {
        TopLevelIncludes topIncludes = new TopLevelIncludes();
        return this.toLatex(topIncludes,LatexMode.TextMode);
    }
    
    
   
}
