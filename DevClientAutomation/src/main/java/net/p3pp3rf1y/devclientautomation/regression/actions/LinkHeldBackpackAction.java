package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class LinkHeldBackpackAction extends TypedRegressionAction<LinkHeldBackpackAction.Arguments> {
	public record Arguments(String linkerRef) {
	}

	public LinkHeldBackpackAction() {
		super("backpack.linkHeld", "1", Set.of("minecraft:client", "sophisticatedbackpacks"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("linkerRef"), description);
		return new Arguments(optionalString(args, "linkerRef", "", description));
	}

	@Override
	protected RegressionValueContract values(Arguments arguments) {
		return arguments.linkerRef().isEmpty()
				? RegressionValueContract.EMPTY
				: RegressionValueContract.consuming(arguments.linkerRef(), RegressionValueTypes.ITEM_STACK);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Nullable
			private CompletableFuture<Void> future;
			private int deadline = -1;

			@Override
			public RegressionActionProgress tick() {
				if (deadline < 0) {
					deadline = context.minecraft().player.tickCount + 100;
				}
				if (future == null) {
					future = context.onServer(player -> {
						ItemStack backpack = player.getMainHandItem();
						if (!(backpack.getItem() instanceof BackpackItem)) {
							throw new IllegalStateException("Main hand does not contain a backpack");
						}
						ItemStack linker = arguments.linkerRef().isEmpty()
								? new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("sophisticatedcore:ender_linker"))
										.orElseThrow(() -> new IllegalStateException("Ender linker item is unavailable")))
								: context.values().require(arguments.linkerRef(), ItemStack.class);
						if (!LinkedStorageService.link((ServerLevel) player.level(), player.getUUID(), linker, backpack)) {
							throw new IllegalStateException("Could not link held backpack");
						}
						player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
						player.getInventory().setChanged();
						player.inventoryMenu.broadcastFullState();
					});
				}
				RegressionActionProgress progress = await(future);
				if (progress != RegressionActionProgress.PASSED) {
					return progress;
				}
				if (context.minecraft().player.getMainHandItem().has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT)) {
					return RegressionActionProgress.PASSED;
				}
				return context.minecraft().player.tickCount >= deadline
						? fail("Linked backpack endpoint did not synchronize to client")
						: RegressionActionProgress.WAITING;
			}
		};
	}
}
