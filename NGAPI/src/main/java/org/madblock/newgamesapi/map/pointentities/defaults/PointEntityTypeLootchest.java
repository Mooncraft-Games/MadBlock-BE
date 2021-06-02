package org.madblock.newgamesapi.map.pointentities.defaults;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockChest;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.inventory.ChestInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import dev.cg360.mc.nukkittables.LootTableRegistry;
import dev.cg360.mc.nukkittables.context.TableRollContext;
import dev.cg360.mc.nukkittables.types.LootTable;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.PointEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

public class PointEntityTypeLootchest extends PointEntityType implements Listener {


    public PointEntityTypeLootchest(GameHandler gameHandler) {
        super("loot_chest", gameHandler);
    }

    @Override
    public void onRegister() {
        NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
        addFunction("generate_loot", this::generateLoot);
    }

    @Override
    public void onUnregister() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void onAddPointEntity(PointEntity entity) {
        executeFunction("generate_loot", entity, getParentManager().getLevelLookup().get(entity), new HashMap<>());
    }

    private void generateLoot(PointEntityCallData pointEntityCallData) {
        Location location = pointEntityCallData.getPointEntity().toLocation(pointEntityCallData.getLevel());
        Block oldblock = location.getLevelBlock();
        if(oldblock instanceof BlockChest){
            BlockEntity blockEntity = location.getLevel().getBlockEntity(location);
            if(blockEntity instanceof BlockEntityChest){
                blockEntity.close();
            }
            location.getLevel().setBlock(location, new BlockAir());
        }

        location.getLevel().setBlock(location, new BlockChest());
        CompoundTag nbt = new CompoundTag("")
                .putList(new ListTag<>("Items"))
                .putString("id", BlockEntity.CHEST)
                .putInt("x", (int) Math.floor(location.getX()))
                .putInt("y", (int) Math.floor(location.getY()))
                .putInt("z", (int) Math.floor(location.getZ()));
        BlockEntityChest container = (BlockEntityChest) BlockEntity.createBlockEntity(BlockEntity.CHEST, location.getLevel().getChunk(((int) Math.floor(location.getX())) >> 4, ((int) Math.floor(location.getZ())) >> 4), nbt);
        location.getLevel().addBlockEntity(container);

        if(container != null){

            String lootTableID = pointEntityCallData.getPointEntity().getStringProperties().getOrDefault("loot_table", "wtf/snowballs");
            Optional<LootTable> t = LootTableRegistry.get().getLootTable(lootTableID);

            if(t.isPresent()){
                LootTable table = t.get();
                Item[] items = table.rollLootTable(new TableRollContext(location));
                ArrayList<Item> shuffleItems = new ArrayList<>();
                for(int i = 0; i < container.getSize(); i++){
                    Item item = i < items.length ? items[i] : new BlockAir().toItem();
                    shuffleItems.add(item);
                }
                Collections.shuffle(shuffleItems);
                ChestInventory inventory = container.getRealInventory();
                for(int i = 0; i < shuffleItems.size(); i++){
                    inventory.setItem(i, shuffleItems.get(i));
                    inventory.sendContents(inventory.getViewers());
                }
            }
        }
    }

}
