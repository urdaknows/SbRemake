package me.carscupcake.sbremake.item;

import lombok.Builder;
import lombok.Getter;
import me.carscupcake.sbremake.item.modifiers.enchantment.SkyblockEnchantment;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.tag.Tag;

import java.util.*;

@Builder
@Getter
public class ItemStackModifiers {
    public static final ItemStackModifiers BASE_MODIFIERS = new ItemStackModifiersBuilder().build();
    public static final ItemStackModifiers ENCHANT_GLINT_MODIFIERS = new ItemStackModifiersBuilder().glint(true).build();
    @Builder.Default
    private boolean glint = false;
    @Builder.Default
    private List<Enchantment> baseEnchantments = new ArrayList<>();
    @Builder.Default
    private Map<String, BinaryTag> baseTags = new HashMap<>();
    @Builder.Default
    private Map<SkyblockEnchantment, Integer> enchantments = new HashMap<>();

    public ItemStack.Builder apply(ItemStack.Builder builder) {
        if (glint || !baseEnchantments.isEmpty())
            builder.set(DataComponents.ENCHANTMENTS, new EnchantmentList(Enchantment.PROTECTION, 1));
        for (Map.Entry<String, BinaryTag> tagEntry : baseTags.entrySet()) {
            builder.set(DataComponents.CUSTOM_DATA, new CustomData(((CompoundBinaryTag) tagEntry.getValue())));
        }
        return builder;
    }

    public ItemStack apply(ItemStack item) {
        CompoundBinaryTag tag = Objects.requireNonNull(item.get(DataComponents.CUSTOM_DATA)).nbt();
        CompoundBinaryTag enchantments = tag.getCompound("enchantments");
        for (Map.Entry<SkyblockEnchantment, Integer> entry : this.enchantments.entrySet())
            enchantments = enchantments.putInt(entry.getKey().getId(), (entry.getValue()));
        return item.with(DataComponents.CUSTOM_DATA, new CustomData(tag.put("enchantments", enchantments)));
    }
}
