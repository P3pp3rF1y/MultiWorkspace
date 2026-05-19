package net.p3pp3rf1y.devclientautomation.recipeviewer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.devclientautomation.JsonUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public final class RecipeJsonUtil {
    private RecipeJsonUtil() {}

    public static String itemStackJson(ItemStack stack) {
        return "{"
                + JsonUtil.property("type", "item") + ","
                + JsonUtil.property("id", itemId(stack)) + ","
                + JsonUtil.property("name", stack.getHoverName().getString()) + ","
                + "\"count\":" + stack.getCount() + ","
                + JsonUtil.property("components", pseudoComponentsJson(stack)) + ","
                + JsonUtil.rawProperty("componentKeys", componentKeysJson(stack)) + ","
                + JsonUtil.rawProperty("encoded", encodedStackJson(stack))
                + "}";
    }

    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String componentKeysJson(ItemStack stack) {
        if (!stack.hasTag()) {
            return "[]";
        }
        return stack.getTag().getAllKeys().stream()
                .map(key -> JsonUtil.property("", key).substring(3))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String encodedStackJson(ItemStack stack) {
        return "{"
                + JsonUtil.property("id", itemId(stack)) + ","
                + "\"count\":" + stack.getCount() + ","
                + JsonUtil.rawProperty("components", pseudoComponentsJson(stack))
                + "}";
    }

    static String pseudoComponentsJson(ItemStack stack) {
        if (!stack.hasTag()) {
            return "{}";
        }
        CompoundTag tag = stack.getTag();
        StringBuilder json = new StringBuilder("{");
        boolean[] first = {true};
        addColorTagValue(json, first, "sophisticatedcore:main_color", tag, tag.contains("mainColor") ? "mainColor" : "clothColor");
        addColorTagValue(json, first, "sophisticatedcore:accent_color", tag, tag.contains("accentColor") ? "accentColor" : "borderColor");
        addTagValue(json, first, "sophisticatedcore:number_of_inventory_slots", tag, "inventorySlots");
        addBackpackDefaultValue(json, first, stack, "sophisticatedcore:number_of_inventory_slots", "inventorySlots", "getNumberOfSlots");
        addTagValue(json, first, "sophisticatedcore:number_of_upgrade_slots", tag, "upgradeSlots");
        addBackpackDefaultValue(json, first, stack, "sophisticatedcore:number_of_upgrade_slots", "upgradeSlots", "getNumberOfUpgradeSlots");
        addCompoundTagValue(json, first, "sophisticatedcore:render_info_tag", tag, "renderInfo");
        addTagValue(json, first, "sophisticatedstorage:wood_type", tag, "woodType");
        addBooleanTagValue(json, first, "sophisticatedstorage:flat_top", tag, "flatTop");
        addBooleanTagValue(json, first, "sophisticatedstorage:double_chest", tag, "doubleChest");
        addTagValue(json, first, "sophisticatedstorageinmotion:boat_type", tag, "boatType");
        if (tag.contains("storageItem", Tag.TAG_COMPOUND)) {
            addComma(json, first);
            json.append(JsonUtil.property("sophisticatedstorageinmotion:storage_item", "").substring(0, "\"sophisticatedstorageinmotion:storage_item\":".length()))
                    .append(encodedStackJson(ItemStack.of(tag.getCompound("storageItem"))));
        }
        json.append('}');
        return json.toString();
    }

    private static void addTagValue(StringBuilder json, boolean[] first, String componentName, CompoundTag tag, String tagName) {
        if (!tag.contains(tagName)) {
            return;
        }
        addComma(json, first);
        json.append(JsonUtil.property(componentName, "").substring(0, componentName.length() + 3));
        Tag value = tag.get(tagName);
        if (value.getId() == Tag.TAG_STRING) {
            json.append(JsonUtil.property("", value.getAsString()).substring(3));
        } else {
            json.append(value.getAsString());
        }
    }

    private static void addBackpackDefaultValue(StringBuilder json, boolean[] first, ItemStack stack, String componentName, String tagName, String methodName) {
        if (stack.getTag().contains(tagName) || !stack.getItem().getClass().getName().equals("net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem")) {
            return;
        }
        try {
            Method method = stack.getItem().getClass().getMethod(methodName);
            Object value = method.invoke(stack.getItem());
            if (value instanceof Integer intValue) {
                addComma(json, first);
                json.append(JsonUtil.property(componentName, "").substring(0, componentName.length() + 3));
                json.append(intValue);
            }
        } catch (ReflectiveOperationException e) {
            // Optional compatibility for the backpack module; omit the pseudo-component if unavailable.
        }
    }

    private static void addColorTagValue(StringBuilder json, boolean[] first, String componentName, CompoundTag tag, String tagName) {
        if (!tag.contains(tagName)) {
            return;
        }
        addComma(json, first);
        json.append(JsonUtil.property(componentName, "").substring(0, componentName.length() + 3));
        json.append(tag.getInt(tagName) | 0xFF000000);
    }

    private static void addBooleanTagValue(StringBuilder json, boolean[] first, String componentName, CompoundTag tag, String tagName) {
        if (!tag.contains(tagName)) {
            return;
        }
        addComma(json, first);
        json.append(JsonUtil.property(componentName, "").substring(0, componentName.length() + 3));
        json.append(tag.getBoolean(tagName));
    }

    private static void addCompoundTagValue(StringBuilder json, boolean[] first, String componentName, CompoundTag tag, String tagName) {
        if (!tag.contains(tagName, Tag.TAG_COMPOUND)) {
            return;
        }
        addComma(json, first);
        json.append(JsonUtil.property(componentName, "").substring(0, componentName.length() + 3));
        json.append(tagJson(tag.getCompound(tagName)));
    }

    private static String tagJson(CompoundTag tag) {
        StringBuilder json = new StringBuilder("{");
        boolean[] first = {true};
        for (String key : tag.getAllKeys()) {
            addComma(json, first);
            json.append(JsonUtil.property(key, "").substring(0, key.length() + 3));
            Tag value = tag.get(key);
            if (value instanceof CompoundTag compoundTag) {
                json.append(tagJson(compoundTag));
            } else if (value.getId() == Tag.TAG_STRING) {
                json.append(JsonUtil.property("", value.getAsString()).substring(3));
            } else {
                json.append(value.getAsString());
            }
        }
        json.append('}');
        return json.toString();
    }

    private static void addComma(StringBuilder json, boolean[] first) {
        if (!first[0]) {
            json.append(',');
        }
        first[0] = false;
    }

    public static String stackJson(String type, String id, String name, long amount, float chance) {
        return "{"
                + JsonUtil.property("type", type) + ","
                + JsonUtil.property("id", id) + ","
                + JsonUtil.property("name", name) + ","
                + "\"amount\":" + amount + ","
                + "\"chance\":" + chance
                + "}";
    }

    public static String ingredientJson(List<String> alternatives) {
        return "{\"alternativeCount\":" + alternatives.size() + ",\"alternatives\":[" + String.join(",", alternatives) + "]}";
    }

    public static String ingredientsJson(List<String> ingredients) {
        return "[" + String.join(",", ingredients) + "]";
    }

    public static String itemStackIngredientsJson(List<ItemStack> stacks) {
        return ingredientsJson(stacks.stream()
                .map(stack -> ingredientJson(List.of(itemStackJson(stack))))
                .collect(Collectors.toList()));
    }

    public static String itemStackIngredientGroupsJson(List<List<ItemStack>> stacks) {
        return ingredientsJson(stacks.stream()
                .map(group -> ingredientJson(group.stream().map(RecipeJsonUtil::itemStackJson).collect(Collectors.toList())))
                .collect(Collectors.toList()));
    }
}
