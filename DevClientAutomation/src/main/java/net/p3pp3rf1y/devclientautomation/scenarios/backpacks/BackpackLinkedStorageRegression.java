package net.p3pp3rf1y.devclientautomation.scenarios.backpacks;

import com.sun.net.httpserver.HttpExchange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.devclientautomation.DevClientAutomation;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.LinkedStorageBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackSettingsScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedbackpacks.crafting.BackpackDyeRecipe;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenMessage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageService;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ControllerBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.requireMethod;
import static net.p3pp3rf1y.devclientautomation.bridge.HttpJson.sendJsonHandling;

/** Exercises the target linked-storage service through real integrated-server endpoint stacks. */
public final class BackpackLinkedStorageRegression {
	private static final Logger LOGGER = LoggerFactory.getLogger(DevClientAutomation.MOD_ID);
	private static final int CANONICAL_INVENTORY_SLOTS = 81;
	private static final int CANONICAL_UPGRADE_SLOTS = 3;
	private static final int CANONICAL_COLUMNS_TAKEN = 2;
	private static final int PRIMARY_INVENTORY_SLOT = 0;
	private static final int RELOCATED_PRIMARY_INVENTORY_SLOT = 1;
	private static final int STASH_INVENTORY_SLOT = 9;
	private static final int STASH_COUNT = 13;
	private static final int FEEDBACK_INVENTORY_SLOT = 10;
	private static final int PLACED_INTERACTION_SLOT = 2;
	private static final int TEST_STORAGE_SOURCE_SLOT = 1;
	private static final int TEST_STORAGE_TARGET_SLOT = 3;
	private static final int INCEPTION_LINKED_CHILD_SLOT = 0;
	private static final int INCEPTION_MOVED_CHILD_INVENTORY_SLOT = 1;
	private static final int INCEPTION_LINKED_CHILD_MARKER_COUNT = 5;

	private BackpackLinkedStorageRegression() {
	}

	public static void handle(HttpExchange exchange) throws IOException {
		requireMethod(exchange, "POST");
		sendJsonHandling(exchange, LOGGER, BackpackLinkedStorageRegression::run);
	}

	private static String run() {
		if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof DeathScreen)) {
			AutomationRuntime.runOnClient(() -> {
				Minecraft.getInstance().player.respawn();
				return null;
			});
		}
		waitForClientPlayerRecovery();
		AutomationRuntime.runOnServer(BackpackLinkedStorageRegression::preparePlayerForFixture);
		LinkedStorageRegressionFixture fixture = AutomationRuntime.runOnServer(BackpackLinkedStorageRegression::setup);
		waitForClientPrimaryEndpoint(fixture.primaryEndpointId());
		EndpointClientLayout primaryLayout = openAndAwaitEndpoint(fixture, false);
		assertVisibleStorageProfile(primaryLayout, fixture.canonicalLayout(), "primary");
		int firstItemChangeTick = moveTestItemIntoLinkedStorage(fixture);
		waitForServerTestItem(fixture);
		waitForClientTestItem(fixture, firstItemChangeTick);
		exercisePrimarySettingsRoundTrip(fixture);
		fixture = relocatePrimaryCarrier(fixture);
		assertVisibleStorageProfile(openAndAwaitEndpoint(fixture, false), fixture.canonicalLayout(), "relocated primary");
		exerciseRelocatedStorageSlotIdentityStability(fixture);
		exercisePrimaryTankUpgradeThroughScreen(fixture);
		stashIntoLinkedBackpackWithInventoryClick(fixture);
		waitForServerStash(fixture);
		waitForClientStashConvergence(fixture);
		EndpointClientLayout placedLayout = openAndAwaitEndpoint(fixture, true);
		assertVisibleStorageProfile(placedLayout, fixture.canonicalLayout(), "placed");
		exercisePlacedTankUpgradeThroughScreen(fixture);
		exerciseLinkedChildInPlacedParent(fixture);
		exerciseInceptionLinkedChildPersistenceRegression();
		runLinkedControllerEndpointJoinDoesNotDuplicateCanonicalContentsRegression();
		return "{\"ok\":true,\"groupId\":\"" + fixture.groupId() + "\",\"primaryEndpointId\":\"" + fixture.primaryEndpointId() + "\",\"secondaryEndpointId\":\""
				+ fixture.secondaryEndpointId() + "\",\"canonicalContents\":true,\"endpointCopy\":true,\"clientMenusOpened\":true,\"placedColumnUpgrade\":true"
				+ ",\"inventoryClickStash\":true,\"stashCount\":" + STASH_COUNT + ",\"visibleStorageSlots\":" + fixture.canonicalLayout().storageSlots()
				+ ",\"primaryTankUiCycles\":2,\"primarySettingsRoundTrip\":true,\"primaryClientPhysicalProjectionSnapshots\":true"
				+ ",\"linkedItemDisplayProjection\":true"
				+ ",\"immediateAndDelayedFirstItemFeedback\":true,\"relocatedTankSlotIdentitiesStableDuringMoves\":true"
				+ ",\"placedTankUiCycles\":2,\"physicalProjectionSnapshots\":true,\"carrierRelocation\":true,\"nestedBlockChild\":true"
				+ ",\"nestedChildItemDisplayProjection\":true"
				+ ",\"inceptionLinkedChildPersistence\":true,\"inceptionMovedLinkedChildDoesNotDuplicate\":true,\"groupRevision\":" + fixture.groupRevision()
				+ ",\"linkedControllerEndpointJoinKeepsOneCanonicalContentIndex\":true" + "}";
	}

	private static void runLinkedControllerEndpointJoinDoesNotDuplicateCanonicalContentsRegression() {
		AutomationRuntime.runOnServer(player -> {
			ServerLevel level = player.serverLevel();
			BlockPos controllerPos = player.blockPosition().offset(0, 0, -24);
			BlockPos storagePos = controllerPos.east();
			BlockPos existingEndpointPos = controllerPos.above();
			BlockPos joiningEndpointPos = storagePos.above();
			clearControllerLinkedStorageFixture(level, controllerPos);
			try {
				var barrelBlock = net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL.get();
				level.setBlockAndUpdate(storagePos, barrelBlock.defaultBlockState());
				barrelBlock.setPlacedBy(level, storagePos, level.getBlockState(storagePos), player,
						new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL_ITEM.get()));
				placeControllerLinkedStorageBackpack(level, existingEndpointPos, player, "existing endpoint");
				placeControllerLinkedStorageBackpack(level, joiningEndpointPos, player, "joining endpoint");
				var controllerBlock = net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CONTROLLER.get();
				level.setBlockAndUpdate(controllerPos, controllerBlock.defaultBlockState());
				controllerBlock.setPlacedBy(level, controllerPos, level.getBlockState(controllerPos), player,
						new ItemStack(net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CONTROLLER_ITEM.get()));
				ControllerBlockEntity controller = level
						.getBlockEntity(controllerPos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get())
						.orElseThrow(() -> new IllegalStateException("Missing controller for linked Backpack endpoint join regression"));
				BackpackBlockEntity existingEndpoint = getControllerLinkedStorageBackpack(level, existingEndpointPos, "existing endpoint");
				BackpackBlockEntity joiningEndpoint = getControllerLinkedStorageBackpack(level, joiningEndpointPos, "joining endpoint");
				int canonicalSlots = existingEndpoint.getStorageWrapper().getInventoryHandler().getSlots();
				int bridgeStorageSlots = level.getBlockEntity(storagePos, net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get())
						.map(storage -> storage.getStorageWrapper().getInventoryHandler().getSlots())
						.orElseThrow(() -> new IllegalStateException("Missing bridge barrel for linked Backpack endpoint join regression"));
				assertTrue(controller.getStoragePositions().size() == 3 && controller.getSlots() == canonicalSlots * 2 + bridgeStorageSlots,
						"Controller did not register both unlinked Backpacks through the bridge barrel before linking");
				existingEndpoint.getBackpackWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
				existingEndpoint.getBackpackWrapper().getInventoryHandler().saveInventory();
				ItemStackKey diamondKey = ItemStackKey.of(new ItemStack(Items.DIAMOND));
				assertTrue(
						controller.extractItem(new ItemStack(Items.DIAMOND, 64), true).getCount() == 7 && controller.getStackStorages(diamondKey).size() == 1,
						"Controller did not track the unlinked Backpack contents before linking");

				ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
				assertTrue(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, existingEndpoint) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not create controller-connected linked Backpack group");
				assertTrue(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, joiningEndpoint) == LinkedStorageService.LinkResult.SUCCESS,
						"Could not add a controller-connected Backpack to its existing linked-storage group");
				LinkedStorageEndpointData existingData = requireEndpoint(existingEndpoint.getBackpackWrapper().getBackpack(), "existing endpoint");
				assertTrue(existingData.groupId().equals(requireEndpoint(joiningEndpoint.getBackpackWrapper().getBackpack(), "joining endpoint").groupId()),
						"Controller-connected Backpacks did not join the same linked-storage group");
				assertTrue(
						controller.getStoragePositions().size() == 2 && controller.getSlots() == canonicalSlots + bridgeStorageSlots
								&& controller.extractItem(new ItemStack(Items.DIAMOND, 64), true).getCount() == 7
								&& controller.getStackStorages(diamondKey).size() == 1,
						"Adding a controller-connected Backpack endpoint to an existing linked group duplicated canonical contents: positions="
								+ controller.getStoragePositions() + ", slots=" + controller.getSlots() + ", diamonds="
								+ controller.extractItem(new ItemStack(Items.DIAMOND, 64), true).getCount() + ", indexed="
								+ controller.getStackStorages(diamondKey));
				return null;
			} finally {
				clearControllerLinkedStorageFixture(level, controllerPos);
			}
		});
	}

	private static void placeControllerLinkedStorageBackpack(ServerLevel level, BlockPos pos, ServerPlayer player, String name) {
		level.setBlockAndUpdate(pos, ModBlocks.GOLD_BACKPACK.get().defaultBlockState());
		getControllerLinkedStorageBackpack(level, pos, name).setBackpack(new ItemStack(ModItems.GOLD_BACKPACK.get()));
	}

	private static BackpackBlockEntity getControllerLinkedStorageBackpack(ServerLevel level, BlockPos pos, String name) {
		return WorldHelper.getBlockEntity(level, pos, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Missing controller-linked Backpack " + name));
	}

	private static void clearControllerLinkedStorageFixture(ServerLevel level, BlockPos center) {
		for (int x = -2; x <= 3; x++) {
			for (int y = -1; y <= 3; y++) {
				for (int z = -2; z <= 2; z++) {
					BlockPos pos = center.offset(x, y, z);
					if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
						storage.clearContent();
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static LinkedStorageRegressionFixture setup(ServerPlayer player) {
		moveToSafeFixtureHeight(player);
		ServerLevel level = player.serverLevel();
		BlockPos placedEndpointPos = fixtureOrigin(player).relative(player.getDirection(), 2);
		player.closeContainer();
		player.getInventory().clearContent();
		level.setBlock(placedEndpointPos, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(placedEndpointPos.below(), Blocks.DIRT.defaultBlockState(), 3);
		ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get(), 2);
		ItemStack primary = createColumnBackpack();
		primary.getCapability(CapabilityBackpackWrapper.getCapabilityInstance())
				.orElseThrow(() -> new IllegalStateException("Could not initialize the ordinary Backpack capability before linking"));
		assertTrue(LinkedStorageService.link(level, player.getUUID(), linker, primary), "Could not create linked Backpack group");

		LinkedStorageEndpointData primaryEndpoint = requireEndpoint(primary, "primary");
		BackpackBlockEntity placedBackpack = placeBackpackAsPlayer(level, player, placedEndpointPos, new ItemStack(ModItems.GOLD_BACKPACK.get()),
				"secondary endpoint");
		assertTrue(LinkedStorageService.linkWithResult(level, player.getUUID(), linker, placedBackpack) == LinkedStorageService.LinkResult.SUCCESS,
				"Could not add placed secondary linked Backpack endpoint");
		ItemStack secondary = placedBackpack.getBackpackWrapper().getBackpack();
		LinkedStorageEndpointData secondaryEndpoint = requireEndpoint(secondary, "placed secondary");
		assertTrue(primaryEndpoint.groupId().equals(secondaryEndpoint.groupId()), "Endpoints did not share a linked storage group");
		assertTrue(!primaryEndpoint.endpointId().equals(secondaryEndpoint.endpointId()), "Linked endpoints reused an endpoint identity");
		primary = dyeLinkedPrimaryEndpoint(level, primary, primaryEndpoint);

		LinkedStorageGroupManager groups = LinkedStorageGroupsSavedData.get(level).manager();
		assertTrue(groups.isPrimaryEndpoint(primaryEndpoint.groupId(), primaryEndpoint.endpointId()), "Primary endpoint was not registered");
		assertTrue(groups.isEndpointMember(secondaryEndpoint.groupId(), secondaryEndpoint.endpointId()), "Placed secondary endpoint was not registered");

		IBackpackWrapper primaryFacade = resolve(level, primary, "primary");
		IBackpackWrapper secondaryFacade = placedBackpack.getBackpackWrapper();
		try {
			long revision = groups.getRevision(primaryEndpoint.groupId());
			primaryFacade.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND, 7));
			assertTrue(
					secondaryFacade.getInventoryHandler().getStackInSlot(0).is(Items.DIAMOND)
							&& secondaryFacade.getInventoryHandler().getStackInSlot(0).getCount() == 7,
					"Secondary endpoint did not resolve canonical primary contents");
			assertTrue(groups.getRevision(primaryEndpoint.groupId()) > revision, "Canonical contents mutation did not advance the group revision");

			ItemStack copiedEndpoint = LinkedStorageService.createSecondaryEndpointCopy(level, secondary)
					.orElseThrow(() -> new IllegalStateException("Could not create linked endpoint copy"));
			LinkedStorageEndpointData copiedData = requireEndpoint(copiedEndpoint, "copied secondary");
			assertTrue(primaryEndpoint.groupId().equals(copiedData.groupId()) && groups.isEndpointMember(copiedData.groupId(), copiedData.endpointId()),
					"Copied endpoint was not registered in the canonical group");
			assertTrue(
					!BackpackLinkedStorageResolver.synchronizeRenderProjection(level, copiedEndpoint)
							|| LinkedStorageStackData.getRenderRevision(copiedEndpoint) == groups.getRenderRevision(copiedData.groupId()),
					"Copied endpoint render projection did not synchronize with the group revision");
			player.getInventory().setItem(PRIMARY_INVENTORY_SLOT, primary);
			player.getInventory().setItem(STASH_INVENTORY_SLOT, new ItemStack(Items.EMERALD, STASH_COUNT));
			player.getInventory().setItem(FEEDBACK_INVENTORY_SLOT, new ItemStack(Items.NETHER_STAR));
			player.getInventory().selected = PRIMARY_INVENTORY_SLOT;
			player.getInventory().setChanged();
			player.inventoryMenu.broadcastChanges();
			CanonicalLayout canonicalLayout = new CanonicalLayout(primaryFacade.getInventoryHandler().getSlots(), primaryFacade.getUpgradeHandler().getSlots(),
					primaryFacade.getColumnsTaken(), primaryFacade.getNumberOfSlotRows());
			assertTrue(canonicalLayout.columnsTaken() == CANONICAL_COLUMNS_TAKEN, "Canonical linked Backpack did not retain its column upgrade");
			assertTrue(secondaryFacade.getColumnsTaken() == CANONICAL_COLUMNS_TAKEN, "Placed linked Backpack did not resolve the canonical column upgrade");
			return new LinkedStorageRegressionFixture(primaryEndpoint.groupId(), primaryEndpoint.endpointId(), secondaryEndpoint.endpointId(),
					groups.getRevision(primaryEndpoint.groupId()), placedEndpointPos, canonicalLayout, PRIMARY_INVENTORY_SLOT);
		} finally {
			close(primaryFacade);
		}
	}

	private static ItemStack dyeLinkedPrimaryEndpoint(ServerLevel level, ItemStack primary, LinkedStorageEndpointData primaryEndpoint) {
		CraftingContainer input = new TransientCraftingContainer(new AbstractContainerMenu(null, 0) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			@Override
			public boolean stillValid(Player player) {
				return false;
			}
		}, 3, 1);
		input.setItem(0, new ItemStack(Items.RED_DYE));
		input.setItem(1, primary);
		input.setItem(2, new ItemStack(Items.BLUE_DYE));
		BackpackDyeRecipe recipe = new BackpackDyeRecipe(new ResourceLocation(DevClientAutomation.MOD_ID, "linked_backpack_dye"), CraftingBookCategory.MISC);
		assertTrue(recipe.matches(input, level), "Linked Backpack dye recipe did not match the production crafting container");

		ItemStack dyed = recipe.assemble(input, level.registryAccess());
		int expectedMainColor = ColorHelper.calculateColor(BackpackWrapper.DEFAULT_CLOTH_COLOR, BackpackWrapper.DEFAULT_CLOTH_COLOR, List.of(DyeColor.RED));
		int expectedAccentColor = ColorHelper.calculateColor(BackpackWrapper.DEFAULT_BORDER_COLOR, BackpackWrapper.DEFAULT_BORDER_COLOR,
				List.of(DyeColor.BLUE));
		IBackpackWrapper dyedWrapper = dyed.getCapability(CapabilityBackpackWrapper.getCapabilityInstance())
				.orElseThrow(() -> new IllegalStateException("Dyed linked Backpack did not attach its wrapper capability"));
		LinkedStorageEndpointData dyedEndpoint = requireEndpoint(dyed, "dyed primary");
		assertTrue(primaryEndpoint.groupId().equals(dyedEndpoint.groupId()) && primaryEndpoint.endpointId().equals(dyedEndpoint.endpointId()),
				"Dyeing changed the linked Backpack group or endpoint identity");
		assertTrue(dyedWrapper.getMainColor() == expectedMainColor && dyedWrapper.getAccentColor() == expectedAccentColor,
				"Dyeing did not apply the expected linked Backpack colors");
		return dyed;
	}

	private static ItemStack createColumnBackpack() {
		ItemStack backpack = new ItemStack(ModItems.GOLD_BACKPACK.get());
		IBackpackWrapper wrapper = new BackpackWrapper(backpack);
		wrapper.setContentsUuid(UUID.randomUUID());
		wrapper.setSlotNumbers(CANONICAL_INVENTORY_SLOTS, CANONICAL_UPGRADE_SLOTS);
		wrapper.getInventoryHandler();
		wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.TANK_UPGRADE.get()));
		wrapper.getUpgradeHandler().saveInventory();
		wrapper.setColumnsTaken(CANONICAL_COLUMNS_TAKEN, false);
		wrapper.onContentsNbtUpdated();
		return backpack;
	}

	private static BlockPos fixtureOrigin(ServerPlayer player) {
		BlockPos position = player.blockPosition();
		return new BlockPos(position.getX(), Math.max(80, position.getY()), position.getZ());
	}

	private static Void preparePlayerForFixture(ServerPlayer player) {
		if (!player.isAlive()) {
			throw new IllegalStateException("Server player did not recover from the void before linked Backpack fixture setup");
		}
		moveToSafeFixtureHeight(player);
		return null;
	}

	private static void waitForClientPlayerRecovery() {
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.player != null && minecraft.player.isAlive() && !(minecraft.screen instanceof DeathScreen);
		}, "Client player did not recover from the void before linked Backpack fixture setup");
	}

	private static void moveToSafeFixtureHeight(ServerPlayer player) {
		if (player.blockPosition().getY() >= 64) {
			return;
		}
		Vec3 position = player.position();
		if (!player.teleportTo(player.serverLevel(), position.x, 80, position.z, Set.of(), player.getYRot(), player.getXRot())) {
			throw new IllegalStateException("Could not move player to a safe linked Backpack fixture height");
		}
		// The 1.20 automation world can be void at the recovered position; retain a support block for client menu interactions.
		player.serverLevel().setBlock(player.blockPosition().below(), Blocks.DIRT.defaultBlockState(), 3);
	}

	private static BackpackBlockEntity placeBackpackAsPlayer(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack, String name) {
		ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
		try {
			level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			player.setShiftKeyDown(true);
			InteractionResult result;
			try {
				result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
			} finally {
				player.setShiftKeyDown(false);
			}
			assertTrue(result.consumesAction(), "Could not place " + name + " Backpack endpoint: " + result);
			return WorldHelper.getBlockEntity(level, pos, BackpackBlockEntity.class)
					.orElseThrow(() -> new IllegalStateException("Could not place " + name + " Backpack endpoint"));
		} finally {
			player.setItemInHand(InteractionHand.MAIN_HAND, held);
			player.getInventory().setChanged();
		}
	}

	private static void waitForClientPrimaryEndpoint(UUID endpointId) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.player != null
						&& Optional.ofNullable(LinkedStorageStackData.getEndpoint(minecraft.player.getInventory().getItem(PRIMARY_INVENTORY_SLOT)))
								.map(LinkedStorageEndpointData::endpointId).filter(endpointId::equals).isPresent();
			})) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the client primary linked Backpack endpoint");
	}

	private static LinkedStorageRegressionFixture relocatePrimaryCarrier(LinkedStorageRegressionFixture fixture) {
		closePrimaryEndpoint();
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
				throw new IllegalStateException("Client inventory menu was not ready to relocate the linked Backpack");
			}

			InventoryMenu menu = minecraft.player.inventoryMenu;
			int sourceSlot = inventoryMenuSlot(fixture.primaryInventorySlot());
			int targetSlot = inventoryMenuSlot(RELOCATED_PRIMARY_INVENTORY_SLOT);
			assertTrue(menu.getSlot(targetSlot).getItem().isEmpty(), "Linked Backpack relocation target was not empty");
			minecraft.gameMode.handleInventoryMouseClick(menu.containerId, sourceSlot, 0, ClickType.PICKUP, minecraft.player);
			minecraft.gameMode.handleInventoryMouseClick(menu.containerId, targetSlot, 0, ClickType.PICKUP, minecraft.player);
			return null;
		});

		LinkedStorageRegressionFixture relocatedFixture = fixture.withPrimaryInventorySlot(RELOCATED_PRIMARY_INVENTORY_SLOT);
		waitForServerCondition(player -> relocatedCarrierMatches(player, relocatedFixture),
				"Server did not preserve the linked Backpack canonical contents and endpoint identity after relocation");
		waitForClientCondition(() -> clientRelocatedCarrierMatches(relocatedFixture), "Client did not synchronize the linked Backpack relocation");
		return relocatedFixture;
	}

	private static int moveTestItemIntoLinkedStorage(LinkedStorageRegressionFixture fixture) {
		return AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			BackpackContainer menu = getOpenPrimaryMenu(fixture);
			Slot targetSlot = menu.getSlot(TEST_STORAGE_SOURCE_SLOT);
			assertTrue(targetSlot.getItem().isEmpty(), "Client feedback linked-storage slot was not empty");
			for (int slotIndex = menu.getNumberOfStorageInventorySlots(); slotIndex < menu.getInventorySlotsSize(); slotIndex++) {
				Slot playerSlot = menu.getSlot(slotIndex);
				if (playerSlot.getItem().is(Items.NETHER_STAR)) {
					clickScreenSlot((BackpackScreen) minecraft.screen, playerSlot, "client feedback Nether Star source");
					assertStack(menu.getCarried(), Items.NETHER_STAR, 1, "Client feedback item click did not immediately pick up the Nether Star");
					clickScreenSlot((BackpackScreen) minecraft.screen, targetSlot, "client feedback linked storage target");
					assertTrue(targetSlot.getItem().is(Items.NETHER_STAR) && targetSlot.getItem().getCount() == 1 && menu.getCarried().isEmpty(),
							"First linked-storage item change was not immediately visible");
					return minecraft.player.tickCount;
				}
			}
			throw new IllegalStateException("Client feedback Nether Star was unavailable in the player inventory");
		});
	}

	private static void waitForServerTestItem(LinkedStorageRegressionFixture fixture) {
		waitForServerCondition(player -> {
			IBackpackWrapper canonical = BackpackLinkedStorageResolver
					.resolveCanonicalHost(player.serverLevel(), player.getInventory().getItem(fixture.primaryInventorySlot())).orElse(null);
			if (canonical == null) {
				return false;
			}
			try {
				ItemStack target = canonical.getInventoryHandler().getStackInSlot(TEST_STORAGE_SOURCE_SLOT);
				return target.is(Items.NETHER_STAR) && target.getCount() == 1 && countItems(canonical, Items.NETHER_STAR) == 1
						&& countPlayerItems(player, Items.NETHER_STAR) == 0 && player.containerMenu.getCarried().isEmpty();
			} finally {
				close(canonical);
			}
		}, "Server did not apply the first linked-storage item change");
	}

	private static void waitForClientTestItem(LinkedStorageRegressionFixture fixture, int firstItemChangeTick) {
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.player.tickCount < firstItemChangeTick + 2) {
				return false;
			}
			BackpackContainer menu = getOpenPrimaryMenu(fixture);
			Slot target = menu.getSlot(TEST_STORAGE_SOURCE_SLOT);
			return target.getItem().is(Items.NETHER_STAR) && target.getItem().getCount() == 1 && menu.getCarried().isEmpty();
		}, "Client first linked-storage item change did not remain visible after two ticks");
	}

	private static void exerciseRelocatedStorageSlotIdentityStability(LinkedStorageRegressionFixture fixture) {
		// Reopening recreates the menu; identity must remain stable within that relocated menu while contents change.
		TankStorageSlots slots = AutomationRuntime.runOnClient(() -> {
			BackpackContainer menu = getOpenPrimaryMenu(fixture);
			return new TankStorageSlots(menu.getSlot(TEST_STORAGE_SOURCE_SLOT), menu.getSlot(TEST_STORAGE_TARGET_SLOT));
		});
		for (int move = 0; move < 4; move++) {
			boolean itemInTarget = move % 2 == 0;
			int moveTick = moveRelocatedStorageItem(fixture, slots, itemInTarget);
			waitForRelocatedServerStorageItem(fixture, itemInTarget);
			waitForRelocatedClientStorageItem(fixture, slots, itemInTarget, moveTick);
		}
	}

	private static int moveRelocatedStorageItem(LinkedStorageRegressionFixture fixture, TankStorageSlots slots, boolean itemInTarget) {
		return AutomationRuntime.runOnClient(() -> {
			BackpackContainer menu = getOpenPrimaryMenu(fixture);
			assertStorageSlotIdentities(menu, slots);
			Slot source = itemInTarget ? slots.source() : slots.target();
			Slot target = itemInTarget ? slots.target() : slots.source();
			assertTrue(source.getItem().is(Items.NETHER_STAR) && target.getItem().isEmpty(), "Relocated Backpack storage move started with wrong contents");
			clickScreenSlot((BackpackScreen) Minecraft.getInstance().screen, source, "relocated storage source");
			clickScreenSlot((BackpackScreen) Minecraft.getInstance().screen, target, "relocated storage target");
			assertStorageSlotIdentities(getOpenPrimaryMenu(fixture), slots);
			assertTrue(target.getItem().is(Items.NETHER_STAR) && source.getItem().isEmpty() && menu.getCarried().isEmpty(),
					"Relocated Backpack storage move did not immediately update the client");
			return Minecraft.getInstance().player.tickCount;
		});
	}

	private static void assertStorageSlotIdentities(BackpackContainer menu, TankStorageSlots slots) {
		assertTrue(menu.getSlot(TEST_STORAGE_SOURCE_SLOT) == slots.source() && menu.getSlot(TEST_STORAGE_TARGET_SLOT) == slots.target(),
				"Relocated Backpack item moves replaced Tank storage Slot instances");
	}

	private static void waitForRelocatedServerStorageItem(LinkedStorageRegressionFixture fixture, boolean itemInTarget) {
		waitForServerCondition(player -> {
			IBackpackWrapper canonical = BackpackLinkedStorageResolver
					.resolveCanonicalHost(player.serverLevel(), player.getInventory().getItem(fixture.primaryInventorySlot())).orElse(null);
			if (canonical == null) {
				return false;
			}
			try {
				ItemStack item = canonical.getInventoryHandler().getStackInSlot(itemInTarget ? TEST_STORAGE_TARGET_SLOT : TEST_STORAGE_SOURCE_SLOT);
				ItemStack empty = canonical.getInventoryHandler().getStackInSlot(itemInTarget ? TEST_STORAGE_SOURCE_SLOT : TEST_STORAGE_TARGET_SLOT);
				return item.is(Items.NETHER_STAR) && empty.isEmpty();
			} finally {
				close(canonical);
			}
		}, "Server did not apply relocated Backpack storage move");
	}

	private static void waitForRelocatedClientStorageItem(LinkedStorageRegressionFixture fixture, TankStorageSlots slots, boolean itemInTarget, int moveTick) {
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.player.tickCount < moveTick + 2) {
				return false;
			}
			BackpackContainer menu = getOpenPrimaryMenu(fixture);
			assertStorageSlotIdentities(menu, slots);
			Slot item = itemInTarget ? slots.target() : slots.source();
			Slot empty = itemInTarget ? slots.source() : slots.target();
			return item.getItem().is(Items.NETHER_STAR) && empty.getItem().isEmpty() && menu.getCarried().isEmpty()
					&& menu.getStorageWrapper().getColumnsTaken() == fixture.canonicalLayout().columnsTaken();
		}, "Client relocated Backpack storage move did not converge after two ticks");
	}

	private static boolean relocatedCarrierMatches(ServerPlayer player, LinkedStorageRegressionFixture fixture) {
		ItemStack source = player.getInventory().getItem(PRIMARY_INVENTORY_SLOT);
		ItemStack relocated = player.getInventory().getItem(fixture.primaryInventorySlot());
		return source.isEmpty()
				&& Optional.ofNullable(LinkedStorageStackData.getEndpoint(relocated)).map(LinkedStorageEndpointData::endpointId)
						.filter(fixture.primaryEndpointId()::equals).isPresent()
				&& BackpackLinkedStorageResolver.resolveCanonicalHost(player.serverLevel(), relocated)
						.map(canonical -> canonical.getInventoryHandler().getStackInSlot(0).is(Items.DIAMOND)).orElse(false);
	}

	private static boolean clientRelocatedCarrierMatches(LinkedStorageRegressionFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
			return false;
		}

		InventoryMenu menu = minecraft.player.inventoryMenu;
		ItemStack relocated = menu.getSlot(inventoryMenuSlot(fixture.primaryInventorySlot())).getItem();
		return menu.getSlot(inventoryMenuSlot(PRIMARY_INVENTORY_SLOT)).getItem().isEmpty() && menu.getCarried().isEmpty()
				&& Optional.ofNullable(LinkedStorageStackData.getEndpoint(relocated)).map(LinkedStorageEndpointData::endpointId)
						.filter(fixture.primaryEndpointId()::equals).isPresent();
	}

	private static EndpointClientLayout openAndAwaitEndpoint(LinkedStorageRegressionFixture fixture, boolean placed) {
		int containerId;
		if (placed) {
			// Match the production client-feedback sequence before sending its single block interaction.
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().selected = PLACED_INTERACTION_SLOT;
				player.connection.send(new ClientboundSetCarriedItemPacket(PLACED_INTERACTION_SLOT));
				return null;
			});
			waitForPlacedInteractionReadiness(fixture);
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.getMainHandItem().isEmpty()) {
					throw new IllegalStateException("Placed linked Backpack interaction hand is unavailable");
				}
				minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(fixture.placedEndpointPos()), Direction.UP, fixture.placedEndpointPos(), false));
				return null;
			});
			containerId = -1;
		} else {
			AutomationRuntime.runOnServer(player -> {
				player.getInventory().selected = fixture.primaryInventorySlot();
				player.connection.send(new ClientboundSetCarriedItemPacket(fixture.primaryInventorySlot()));
				return null;
			});
			waitForPrimaryInteractionReadiness(fixture);
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null || minecraft.gameMode == null
						|| !fixture.primaryEndpointId().equals(Optional.ofNullable(LinkedStorageStackData.getEndpoint(minecraft.player.getMainHandItem()))
								.map(LinkedStorageEndpointData::endpointId).orElse(null))) {
					throw new IllegalStateException("Primary linked Backpack interaction hand is unavailable");
				}
				minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
				return null;
			});
			containerId = -1;
		}
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			Optional<EndpointClientLayout> layout = AutomationRuntime
					.runOnClient(() -> getOpenEndpointLayout(containerId, fixture.placedEndpointPos(), placed));
			if (layout.isPresent()) {
				return layout.get();
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(
				"Timed out waiting for " + (placed ? "placed" : "primary") + " linked Backpack screen to open; " + getPlacedInteractionSnapshot(fixture));
	}

	private static void waitForPrimaryInteractionReadiness(LinkedStorageRegressionFixture fixture) {
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.player != null && minecraft.player.containerMenu == minecraft.player.inventoryMenu
					&& minecraft.player.getInventory().selected == fixture.primaryInventorySlot()
					&& fixture.primaryEndpointId().equals(Optional.ofNullable(LinkedStorageStackData.getEndpoint(minecraft.player.getMainHandItem()))
							.map(LinkedStorageEndpointData::endpointId).orElse(null));
		}, "Timed out waiting for primary linked Backpack interaction readiness");
	}

	private static void waitForPlacedInteractionReadiness(LinkedStorageRegressionFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean serverReady = AutomationRuntime.runOnServer(player -> serverPlacedInteractionReady(player, fixture));
			boolean clientReady = AutomationRuntime.runOnClient(() -> clientPlacedInteractionReady(fixture));
			if (serverReady && clientReady) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for placed linked Backpack interaction readiness; " + getPlacedInteractionSnapshot(fixture));
	}

	private static boolean serverPlacedInteractionReady(ServerPlayer player, LinkedStorageRegressionFixture fixture) {
		return player.isAlive() && player.containerMenu == player.inventoryMenu && player.getInventory().selected == PLACED_INTERACTION_SLOT
				&& player.getMainHandItem().isEmpty() && player.distanceToSqr(Vec3.atCenterOf(fixture.placedEndpointPos())) < 36D && hasExpectedPlacedEndpoint(
						WorldHelper.getBlockEntity(player.serverLevel(), fixture.placedEndpointPos(), BackpackBlockEntity.class).orElse(null), fixture);
	}

	private static boolean clientPlacedInteractionReady(LinkedStorageRegressionFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null && minecraft.level != null && minecraft.player.isAlive()
				&& minecraft.player.containerMenu == minecraft.player.inventoryMenu && minecraft.player.getInventory().selected == PLACED_INTERACTION_SLOT
				&& minecraft.player.getMainHandItem().isEmpty() && !(minecraft.screen instanceof BackpackScreen)
				&& !(minecraft.screen instanceof BackpackSettingsScreen) && minecraft.player.distanceToSqr(Vec3.atCenterOf(fixture.placedEndpointPos())) < 36D
				&& hasExpectedPlacedEndpoint(WorldHelper.getBlockEntity(minecraft.level, fixture.placedEndpointPos(), BackpackBlockEntity.class).orElse(null),
						fixture);
	}

	private static boolean hasExpectedPlacedEndpoint(BackpackBlockEntity backpack, LinkedStorageRegressionFixture fixture) {
		return backpack != null && Optional.ofNullable(LinkedStorageStackData.getEndpoint(backpack.getBackpackWrapper().getBackpack()))
				.map(endpoint -> endpoint.groupId().equals(fixture.groupId()) && endpoint.endpointId().equals(fixture.secondaryEndpointId())).orElse(false);
	}

	private static String getPlacedInteractionSnapshot(LinkedStorageRegressionFixture fixture) {
		String server = AutomationRuntime.runOnServer(player -> "position=" + player.position() + ", distance="
				+ player.distanceToSqr(Vec3.atCenterOf(fixture.placedEndpointPos())) + ", selected=" + player.getInventory().selected + ", mainHand="
				+ player.getMainHandItem() + ", container=" + player.containerMenu.getClass().getSimpleName() + "#" + player.containerMenu.containerId
				+ ", inventoryMenu=" + (player.containerMenu == player.inventoryMenu) + ", carried=" + player.containerMenu.getCarried() + ", block="
				+ player.serverLevel().getBlockState(fixture.placedEndpointPos()) + ", blockEntity=" + placedEndpointSnapshot(
						WorldHelper.getBlockEntity(player.serverLevel(), fixture.placedEndpointPos(), BackpackBlockEntity.class).orElse(null)));
		String client = AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.level == null) {
				return "playerOrLevel=unavailable, screen=" + className(minecraft.screen);
			}
			return "position=" + minecraft.player.position() + ", distance=" + minecraft.player.distanceToSqr(Vec3.atCenterOf(fixture.placedEndpointPos()))
					+ ", selected=" + minecraft.player.getInventory().selected + ", mainHand=" + minecraft.player.getMainHandItem() + ", screen="
					+ className(minecraft.screen) + ", container=" + minecraft.player.containerMenu.getClass().getSimpleName() + "#"
					+ minecraft.player.containerMenu.containerId + ", inventoryMenu=" + (minecraft.player.containerMenu == minecraft.player.inventoryMenu)
					+ ", carried=" + minecraft.player.containerMenu.getCarried() + ", block=" + minecraft.level.getBlockState(fixture.placedEndpointPos())
					+ ", blockEntity="
					+ placedEndpointSnapshot(WorldHelper.getBlockEntity(minecraft.level, fixture.placedEndpointPos(), BackpackBlockEntity.class).orElse(null));
		});
		return "server={" + server + "}, client={" + client + "}";
	}

	private static String placedEndpointSnapshot(BackpackBlockEntity backpack) {
		if (backpack == null) {
			return "missing";
		}
		return backpack.getClass().getSimpleName() + ", endpoint=" + LinkedStorageStackData.getEndpoint(backpack.getBackpackWrapper().getBackpack());
	}

	private static String className(Object value) {
		return value == null ? "null" : value.getClass().getSimpleName();
	}

	private static Optional<EndpointClientLayout> getOpenEndpointLayout(int containerId, BlockPos placedEndpointPos, boolean placed) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof BackpackScreen) || minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackContainer menu)) {
			return Optional.empty();
		}
		boolean matchesEndpoint = placed ? menu.getBlockPosition().filter(placedEndpointPos::equals).isPresent() : menu.getBlockPosition().isEmpty();
		if ((containerId >= 0 && menu.containerId != containerId) || !matchesEndpoint) {
			return Optional.empty();
		}
		return Optional.of(new EndpointClientLayout(menu.getNumberOfStorageInventorySlots(), menu.getStorageWrapper().getInventoryHandler().getSlots(),
				menu.getNumberOfUpgradeSlots(), menu.getStorageWrapper().getColumnsTaken(), menu.getStorageWrapper().getNumberOfSlotRows()));
	}

	private static void assertVisibleStorageProfile(EndpointClientLayout layout, CanonicalLayout canonical, String endpoint) {
		assertTrue(layout.storageSlots() == canonical.storageSlots() && layout.inventoryHandlerSlots() == canonical.storageSlots()
				&& layout.upgradeSlots() == canonical.upgradeSlots() && layout.columnsTaken() == canonical.columnsTaken() && layout.rows() == canonical.rows(),
				endpoint + " linked Backpack client menu did not match the canonical storage layout");
	}

	private static void exercisePrimaryTankUpgradeThroughScreen(LinkedStorageRegressionFixture fixture) {
		for (int cycle = 1; cycle <= 2; cycle++) {
			clickPrimaryTankUpgrade(fixture, "remove cycle " + cycle);
			PrimaryServerSnapshot removed = waitForPrimaryServerSnapshot(fixture, false, "remove cycle " + cycle);
			waitForPrimaryClientSnapshot(fixture, removed, false, true, "remove cycle " + cycle);

			closePrimaryEndpoint();
			openAndAwaitEndpoint(fixture, false);
			clickPrimaryTankUpgrade(fixture, "insert cycle " + cycle);
			PrimaryServerSnapshot inserted = waitForPrimaryServerSnapshot(fixture, true, "insert cycle " + cycle);
			waitForPrimaryClientSnapshot(fixture, inserted, true, false, "insert cycle " + cycle);

			if (cycle == 1) {
				closePrimaryEndpoint();
				openAndAwaitEndpoint(fixture, false);
			}
		}
	}

	private static void clickPrimaryTankUpgrade(LinkedStorageRegressionFixture fixture, String phase) {
		AutomationRuntime.runOnClient(() -> {
			BackpackContainer menu = getOpenPrimaryMenu(fixture);
			Slot upgradeSlot = menu.upgradeSlots.get(1);
			if (!upgradeSlot.getItem().isEmpty()) {
				assertStack(upgradeSlot.getItem(), ModItems.TANK_UPGRADE.get(), 1, phase + " primary upgrade slot 1 did not contain Tank");
				assertTrue(menu.getCarried().isEmpty(), phase + " began while carrying an item");
				clickScreenSlot((BackpackScreen) Minecraft.getInstance().screen, upgradeSlot, phase + " primary Tank upgrade");
				return null;
			}

			assertTrue(menu.getCarried().isEmpty(), phase + " began while carrying an item");
			clickScreenSlot((BackpackScreen) Minecraft.getInstance().screen, findTankInPlayerInventory(menu, "primary"),
					phase + " primary Tank inventory slot");
			menu = getOpenPrimaryMenu(fixture); // The inventory pickup can rebuild the layout, so do not retain its prior slots.
			assertStack(menu.getCarried(), ModItems.TANK_UPGRADE.get(), 1, phase + " did not carry the Tank upgrade");
			upgradeSlot = menu.upgradeSlots.get(1);
			assertTrue(upgradeSlot.getItem().isEmpty(), phase + " primary upgrade slot 1 was not empty after the menu rebuild");
			clickScreenSlot((BackpackScreen) Minecraft.getInstance().screen, upgradeSlot, phase + " primary Tank upgrade");
			return null;
		});
	}

	private static void exercisePrimarySettingsRoundTrip(LinkedStorageRegressionFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			BackpackScreen screen = (BackpackScreen) Minecraft.getInstance().screen;
			StorageSettingsTab settingsTab = findChild(screen, StorageSettingsTab.class)
					.orElseThrow(() -> new IllegalStateException("Primary linked Backpack settings tab was not present"));
			settingsTab.mouseClicked(settingsTab.getX() + 9, settingsTab.getY() + 12, 0);
			return null;
		});

		waitForPrimarySettingsMenu(fixture);
		setPrimaryItemDisplaySelection(true);
		waitForLinkedItemDisplayProjection(fixture, true, "setting item display");
		setPrimaryItemDisplaySelection(false);
		waitForLinkedItemDisplayProjection(fixture, false, "clearing item display");
		closePrimarySettings();
		EndpointClientLayout reopened = openAndAwaitEndpoint(fixture, false);
		assertVisibleStorageProfile(reopened, fixture.canonicalLayout(), "primary after settings roundtrip");
	}

	private static void setPrimaryItemDisplaySelection(boolean selected) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu)) {
				throw new IllegalStateException("Primary linked Backpack settings menu is unavailable for item display selection");
			}
			final boolean[] updated = {false};
			settingsMenu.forEachSettingsContainer((name, container) -> {
				if (ItemDisplaySettingsCategory.NAME.equals(name) && container instanceof ItemDisplaySettingsContainer itemDisplaySettings) {
					if (selected) {
						itemDisplaySettings.selectSlot(0);
						itemDisplaySettings.setDisplaySide(DisplaySide.FRONT);
					} else {
						itemDisplaySettings.unselectSlot(0);
					}
					updated[0] = true;
				}
			});
			assertTrue(updated[0], "Primary linked Backpack item display settings container was unavailable");
			return null;
		});
	}

	private static void waitForLinkedItemDisplayProjection(LinkedStorageRegressionFixture fixture, boolean displayPresent, String phase) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean serverMatches = AutomationRuntime.runOnServer(player -> serverLinkedItemDisplayProjectionMatches(player, fixture, displayPresent));
			boolean clientMatches = AutomationRuntime.runOnClient(() -> clientPlacedItemDisplayProjectionMatches(fixture, displayPresent));
			if (serverMatches && clientMatches) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		assertTrue(AutomationRuntime.runOnServer(player -> serverLinkedItemDisplayProjectionMatches(player, fixture, displayPresent)),
				phase + " did not update the server canonical and physical item display projections");
		assertTrue(AutomationRuntime.runOnClient(() -> clientPlacedItemDisplayProjectionMatches(fixture, displayPresent)),
				phase + " did not update the client placed Backpack physical item display projection");
	}

	private static boolean serverLinkedItemDisplayProjectionMatches(ServerPlayer player, LinkedStorageRegressionFixture fixture, boolean displayPresent) {
		ItemStack primary = player.getInventory().getItem(fixture.primaryInventorySlot());
		IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.serverLevel(), primary).orElse(null);
		BackpackBlockEntity placedBackpack = WorldHelper.getBlockEntity(player.serverLevel(), fixture.placedEndpointPos(), BackpackBlockEntity.class)
				.orElse(null);
		if (canonical == null || placedBackpack == null) {
			return false;
		}

		IBackpackWrapper primaryPhysical = new BackpackWrapper(primary);
		ItemStack placedBackpackStack = placedBackpack.getBackpackWrapper().getBackpack();
		IBackpackWrapper placedPhysical = new BackpackWrapper(placedBackpackStack);
		ItemStack sharedPlacedBackpackStack = placedBackpackStack.copy();
		sharedPlacedBackpackStack.setTag(sharedPlacedBackpackStack.getItem().getShareTag(sharedPlacedBackpackStack));
		IBackpackWrapper sharedPlacedPhysical = new BackpackWrapper(sharedPlacedBackpackStack);
		return itemDisplayProjectionMatches(canonical, primaryPhysical, displayPresent)
				&& itemDisplayProjectionMatches(canonical, placedPhysical, displayPresent)
				&& itemDisplayProjectionMatches(canonical, sharedPlacedPhysical, displayPresent);
	}

	private static boolean clientPlacedItemDisplayProjectionMatches(LinkedStorageRegressionFixture fixture, boolean displayPresent) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu)
				|| Optional.ofNullable(LinkedStorageStackData.getEndpoint(settingsMenu.getStorageWrapper().getBackpack()))
						.map(LinkedStorageEndpointData::endpointId).filter(fixture.primaryEndpointId()::equals).isEmpty()) {
			return false;
		}

		return WorldHelper.getBlockEntity(minecraft.level, fixture.placedEndpointPos(), BackpackBlockEntity.class)
				.map(placedBackpack -> displayPresent
						? hasRenderedItem(placedBackpack, Items.DIAMOND)
						: placedBackpack.getBackpackWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().isEmpty())
				.orElse(false);
	}

	private static boolean hasRenderedItem(BackpackBlockEntity backpack, Item item) {
		return backpack.getBackpackWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().stream()
				.anyMatch(displayItem -> displayItem.getItem().is(item));
	}

	private static boolean itemDisplayProjectionMatches(IBackpackWrapper canonical, IBackpackWrapper physical, boolean displayPresent) {
		if (!displayPresent) {
			return canonical.getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().isEmpty()
					&& physical.getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().isEmpty();
		}
		if (!hasExpectedCanonicalDisplayItem(canonical)) {
			return false;
		}

		RenderInfo.DisplayItem canonicalDisplayItem = canonical.getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().get(0);
		if (physical.getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().size() != 1) {
			return false;
		}
		RenderInfo.DisplayItem physicalDisplayItem = physical.getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().get(0);
		return ItemStack.isSameItemSameTags(canonicalDisplayItem.getItem(), physicalDisplayItem.getItem())
				&& canonicalDisplayItem.getItem().getCount() == physicalDisplayItem.getItem().getCount()
				&& canonicalDisplayItem.getRotation() == physicalDisplayItem.getRotation()
				&& canonicalDisplayItem.getSlotIndex() == physicalDisplayItem.getSlotIndex()
				&& canonicalDisplayItem.getDisplaySide() == physicalDisplayItem.getDisplaySide()
				&& canonicalDisplayItem.getZOffset() == physicalDisplayItem.getZOffset();
	}

	private static boolean hasExpectedCanonicalDisplayItem(IBackpackWrapper canonical) {
		if (canonical.getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().size() != 1) {
			return false;
		}
		RenderInfo.DisplayItem displayItem = canonical.getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().get(0);
		return displayItem.getItem().is(Items.DIAMOND) && displayItem.getItem().getCount() == 1 && displayItem.getRotation() == 0
				&& displayItem.getSlotIndex() == 0 && displayItem.getDisplaySide() == DisplaySide.FRONT && displayItem.getZOffset() == 0;
	}

	private static void waitForPrimarySettingsMenu(LinkedStorageRegressionFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean serverReady = AutomationRuntime.runOnServer(player -> player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu
					&& matchesExpectedProfile(snapshot(settingsMenu.getStorageWrapper()), fixture, true));
			boolean clientReady = AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				return minecraft.screen instanceof BackpackSettingsScreen && minecraft.player != null
						&& minecraft.player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu
						&& matchesExpectedProfile(snapshot(settingsMenu.getStorageWrapper()), fixture, true);
			});
			if (serverReady && clientReady) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the primary linked Backpack settings screen");
	}

	private static void closePrimarySettings() {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof BackpackSettingsScreen screen) || minecraft.player == null
					|| !(minecraft.player.containerMenu instanceof BackpackSettingsContainerMenu)) {
				throw new IllegalStateException("Primary linked Backpack settings screen was not open before closing it");
			}
			screen.onClose();
			return null;
		});
		waitForClientAndServerInventoryMenu("primary linked Backpack settings screen");
	}

	private static void exercisePlacedTankUpgradeThroughScreen(LinkedStorageRegressionFixture fixture) {
		for (int cycle = 1; cycle <= 2; cycle++) {
			clickPlacedTankUpgrade(fixture, "remove cycle " + cycle);
			PlacedServerSnapshot removed = waitForPlacedServerSnapshot(fixture, false, "remove cycle " + cycle);
			waitForPlacedClientSnapshot(fixture, removed, false, true, "remove cycle " + cycle);

			closePlacedEndpoint();
			openAndAwaitEndpoint(fixture, true);
			clickPlacedTankUpgrade(fixture, "insert cycle " + cycle);
			PlacedServerSnapshot inserted = waitForPlacedServerSnapshot(fixture, true, "insert cycle " + cycle);
			waitForPlacedClientSnapshot(fixture, inserted, true, false, "insert cycle " + cycle);

			if (cycle == 1) {
				closePlacedEndpoint();
				openAndAwaitEndpoint(fixture, true);
			}
		}
	}

	private static void clickPlacedTankUpgrade(LinkedStorageRegressionFixture fixture, String phase) {
		AutomationRuntime.runOnClient(() -> {
			BackpackContainer menu = getOpenPlacedMenu(fixture);
			Slot upgradeSlot = menu.upgradeSlots.get(1);
			if (!upgradeSlot.getItem().isEmpty()) {
				assertStack(upgradeSlot.getItem(), ModItems.TANK_UPGRADE.get(), 1, phase + " upgrade slot 1 did not contain Tank");
				assertTrue(menu.getCarried().isEmpty(), phase + " began while carrying an item");
				clickScreenSlot((BackpackScreen) Minecraft.getInstance().screen, upgradeSlot, phase + " Tank upgrade");
				return null;
			}

			assertTrue(menu.getCarried().isEmpty(), phase + " began while carrying an item");
			clickScreenSlot((BackpackScreen) Minecraft.getInstance().screen, findTankInPlayerInventory(menu, "placed"), phase + " Tank inventory slot");
			menu = getOpenPlacedMenu(fixture); // The inventory pickup can rebuild the layout, so do not retain its prior slots.
			assertStack(menu.getCarried(), ModItems.TANK_UPGRADE.get(), 1, phase + " did not carry the Tank upgrade");
			upgradeSlot = menu.upgradeSlots.get(1);
			assertTrue(upgradeSlot.getItem().isEmpty(), phase + " upgrade slot 1 was not empty after the menu rebuild");
			clickScreenSlot((BackpackScreen) Minecraft.getInstance().screen, upgradeSlot, phase + " Tank upgrade");
			return null;
		});
	}

	private static Slot findTankInPlayerInventory(BackpackContainer menu, String endpoint) {
		for (int slotIndex = menu.getNumberOfStorageInventorySlots(); slotIndex < menu.getInventorySlotsSize(); slotIndex++) {
			Slot slot = menu.getSlot(slotIndex);
			if (slot.getItem().is(ModItems.TANK_UPGRADE.get())) {
				return slot;
			}
		}
		throw new IllegalStateException("Tank upgrade was not returned to the player inventory when the " + endpoint + " Backpack closed");
	}

	private static void clickScreenSlot(BackpackScreen screen, Slot slot, String description) {
		double x = screen.getGuiLeft() + slot.x + 8.0;
		double y = screen.getGuiTop() + slot.y + 8.0;
		assertTrue(screen.mouseClicked(x, y, 0), description + " click was not handled by BackpackScreen");
		screen.mouseReleased(x, y, 0);
	}

	private static BackpackContainer getOpenPlacedMenu(LinkedStorageRegressionFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof BackpackScreen) || minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
				|| menu.getBlockPosition().filter(fixture.placedEndpointPos()::equals).isEmpty()) {
			throw new IllegalStateException("Placed linked Backpack screen is not open");
		}
		return menu;
	}

	private static BackpackContainer getOpenPrimaryMenu(LinkedStorageRegressionFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof BackpackScreen) || minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
				|| menu.getBlockPosition().isPresent()) {
			throw new IllegalStateException("Primary linked Backpack screen is not open");
		}
		ItemStack primary = minecraft.player.getInventory().getItem(fixture.primaryInventorySlot());
		if (Optional.ofNullable(LinkedStorageStackData.getEndpoint(primary)).map(LinkedStorageEndpointData::endpointId)
				.filter(fixture.primaryEndpointId()::equals).isEmpty()) {
			throw new IllegalStateException("Open item Backpack menu was not the primary linked endpoint");
		}
		return menu;
	}

	private static void closePlacedEndpoint() {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof BackpackScreen screen) || minecraft.player == null
					|| !(minecraft.player.containerMenu instanceof BackpackContainer menu) || menu.getBlockPosition().isEmpty()) {
				throw new IllegalStateException("Placed linked Backpack screen was not open before closing it");
			}
			screen.onClose();
			return null;
		});

		waitForClientAndServerInventoryMenu("placed linked Backpack screen");
	}

	private static void closePrimaryEndpoint() {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof BackpackScreen screen) || minecraft.player == null
					|| !(minecraft.player.containerMenu instanceof BackpackContainer menu) || menu.getBlockPosition().isPresent()) {
				throw new IllegalStateException("Primary linked Backpack screen was not open before closing it");
			}
			screen.onClose();
			return null;
		});

		waitForClientAndServerInventoryMenu("primary linked Backpack screen");
	}

	private static void waitForClientAndServerInventoryMenu(String closedScreen) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean closed = AutomationRuntime
					.runOnServer(player -> player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty())
					&& AutomationRuntime.runOnClient(() -> {
						Minecraft minecraft = Minecraft.getInstance();
						return minecraft.player != null && minecraft.player.containerMenu == minecraft.player.inventoryMenu
								&& minecraft.player.containerMenu.getCarried().isEmpty() && !(minecraft.screen instanceof BackpackScreen)
								&& !(minecraft.screen instanceof BackpackSettingsScreen);
					});
			if (closed) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the " + closedScreen + " to close");
	}

	private static PrimaryServerSnapshot waitForPrimaryServerSnapshot(LinkedStorageRegressionFixture fixture, boolean tankPresent, String phase) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			PrimaryServerSnapshot snapshot = AutomationRuntime.runOnServer(player -> getPrimaryServerSnapshot(player, fixture));
			if (matchesExpectedPrimaryServerSnapshot(snapshot, fixture, tankPresent)) {
				return snapshot;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		PrimaryServerSnapshot snapshot = AutomationRuntime.runOnServer(player -> getPrimaryServerSnapshot(player, fixture));
		assertPrimaryServerSnapshot(snapshot, fixture, tankPresent, phase);
		return snapshot;
	}

	private static PrimaryServerSnapshot getPrimaryServerSnapshot(ServerPlayer player, LinkedStorageRegressionFixture fixture) {
		ItemStack primary = player.getInventory().getItem(fixture.primaryInventorySlot());
		IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.serverLevel(), primary)
				.orElseThrow(() -> new IllegalStateException("Could not resolve canonical linked Backpack while checking primary Tank state"));
		return new PrimaryServerSnapshot(snapshot(canonical), snapshot(new BackpackWrapper(primary)));
	}

	private static void waitForPrimaryClientSnapshot(LinkedStorageRegressionFixture fixture, PrimaryServerSnapshot serverSnapshot, boolean tankPresent,
			boolean tankCarried, String phase) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			Optional<PrimaryClientSnapshot> snapshot = AutomationRuntime.runOnClient(() -> getPrimaryClientSnapshot(fixture));
			if (snapshot.isPresent() && matchesExpectedPrimaryClientSnapshot(snapshot.get(), serverSnapshot, fixture, tankPresent, tankCarried)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		PrimaryClientSnapshot snapshot = AutomationRuntime.runOnClient(() -> getPrimaryClientSnapshot(fixture)).orElseThrow(() -> new IllegalStateException(
				"Primary linked Backpack screen closed before " + phase + " client snapshot; " + getPrimaryMenuLifecycleSnapshot(fixture)));
		assertPrimaryClientSnapshot(snapshot, serverSnapshot, fixture, tankPresent, tankCarried, phase);
	}

	private static String getPrimaryMenuLifecycleSnapshot(LinkedStorageRegressionFixture fixture) {
		String server = AutomationRuntime.runOnServer(player -> "container=" + player.containerMenu.getClass().getSimpleName() + "#"
				+ player.containerMenu.containerId + ", inventoryMenu=" + (player.containerMenu == player.inventoryMenu) + ", carried="
				+ player.containerMenu.getCarried() + ", primary=" + player.getInventory().getItem(fixture.primaryInventorySlot()));
		String client = AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null) {
				return "player=unavailable, screen=" + className(minecraft.screen);
			}
			ItemStack menuPrimary = minecraft.player.inventoryMenu.getSlot(inventoryMenuSlot(fixture.primaryInventorySlot())).getItem();
			return "screen=" + className(minecraft.screen) + ", container=" + minecraft.player.containerMenu.getClass().getSimpleName() + "#"
					+ minecraft.player.containerMenu.containerId + ", inventoryMenu=" + (minecraft.player.containerMenu == minecraft.player.inventoryMenu)
					+ ", carried=" + minecraft.player.containerMenu.getCarried() + ", primary="
					+ minecraft.player.getInventory().getItem(fixture.primaryInventorySlot()) + ", inventoryMenuPrimary=" + menuPrimary + ", selected="
					+ minecraft.player.getInventory().selected;
		});
		return "server={" + server + "}, client={" + client + "}";
	}

	private static Optional<PrimaryClientSnapshot> getPrimaryClientSnapshot(LinkedStorageRegressionFixture fixture) {
		try {
			BackpackContainer menu = getOpenPrimaryMenu(fixture);
			Minecraft minecraft = Minecraft.getInstance();
			return Optional.of(new PrimaryClientSnapshot(snapshot(menu.getStorageWrapper()),
					snapshot(new BackpackWrapper(minecraft.player.getInventory().getItem(fixture.primaryInventorySlot()))),
					menu.getCarried().is(ModItems.TANK_UPGRADE.get())));
		} catch (IllegalStateException e) {
			return Optional.empty();
		}
	}

	private static PlacedServerSnapshot waitForPlacedServerSnapshot(LinkedStorageRegressionFixture fixture, boolean tankPresent, String phase) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			PlacedServerSnapshot snapshot = AutomationRuntime.runOnServer(player -> getPlacedServerSnapshot(player, fixture));
			if (matchesExpectedServerSnapshot(snapshot, fixture, tankPresent)) {
				return snapshot;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		PlacedServerSnapshot snapshot = AutomationRuntime.runOnServer(player -> getPlacedServerSnapshot(player, fixture));
		assertPlacedServerSnapshot(snapshot, fixture, tankPresent, phase);
		return snapshot;
	}

	private static PlacedServerSnapshot getPlacedServerSnapshot(ServerPlayer player, LinkedStorageRegressionFixture fixture) {
		IBackpackWrapper canonical = BackpackLinkedStorageResolver
				.resolveCanonicalHost(player.serverLevel(), player.getInventory().getItem(fixture.primaryInventorySlot()))
				.orElseThrow(() -> new IllegalStateException("Could not resolve canonical linked Backpack while checking placed Tank state"));
		BackpackBlockEntity placedBackpack = WorldHelper.getBlockEntity(player.serverLevel(), fixture.placedEndpointPos(), BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Could not resolve placed linked Backpack while checking Tank state"));
		BackpackWrapper physical = new BackpackWrapper(placedBackpack.getBackpackWrapper().getBackpack());
		return new PlacedServerSnapshot(snapshot(canonical), snapshot(physical));
	}

	private static void waitForPlacedClientSnapshot(LinkedStorageRegressionFixture fixture, PlacedServerSnapshot serverSnapshot, boolean tankPresent,
			boolean tankCarried, String phase) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			Optional<PlacedClientSnapshot> snapshot = AutomationRuntime.runOnClient(() -> getPlacedClientSnapshot(fixture));
			if (snapshot.isPresent() && matchesExpectedClientSnapshot(snapshot.get(), serverSnapshot, fixture, tankPresent, tankCarried)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		PlacedClientSnapshot snapshot = AutomationRuntime.runOnClient(() -> getPlacedClientSnapshot(fixture))
				.orElseThrow(() -> new IllegalStateException("Placed linked Backpack screen closed before " + phase + " client snapshot"));
		assertPlacedClientSnapshot(snapshot, serverSnapshot, fixture, tankPresent, tankCarried, phase);
	}

	private static Optional<PlacedClientSnapshot> getPlacedClientSnapshot(LinkedStorageRegressionFixture fixture) {
		try {
			BackpackContainer menu = getOpenPlacedMenu(fixture);
			return Optional.of(new PlacedClientSnapshot(snapshot(menu.getStorageWrapper()), menu.getCarried().is(ModItems.TANK_UPGRADE.get())));
		} catch (IllegalStateException e) {
			return Optional.empty();
		}
	}

	private static StorageProfile snapshot(IBackpackWrapper wrapper) {
		return new StorageProfile(wrapper.getInventoryHandler().getSlots(), wrapper.getUpgradeHandler().getSlots(), wrapper.getColumnsTaken(),
				wrapper.getNumberOfSlotRows(), wrapper.getUpgradeHandler().getStackInSlot(1).is(ModItems.TANK_UPGRADE.get()),
				wrapper.getRenderInfo().getNbt().copy());
	}

	private static boolean matchesExpectedPrimaryServerSnapshot(PrimaryServerSnapshot snapshot, LinkedStorageRegressionFixture fixture, boolean tankPresent) {
		return matchesExpectedProfile(snapshot.canonical(), fixture, tankPresent) && matchesExpectedPhysicalProfile(snapshot.physical(), fixture, tankPresent)
				&& snapshot.physical().columnsTaken() == snapshot.canonical().columnsTaken()
				&& snapshot.physical().renderInfo().equals(snapshot.canonical().renderInfo());
	}

	private static void assertPrimaryServerSnapshot(PrimaryServerSnapshot snapshot, LinkedStorageRegressionFixture fixture, boolean tankPresent, String phase) {
		assertTrue(matchesExpectedProfile(snapshot.canonical(), fixture, tankPresent), phase + " primary canonical linked Backpack profile did not converge: "
				+ snapshot.canonical() + ", expected layout=" + fixture.canonicalLayout());
		assertTrue(matchesExpectedPhysicalProfile(snapshot.physical(), fixture, tankPresent),
				phase + " primary Backpack physical inventory stack did not remain a plain endpoint projection");
		assertTrue(snapshot.physical().columnsTaken() == snapshot.canonical().columnsTaken(),
				phase + " primary Backpack physical inventory columns did not project canonical layout");
		assertTrue(snapshot.physical().renderInfo().equals(snapshot.canonical().renderInfo()),
				phase + " primary Backpack physical inventory render did not project canonical render state");
	}

	private static boolean matchesExpectedPrimaryClientSnapshot(PrimaryClientSnapshot snapshot, PrimaryServerSnapshot serverSnapshot,
			LinkedStorageRegressionFixture fixture, boolean tankPresent, boolean tankCarried) {
		return matchesExpectedProfile(snapshot.menuProfile(), fixture, tankPresent)
				&& matchesExpectedPhysicalProfile(snapshot.physicalProfile(), fixture, tankPresent) && snapshot.tankCarried() == tankCarried
				&& snapshot.physicalProfile().columnsTaken() == serverSnapshot.physical().columnsTaken()
				&& snapshot.physicalProfile().renderInfo().equals(serverSnapshot.physical().renderInfo())
				&& snapshot.menuProfile().renderInfo().equals(snapshot.physicalProfile().renderInfo());
	}

	private static void assertPrimaryClientSnapshot(PrimaryClientSnapshot snapshot, PrimaryServerSnapshot serverSnapshot,
			LinkedStorageRegressionFixture fixture, boolean tankPresent, boolean tankCarried, String phase) {
		assertTrue(matchesExpectedProfile(snapshot.menuProfile(), fixture, tankPresent),
				phase + " primary Backpack client menu layout/profile did not converge: client=" + snapshot + ", server=" + serverSnapshot);
		assertTrue(matchesExpectedPhysicalProfile(snapshot.physicalProfile(), fixture, tankPresent),
				phase + " primary Backpack client inventory stack did not remain a plain endpoint projection");
		assertTrue(snapshot.tankCarried() == tankCarried, phase + " primary Backpack client cursor Tank state did not converge");
		assertTrue(snapshot.physicalProfile().columnsTaken() == serverSnapshot.physical().columnsTaken(),
				phase + " primary Backpack client inventory columns did not match the server physical projection");
		assertTrue(snapshot.physicalProfile().renderInfo().equals(serverSnapshot.physical().renderInfo()),
				phase + " primary Backpack client inventory render did not match the server physical projection");
		assertTrue(snapshot.menuProfile().renderInfo().equals(snapshot.physicalProfile().renderInfo()),
				phase + " primary Backpack client menu render did not match the inventory projection");
	}

	private static boolean matchesExpectedServerSnapshot(PlacedServerSnapshot snapshot, LinkedStorageRegressionFixture fixture, boolean tankPresent) {
		return matchesExpectedProfile(snapshot.canonical(), fixture, tankPresent) && matchesExpectedPhysicalProfile(snapshot.physical(), fixture, tankPresent)
				&& snapshot.physical().columnsTaken() == snapshot.canonical().columnsTaken()
				&& snapshot.physical().renderInfo().equals(snapshot.canonical().renderInfo());
	}

	private static void assertPlacedServerSnapshot(PlacedServerSnapshot snapshot, LinkedStorageRegressionFixture fixture, boolean tankPresent, String phase) {
		assertTrue(matchesExpectedProfile(snapshot.canonical(), fixture, tankPresent),
				phase + " canonical linked Backpack profile did not converge: " + snapshot.canonical() + ", expected layout=" + fixture.canonicalLayout());
		assertTrue(matchesExpectedPhysicalProfile(snapshot.physical(), fixture, tankPresent),
				phase + " placed Backpack physical stack profile did not remain a plain endpoint projection");
		assertTrue(snapshot.physical().columnsTaken() == snapshot.canonical().columnsTaken(),
				phase + " placed Backpack physical columns did not project canonical layout");
		assertTrue(snapshot.physical().renderInfo().equals(snapshot.canonical().renderInfo()),
				phase + " placed Backpack physical render did not project canonical render state");
	}

	private static boolean matchesExpectedClientSnapshot(PlacedClientSnapshot snapshot, PlacedServerSnapshot serverSnapshot,
			LinkedStorageRegressionFixture fixture, boolean tankPresent, boolean tankCarried) {
		return matchesExpectedProfile(snapshot.profile(), fixture, tankPresent) && snapshot.tankCarried() == tankCarried
				&& snapshot.profile().renderInfo().equals(serverSnapshot.physical().renderInfo());
	}

	private static void assertPlacedClientSnapshot(PlacedClientSnapshot snapshot, PlacedServerSnapshot serverSnapshot, LinkedStorageRegressionFixture fixture,
			boolean tankPresent, boolean tankCarried, String phase) {
		assertTrue(matchesExpectedProfile(snapshot.profile(), fixture, tankPresent), phase + " placed Backpack client layout/profile did not converge");
		assertTrue(snapshot.tankCarried() == tankCarried, phase + " placed Backpack client cursor Tank state did not converge");
		assertTrue(snapshot.profile().renderInfo().equals(serverSnapshot.physical().renderInfo()),
				phase + " placed Backpack client render did not match the server physical projection");
	}

	private static boolean matchesExpectedProfile(StorageProfile profile, LinkedStorageRegressionFixture fixture, boolean tankPresent) {
		return matchesExpectedLayout(profile, fixture, tankPresent) && profile.tankInSlotOne() == tankPresent;
	}

	private static boolean matchesExpectedPhysicalProfile(StorageProfile profile, LinkedStorageRegressionFixture fixture, boolean tankPresent) {
		return matchesExpectedLayout(profile, fixture, tankPresent) && !profile.tankInSlotOne();
	}

	private static boolean matchesExpectedLayout(StorageProfile profile, LinkedStorageRegressionFixture fixture, boolean tankPresent) {
		CanonicalLayout canonical = fixture.canonicalLayout();
		int expectedColumns = tankPresent ? canonical.columnsTaken() : 0;
		int expectedStorageSlots = tankPresent ? canonical.storageSlots() : canonical.storageSlots() + canonical.columnsTaken() * canonical.rows();
		return profile.storageSlots() == expectedStorageSlots && profile.upgradeSlots() == canonical.upgradeSlots() && profile.columnsTaken() == expectedColumns
				&& profile.rows() == canonical.rows();
	}

	private static void stashIntoLinkedBackpackWithInventoryClick(LinkedStorageRegressionFixture fixture) {
		AutomationRuntime.runOnServer(player -> {
			player.closeContainer();
			return null;
		});

		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> clickLinkedBackpackFromInventory(fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for the player inventory menu before stashing into the linked Backpack");
	}

	private static boolean clickLinkedBackpackFromInventory(LinkedStorageRegressionFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
			return false;
		}

		InventoryMenu menu = minecraft.player.inventoryMenu;
		int primaryMenuSlot = inventoryMenuSlot(fixture.primaryInventorySlot());
		int stashMenuSlot = inventoryMenuSlot(STASH_INVENTORY_SLOT);
		assertStack(menu.getSlot(primaryMenuSlot).getItem(), ModItems.GOLD_BACKPACK.get(), 1, "Client primary Backpack slot was not synchronized");
		assertTrue(Optional.ofNullable(LinkedStorageStackData.getEndpoint(menu.getSlot(primaryMenuSlot).getItem())).map(LinkedStorageEndpointData::endpointId)
				.filter(fixture.primaryEndpointId()::equals).isPresent(), "Client primary Backpack slot was not the linked endpoint");
		assertStack(menu.getSlot(stashMenuSlot).getItem(), Items.EMERALD, STASH_COUNT, "Client stash stack was not synchronized");
		assertTrue(menu.getCarried().isEmpty(), "Client inventory menu was already carrying an item");

		minecraft.gameMode.handleInventoryMouseClick(menu.containerId, stashMenuSlot, 0, ClickType.PICKUP, minecraft.player);
		minecraft.gameMode.handleInventoryMouseClick(menu.containerId, primaryMenuSlot, 1, ClickType.PICKUP, minecraft.player);
		return true;
	}

	private static int inventoryMenuSlot(int inventorySlot) {
		return inventorySlot < 9 ? InventoryMenu.USE_ROW_SLOT_START + inventorySlot : InventoryMenu.INV_SLOT_START + inventorySlot - 9;
	}

	private static void waitForServerStash(LinkedStorageRegressionFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(player -> serverStashMatches(player, fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		assertTrue(AutomationRuntime.runOnServer(player -> serverStashMatches(player, fixture)),
				"Canonical linked storage did not contain the stash exactly once with no physical player-inventory duplicate");
	}

	private static boolean serverStashMatches(ServerPlayer player, LinkedStorageRegressionFixture fixture) {
		ItemStack primary = player.getInventory().getItem(fixture.primaryInventorySlot());
		IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.serverLevel(), primary)
				.orElseThrow(() -> new IllegalStateException("Could not resolve the canonical linked Backpack inventory after the inventory click"));
		BackpackBlockEntity placedBackpack = WorldHelper.getBlockEntity(player.serverLevel(), fixture.placedEndpointPos(), BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Could not resolve the placed linked Backpack after the inventory click"));
		LinkedStorageGroupManager groups = LinkedStorageGroupsSavedData.get(player.serverLevel()).manager();
		return countItems(canonical, Items.EMERALD) == STASH_COUNT && countItems(placedBackpack.getBackpackWrapper(), Items.EMERALD) == STASH_COUNT
				&& countPlayerItems(player, Items.EMERALD) == 0 && player.getInventory().getItem(STASH_INVENTORY_SLOT).isEmpty()
				&& player.containerMenu.getCarried().isEmpty() && groups.getRevision(fixture.groupId()) > fixture.groupRevision();
	}

	private static void waitForClientStashConvergence(LinkedStorageRegressionFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(() -> clientStashConverged(fixture))) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		assertTrue(AutomationRuntime.runOnClient(() -> clientStashConverged(fixture)),
				"Client inventory menu did not converge after stashing into the linked Backpack");
	}

	private static boolean clientStashConverged(LinkedStorageRegressionFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
			return false;
		}

		InventoryMenu menu = minecraft.player.inventoryMenu;
		ItemStack primary = menu.getSlot(inventoryMenuSlot(fixture.primaryInventorySlot())).getItem();
		return primary.is(ModItems.GOLD_BACKPACK.get()) && primary.getCount() == 1
				&& Optional.ofNullable(LinkedStorageStackData.getEndpoint(primary)).map(LinkedStorageEndpointData::endpointId)
						.filter(fixture.primaryEndpointId()::equals).isPresent()
				&& menu.getSlot(inventoryMenuSlot(STASH_INVENTORY_SLOT)).getItem().isEmpty() && menu.getCarried().isEmpty();
	}

	private static int countItems(IBackpackWrapper wrapper, Item item) {
		int count = 0;
		for (int slot = 0; slot < wrapper.getInventoryHandler().getSlots(); slot++) {
			ItemStack stack = wrapper.getInventoryHandler().getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int countPlayerItems(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static void assertStack(ItemStack stack, Item item, int count, String message) {
		assertTrue(stack.is(item) && stack.getCount() == count, message);
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for linked Backpack screen", e);
		}
	}

	private static <T extends GuiEventListener> Optional<T> findChild(GuiEventListener parent, Class<T> childClass) {
		if (childClass.isInstance(parent)) {
			return Optional.of(childClass.cast(parent));
		}
		if (parent instanceof ContainerEventHandler containerEventHandler) {
			for (GuiEventListener child : containerEventHandler.children()) {
				Optional<T> found = findChild(child, childClass);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}

	private static IBackpackWrapper resolve(ServerLevel level, ItemStack backpack, String name) {
		return BackpackLinkedStorageResolver.resolve(level, backpack)
				.orElseThrow(() -> new IllegalStateException("Could not resolve " + name + " linked Backpack endpoint"));
	}

	private static LinkedStorageEndpointData requireEndpoint(ItemStack stack, String name) {
		LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(stack);
		if (endpoint == null) {
			throw new IllegalStateException("Missing linked storage endpoint data for " + name);
		}
		return endpoint;
	}

	private static void close(IBackpackWrapper wrapper) {
		if (wrapper instanceof LinkedStorageBackpackWrapper linkedStorageBackpack) {
			linkedStorageBackpack.close();
		}
	}

	private static void exerciseInceptionLinkedChildPersistenceRegression() {
		InceptionLinkedChildFixture fixture = AutomationRuntime.runOnServer(BackpackLinkedStorageRegression::setupInceptionLinkedChild);
		try {
			waitForClientCondition(
					() -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getInventory().selected == 0
							&& Minecraft.getInstance().player.getMainHandItem().is(ModItems.NETHERITE_BACKPACK.get())
							&& Minecraft.getInstance().player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT).isEmpty(),
					"Inception linked child fixture");
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				assertTrue(minecraft.player != null && minecraft.gameMode != null && minecraft.player.getMainHandItem().is(ModItems.NETHERITE_BACKPACK.get()),
						"Client Inception parent Backpack is unavailable");
				assertTrue(minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND).consumesAction(),
						"Client Inception parent Backpack use did not consume the interaction");
				return null;
			});
			waitForInceptionParentMenu(fixture);
			AutomationRuntime.runOnClient(() -> {
				BackpackContainer menu = getOpenInceptionParentMenu(fixture);
				Slot childSlot = menu.getSlot(INCEPTION_LINKED_CHILD_SLOT);
				assertTrue(fixture.childEndpoint().equals(LinkedStorageStackData.getEndpoint(childSlot.getItem())),
						"Client Inception parent menu lost the linked child endpoint before open");
				SBPPacketHandler.INSTANCE.sendToServer(new BackpackOpenMessage(childSlot.index));
				return null;
			});
			waitForInceptionChildMenu(fixture);
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				assertTrue(
						minecraft.player != null && minecraft.player.containerMenu instanceof BackpackContainer menu
								&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK
								&& fixture.childEndpoint().equals(LinkedStorageStackData.getEndpoint(menu.getStorageWrapper().getBackpack()))
								&& menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0).is(Items.NETHER_STAR)
								&& menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0).getCount() == INCEPTION_LINKED_CHILD_MARKER_COUNT,
						"Client Inception linked child menu did not retain the endpoint and marker");
				SBPPacketHandler.INSTANCE.sendToServer(new BackpackOpenMessage());
				return null;
			});
			waitForInceptionParentMenu(fixture);
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				BackpackContainer menu = getOpenInceptionParentMenu(fixture);
				Slot childSlot = menu.getSlot(INCEPTION_LINKED_CHILD_SLOT);
				Slot playerSlot = getInceptionMovedChildPlayerSlot(menu);
				assertTrue(playerSlot.getItem().isEmpty(), "Client Inception child player inventory destination was occupied");
				minecraft.gameMode.handleInventoryMouseClick(menu.containerId, childSlot.index, 0, ClickType.PICKUP, minecraft.player);
				assertTrue(fixture.childEndpoint().equals(LinkedStorageStackData.getEndpoint(menu.getCarried())),
						"Client Inception child move did not pick up the linked endpoint");
				minecraft.gameMode.handleInventoryMouseClick(menu.containerId, playerSlot.index, 0, ClickType.PICKUP, minecraft.player);
				return null;
			});
			waitForServerCondition(player -> hasServerMovedInceptionLinkedChild(player, fixture), "Inception linked child move");
			waitForClientCondition(() -> hasClientMovedInceptionLinkedChild(fixture), "Inception linked child move");
			AutomationRuntime.runOnClient(() -> {
				((BackpackScreen) Minecraft.getInstance().screen).onClose();
				return null;
			});
			waitForClientAndServerInventoryMenu("Inception parent Backpack screen");
			AutomationRuntime.runOnClient(() -> {
				assertTrue(
						Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu == Minecraft.getInstance().player.inventoryMenu,
						"Client moved Inception linked child is unavailable in the player inventory");
				SBPPacketHandler.INSTANCE
						.sendToServer(new BackpackOpenMessage(INCEPTION_MOVED_CHILD_INVENTORY_SLOT, "", PlayerInventoryProvider.MAIN_INVENTORY));
				return null;
			});
			waitForInceptionMovedChildMenu(fixture);
			AutomationRuntime.runOnClient(() -> {
				BackpackScreen screen = (BackpackScreen) Minecraft.getInstance().screen;
				StorageSettingsTab settingsTab = findChild(screen, StorageSettingsTab.class)
						.orElseThrow(() -> new IllegalStateException("Client moved Inception linked child storage settings tab was unavailable"));
				assertTrue(settingsTab.mouseClicked(settingsTab.getX() + 9, settingsTab.getY() + 12, 0),
						"Client moved Inception linked child storage settings tab did not handle the click");
				return null;
			});
			waitForInceptionMovedChildSettingsMenu(fixture);
			AutomationRuntime.runOnClient(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				assertTrue(minecraft.player != null && minecraft.player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu,
						"Client moved Inception linked child settings screen is unavailable");
				BackpackSettingsContainerMenu settingsMenu = (BackpackSettingsContainerMenu) minecraft.player.containerMenu;
				final boolean[] selected = {false};
				settingsMenu.forEachSettingsContainer((name, container) -> {
					if (ItemDisplaySettingsCategory.NAME.equals(name) && container instanceof ItemDisplaySettingsContainer itemDisplaySettings) {
						itemDisplaySettings.selectSlot(0);
						itemDisplaySettings.setDisplaySide(DisplaySide.FRONT);
						selected[0] = true;
					}
				});
				assertTrue(selected[0], "Client moved Inception linked child item display settings container was unavailable");
				return null;
			});
			waitForServerCondition(player -> hasServerMovedInceptionLinkedChildItemDisplay(player, fixture),
					"moved Inception linked child item display setting");
			waitForClientCondition(() -> hasClientMovedInceptionLinkedChildItemDisplay(fixture), "moved Inception linked child item display setting");
			AutomationRuntime.runOnClient(() -> {
				((BackpackSettingsScreen) Minecraft.getInstance().screen).onClose();
				return null;
			});
			waitForClientAndServerInventoryMenu("Inception linked child settings screen");
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				player.getInventory().clearContent();
				player.getInventory().setChanged();
				player.setGameMode(fixture.originalGameMode());
				return null;
			});
		}
	}

	private static InceptionLinkedChildFixture setupInceptionLinkedChild(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		GameType originalGameMode = player.gameMode.getGameModeForPlayer();
		player.closeContainer();
		player.getInventory().clearContent();
		player.setGameMode(GameType.SURVIVAL);
		ItemStack child = new ItemStack(ModItems.GOLD_BACKPACK.get());
		IBackpackWrapper childWrapper = new BackpackWrapper(child);
		childWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.NETHER_STAR, INCEPTION_LINKED_CHILD_MARKER_COUNT));
		childWrapper.getInventoryHandler().saveInventory();
		assertTrue(LinkedStorageService.link(level, player.getUUID(), new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get()), child),
				"Could not link the Inception child Backpack");
		LinkedStorageEndpointData childEndpoint = requireEndpoint(child, "Inception linked child");
		assertInceptionLinkedChildState(level, child, childEndpoint, "before insertion");
		ItemStack parent = new ItemStack(ModItems.NETHERITE_BACKPACK.get());
		IBackpackWrapper parentWrapper = new BackpackWrapper(parent);
		parentWrapper.getInventoryHandler();
		parentWrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.INCEPTION_UPGRADE.get()));
		parentWrapper.getUpgradeHandler().saveInventory();
		parentWrapper.getInventoryHandler().setStackInSlot(INCEPTION_LINKED_CHILD_SLOT, child);
		parentWrapper.getInventoryHandler().saveInventory();
		parentWrapper.getInventoryForUpgradeProcessing().getSlots();
		assertInceptionLinkedChildState(level, parentWrapper.getInventoryHandler().getStackInSlot(INCEPTION_LINKED_CHILD_SLOT), childEndpoint,
				"after Inception initialization");
		player.getInventory().setItem(0, parent);
		player.getInventory().selected = 0;
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastFullState();
		player.connection.send(new ClientboundSetCarriedItemPacket(0));
		return new InceptionLinkedChildFixture(childEndpoint, originalGameMode);
	}

	private static void assertInceptionLinkedChildState(ServerLevel level, ItemStack child, LinkedStorageEndpointData endpoint, String phase) {
		assertTrue(endpoint.equals(LinkedStorageStackData.getEndpoint(child)), "Inception linked child lost its endpoint " + phase);
		IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(level, child)
				.orElseThrow(() -> new IllegalStateException("Inception linked child did not resolve its canonical group " + phase));
		assertTrue(
				endpoint.groupId().equals(canonical.getContentsUuid().orElse(null)) && canonical.getInventoryHandler().getStackInSlot(0).is(Items.NETHER_STAR)
						&& canonical.getInventoryHandler().getStackInSlot(0).getCount() == INCEPTION_LINKED_CHILD_MARKER_COUNT,
				"Inception linked child lost its canonical group or marker contents " + phase);
	}

	private static BackpackContainer getOpenInceptionParentMenu(InceptionLinkedChildFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof BackpackScreen) || minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
				|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK
				|| menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper || !fixture.childEndpoint().equals(
						LinkedStorageStackData.getEndpoint(menu.getStorageWrapper().getInventoryHandler().getStackInSlot(INCEPTION_LINKED_CHILD_SLOT)))) {
			throw new IllegalStateException("Client Inception parent Backpack menu is unavailable");
		}
		return menu;
	}

	private static void waitForInceptionParentMenu(InceptionLinkedChildFixture fixture) {
		waitForClientCondition(() -> {
			try {
				getOpenInceptionParentMenu(fixture);
				return true;
			} catch (IllegalStateException e) {
				return false;
			}
		}, "Inception parent Backpack menu");
	}

	private static void waitForInceptionChildMenu(InceptionLinkedChildFixture fixture) {
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.screen instanceof BackpackScreen && minecraft.player != null && minecraft.player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_SUB_BACKPACK
					&& menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
					&& fixture.childEndpoint().equals(LinkedStorageStackData.getEndpoint(menu.getStorageWrapper().getBackpack()))
					&& menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0).is(Items.NETHER_STAR)
					&& menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0).getCount() == INCEPTION_LINKED_CHILD_MARKER_COUNT;
		}, "Inception linked child Backpack menu");
	}

	private static Slot getInceptionMovedChildPlayerSlot(BackpackContainer menu) {
		for (int slotIndex = menu.getNumberOfStorageInventorySlots(); slotIndex < menu.getInventorySlotsSize(); slotIndex++) {
			Slot slot = menu.getSlot(slotIndex);
			if (slot.getContainerSlot() == INCEPTION_MOVED_CHILD_INVENTORY_SLOT) {
				return slot;
			}
		}
		throw new IllegalStateException("Client Inception child player inventory slot is unavailable");
	}

	private static boolean hasServerMovedInceptionLinkedChild(ServerPlayer player, InceptionLinkedChildFixture fixture) {
		ItemStack parent = player.getInventory().getItem(0);
		ItemStack movedChild = player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT);
		return parent.is(ModItems.NETHERITE_BACKPACK.get())
				&& new BackpackWrapper(parent).getInventoryHandler().getStackInSlot(INCEPTION_LINKED_CHILD_SLOT).isEmpty()
				&& fixture.childEndpoint().equals(LinkedStorageStackData.getEndpoint(movedChild))
				&& countEndpoint(player.getInventory(), fixture.childEndpoint()) == 1;
	}

	private static boolean hasClientMovedInceptionLinkedChild(InceptionLinkedChildFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
				|| menu.getBackpackContext().getType() != BackpackContext.ContextType.ITEM_BACKPACK) {
			return false;
		}
		return menu.getSlot(INCEPTION_LINKED_CHILD_SLOT).getItem().isEmpty()
				&& fixture.childEndpoint()
						.equals(LinkedStorageStackData.getEndpoint(minecraft.player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT)))
				&& countEndpoint(minecraft.player.getInventory(), fixture.childEndpoint()) == 1;
	}

	private static void waitForInceptionMovedChildMenu(InceptionLinkedChildFixture fixture) {
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.screen instanceof BackpackScreen && minecraft.player != null && minecraft.player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.ITEM_BACKPACK
					&& menu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
					&& fixture.childEndpoint().equals(LinkedStorageStackData.getEndpoint(menu.getStorageWrapper().getBackpack()));
		}, "moved Inception linked child Backpack menu");
	}

	private static void waitForInceptionMovedChildSettingsMenu(InceptionLinkedChildFixture fixture) {
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.screen instanceof BackpackSettingsScreen && minecraft.player != null
					&& minecraft.player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu
					&& settingsMenu.getStorageWrapper() instanceof LinkedStorageBackpackWrapper
					&& fixture.childEndpoint().equals(LinkedStorageStackData.getEndpoint(settingsMenu.getStorageWrapper().getBackpack()));
		}, "moved Inception linked child settings screen");
	}

	private static boolean hasServerMovedInceptionLinkedChildItemDisplay(ServerPlayer player, InceptionLinkedChildFixture fixture) {
		if (!hasServerMovedInceptionLinkedChild(player, fixture)) {
			return false;
		}
		IBackpackWrapper canonical = BackpackLinkedStorageResolver
				.resolveCanonicalHost(player.serverLevel(), player.getInventory().getItem(INCEPTION_MOVED_CHILD_INVENTORY_SLOT)).orElse(null);
		if (canonical == null) {
			return false;
		}
		ItemDisplaySettingsCategory itemDisplaySettings = canonical.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
		return itemDisplaySettings.getSlots().contains(0) && itemDisplaySettings.getDisplaySide() == DisplaySide.FRONT;
	}

	private static boolean hasClientMovedInceptionLinkedChildItemDisplay(InceptionLinkedChildFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu)
				|| !fixture.childEndpoint().equals(LinkedStorageStackData.getEndpoint(settingsMenu.getStorageWrapper().getBackpack()))) {
			return false;
		}
		final boolean[] matches = {false};
		settingsMenu.forEachSettingsContainer((name, container) -> {
			if (ItemDisplaySettingsCategory.NAME.equals(name) && container instanceof ItemDisplaySettingsContainer itemDisplaySettings) {
				matches[0] = itemDisplaySettings.isSlotSelected(0) && itemDisplaySettings.getDisplaySide() == DisplaySide.FRONT;
			}
		});
		return matches[0] && countEndpoint(minecraft.player.getInventory(), fixture.childEndpoint()) == 1;
	}

	private static int countEndpoint(Inventory inventory, LinkedStorageEndpointData endpoint) {
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (endpoint.equals(LinkedStorageStackData.getEndpoint(inventory.getItem(slot)))) {
				count++;
			}
		}
		return count;
	}

	private static void assertTrue(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}

	private record LinkedStorageRegressionFixture(UUID groupId, UUID primaryEndpointId, UUID secondaryEndpointId, long groupRevision,
			BlockPos placedEndpointPos, CanonicalLayout canonicalLayout, int primaryInventorySlot) {
		private LinkedStorageRegressionFixture withPrimaryInventorySlot(int primaryInventorySlot) {
			return new LinkedStorageRegressionFixture(groupId, primaryEndpointId, secondaryEndpointId, groupRevision, placedEndpointPos, canonicalLayout,
					primaryInventorySlot);
		}
	}

	private static void exerciseLinkedChildInPlacedParent(LinkedStorageRegressionFixture fixture) {
		NestedLinkedChildFixture nestedFixture = AutomationRuntime.runOnServer(player -> setupNestedLinkedChild(player, fixture));
		try {
			waitForNestedParentClientFixture(nestedFixture);
			openNestedParentThroughClientInteraction(nestedFixture);
			waitForNestedParentMenu(nestedFixture);
			openNestedChildThroughBackpackMessage(nestedFixture);
			waitForNestedChildMenu(nestedFixture);
			AutomationRuntime.runOnClient(() -> {
				BackpackContainer menu = getOpenNestedChildMenu(nestedFixture);
				assertTrue(matchesNestedChild(menu.getStorageWrapper(), nestedFixture), "Nested BLOCK_SUB_BACKPACK did not resolve the linked child endpoint");
				Slot upgradeSlot = menu.upgradeSlots.get(1);
				assertStack(upgradeSlot.getItem(), ModItems.TANK_UPGRADE.get(), 1, "Nested linked child Tank upgrade was not present");
				clickScreenSlot((BackpackScreen) Minecraft.getInstance().screen, upgradeSlot, "nested linked child Tank upgrade");
				return null;
			});
			waitForNestedChildProjection(nestedFixture);
			openNestedChildSettings(nestedFixture);
			selectNestedChildItemDisplay(nestedFixture);
			waitForNestedChildItemDisplayProjection(nestedFixture);
		} finally {
			AutomationRuntime.runOnServer(player -> {
				player.closeContainer();
				PlayerLocation location = nestedFixture.originalPlayerLocation();
				assertTrue(player.teleportTo(player.serverLevel(), location.position().x, location.position().y, location.position().z, Set.of(),
						location.yRot(), location.xRot()), "Could not restore the player after the nested linked child regression");
				return null;
			});
		}
	}

	private static NestedLinkedChildFixture setupNestedLinkedChild(ServerPlayer player, LinkedStorageRegressionFixture fixture) {
		ServerLevel level = player.serverLevel();
		PlayerLocation originalPlayerLocation = new PlayerLocation(player.position(), player.getYRot(), player.getXRot());
		BlockPos parentPosition = fixture.placedEndpointPos().relative(player.getDirection(), 2);
		player.closeContainer();
		level.setBlock(parentPosition, Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(parentPosition.below(), Blocks.DIRT.defaultBlockState(), 3);
		level.setBlock(parentPosition, ModBlocks.GOLD_BACKPACK.get().defaultBlockState(), 3);
		BackpackBlockEntity parent = WorldHelper.getBlockEntity(level, parentPosition, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Could not create the ordinary placed parent Backpack"));
		parent.setBackpack(new ItemStack(ModItems.GOLD_BACKPACK.get()));
		ItemStack child = createColumnBackpack();
		IBackpackWrapper childWrapper = new BackpackWrapper(child);
		childWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
		childWrapper.getInventoryHandler().saveInventory();
		ItemStack linker = new ItemStack(net.p3pp3rf1y.sophisticatedcore.init.ModItems.ENDER_LINKER.get());
		assertTrue(LinkedStorageService.link(level, player.getUUID(), linker, child), "Could not create the nested linked Backpack group");
		LinkedStorageEndpointData childEndpoint = requireEndpoint(child, "nested child");
		parent.getBackpackWrapper().getInventoryHandler().setStackInSlot(0, child);
		parent.getBackpackWrapper().getInventoryHandler().saveInventory();
		Direction approachDirection = player.getDirection().getOpposite();
		Vec3 nearbyPosition = new Vec3(parentPosition.getX() + 0.5D + approachDirection.getStepX() * 2D, player.getY(),
				parentPosition.getZ() + 0.5D + approachDirection.getStepZ() * 2D);
		assertTrue(player.teleportTo(level, nearbyPosition.x, nearbyPosition.y, nearbyPosition.z, Set.of(), player.getYRot(), player.getXRot()),
				"Could not move the player near the nested linked child parent");
		var chunk = level.getChunkAt(parentPosition);
		player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
		return new NestedLinkedChildFixture(parentPosition, childEndpoint.groupId(), childEndpoint.endpointId(), originalPlayerLocation);
	}

	private static void waitForNestedParentClientFixture(NestedLinkedChildFixture fixture) {
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null || !minecraft.player.getMainHandItem().isEmpty()
					|| minecraft.player.distanceToSqr(Vec3.atCenterOf(fixture.parentPosition())) >= 16D) {
				return false;
			}
			return WorldHelper.getBlockEntity(minecraft.level, fixture.parentPosition(), BackpackBlockEntity.class).isPresent();
		}, "Timed out waiting for the nearby nested linked child parent fixture");
	}

	private static void openNestedParentThroughClientInteraction(NestedLinkedChildFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			assertTrue(minecraft.player != null && minecraft.gameMode != null && minecraft.player.getMainHandItem().isEmpty(),
					"Client player or empty interaction hand was unavailable for nested parent interaction");
			assertTrue(
					minecraft.gameMode
							.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
									new BlockHitResult(Vec3.atCenterOf(fixture.parentPosition()), Direction.UP, fixture.parentPosition(), false))
							.consumesAction(),
					"Nested linked child parent Backpack did not open through the client interaction");
			return null;
		});
	}

	private static void waitForNestedParentMenu(NestedLinkedChildFixture fixture) {
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.screen instanceof BackpackScreen && minecraft.player != null && minecraft.player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_BACKPACK
					&& menu.getBlockPosition().filter(fixture.parentPosition()::equals).isPresent() && findNestedChildSlot(menu, fixture).isPresent();
		}, "Timed out waiting for the ordinary placed parent Backpack screen");
	}

	private static Optional<Slot> findNestedChildSlot(BackpackContainer menu, NestedLinkedChildFixture fixture) {
		for (int slotIndex = 0; slotIndex < menu.getNumberOfStorageInventorySlots(); slotIndex++) {
			Slot slot = menu.getSlot(slotIndex);
			LinkedStorageEndpointData endpoint = LinkedStorageStackData.getEndpoint(slot.getItem());
			if (endpoint != null && fixture.childEndpointId().equals(endpoint.endpointId())) {
				return Optional.of(slot);
			}
		}
		return Optional.empty();
	}

	private static void openNestedChildThroughBackpackMessage(NestedLinkedChildFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof BackpackScreen) || minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackContainer)) {
				throw new IllegalStateException("Nested linked child parent Backpack screen is unavailable for navigation");
			}
			BackpackContainer menu = (BackpackContainer) minecraft.player.containerMenu;
			Slot childSlot = findNestedChildSlot(menu, fixture)
					.orElseThrow(() -> new IllegalStateException("Nested linked child storage slot was unavailable for navigation"));
			// The target packet is the 1.20 equivalent of the final source's direct child-open payload.
			SBPPacketHandler.INSTANCE.sendToServer(new BackpackOpenMessage(childSlot.index));
			return null;
		});
	}

	private static void waitForNestedChildMenu(NestedLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean serverReady = AutomationRuntime.runOnServer(player -> player.containerMenu instanceof BackpackContainer menu
					&& menu.getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_SUB_BACKPACK);
			boolean clientReady = AutomationRuntime.runOnClient(() -> {
				try {
					return getOpenNestedChildMenu(fixture).getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_SUB_BACKPACK;
				} catch (IllegalStateException e) {
					return false;
				}
			});
			if (serverReady && clientReady) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException("Timed out waiting for BLOCK_SUB_BACKPACK linked child navigation");
	}

	private static BackpackContainer getOpenNestedChildMenu(NestedLinkedChildFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof BackpackScreen) || minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
				|| menu.getBackpackContext().getType() != BackpackContext.ContextType.BLOCK_SUB_BACKPACK
				|| menu.getBlockPosition().filter(fixture.parentPosition()::equals).isEmpty()) {
			throw new IllegalStateException("Nested linked child Backpack screen is not open");
		}
		return menu;
	}

	private static boolean matchesNestedChild(IBackpackWrapper wrapper, NestedLinkedChildFixture fixture) {
		return Optional.ofNullable(LinkedStorageStackData.getEndpoint(wrapper.getBackpack())).map(LinkedStorageEndpointData::endpointId)
				.filter(fixture.childEndpointId()::equals).isPresent() && wrapper.getUpgradeHandler().getStackInSlot(1).is(ModItems.TANK_UPGRADE.get());
	}

	private static void waitForNestedChildProjection(NestedLinkedChildFixture fixture) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			boolean serverReady = AutomationRuntime.runOnServer(player -> nestedChildServerProjectionMatches(player, fixture));
			boolean clientReady = AutomationRuntime.runOnClient(() -> nestedChildClientMenuMatches(fixture));
			if (serverReady && clientReady) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		assertTrue(AutomationRuntime.runOnServer(player -> nestedChildServerProjectionMatches(player, fixture)),
				"Nested linked child canonical contents did not persist into the placed parent");
		assertTrue(AutomationRuntime.runOnClient(() -> nestedChildClientMenuMatches(fixture)),
				"Nested linked child client menu did not retain the endpoint and canonical contents");
	}

	private static boolean nestedChildServerProjectionMatches(ServerPlayer player, NestedLinkedChildFixture fixture) {
		BackpackBlockEntity parent = WorldHelper.getBlockEntity(player.serverLevel(), fixture.parentPosition(), BackpackBlockEntity.class).orElse(null);
		if (parent == null) {
			return false;
		}
		ItemStack child = parent.getBackpackWrapper().getInventoryHandler().getStackInSlot(0);
		IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.serverLevel(), child).orElse(null);
		if (canonical == null) {
			return false;
		}
		BackpackWrapper physical = new BackpackWrapper(child);
		return canonical.getUpgradeHandler().getStackInSlot(1).isEmpty() && canonical.getColumnsTaken() == physical.getColumnsTaken()
				&& canonical.getRenderInfo().getNbt().equals(physical.getRenderInfo().getNbt());
	}

	private static boolean nestedChildClientMenuMatches(NestedLinkedChildFixture fixture) {
		try {
			BackpackContainer menu = getOpenNestedChildMenu(fixture);
			return Optional.ofNullable(LinkedStorageStackData.getEndpoint(menu.getStorageWrapper().getBackpack())).map(LinkedStorageEndpointData::endpointId)
					.filter(fixture.childEndpointId()::equals).isPresent() && menu.getStorageWrapper().getUpgradeHandler().getStackInSlot(1).isEmpty();
		} catch (IllegalStateException e) {
			return false;
		}
	}

	private static void openNestedChildSettings(NestedLinkedChildFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			BackpackScreen screen = (BackpackScreen) Minecraft.getInstance().screen;
			assertTrue(getOpenNestedChildMenu(fixture).getBackpackContext().getType() == BackpackContext.ContextType.BLOCK_SUB_BACKPACK,
					"Nested linked child menu was unavailable before opening settings");
			StorageSettingsTab settingsTab = findChild(screen, StorageSettingsTab.class)
					.orElseThrow(() -> new IllegalStateException("Nested linked child settings tab was unavailable"));
			assertTrue(settingsTab.mouseClicked(settingsTab.getX() + 9, settingsTab.getY() + 12, 0),
					"Nested linked child settings tab did not handle the click");
			return null;
		});
		waitForClientCondition(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.screen instanceof BackpackSettingsScreen && minecraft.player != null
					&& minecraft.player.containerMenu instanceof BackpackSettingsContainerMenu settingsMenu
					&& Optional.ofNullable(LinkedStorageStackData.getEndpoint(settingsMenu.getStorageWrapper().getBackpack()))
							.map(LinkedStorageEndpointData::endpointId).filter(fixture.childEndpointId()::equals).isPresent();
		}, "Nested linked child settings menu did not open");
	}

	private static void selectNestedChildItemDisplay(NestedLinkedChildFixture fixture) {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			assertTrue(minecraft.player != null && minecraft.player.containerMenu instanceof BackpackSettingsContainerMenu,
					"Nested linked child settings menu was unavailable for Item Display selection");
			BackpackSettingsContainerMenu settingsMenu = (BackpackSettingsContainerMenu) minecraft.player.containerMenu;
			assertTrue(
					Optional.ofNullable(LinkedStorageStackData.getEndpoint(settingsMenu.getStorageWrapper().getBackpack()))
							.map(LinkedStorageEndpointData::endpointId).filter(fixture.childEndpointId()::equals).isPresent(),
					"Nested linked child settings menu resolved the wrong endpoint");
			final boolean[] selected = {false};
			settingsMenu.forEachSettingsContainer((name, container) -> {
				if (ItemDisplaySettingsCategory.NAME.equals(name) && container instanceof ItemDisplaySettingsContainer itemDisplaySettings) {
					itemDisplaySettings.selectSlot(0);
					itemDisplaySettings.setDisplaySide(DisplaySide.FRONT);
					selected[0] = true;
				}
			});
			assertTrue(selected[0], "Nested linked child Item Display settings container was unavailable");
			return null;
		});
	}

	private static void waitForNestedChildItemDisplayProjection(NestedLinkedChildFixture fixture) {
		waitForServerCondition(player -> nestedChildServerItemDisplayMatches(player, fixture),
				"Nested linked child canonical and physical Item Display projections did not converge on the server");
		// The client receives the persisted physical child through the reopened parent menu, as in the final source.
		AutomationRuntime.runOnServer(player -> {
			BackpackContext.Block context = new BackpackContext.Block(fixture.parentPosition());
			player.closeContainer();
			NetworkHooks.openScreen(player, new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new BackpackContainer(windowId, menuPlayer, context),
					Component.literal("Nested Linked Child Projection")), context::toBuffer);
			return null;
		});
		waitForClientCondition(() -> nestedChildClientMenuItemDisplayMatches(fixture),
				"Nested linked child physical Item Display projection did not converge in the reopened parent menu");
	}

	private static boolean nestedChildServerItemDisplayMatches(ServerPlayer player, NestedLinkedChildFixture fixture) {
		BackpackBlockEntity parent = WorldHelper.getBlockEntity(player.serverLevel(), fixture.parentPosition(), BackpackBlockEntity.class).orElse(null);
		if (parent == null) {
			return false;
		}
		ItemStack child = parent.getBackpackWrapper().getInventoryHandler().getStackInSlot(0);
		IBackpackWrapper canonical = BackpackLinkedStorageResolver.resolveCanonicalHost(player.serverLevel(), child).orElse(null);
		if (canonical == null) {
			return false;
		}
		ItemDisplaySettingsCategory settings = canonical.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
		return settings.getSlots().contains(0) && settings.getDisplaySide() == DisplaySide.FRONT
				&& itemDisplayProjectionMatches(canonical, new BackpackWrapper(child), true);
	}

	private static boolean nestedChildClientMenuItemDisplayMatches(NestedLinkedChildFixture fixture) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !(minecraft.player.containerMenu instanceof BackpackContainer menu)
				|| menu.getBackpackContext().getType() != BackpackContext.ContextType.BLOCK_BACKPACK
				|| menu.getBlockPosition().filter(fixture.parentPosition()::equals).isEmpty()) {
			return false;
		}
		ItemStack child = menu.getStorageWrapper().getInventoryHandler().getStackInSlot(0);
		return fixture.childEndpointId()
				.equals(Optional.ofNullable(LinkedStorageStackData.getEndpoint(child)).map(LinkedStorageEndpointData::endpointId).orElse(null))
				&& new BackpackWrapper(child).getRenderInfo().getItemDisplayRenderInfo().getDisplayItems().stream()
						.anyMatch(displayItem -> displayItem.getItem().is(Items.DIAMOND));
	}

	private static void closeNestedChildSettings() {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof BackpackSettingsScreen screen)) {
				throw new IllegalStateException("Nested linked child settings screen was not open before closing it");
			}
			screen.onClose();
			return null;
		});
		waitForClientAndServerInventoryMenu("nested linked child settings screen");
	}

	private static void closeNestedChild() {
		AutomationRuntime.runOnClient(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(minecraft.screen instanceof BackpackScreen screen)) {
				throw new IllegalStateException("Nested linked child Backpack screen was not open before closing it");
			}
			screen.onClose();
			return null;
		});
		waitForClientAndServerInventoryMenu("nested linked child Backpack screen");
	}

	private static void waitForClientCondition(java.util.function.BooleanSupplier condition, String message) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnClient(condition::getAsBoolean)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(message);
	}

	private static void waitForServerCondition(Predicate<ServerPlayer> condition, String message) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (AutomationRuntime.runOnServer(condition::test)) {
				return;
			}
			sleep(50);
		} while (System.nanoTime() < deadline);
		throw new IllegalStateException(message);
	}

	private record CanonicalLayout(int storageSlots, int upgradeSlots, int columnsTaken, int rows) {
	}

	private record EndpointClientLayout(int storageSlots, int inventoryHandlerSlots, int upgradeSlots, int columnsTaken, int rows) {
	}

	private record NestedLinkedChildFixture(BlockPos parentPosition, UUID childGroupId, UUID childEndpointId, PlayerLocation originalPlayerLocation) {
	}

	private record PlayerLocation(Vec3 position, float yRot, float xRot) {
	}

	private record InceptionLinkedChildFixture(LinkedStorageEndpointData childEndpoint, GameType originalGameMode) {
	}

	private record StorageProfile(int storageSlots, int upgradeSlots, int columnsTaken, int rows, boolean tankInSlotOne, CompoundTag renderInfo) {
	}

	private record PrimaryServerSnapshot(StorageProfile canonical, StorageProfile physical) {
	}

	private record PrimaryClientSnapshot(StorageProfile menuProfile, StorageProfile physicalProfile, boolean tankCarried) {
	}

	private record PlacedServerSnapshot(StorageProfile canonical, StorageProfile physical) {
	}

	private record PlacedClientSnapshot(StorageProfile profile, boolean tankCarried) {
	}

	private record TankStorageSlots(Slot source, Slot target) {
	}
}
