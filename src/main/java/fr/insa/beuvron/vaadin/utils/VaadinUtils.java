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
package fr.insa.beuvron.vaadin.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;
import java.awt.Color;

/**
 *
 * @author francois
 */
public class VaadinUtils {

    public static void encadre(Component c,
            CSSUtils.BorderStyle style,
            int largeurEnPixel,
            Color couleur) {
        c.getStyle().set("border-style", style.getCss());
        c.getStyle().set("border-width", largeurEnPixel + "px");
        c.getStyle().set("border-color", CSSUtils.toCSSColor(couleur));
        c.getStyle().set("border-radius", "0px");
    }

    public static void colorPaddingMargin(Component c,
            Color paddingColor, int paddingSizeInPixel,
            Color marginColor, int marginSizeInPixel
    ) {
        c.getStyle().set("padding", paddingSizeInPixel + "px");
        c.getStyle().set("background-color", CSSUtils.toCSSColor(paddingColor));
        c.getStyle().set("margin", marginSizeInPixel + "px");
        c.getStyle().set("box-shadow", "0px 0px 0px " + marginSizeInPixel + "px "
                + CSSUtils.toCSSColor(marginColor));
    }

}
