package net.p3pp3rf1y.devclientautomation.recipeviewer;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class RecipeViewerFocus {
	private RecipeViewerFocus() {
	}

	public static Optional<ItemStack> encodedStack(JsonObject request) {
		return RecipeViewerRequest.focus(request).filter(focus -> focus.has("encoded") && focus.get("encoded").isJsonObject()).flatMap(focus -> ItemStack.CODEC
				.parse(Minecraft.getInstance().level.registryAccess().createSerializationContext(JsonOps.INSTANCE), focus.get("encoded")).result());
	}
}
