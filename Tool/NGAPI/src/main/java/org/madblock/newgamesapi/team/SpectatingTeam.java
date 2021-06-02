package org.madblock.newgamesapi.team;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import org.madblock.newgamesapi.game.GameHandler;

public class SpectatingTeam extends Team {

    protected SpectatingTeam(GameHandler handler, String id, String displayname, Colour colour, boolean isTeamNameDisplayed, boolean canPlaceAndBreakBlocks, boolean canDropItems, boolean canInteractWithBlocks, boolean canPickUpItems, boolean isVisible, boolean isFlightEnabled, boolean isNoClipEnabled) {
        super(handler, id, displayname, colour, false, false, false, false, isTeamNameDisplayed, canPlaceAndBreakBlocks, canDropItems, canInteractWithBlocks, canPickUpItems, isVisible, false, isFlightEnabled, isNoClipEnabled);
    }

    @Override
    protected void updateHungerState(Player player) {
        player.getFoodData().setLevel(20, 20);
        player.setFoodEnabled(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event){
        if(event.getDamager() instanceof Player){
            if(players.contains((Player) event.getDamager())){
                event.setCancelled();
                return;
            }
        }
        if(event.getEntity() instanceof Player){
            if(players.contains((Player) event.getEntity())){
                event.setCancelled();
                return;
            }
        }
    }

    @EventHandler
    public void onMiscDamage(EntityDamageEvent event){
        if(event.getEntity() instanceof Player){
            if(players.contains((Player) event.getEntity())){
                event.setCancelled();
            }
        }
    }

    public static SpectatorBuilder newSpectatorTeamBuilder(String id, String teamname, Colour colour){
        return new SpectatorBuilder(id, teamname, colour);
    }

    public static class SpectatorBuilder extends GenericTeamBuilder {

        protected boolean isTeamNameDisplayed;
        protected boolean canPlaceAndBreakBlocks;
        protected boolean canPlayersDropItems;
        protected boolean canPlayersInteractWithBlocks;
        protected boolean canPlayersPickUpItems;
        protected boolean arePlayersVisible;
        protected boolean isFlightEnabled;
        protected boolean isNoClipEnabled;

        public SpectatorBuilder(String id, String displayName, Colour colour) {
            super(id, displayName, colour);
            setActiveGameTeam(false);
            setDoesParticipateInTeamBalancing(false);

            this.isTeamNameDisplayed = false;
            this.canPlaceAndBreakBlocks = false;
            this.canPlayersDropItems = false;
            this.canPlayersInteractWithBlocks = false;
            this.canPlayersPickUpItems = false;
            this.arePlayersVisible = false;
            this.isFlightEnabled = true;
            this.isNoClipEnabled = true;
        }

        @Override
        public SpectatingTeam build(GameHandler handler){
            verify(handler);
            SpectatingTeam team = new SpectatingTeam(
                    handler,
                    id,
                    displayName,
                    colour,
                    isTeamNameDisplayed,
                    canPlaceAndBreakBlocks,
                    canPlayersDropItems,
                    canPlayersInteractWithBlocks,
                    canPlayersPickUpItems,
                    arePlayersVisible,
                    isFlightEnabled,
                    isNoClipEnabled
            );
            team.activate();
            return team;
        }

        @Override
        protected void verify(GameHandler handler){
            super.verify(handler);
            if(isNoClipEnabled) isFlightEnabled = true;
        }

        public boolean isTeamNameDisplayed() { return isTeamNameDisplayed; }
        public boolean canPlayersPlaceAndBreakBlocks() { return canPlaceAndBreakBlocks; }
        public boolean canPlayersDropItems() { return canPlayersDropItems; }
        public boolean canPlayersInteractWithBlocks() { return canPlayersInteractWithBlocks; }
        public boolean canPlayersPickUpItems() { return canPlayersPickUpItems; }
        public boolean isArePlayersVisible() { return arePlayersVisible; }
        public boolean isFlightEnabled() { return isFlightEnabled; }
        public boolean isNoClipEnabled() { return isNoClipEnabled; }

        public SpectatorBuilder setTeamNameDisplayed(boolean teamNameDisplayed) { isTeamNameDisplayed = teamNameDisplayed; return this; }
        public SpectatorBuilder setCanPlayersPlaceAndBreakBlocks(boolean canPlaceAndBreakBlocks) { this.canPlaceAndBreakBlocks = canPlaceAndBreakBlocks; return this; }
        public SpectatorBuilder setCanPlayersDropItems(boolean canPlayersDropItems) { this.canPlayersDropItems = canPlayersDropItems; return this; }
        public SpectatorBuilder setCanPlayersInteractWithBlocks(boolean canPlayersInteractWithBlocks) { this.canPlayersInteractWithBlocks = canPlayersInteractWithBlocks; return this; }
        public SpectatorBuilder setCanPlayersPickUpItems(boolean canPlayersPickUpItems) { this.canPlayersPickUpItems = canPlayersPickUpItems; return this; }
        public SpectatorBuilder setArePlayersVisible(boolean arePlayersVisible) { this.arePlayersVisible = arePlayersVisible; return this; }
    }

}
