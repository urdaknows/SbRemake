package me.carscupcake.sbremake.util.lootTable;

import me.carscupcake.sbremake.Stat;
import me.carscupcake.sbremake.item.ISbItem;
import me.carscupcake.sbremake.item.SbItemStack;
import me.carscupcake.sbremake.item.modifiers.Modifier;
import me.carscupcake.sbremake.player.SkyblockPlayer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomData;

import java.util.*;

public record ItemLoot(SbItemStack item, int min, int max, double chance, boolean magicFind, boolean petLuck,
                       MessageBuilder builder) implements LootTable.Loot<SbItemStack> {
    public ItemLoot(SbItemStack item, int min, int max, double chance) {
        this(item, min, max, chance, chance <= 0.05, false, NormalMessages.messageBuilder(chance));
    }

    public ItemLoot(SbItemStack item, int amount, double chance) {
        this(item, amount, amount, chance);
    }

    public ItemLoot(ISbItem item, int min, int max, double chance) {
        this(item.create(), min, max, chance, chance <= 0.05, false, NormalMessages.messageBuilder(chance));
    }

    public ItemLoot(ISbItem item, int amount, double chance) {
        this(item, amount, amount, chance);
    }

    public ItemLoot(Class<? extends ISbItem> item, int min, int max, double chance) {
        this(ISbItem.get(item), min, max, chance);
    }

    public ItemLoot(Class<? extends ISbItem> item, int amount, double chance) {
        this(item, amount, amount, chance);
    }

    public ItemLoot(Class<? extends ISbItem> clazz) {
        this(clazz, 1, 1);
    }

    public ItemLoot(Material material) {
        this(SbItemStack.base(material), 1, 1);
    }
    public ItemLoot(Material material, int amount) {
        this(SbItemStack.base(material), amount, 1);
    }
    public ItemLoot(Material material, int min, int max, double chance) {
        this(SbItemStack.base(material), min, max, chance);
    }

    @Override
    public Set<SbItemStack> loot(SkyblockPlayer player) {
        int amount = (min == max) ? max : (new Random().nextInt(max - min) + min);
        SbItemStack item = (this.item.sbItem().isUnstackable()) ? SbItemStack.from(this.item.item().with(DataComponents.CUSTOM_DATA, new CustomData(Objects.requireNonNull(this.item.item().get(DataComponents.CUSTOM_DATA)).nbt()
                .putString("uuid", UUID.randomUUID().toString())))) : this.item;
        item = item.withAmount(amount);
        if (item == null) return new HashSet<>(0);
        if (builder != null)
            player.sendMessage(builder.message(player, (item.getRarity().getPrefix()) + (item.displayName()), amount, magicFind));
        return Set.of(Objects.requireNonNull(item));
    }

    @Override
    public double chance(SkyblockPlayer player) {
        return chance * (1d + (((magicFind ? player.getStat(Stat.MagicFind) : 0d) + (petLuck ? player.getStat(Stat.PetLuck) : 0d)) / 100d));
    }

    public interface MessageBuilder {
        String message(SkyblockPlayer player, String itemName, int amount, boolean magicFind);
    }

    public enum NormalMessages implements MessageBuilder {
        Rare {
            @Override
            public String message(SkyblockPlayer player, String itemName, int amount, boolean magicFind) {
                return "§6§lRARE DROP! " + (itemName) + " " + ((amount != 1) ? "§8" + (amount) + "x " : "") + (magicFind ? "§b(+" + (player.getStat(Stat.MagicFind)) + " " + (Stat.MagicFind) + ")" : "");
            }
        },
        Legendary {
            @Override
            public String message(SkyblockPlayer player, String itemName, int amount, boolean magicFind) {
                return "§6§lLEGENDARY DROP! " + (itemName) + " " + ((amount != 1) ? "§8" + (amount) + "x " : "") + (magicFind ? "§b(+" + (player.getStat(Stat.MagicFind)) + " " + (Stat.MagicFind) + ")" : "");
            }
        },
        RNGesus {
            @Override
            public String message(SkyblockPlayer player, String itemName, int amount, boolean magicFind) {
                return "§d§lRNGesus DROP! " + (itemName) + " " + ((amount != 1) ? "§8" + (amount) + "x " : "") + (magicFind ? "§b(+" + (player.getStat(Stat.MagicFind)) + " " + (Stat.MagicFind) + ")" : "");
            }
        };

        public static MessageBuilder messageBuilder(double chance) {
            if (chance <= 0.0001) return RNGesus;
            else if (chance <= 0.001) return Legendary;
            else if (chance <= 0.01) return Rare;
            return null;
        }
    }
}
