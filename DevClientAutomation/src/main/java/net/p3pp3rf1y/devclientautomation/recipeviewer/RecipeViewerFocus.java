package net.p3pp3rf1y.devclientautomation.recipeviewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

public final class RecipeViewerFocus {
    private RecipeViewerFocus() {}

    public static Optional<ItemStack> encodedStack(JsonObject request) {
        return RecipeViewerRequest.focus(request)
                .filter(focus -> focus.has("encoded") && focus.get("encoded").isJsonObject())
                .flatMap(focus -> stackFromEncoded(focus.getAsJsonObject("encoded")));
    }

    private static Optional<ItemStack> stackFromEncoded(JsonObject encoded) {
        if (!encoded.has("id")) {
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(encoded.get("id").getAsString()));
        ItemStack stack = new ItemStack(item, encoded.has("count") ? encoded.get("count").getAsInt() : 1);
        if (encoded.has("components") && encoded.get("components").isJsonObject()) {
            stack.setTag(tagFromComponents(item, encoded.getAsJsonObject("components")));
        }
        return Optional.of(stack);
    }

    private static CompoundTag tagFromComponents(Item item, JsonObject components) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, JsonElement> entry : components.entrySet()) {
            putComponent(item, tag, entry.getKey(), entry.getValue());
        }
        return tag;
    }

    private static void putComponent(Item item, CompoundTag tag, String key, JsonElement value) {
        switch (key) {
            case "sophisticatedcore:main_color" -> {
                putMainColor(item, tag, value);
            }
            case "sophisticatedcore:accent_color" -> {
                putAccentColor(item, tag, value);
            }
            case "sophisticatedstorage:wood_type" -> tag.putString("woodType", value.getAsString());
            case "sophisticatedstorage:flat_top" -> tag.putBoolean("flatTop", value.getAsBoolean());
            case "sophisticatedstorage:double_chest" -> tag.putBoolean("doubleChest", value.getAsBoolean());
            case "sophisticatedstorageinmotion:boat_type" -> tag.putString("boatType", value.getAsString());
            case "sophisticatedstorageinmotion:storage_item" -> stackFromEncoded(value.getAsJsonObject()).ifPresent(storage -> tag.put("storageItem", storage.save(new CompoundTag())));
            case "sophisticatedcore:number_of_inventory_slots" -> tag.putInt("inventorySlots", value.getAsInt());
            case "sophisticatedcore:number_of_upgrade_slots" -> tag.putInt("upgradeSlots", value.getAsInt());
            case "sophisticatedcore:render_info_tag" -> tag.put("renderInfo", tagFromJson(value.getAsJsonObject()));
            default -> putLegacyBackpackColor(item, tag, key, value);
        }
    }

    private static CompoundTag tagFromJson(JsonObject json) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                tag.put(entry.getKey(), tagFromJson(value.getAsJsonObject()));
            } else if (value.isJsonPrimitive()) {
                putPrimitive(tag, entry.getKey(), value.getAsJsonPrimitive());
            }
        }
        return tag;
    }

    private static void putPrimitive(CompoundTag tag, String key, JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            tag.putBoolean(key, primitive.getAsBoolean());
        } else if (primitive.isNumber()) {
            tag.putInt(key, primitive.getAsInt());
        } else if (primitive.isString()) {
            tag.putString(key, primitive.getAsString());
        }
    }

    private static void putLegacyBackpackColor(Item item, CompoundTag tag, String key, JsonElement value) {
        if (key.equals("sophisticatedcore:main_color")) {
            putMainColor(item, tag, value);
        } else if (key.equals("sophisticatedcore:accent_color")) {
            putAccentColor(item, tag, value);
        }
    }

    private static void putMainColor(Item item, CompoundTag tag, JsonElement value) {
        int color = legacyColor(value.getAsInt());
        tag.putInt(isBackpack(item) ? "clothColor" : "mainColor", color);
    }

    private static void putAccentColor(Item item, CompoundTag tag, JsonElement value) {
        int color = legacyColor(value.getAsInt());
        tag.putInt(isBackpack(item) ? "borderColor" : "accentColor", color);
    }

    private static boolean isBackpack(Item item) {
        return item.getClass().getName().equals("net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem");
    }

    private static int legacyColor(int color) {
        return color & 0xFFFFFF;
    }
}
