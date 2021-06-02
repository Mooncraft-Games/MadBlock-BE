package org.madblock.punishments.forms;

import cn.nukkit.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PunishmentFormManager {

    private Map<Player, PunishmentFormData> forms = new HashMap<>();

    public void setPunishmentFormData (Player player, PunishmentFormData formData) {
        this.forms.put(player, formData);
    }

    public Optional<PunishmentFormData> getPunishmentFormData (Player player) {
        if (!this.forms.containsKey(player)) {
            return Optional.empty();
        }
        return Optional.of(this.forms.get(player));
    }

    public void deletePunishmentFormData (Player player) {
        this.forms.remove(player);
    }

    public static class PunishmentFormData {

        private int id;

        private Map<String, String> additionalData;

        private String offenseType;

        private String target;

        public int getFormId () {
            return this.id;
        }

        public String getOffenseType () {
            return this.offenseType;
        }

        public String getTarget () {
            return this.target;
        }

        public Optional<String> getParameter (String param) {
            if (this.additionalData.containsKey(param)) {
                return Optional.of(this.additionalData.get(param));
            }
            return Optional.empty();
        }

        public PunishmentFormData (String targetName, int formId, String offenseType, Map<String, String> additionalData) {
            this.id = formId;
            this.offenseType = offenseType;
            this.additionalData = additionalData;
            this.target = targetName;
        }

    }

}
