package me.carscupcake.sbremake.config;

import com.google.gson.*;
import com.google.gson.internal.LazilyParsedNumber;
import me.carscupcake.sbremake.Main;
import me.carscupcake.sbremake.item.SbItemStack;
import me.carscupcake.sbremake.item.modifiers.enchantment.SkyblockEnchantment;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.tag.Tag;
import org.junit.Assert;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
public class ConfigSection {
    public static final Data<ConfigSection> SECTION = new ClassicGetter<>(ConfigSection::new, ConfigSection::getRawElement);
    public static final Data<Boolean> BOOLEAN = new ClassicGetter<>(JsonElement::getAsBoolean, JsonPrimitive::new);
    public static final Data<String> STRING = new ClassicGetter<>(JsonElement::getAsString, JsonPrimitive::new);
    public static final Data<Integer> INTEGER = new ClassicGetter<>(JsonElement::getAsInt, JsonPrimitive::new);
    public static final Data<Long> LONG = new ClassicGetter<>(JsonElement::getAsLong, JsonPrimitive::new);
    public static final Data<Float> FLOAT = new ClassicGetter<>(JsonElement::getAsFloat, JsonPrimitive::new);
    public static final Data<Double> DOUBLE = new ClassicGetter<>(JsonElement::getAsDouble, JsonPrimitive::new);
    public static final Data<Byte> BYTE = new ClassicGetter<>(JsonElement::getAsByte, JsonPrimitive::new);
    public static final Data<String[]> STRING_ARRAY = new ClassicGetter<>(element1 -> {
        assert element1.isJsonArray();
        JsonArray array = element1.getAsJsonArray();
        String[] stringArray = new String[array.size()];
        int i = 0;
        for (JsonElement el : array) {
            stringArray[i] = el.getAsString();
            i++;
        }
        return stringArray;
    }, strings -> {
        JsonArray array = new JsonArray(strings.length);
        for (String s : strings)
            array.add(new JsonPrimitive(s));
        return array;
    });
    public static final Data<Point> POSITION = new ClassicGetter<>(element -> {
        JsonObject o = element.getAsJsonObject();
        return new Pos(o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble());
    }, pos -> {
        JsonObject o = new JsonObject();
        o.addProperty("x", pos.x());
        o.addProperty("y", pos.y());
        o.addProperty("z", pos.z());
        return o;
    });
    public static final Data<SbItemStack> ITEM = new ClassicGetter<>(element -> {
        return jsonElementToItem(element);
    }, stack -> {
        return itemToJson(stack);
    });

    public static final Data<SbItemStack[]> ITEM_ARRAY = new ClassicGetter<>(jsonElement -> {
        if (!jsonElement.isJsonArray()) {
            return null;
        }
        var array = jsonElement.getAsJsonArray();
        var stack = new SbItemStack[array.size()];
        for (int i = 0; i < array.size(); i++) {
            stack[i] = jsonElementToItem(array.get(i));
        }
        return stack;
    }, sbItemStacks -> {
        var array = new JsonArray(sbItemStacks.length);
        for (int i = 0; i < sbItemStacks.length; i++) {
            array.set(i, itemToJson(sbItemStacks[i]));
        }
        return array;
    });

    public static final Data<UUID> UUID = new ClassicGetter<>(jsonElement -> java.util.UUID.fromString(jsonElement.getAsString()), uuid -> new JsonPrimitive(uuid.toString()));

    public static JsonElement itemToJson(SbItemStack stack) {
        JsonObject object = new JsonObject();
        int size = stack.item().amount();
        object.addProperty("size", size);
        object.add("nbt", nbtCompoundToJson(Objects.requireNonNull(stack.item().get(DataComponents.CUSTOM_DATA)).nbt()));
        return object;
    }

    private static SbItemStack jsonElementToItem(JsonElement element) {
        CompoundBinaryTag tag = computeTag(element.getAsJsonObject().get("nbt").getAsJsonObject());
        int size = element.getAsJsonObject().get("size").getAsInt();
        String id = tag.getString("id");
        SbItemStack stack = SbItemStack.from(id);
        if (stack == null) {
            Main.LOGGER.warn("Could not find ISbItem with id {}", id);
            stack = SbItemStack.AIR;
        }
        ItemStack item = stack.item().with(DataComponents.CUSTOM_DATA, new CustomData(tag)).withAmount(size);
        SbItemStack sbItemStack = SbItemStack.from(item);
        Map<SkyblockEnchantment, Integer> enchantmentIntegerMap = sbItemStack.getEnchantments();
        if (!enchantmentIntegerMap.isEmpty()) {
            EnchantmentList enchantmentList = new EnchantmentList(Enchantment.PROTECTION, 1);
            item = sbItemStack.item().with(DataComponents.ENCHANTMENTS, enchantmentList).withoutExtraTooltip();
            sbItemStack = SbItemStack.from(item);
        }
        return sbItemStack;
    }

    private static JsonObject nbtCompoundToJson(CompoundBinaryTag compoundBinaryTag) {
        JsonObject object = new JsonObject();
        for (String key : compoundBinaryTag.keySet()) {
            JsonElement e = nbtTagToJson(Objects.requireNonNull(compoundBinaryTag.get(key)));
            object.add(key, e);
        }
        return object;
    }

    public static ConfigSection empty() {
        return new ConfigSection(new JsonObject());
    }

    public static ConfigSection emptyArray() {
        return new ConfigSection(new JsonArray());
    }

    private static JsonElement nbtTagToJson(BinaryTag binaryTag) {
        if (binaryTag instanceof StringBinaryTag stringBinaryTag) {
        }
        return switch (binaryTag) {
            case StringBinaryTag tag -> new JsonPrimitive(tag.value());
            case IntBinaryTag tag -> new JsonPrimitive(tag.value());
            case LongBinaryTag tag -> new JsonPrimitive(tag.value());
            case ShortBinaryTag tag -> new JsonPrimitive(tag.value());
            case ByteBinaryTag tag -> new JsonPrimitive(tag.value());
            case DoubleBinaryTag tag -> new JsonPrimitive(tag.value());
            case FloatBinaryTag tag -> new JsonPrimitive(tag.value());
            case ListBinaryTag tags -> {
                JsonArray array = new JsonArray();
                for (BinaryTag tag : tags)
                    array.add(nbtTagToJson(tag));
                yield array;
            }
            case CompoundBinaryTag tag -> nbtCompoundToJson(tag);
            case IntArrayBinaryTag tags -> {
                JsonArray array = new JsonArray();
                for (int tag : tags.value())
                    array.add(new JsonPrimitive(tag));
                yield array;
            }
            case ByteArrayBinaryTag tags -> {
                JsonArray array = new JsonArray();
                for (byte tag : tags.value())
                    array.add(new JsonPrimitive(tag));
                yield array;
            }
            case LongArrayBinaryTag tags -> {
                JsonArray array = new JsonArray();
                for (long tag : tags.value())
                    array.add(new JsonPrimitive(tag));
                yield array;
            }
            default -> throw new IllegalStateException("Unexpected value: " + (binaryTag.getClass().getSimpleName()));
        };
    }

    private static BinaryTag computeElement(JsonElement element) {
        if (element.isJsonObject()) return computeTag(element.getAsJsonObject());
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) return ByteBinaryTag.byteBinaryTag((byte) ((primitive.getAsBoolean()) ? 1 : 0));
            else if (primitive.isNumber()) {
                switch (primitive.getAsNumber()) {
                    case Double d -> {
                        return DoubleBinaryTag.doubleBinaryTag(d);
                    }
                    case Float d -> {
                        return FloatBinaryTag.floatBinaryTag(d);
                    }
                    case Integer d -> {
                        return IntBinaryTag.intBinaryTag(d);
                    }
                    case Long d -> {
                        return LongBinaryTag.longBinaryTag(d);
                    }
                    case Short d -> {
                        return ShortBinaryTag.shortBinaryTag(d);
                    }
                    case Byte d -> {
                        return ByteBinaryTag.byteBinaryTag(d);
                    }

                    case LazilyParsedNumber number -> {
                        try {
                            return IntBinaryTag.intBinaryTag(number.intValue());
                        } catch (Exception ignored) {
                            return DoubleBinaryTag.doubleBinaryTag(number.doubleValue());
                        }
                    }
                    default ->
                            throw new IllegalStateException("Unexpected value: " + (primitive.getAsNumber().getClass()));
                }
            } else if (primitive.isString()) return StringBinaryTag.stringBinaryTag(primitive.getAsString());
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            ListBinaryTag.Builder<BinaryTag> tags = ListBinaryTag.builder();
            for (JsonElement e : array)
                tags.add(computeElement(e));
            return tags.build();
        }
        return null;
    }

    private static CompoundBinaryTag computeTag(JsonObject object) {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
        for (Map.Entry<String, JsonElement> elementEntry : object.entrySet()) {
            String key = elementEntry.getKey();
            BinaryTag tag = computeElement(elementEntry.getValue());
            if (tag == null) continue;
            builder.put(key, tag);
        }
        return builder.build();
    }

    protected JsonElement element;

    public ConfigSection(JsonElement base) {
        element = base;
    }

    public <T> T get(String key, ConfigFile.Data<T> data) {
        if (element == null) element = new JsonObject();
        return data.get(element, key);
    }

    public <T> T get(String key, ConfigFile.Data<T> data, T def) {
        if (element == null) element = new JsonObject();
        if (!has(key)) return def;
        return data.get(element, key);
    }

    public <T> T getOrSetDefault(String key, ConfigFile.Data<T> data, T def) {
        if (element == null) element = new JsonObject();
        if (!has(key)) {
            set(key, def, data);
            return def;
        }
        return data.get(element, key);
    }

    public <T> void set(String key, T value, ConfigFile.Data<T> type) {
        if (element == null) element = new JsonObject();
        if (value == null) {
            if (element instanceof JsonObject object)
                if (object.has(key))
                    object.remove(key);
            return;
        }
        type.set(element, key, value);
    }

    public <T> T as(ConfigFile.Data<T> data) {
        if (element == null) element = new JsonObject();
        return data.get(element, null);
    }

    public boolean has(String key) {
        if (element == null) element = new JsonObject();
        return element.getAsJsonObject().has(key);
    }

    public JsonElement getRawElement() {
        return element;
    }

    public void setRawElement(JsonElement element) {
        this.element = element;
    }

    public void forEach(Consumer<ConfigSection> elementConsumer) {
        if (element == null) element = new JsonArray();
        Assert.assertTrue("Json is not an array", element.isJsonArray());
        element.getAsJsonArray().forEach(element1 -> {
            elementConsumer.accept(new ConfigSection(element1));
        });
    }

    public void forEntries(BiConsumer<String, ConfigSection> entryConsumer) {
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().asMap().entrySet()) {
            entryConsumer.accept(entry.getKey(), new ConfigSection(entry.getValue()));
        }
    }

    public interface Data<T> {
        T get(JsonElement element, String key);

        void set(JsonElement element, String key, T data);
    }

    public static class ClassicGetter<T> implements Data<T> {
        private final Function<JsonElement, T> fun;
        private final Function<T, JsonElement> elementBuilder;

        public ClassicGetter(Function<JsonElement, T> tFunction, Function<T, JsonElement> elementBuilder) {
            this.fun = tFunction;
            this.elementBuilder = elementBuilder;
        }

        @Override
        public T get(JsonElement element, String key) {
            if (key == null) {
                return fun.apply(element);
            }
            JsonElement element1 = element.getAsJsonObject().get(key);
            if (element1 == null) return null;
            return fun.apply(element1);
        }

        @Override
        public void set(JsonElement element, String key, T data) {
            if (element.isJsonArray())
                element.getAsJsonArray().add(elementBuilder.apply(data));
            else
                element.getAsJsonObject().add(key, elementBuilder.apply(data));
        }
    }

}
