package org.madblock.punishments.forms.additionalfunctions;

import cn.nukkit.Server;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindowCustom;
import org.madblock.newgamesapi.NewGamesAPI1;

public class GamemodeFunction {

    public static void addElements(FormWindowCustom form) {

        if (gameApiEnabled()) {
            ElementDropdown dropdown = new ElementDropdown("Please specify a game");

            for (String game : NewGamesAPI1.getGameRegistry().getGames()) {
                dropdown.addOption(game);
            }
            form.addElement(dropdown);
        } else {
            form.addElement(new ElementLabel("Confirm?"));
        }
    }

    public static String parseResponse(FormResponseCustom response) {
        return gameApiEnabled() ? " [" + response.getDropdownResponse(0).getElementContent() + "]" : "";
    }

    private static boolean gameApiEnabled () {
        return Server.getInstance().getPluginManager().getPlugin("NewGamesAPI") != null;
    }
}
