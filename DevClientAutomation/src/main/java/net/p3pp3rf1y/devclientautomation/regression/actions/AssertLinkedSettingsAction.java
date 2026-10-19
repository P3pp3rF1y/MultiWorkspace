package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.p3pp3rf1y.sophisticatedbackpacks.settings.BackpackMainSettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AssertLinkedSettingsAction extends TypedRegressionAction<AssertLinkedSettingsAction.Arguments> {
	public record Arguments(boolean backpackPrimary) {
	}

	public AssertLinkedSettingsAction() {
		super("assert.linkedSettings", "1", Set.of("minecraft:client", "sophisticatedbackpacks", "sophisticatedcore"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("backpackPrimary"), description);
		if (!args.has("backpackPrimary") || !args.get("backpackPrimary").isJsonPrimitive() || !args.get("backpackPrimary").getAsJsonPrimitive().isBoolean()) {
			throw new IllegalArgumentException(description + " requires boolean backpackPrimary");
		}
		return new Arguments(args.get("backpackPrimary").getAsBoolean());
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			private int deadline = -1;
			private boolean serverReady;
			private Set<String> categories = Set.of();

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (context.minecraft().player.tickCount >= deadline) {
					return fail("Linked settings screen did not open with canonical categories");
				}
				if (future == null) {
					future = context.onServer(player -> {
						if (!(player.containerMenu instanceof SettingsContainerMenu<?> menu)) {
							return;
						}
						IStorageWrapper wrapper = menu.getStorageWrapper();
						IStorageWrapper host = (IStorageWrapper) LinkedStorageGroupsSavedData.get(player.level()).manager()
								.resolveVirtualHost(context.values().require("settings-group", UUID.class)).orElseThrow();
						if (wrapper.getSettingsHandler() != host.getSettingsHandler()) {
							throw new IllegalStateException("Settings menu does not use canonical settings");
						}
						categories = Set.copyOf(host.getSettingsHandler().getSettingsCategories().keySet());
						String expectedMain = MainSettingsCategory.NAME;
						if (!categories.contains(expectedMain)) {
							throw new IllegalStateException("Settings categories do not match the primary family");
						}
						assertSettingsContainers(menu, arguments.backpackPrimary(), "server");
						serverReady = true;
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress progress = await(future);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				if (!serverReady) {
					future = null;
					return RegressionActionProgress.WAITING;
				}
				if (!(context.minecraft().gui.screen() instanceof SettingsScreen screen)
						|| !screen.getMenu().getStorageWrapper().getSettingsHandler().getSettingsCategories().keySet().equals(categories)) {
					return RegressionActionProgress.WAITING;
				}
				assertSettingsContainers(screen.getMenu(), arguments.backpackPrimary(), "client");
				return RegressionActionProgress.PASSED;
			}
		};
	}

	private static void assertSettingsContainers(SettingsContainerMenu<?> menu, boolean backpackPrimary, String side) {
		boolean[] hasMainContainer = {false};
		menu.forEachSettingsContainer((name, container) -> {
			if (container == null) {
				throw new IllegalStateException("Missing " + side + " settings container " + name);
			}
			if (MainSettingsCategory.NAME.equals(name)) {
				hasMainContainer[0] = true;
				if (backpackPrimary != (container instanceof BackpackMainSettingsContainer)) {
					throw new IllegalStateException("Settings main container does not use the required primary family on " + side);
				}
			}
		});
		if (!hasMainContainer[0]) {
			throw new IllegalStateException("Missing " + side + " main settings container");
		}
	}
}
