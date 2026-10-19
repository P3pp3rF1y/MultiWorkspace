package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.devclientautomation.recipeviewer.jei.JeiRecipeViewerAutomation;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;

import java.util.Set;

public final class DropSettingsGhostAction extends TypedRegressionAction<DropSettingsGhostAction.Arguments> {
	public record Arguments(int slot) {
	}

	public DropSettingsGhostAction() {
		super("recipeviewer.dropSettingsGhost", "1", Set.of("minecraft:client", "jei"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot"), description);
		return new Arguments(optionalNonNegativeInt(args, "slot", 0, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int ticks;

			@Override
			public RegressionActionProgress tick() {
				if (ticks++ == 0) {
					if (!(context.minecraft().gui.screen() instanceof SettingsScreen screen)) {
						return fail("Settings screen is not open");
					}
					JeiRecipeViewerAutomation.dropSettingsGhost(screen, arguments.slot(), new ItemStack(Items.DIAMOND));
				}
				return ticks >= 4 ? RegressionActionProgress.PASSED : RegressionActionProgress.WAITING;
			}
		};
	}
}
