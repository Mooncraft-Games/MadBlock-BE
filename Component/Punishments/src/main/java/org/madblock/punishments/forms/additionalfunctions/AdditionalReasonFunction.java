package org.madblock.punishments.forms.additionalfunctions;

import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindowCustom;

public class AdditionalReasonFunction {

    public static void addElements(FormWindowCustom form) {
        form.addElement(new ElementInput("Please specify any additional information you'd like to add."));
    }

    public static String parseResponse(FormResponseCustom response) {
        return " - " + response.getInputResponse(0);
    }
}
