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

import java.io.Writer;

/**
 *
 * @author francois
 */
public class DirectLatex implements LatexProducer {
    
    private String latexCode;
    
    private DirectLatex(String latexCode) {
        this.latexCode = latexCode;
    }
    
    public static DirectLatex verbatim(String latexCode) {
        return new DirectLatex(latexCode);      
    }

    @Override
    public String toLatex(TopLevelIncludes collect,LatexMode m) {
        return this.latexCode;
    }

    
}
