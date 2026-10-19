package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsTab;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssertSettingsSelectionAction extends TypedRegressionAction<AssertSettingsSelectionAction.Arguments> {
	public record Arguments(int slot, boolean display) {
	}
	public AssertSettingsSelectionAction() {
		super("assert.settingsSelection", "1", Set.of("minecraft:client", "sophisticatedcore"));
	}
	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("slot", "display"), description);
		if (!args.has("display") || !args.get("display").isJsonPrimitive() || !args.getAsJsonPrimitive("display").isBoolean()) {
			throw new IllegalArgumentException(description + " requires boolean display");
		}
		return new Arguments(optionalNonNegativeInt(args, "slot", 0, description), args.get("display").getAsBoolean());
	}
	private static boolean matches(IStorageWrapper wrapper, Arguments args) {
		SettingsHandler settings = wrapper.getSettingsHandler();
		boolean memory = settings.getTypeCategory(MemorySettingsCategory.class).getSlotFilterStack(args.slot(), false).filter(stack -> stack.is(Items.DIAMOND))
				.isPresent();
		ItemDisplaySettingsCategory display = settings.getTypeCategory(ItemDisplaySettingsCategory.class);
		return memory && display.getSlots().contains(args.slot()) == args.display() && display.getSlotColor(args.slot()).isPresent() == args.display();
	}
	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			private boolean serverMatched;
			private String serverState = "";
			private String clientState = "";
			private int ticks;
			@Override
			public RegressionActionProgress tick() {
				if (ticks++ > 100) {
					return fail("Memory/display settings did not persist on server and client; server=" + serverState + "; client=" + clientState);
				}
				if (future == null) {
					future = context.onServer(player -> {
						IStorageWrapper wrapper = player.containerMenu instanceof SettingsContainerMenu<?> menu
								? menu.getStorageWrapper()
								: player.containerMenu instanceof StorageContainerMenuBase<?> menu ? menu.getStorageWrapper() : null;
						serverMatched = wrapper != null && matches(wrapper, arguments);
						serverState = wrapper == null ? "no menu" : wrapper.getSettingsHandler().getSettingsData().toString();
					});
					return RegressionActionProgress.WAITING;
				}
				RegressionActionProgress progress = await(future);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				IStorageWrapper wrapper = context.minecraft().player.containerMenu instanceof SettingsContainerMenu<?> menu
						? menu.getStorageWrapper()
						: context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu ? menu.getStorageWrapper() : null;
				boolean overlay = true;
				clientState = wrapper == null ? "no menu" : wrapper.getSettingsHandler().getSettingsData().toString();
				if (arguments.display() && context.minecraft().screen instanceof SettingsScreen screen) {
					overlay = screen.getSettingsTabControl().children().stream().filter(ItemDisplaySettingsTab.class::isInstance)
							.map(ItemDisplaySettingsTab.class::cast).anyMatch(tab -> tab.getSlotOverlayColor(arguments.slot(), false).isPresent());
				}
				if (context.minecraft().player.containerMenu instanceof StorageContainerMenuBase<?> menu) {
					boolean ghost = menu.getMemorizedStackInSlot(arguments.slot()).filter(stack -> stack.is(Items.DIAMOND)).isPresent();
					boolean displayOverlay = !arguments.display()
							|| wrapper != null && wrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).getSlotColor(arguments.slot())
									.filter(color -> menu.getSlotOverlayColors(arguments.slot()).contains(color)).isPresent();
					overlay = overlay && ghost && displayOverlay;
				}
				if (serverMatched && wrapper != null && matches(wrapper, arguments) && overlay) {
					return RegressionActionProgress.PASSED;
				}
				future = null;
				return RegressionActionProgress.WAITING;
			}
		};
	}
}
