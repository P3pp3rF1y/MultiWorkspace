package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Isolates client rules/offsets and restores their exact original values on every terminal path. */
public final class FixtureInventoryInteractionSettingsAction extends TypedRegressionAction<Void> {
	public FixtureInventoryInteractionSettingsAction() {
		super("fixture.inventoryInteractionSettings", "1", Set.of("minecraft:client", "sophisticatedinventoryinteractions"));
	}

	@Override
	protected Void parseArguments(JsonObject args, String description) {
		requireNoArguments(args, description);
		return null;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Void arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				try {
					Object client = Class.forName("net.p3pp3rf1y.sophisticatedinventoryinteractions.Config").getField("CLIENT").get(null);
					var injectorField = Class.forName("net.p3pp3rf1y.sophisticatedinventoryinteractions.client.ClientEventHandler")
							.getDeclaredField("SCREEN_INTERACTION_INJECTOR");
					injectorField.setAccessible(true);
					Object injector = injectorField.get(null);
					var editorField = injector.getClass().getDeclaredField("layoutEditor");
					editorField.setAccessible(true);
					Object editor = editorField.get(injector);
					Map<ModConfigSpec.ConfigValue<List<? extends String>>, List<? extends String>> originals = new LinkedHashMap<>();
					for (String name : List.of("includeScreenClasses", "excludeScreenClasses", "includeContainerScreenClasses", "excludeContainerScreenClasses",
							"includeTransferScreenClasses", "excludeTransferScreenClasses", "includePlayerSortScreenClasses", "excludePlayerSortScreenClasses",
							"anchorOffsetOverrides", "playerOffsetOverrides", "playerSortOffsetOverrides")) {
						@SuppressWarnings("unchecked")
						ModConfigSpec.ConfigValue<List<? extends String>> value = (ModConfigSpec.ConfigValue<List<? extends String>>) client.getClass()
								.getField(name).get(client);
						originals.put(value, List.copyOf(value.get()));
					}
					context.registerClientRestoration("inventory-interaction-settings", () -> {
						if (context.minecraft().screen instanceof AbstractContainerScreen<?> screen) {
							screen.onClose();
						}
						try {
							editor.getClass().getMethod("cancelPending").invoke(editor);
						} catch (ReflectiveOperationException e) {
							throw new IllegalStateException("Cannot cancel pending layout adjustment", e);
						} finally {
							originals.forEach((value, original) -> {
								value.set(original);
								value.save();
							});
						}
					});
					originals.keySet().forEach(value -> value.set(List.of()));
					return RegressionActionProgress.PASSED;
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("Cannot isolate SII client settings", e);
				}
			}
		};
	}
}
