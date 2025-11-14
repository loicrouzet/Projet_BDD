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
package fr.insa.toto.model;

/**
 *
 * @author lcrouzet01
 */
public class Utilisateur {

    private String surnom;
    private String pass;
    private int role;
    private int id;
          
    
    public Utilisateur(String s, String p, int r) {
        surnom = s;
        pass = p;
        role = r;  
    }
    
    public Utilisateur(int i, String s, String p, int r) {
        surnom = s;
        pass = p;
        role = r; 
        id = i;
    }

    public String getSurnom() {
        return surnom;
    }

    public String getPass() {
        return pass;
    }

    public void setSurnom(String surnom) {
        this.surnom = surnom;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
    
    public static void entreeConsole(){
    
    }
    
    public static void tousLesUtilisateurs(){
    
    }
    
    
    
}
