package net.p3pp3rf1y.devclientautomation.recipeviewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;

import java.util.Optional;

public final class RecipeViewerFocus {
    private RecipeViewerFocus() {}

    public static Optional<ItemStack> encodedStack(JsonObject request) {
        return RecipeViewerRequest.focus(request)
                .filter(focus -> focus.has("encoded") && focus.get("encoded").isJsonObject())
                .flatMap(focus -> ItemStack.CODEC.parse(Minecraft.getInstance().level.registryAccess().createSerializationContext(JsonOps.INSTANCE), focus.get("encoded"))
                        .result()
                        .map(stack -> applySophisticatedCoreComponents(stack, focus.getAsJsonObject("encoded"))));
    }

    private static ItemStack applySophisticatedCoreComponents(ItemStack stack, JsonObject encoded) {
        if (!encoded.has("components") || !encoded.get("components").isJsonObject()) {
            return stack;
        }
        JsonObject components = encoded.getAsJsonObject("components");
        setInt(components, "sophisticatedcore:number_of_inventory_slots", value -> stack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS.get(), value));
        setInt(components, "sophisticatedcore:number_of_upgrade_slots", value -> stack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS.get(), value));
        setInt(components, "sophisticatedcore:main_color", value -> stack.set(ModCoreDataComponents.MAIN_COLOR.get(), value));
        setInt(components, "sophisticatedcore:accent_color", value -> stack.set(ModCoreDataComponents.ACCENT_COLOR.get(), value));
		setRenderData(components, "sophisticatedcore:render_info_tag", stack);
		setRenderData(components, "sophisticatedcore:render_data", stack);
		return stack;
	}

	private static void setRenderData(JsonObject components, String key, ItemStack stack) {
		if (components.has(key) && components.get(key).isJsonObject()) {
			JsonObject renderDataJson = components.getAsJsonObject(key);
			RenderData.CODEC.parse(Minecraft.getInstance().level.registryAccess().createSerializationContext(JsonOps.INSTANCE), renderDataJson)
					.result()
					.or(() -> RenderData.CODEC.parse(NbtOps.INSTANCE, tagFromJson(renderDataJson)).result())
					.ifPresent(renderData -> stack.set(ModCoreDataComponents.RENDER_DATA.get(), renderData));
		}
	}

    private static CompoundTag tagFromJson(JsonObject json) {
        CompoundTag tag = new CompoundTag();
        for (String key : json.keySet()) {
            JsonElement value = json.get(key);
            if (value.isJsonObject()) {
                tag.put(key, tagFromJson(value.getAsJsonObject()));
            } else if (value.isJsonPrimitive()) {
                putPrimitive(tag, key, value.getAsJsonPrimitive());
            }
        }
        return tag;
    }

    private static void putPrimitive(CompoundTag tag, String key, JsonPrimitive value) {
        if (value.isBoolean()) {
            tag.putBoolean(key, value.getAsBoolean());
        } else if (value.isNumber()) {
            tag.putInt(key, value.getAsInt());
        } else if (value.isString()) {
            tag.putString(key, value.getAsString());
        }
    }

    private static void setInt(JsonObject components, String key, java.util.function.IntConsumer setter) {
        if (components.has(key)) {
            setter.accept(components.get(key).getAsInt());
        }
    }
}
