package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;

import java.util.List;
import java.util.Set;

public final class ClickCapturedMobAction extends TypedRegressionAction<ClickCapturedMobAction.Arguments> {
	public record Arguments(int index) {
	}

	public ClickCapturedMobAction() {
		super("menu.clickCapturedMob", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("index"), description);
		return new Arguments(optionalNonNegativeInt(args, "index", 0, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private int clickedAt = -1;

			@Override
			public RegressionActionProgress tick() {
				Minecraft minecraft = context.minecraft();
				if (clickedAt >= 0) {
					return minecraft.player.tickCount - clickedAt >= 4 ? RegressionActionProgress.PASSED : RegressionActionProgress.WAITING;
				}
				if (!(minecraft.gui.screen() instanceof StorageScreenBase<?> screen) || !(screen.getMenu() instanceof BackpackContainer menu)) {
					return fail("Backpack screen is not open");
				}
				List<CapturedMob> mobs = MobCatcherStorage.getCapturedMobs(menu.getStorageWrapper());
				if (arguments.index() >= mobs.size()) {
					return fail("Captured mob index is missing");
				}
				Slot slot = menu.getSlot(mobs.get(arguments.index()).slot());
				double x = screen.getLeftPos() + slot.x + 8;
				double y = screen.getTopPos() + slot.y + 8;
				screen.mouseMoved(x, y);
				MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
				screen.mouseClicked(event, false);
				screen.mouseReleased(event);
				clickedAt = minecraft.player.tickCount;
				return RegressionActionProgress.WAITING;
			}
		};
	}
}
