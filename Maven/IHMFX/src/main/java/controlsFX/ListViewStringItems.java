package controlsFX;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import javafx.scene.control.ListView;

/**
 * @author didier
 */
public class ListViewStringItems extends ListView<String> {

    /**
     * Renvoie les données dans la liste sous forme d'items séparés par des tags
     * Il doit y avoir un tag en moins que le nombre d'items
     *
     * @return La liste sous forme d'un seul String
     */
    public String getData() {
        String retour = "";
        int i = 0;
        for (String elt : getItems()) {
            retour += elt;
            i++;
            if (i < getItems().size()) {
                retour += "<@>";
            }
        }
        return retour;
    }

    public void setData(String donnees) {
        String[] dataStrings = donnees.split("<@>");
        getItems().setAll(dataStrings);
    }
}
