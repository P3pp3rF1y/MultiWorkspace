package net.p3pp3rf1y.devclientautomation.scenarios.storage;

import com.google.gson.JsonObject;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssembleRailType;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.logistics.chute.SmartChuteBlock;
import com.simibubi.create.content.logistics.chute.SmartChuteBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.devclientautomation.bridge.AutomationRuntime;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.create.ContraptionHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.create.MountedStorageBase;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTab;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsTab;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.DecorationTableScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.DecorationTableMenu;
import net.p3pp3rf1y.sophisticatedstorage.entity.MovingStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.IMovingStorageEntity;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageBoat;
import net.p3pp3rf1y.sophisticatedstorageinmotion.entity.StorageMinecart;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public final class StoragePreviewScenarios {
	private static volatile Issue23SetupResult issue23SetupResult;

	private StoragePreviewScenarios() {
	}

	public static String openItemDisplayPreview(String scenario, DisplaySide displaySide) {
		ItemDisplayPreviewSetupResult setupResult = AutomationRuntime.runOnServer(player -> setupStorageItemDisplayPreview(player, scenario, displaySide));
		if (setupResult.targetType() == ItemDisplayPreviewTargetType.PLACED_STORAGE) {
			waitForClientStorageBlockEntity(setupResult.menuPos(), setupResult.limitedBarrel());
			AutomationRuntime.runOnServer(player -> openStorageInventory(player, setupResult.menuPos()));
		} else if (setupResult.targetType() == ItemDisplayPreviewTargetType.BACKPACK) {
			waitForClientBackpackInHotbar();
			AutomationRuntime.runOnServer(StoragePreviewScenarios::openMainBackpack);
		} else if (setupResult.targetType() == ItemDisplayPreviewTargetType.MOVING_STORAGE) {
			waitForClientEntity(setupResult.entityId());
			AutomationRuntime.runOnServer(player -> openMovingStorageInventory(player, setupResult.entityId()));
		} else if (setupResult.targetType() == ItemDisplayPreviewTargetType.CREATE_CONTRAPTION) {
			waitForClientEntity(setupResult.entityId());
			AutomationRuntime.runOnServer(player -> openCreateContraptionStorage(player, setupResult.entityId(), setupResult.localPos()));
		}
		waitForStorageScreen();
		waitForStorageScreenContents();
		waitForStorageScreenAndClickSettingsTab();
		String screenName = waitForSettingsScreenAndOpenItemDisplayTab();
		JsonObject result = new JsonObject();
		result.addProperty("ok", true);
		result.addProperty("scenario", setupResult.scenario());
		result.addProperty("displaySide", displaySide.getSerializedName());
		result.addProperty("menuPos", setupResult.menuPos() == null ? null : setupResult.menuPos().toShortString());
		result.addProperty("localPos", setupResult.localPos() == null ? null : setupResult.localPos().toShortString());
		result.addProperty("entityId", setupResult.entityId());
		result.addProperty("target", setupResult.target());
		result.addProperty("screen", screenName);
		return result.toString();
	}

	public static String openDecorationTableRenderPreview(String itemName) {
		DecorationTableRenderPreviewSetupResult setupResult = AutomationRuntime.runOnServer(player -> setupDecorationTableRenderPreview(player, itemName));
		waitForClientDecorationTable(setupResult.tablePos(), setupResult.resultItem());
		AutomationRuntime.runOnServer(player -> {
			openDecorationTableScreen(player, setupResult.tablePos());
			return "";
		});
		waitForClientDecorationTableScreen();
		JsonObject result = AutomationRuntime.runOnClient(StoragePreviewScenarios::getDecorationTableRenderBoundsJson);
		result.addProperty("ok", true);
		result.addProperty("item", setupResult.itemName());
		result.addProperty("tablePos", setupResult.tablePos().toShortString());
		return result.toString();
	}

	public static String dragDecorationTableRenderPreview(double dragX, double dragY) {
		Screen screen = Minecraft.getInstance().screen;
		if (!(screen instanceof DecorationTableScreen decorationTableScreen)) {
			throw new IllegalStateException("Decoration table screen is not open");
		}
		DecorationTableMenu menu = decorationTableScreen.getMenu();
		Slot lastDyeSlot = menu.getSlot(menu.getDyeSlotRange().firstSlot() + menu.getDyeSlotRange().numberOfSlots() - 1);
		Slot resultSlot = menu.getResultSlot();
		double x = decorationTableScreen.getGuiLeft() + lastDyeSlot.x + 26 + 40D;
		double y = decorationTableScreen.getGuiTop() + lastDyeSlot.y + (resultSlot.y - lastDyeSlot.y + 20) / 2D;
		boolean dragged = decorationTableScreen.mouseDragged(x, y, 0, dragX, dragY);
		return "{\"ok\":" + dragged + ",\"dragged\":" + dragged + '}';
	}

	public static Supplier<String> reproduceIssue23() {
		issue23SetupResult = AutomationRuntime.runOnServer(StoragePreviewScenarios::setupStorageIssue23Reproduction);
		Issue23SetupResult setupResult = getIssue23SetupResult();
		waitForServerCondition("Smart Chute to transfer exactly 64 items",
				player -> countItemsInStorage(player.serverLevel(), setupResult.receiverPos(), Items.COBBLESTONE) == 64);
		// Allow repeated exact-mode attempts to run after the first completed batch.
		sleep(5000);
		return () -> AutomationRuntime.runOnServer(player -> issue23ReproductionResultJson(player, setupResult));
	}

	public static Supplier<String> issue23Status() {
		Issue23SetupResult setupResult = getIssue23SetupResult();
		return () -> AutomationRuntime.runOnServer(player -> issue23ReproductionResultJson(player, setupResult));
	}

	public static Supplier<String> openIssue23SourceStorage() {
		Issue23SetupResult setupResult = getIssue23SetupResult();
		return () -> {
			AutomationRuntime.runOnServer(player -> openCreateContraptionStorage(player, setupResult.contraptionEntityId(), setupResult.mountedStoragePos()));
			return "{\"ok\":true}";
		};
	}

	private static void openDecorationTableScreen(ServerPlayer player, BlockPos tablePos) {
		player.openMenu(new SimpleMenuProvider((windowId, inventory, menuPlayer) -> new DecorationTableMenu(windowId, menuPlayer, tablePos),
				ModBlocks.DECORATION_TABLE.get().getName()), tablePos);
	}

	private static DecorationTableRenderPreviewSetupResult setupDecorationTableRenderPreview(ServerPlayer player, String itemName) {
		ServerLevel level = player.serverLevel();
		BlockPos tablePos = player.blockPosition().offset(4, 0, 0);
		clearDecorationTableRenderPreviewArea(level, tablePos);
		player.getInventory().clearContent();
		player.getInventory().selected = 0;
		player.inventoryMenu.broadcastChanges();
		level.setBlock(tablePos, ModBlocks.DECORATION_TABLE.get().defaultBlockState().setValue(DecorationTableBlock.FACING, Direction.NORTH), 3);
		DecorationTableBlockEntity table = WorldHelper.getBlockEntity(level, tablePos, DecorationTableBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Decoration table block entity missing"));
		ItemStack resultItem = getDecorationTablePreviewItem(itemName);
		insertDecorationTableStack(table.getStorageBlock(), resultItem);
		if ("barrel_directional".equalsIgnoreCase(itemName)) {
			table.getDecorativeBlocks().setStackInSlot(DecorationTableBlockEntity.TOP_CORE_SLOT, new ItemStack(Blocks.JIGSAW));
			table.getDecorativeBlocks().setStackInSlot(DecorationTableBlockEntity.SIDE_CORE_SLOT, new ItemStack(Blocks.FURNACE));
			table.getDecorativeBlocks().setStackInSlot(DecorationTableBlockEntity.BOTTOM_CORE_SLOT, new ItemStack(Blocks.STRUCTURE_BLOCK));
		} else if (resultItem.is(ModBlocks.BARREL_ITEM.get())) {
			table.getDecorativeBlocks().setStackInSlot(DecorationTableBlockEntity.TOP_CORE_SLOT, new ItemStack(Blocks.DIAMOND_BLOCK));
			table.getDecorativeBlocks().setStackInSlot(DecorationTableBlockEntity.SIDE_CORE_SLOT, new ItemStack(Blocks.REDSTONE_BLOCK));
			table.getDecorativeBlocks().setStackInSlot(DecorationTableBlockEntity.BOTTOM_CORE_SLOT, new ItemStack(Blocks.GOLD_BLOCK));
		} else if (resultItem.getItem() instanceof SimpleMaterialBlockItem || resultItem.is(ModBlocks.LIMITED_BARREL_3_ITEM.get())) {
			insertDecorationTableStack(table.getDecorativeBlocks(), new ItemStack(Blocks.DIAMOND_BLOCK));
		} else {
			table.setMainColor(0xFFFF00FF);
		}
		WorldHelper.notifyBlockUpdate(table);

		BlockPos cameraPos = tablePos.south(3);
		if (!player.teleportTo(level, cameraPos.getX() + 0.5D, cameraPos.getY(), cameraPos.getZ() + 0.5D, Set.of(), 180.0F, 7.0F)) {
			throw new IllegalStateException("Failed to position player for decoration table render preview");
		}
		return new DecorationTableRenderPreviewSetupResult(itemName, tablePos, resultItem.getItem());
	}

	private static void clearDecorationTableRenderPreviewArea(ServerLevel level, BlockPos tablePos) {
		level.getEntitiesOfClass(Entity.class, new AABB(tablePos).inflate(8), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
		for (int x = -4; x <= 4; x++) {
			for (int y = -1; y <= 4; y++) {
				for (int z = -4; z <= 4; z++) {
					level.setBlock(tablePos.offset(x, y, z), y == -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		level.getEntitiesOfClass(Entity.class, new AABB(tablePos).inflate(8), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
	}

	private static ItemStack getDecorationTablePreviewItem(String itemName) {
		return switch (itemName.toLowerCase(Locale.ROOT)) {
			case "backpack" -> new ItemStack(ModItems.BACKPACK.get());
			case "barrel", "barrel_directional" -> new ItemStack(ModBlocks.BARREL_ITEM.get());
			case "chest" -> new ItemStack(ModBlocks.CHEST_ITEM.get());
			case "controller" -> new ItemStack(ModBlocks.CONTROLLER_ITEM.get());
			case "leather_boots" -> new ItemStack(Items.LEATHER_BOOTS);
			case "leather_chestplate" -> new ItemStack(Items.LEATHER_CHESTPLATE);
			case "leather_helmet" -> new ItemStack(Items.LEATHER_HELMET);
			case "leather_leggings" -> new ItemStack(Items.LEATHER_LEGGINGS);
			case "limited_barrel_3" -> new ItemStack(ModBlocks.LIMITED_BARREL_3_ITEM.get());
			case "shulker_box" -> new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
			case "storage_link" -> new ItemStack(ModBlocks.STORAGE_LINK_ITEM.get());
			case "storage_io" -> new ItemStack(ModBlocks.STORAGE_IO_ITEM.get());
			default -> throw new IllegalArgumentException("Unknown decoration table preview item " + itemName);
		};
	}

	private static void insertDecorationTableStack(ItemStackHandler handler, ItemStack stack) {
		handler.setStackInSlot(0, stack);
	}

	private static ItemDisplayPreviewSetupResult setupStorageItemDisplayPreview(ServerPlayer player, String scenario, DisplaySide displaySide) {
		ServerLevel level = player.serverLevel();
		BlockPos basePos = player.blockPosition().offset(4, 0, 0);
		clearItemDisplayPreviewArea(level, basePos);

		String normalizedScenario = scenario.toLowerCase(Locale.ROOT);
		return switch (normalizedScenario) {
			case "backpack_item" -> setupBackpackItemDisplayPreview(player, normalizedScenario, displaySide);
			case "barrel_east" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
					ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST), displaySide, false);
			case "barrel_up" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
					ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), displaySide, false);
			case "barrel_down" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
					ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.DOWN), displaySide, false);
			case "limited_barrel_north" -> setupSingleStoragePreview(
					level, player, normalizedScenario, basePos, ModBlocks.LIMITED_BARREL_1.get().defaultBlockState()
							.setValue(LimitedBarrelBlock.HORIZONTAL_FACING, Direction.NORTH).setValue(LimitedBarrelBlock.VERTICAL_FACING, VerticalFacing.NO),
					displaySide, true);
			case "limited_barrel_up" -> setupSingleStoragePreview(
					level, player, normalizedScenario, basePos, ModBlocks.LIMITED_BARREL_1.get().defaultBlockState()
							.setValue(LimitedBarrelBlock.HORIZONTAL_FACING, Direction.NORTH).setValue(LimitedBarrelBlock.VERTICAL_FACING, VerticalFacing.UP),
					displaySide, true);
			case "limited_barrel_down" -> setupSingleStoragePreview(
					level, player, normalizedScenario, basePos, ModBlocks.LIMITED_BARREL_1.get().defaultBlockState()
							.setValue(LimitedBarrelBlock.HORIZONTAL_FACING, Direction.NORTH).setValue(LimitedBarrelBlock.VERTICAL_FACING, VerticalFacing.DOWN),
					displaySide, true);
			case "single_chest_north" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
					ModBlocks.CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.SINGLE),
					displaySide, false);
			case "double_chest_north" -> setupDoubleChestPreview(level, player, normalizedScenario, basePos, displaySide);
			case "shulker_north" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
					ModBlocks.SHULKER_BOX.get().defaultBlockState().setValue(ShulkerBoxBlock.FACING, Direction.NORTH), displaySide, false);
			case "shulker_up" -> setupSingleStoragePreview(level, player, normalizedScenario, basePos,
					ModBlocks.SHULKER_BOX.get().defaultBlockState().setValue(ShulkerBoxBlock.FACING, Direction.UP), displaySide, false);
			case "moving_minecart_barrel" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide,
					MovingStoragePreviewVehicle.MINECART, createStoragePreviewStack("barrel"), 0);
			case "moving_minecart_chest" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide,
					MovingStoragePreviewVehicle.MINECART, createStoragePreviewStack("chest"), 90);
			case "moving_minecart_shulker" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide,
					MovingStoragePreviewVehicle.MINECART, createStoragePreviewStack("shulker"), 180);
			case "moving_boat_barrel" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide, MovingStoragePreviewVehicle.BOAT,
					createStoragePreviewStack("barrel"), 0);
			case "moving_boat_chest" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide, MovingStoragePreviewVehicle.BOAT,
					createStoragePreviewStack("chest"), 90);
			case "moving_boat_shulker" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide, MovingStoragePreviewVehicle.BOAT,
					createStoragePreviewStack("shulker"), 180);
			case "moving_boat_limited_barrel" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide,
					MovingStoragePreviewVehicle.BOAT, createStoragePreviewStack("limited_barrel"), 270);
			case "llama_barrel" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide, MovingStoragePreviewVehicle.LLAMA,
					createStoragePreviewStack("barrel"), 0);
			case "llama_chest" -> setupMovingStoragePreview(level, player, normalizedScenario, basePos, displaySide, MovingStoragePreviewVehicle.LLAMA,
					createStoragePreviewStack("chest"), 90);
			case "create_cart_barrel_north" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
					ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.NORTH), false, 0);
			case "create_cart_birch_barrel_north" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
					ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.NORTH), false, 0, Optional.of(WoodType.BIRCH));
			case "create_cart_barrel_east" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
					ModBlocks.BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST), false, 90);
			case "create_cart_chest" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
					ModBlocks.CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.SINGLE), false,
					180);
			case "create_cart_double_chest" -> setupCreateContraptionDoubleChestPreview(level, player, normalizedScenario, basePos, displaySide, 180);
			case "create_cart_shulker" -> setupCreateContraptionStoragePreview(level, player, normalizedScenario, basePos, displaySide,
					ModBlocks.SHULKER_BOX.get().defaultBlockState().setValue(ShulkerBoxBlock.FACING, Direction.NORTH), false, 270);
			case "create_cart_limited_barrel" -> setupCreateContraptionStoragePreview(
					level, player, normalizedScenario, basePos, displaySide, ModBlocks.LIMITED_BARREL_1.get().defaultBlockState()
							.setValue(LimitedBarrelBlock.HORIZONTAL_FACING, Direction.NORTH).setValue(LimitedBarrelBlock.VERTICAL_FACING, VerticalFacing.NO),
					true, 0);
			case "create_cart_backpack" -> setupCreateContraptionBackpackPreview(level, player, normalizedScenario, basePos, displaySide, 90);
			default -> throw new IllegalArgumentException("Unknown item display preview scenario: " + normalizedScenario);
		};
	}

	private static void clearItemDisplayPreviewArea(ServerLevel level, BlockPos basePos) {
		level.getEntitiesOfClass(Entity.class, new AABB(basePos).inflate(8), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
		for (int x = -2; x <= 3; x++) {
			for (int y = -1; y <= 2; y++) {
				for (int z = -2; z <= 2; z++) {
					level.setBlock(basePos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static ItemDisplayPreviewSetupResult setupSingleStoragePreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos pos,
			BlockState state, DisplaySide displaySide, boolean limitedBarrel) {
		if (limitedBarrel) {
			placeLimitedBarrelWithItem(level, player, pos, state);
		} else {
			level.setBlock(pos, state, 3);
		}
		StorageBlockEntity storageBlockEntity = WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Storage block entity missing for " + scenario));
		configureItemDisplayPreviewStorage(storageBlockEntity, displaySide, limitedBarrel);
		return new ItemDisplayPreviewSetupResult(scenario, pos, null, -1, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), limitedBarrel,
				ItemDisplayPreviewTargetType.PLACED_STORAGE);
	}

	private static void placeLimitedBarrelWithItem(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState desiredState) {
		BlockPos supportPos = pos.below();
		level.setBlock(supportPos, Blocks.DIRT.defaultBlockState(), 3);
		player.setYRot(getPlayerYawForLimitedBarrelFacing(desiredState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)));
		player.setXRot(switch (desiredState.getValue(LimitedBarrelBlock.VERTICAL_FACING)) {
			case UP -> 90;
			case DOWN -> -90;
			case NO -> 0;
		});
		ItemStack stack = new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(supportPos), Direction.UP, supportPos, false);
		player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hitResult);

		BlockState placedState = level.getBlockState(pos);
		if (!placedState.is(ModBlocks.LIMITED_BARREL_1.get())
				|| placedState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING) != desiredState.getValue(LimitedBarrelBlock.HORIZONTAL_FACING)
				|| placedState.getValue(LimitedBarrelBlock.VERTICAL_FACING) != desiredState.getValue(LimitedBarrelBlock.VERTICAL_FACING)) {
			throw new IllegalStateException("Limited barrel placement produced " + placedState + " instead of " + desiredState);
		}
	}

	private static float getPlayerYawForLimitedBarrelFacing(Direction horizontalFacing) {
		return switch (horizontalFacing) {
			case NORTH -> 0;
			case SOUTH -> 180;
			case EAST -> 90;
			case WEST -> -90;
			default -> 0;
		};
	}

	private static ItemDisplayPreviewSetupResult setupDoubleChestPreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos leftPos,
			DisplaySide displaySide) {
		BlockPos rightPos = leftPos.east();
		level.setBlock(leftPos,
				ModBlocks.CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT), 3);
		level.setBlock(rightPos,
				ModBlocks.CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT), 3);
		setPreviewWoodType(level, leftPos, WoodType.BIRCH);
		StorageBlockEntity storageBlockEntity = level.getBlockEntity(rightPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(be -> (StorageBlockEntity) be)
				.orElseThrow(() -> new IllegalStateException("Double chest main block entity missing"));
		configureItemDisplayPreviewStorage(storageBlockEntity, displaySide, false);
		return new ItemDisplayPreviewSetupResult(scenario, rightPos, null, -1, BuiltInRegistries.BLOCK.getKey(ModBlocks.CHEST.get()).toString(), false,
				ItemDisplayPreviewTargetType.PLACED_STORAGE);
	}

	private static ItemDisplayPreviewSetupResult setupBackpackItemDisplayPreview(ServerPlayer player, String scenario, DisplaySide displaySide) {
		player.getInventory().clearContent();
		ItemStack backpack = createBackpackStack();
		IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
		configureItemDisplayPreviewWrapper(backpackWrapper, displaySide, false);
		backpackWrapper.getInventoryHandler().saveInventory();
		player.getInventory().setItem(0, backpack);
		player.getInventory().selected = 0;
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		return new ItemDisplayPreviewSetupResult(scenario, null, null, -1, BuiltInRegistries.ITEM.getKey(backpack.getItem()).toString(), false,
				ItemDisplayPreviewTargetType.BACKPACK);
	}

	private static ItemDisplayPreviewSetupResult setupMovingStoragePreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos basePos,
			DisplaySide displaySide, MovingStoragePreviewVehicle vehicle, ItemStack storageStack, float yRot) {
		Entity entity = switch (vehicle) {
			case MINECART -> new StorageMinecart(level, basePos.getX() + 0.5D, basePos.getY(), basePos.getZ() + 0.5D);
			case BOAT -> new StorageBoat(level, basePos.getX() + 0.5D, basePos.getY(), basePos.getZ() + 0.5D);
			case LLAMA -> {
				Llama llama = EntityType.LLAMA.create(level);
				if (llama == null) {
					throw new IllegalStateException("Failed to create llama for item display preview");
				}
				llama.setTamed(true);
				yield llama;
			}
		};
		entity.moveTo(basePos.getX() + 0.5D, basePos.getY(), basePos.getZ() + 0.5D, yRot, 0);
		if (!(entity instanceof IMovingStorageEntity movingStorageEntity)) {
			throw new IllegalStateException(entity.getClass().getName() + " is not a moving storage entity");
		}
		movingStorageEntity.getStorageHolder().setStorageItemFrom(storageStack, true);
		configureItemDisplayPreviewWrapper(movingStorageEntity.getStorageHolder().getStorageWrapper(), displaySide,
				MovingStorageWrapper.isLimitedBarrel(storageStack));
		level.addFreshEntity(entity);
		return new ItemDisplayPreviewSetupResult(scenario, null, null, entity.getId(), BuiltInRegistries.ITEM.getKey(storageStack.getItem()).toString(),
				MovingStorageWrapper.isLimitedBarrel(storageStack), ItemDisplayPreviewTargetType.MOVING_STORAGE);
	}

	private static ItemDisplayPreviewSetupResult setupCreateContraptionStoragePreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos basePos,
			DisplaySide displaySide, BlockState storageState, boolean limitedBarrel, float cartYaw) {
		return setupCreateContraptionStoragePreview(level, player, scenario, basePos, displaySide, storageState, limitedBarrel, cartYaw, Optional.empty());
	}

	private static ItemDisplayPreviewSetupResult setupCreateContraptionStoragePreview(ServerLevel level, ServerPlayer player, String scenario, BlockPos basePos,
			DisplaySide displaySide, BlockState storageState, boolean limitedBarrel, float cartYaw, Optional<WoodType> woodType) {
		BlockPos assemblerPos = basePos;
		BlockPos storagePos = assemblerPos.above();
		level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
		level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		level.setBlock(assemblerPos, AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
				.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
		level.setBlock(storagePos, storageState, 3);
		StorageBlockEntity storageBlockEntity = WorldHelper.getBlockEntity(level, storagePos, StorageBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Create contraption storage block entity missing for " + scenario));
		if (woodType.isPresent()) {
			if (!(storageBlockEntity instanceof WoodStorageBlockEntity woodStorageBlockEntity)) {
				throw new IllegalStateException("Create contraption storage does not support wood types for " + scenario);
			}
			woodStorageBlockEntity.setWoodType(woodType.get());
		}
		configureItemDisplayPreviewStorage(storageBlockEntity, displaySide, limitedBarrel);

		AbstractContraptionEntity contraptionEntity = assembleCreateCartContraption(level, assemblerPos, cartYaw);
		BlockPos localPos = findMountedStorageLocalPos(contraptionEntity);
		return new ItemDisplayPreviewSetupResult(scenario, null, localPos, contraptionEntity.getId(),
				BuiltInRegistries.BLOCK.getKey(storageState.getBlock()).toString(), limitedBarrel, ItemDisplayPreviewTargetType.CREATE_CONTRAPTION);
	}

	private static ItemDisplayPreviewSetupResult setupCreateContraptionDoubleChestPreview(ServerLevel level, ServerPlayer player, String scenario,
			BlockPos basePos, DisplaySide displaySide, float cartYaw) {
		BlockPos assemblerPos = basePos;
		BlockPos leftPos = assemblerPos.above();
		BlockPos rightPos = leftPos.east();
		level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
		level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		level.setBlock(assemblerPos, AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
				.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
		BlockState leftState = ModBlocks.CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
		BlockState rightState = ModBlocks.CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE,
				ChestType.RIGHT);
		level.setBlock(leftPos, leftState, 3);
		level.setBlock(rightPos, rightState, 3);
		level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(leftPos, rightPos)));

		setPreviewWoodType(level, leftPos, WoodType.BIRCH);
		StorageBlockEntity storageBlockEntity = WorldHelper.getBlockEntity(level, rightPos, StorageBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Create contraption double chest main block entity missing for " + scenario));
		configureItemDisplayPreviewStorage(storageBlockEntity, displaySide, false);

		AbstractContraptionEntity contraptionEntity = assembleCreateCartContraption(level, assemblerPos, cartYaw);
		BlockPos localPos = findMountedDoubleChestLocalPos(contraptionEntity);
		return new ItemDisplayPreviewSetupResult(scenario, null, localPos, contraptionEntity.getId(),
				BuiltInRegistries.BLOCK.getKey(ModBlocks.CHEST.get()).toString(), false, ItemDisplayPreviewTargetType.CREATE_CONTRAPTION);
	}

	private static ItemDisplayPreviewSetupResult setupCreateContraptionBackpackPreview(ServerLevel level, ServerPlayer player, String scenario,
			BlockPos basePos, DisplaySide displaySide, float cartYaw) {
		BlockPos assemblerPos = basePos;
		BlockPos backpackPos = assemblerPos.above();
		level.setBlock(assemblerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
		level.setBlock(assemblerPos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		level.setBlock(assemblerPos, AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, RailShape.EAST_WEST)
				.setValue(CartAssemblerBlock.RAIL_TYPE, CartAssembleRailType.REGULAR).setValue(CartAssemblerBlock.POWERED, true), 3);
		BlockState backpackState = net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks.DIAMOND_BACKPACK.get().defaultBlockState().setValue(BackpackBlock.FACING,
				Direction.NORTH);
		level.setBlock(backpackPos, backpackState, 3);
		BackpackBlockEntity backpackBlockEntity = WorldHelper.getBlockEntity(level, backpackPos, BackpackBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Create contraption backpack block entity missing for " + scenario));
		ItemStack backpack = createBackpackStack();
		IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
		configureItemDisplayPreviewWrapper(backpackWrapper, displaySide, false);
		backpackWrapper.getInventoryHandler().saveInventory();
		backpackBlockEntity.setBackpack(backpack);
		backpackBlockEntity.setChanged();
		WorldHelper.notifyBlockUpdate(backpackBlockEntity);

		AbstractContraptionEntity contraptionEntity = assembleCreateCartContraption(level, assemblerPos, cartYaw);
		BlockPos localPos = findMountedStorageLocalPos(contraptionEntity);
		return new ItemDisplayPreviewSetupResult(scenario, null, localPos, contraptionEntity.getId(),
				BuiltInRegistries.BLOCK.getKey(backpackState.getBlock()).toString(), false, ItemDisplayPreviewTargetType.CREATE_CONTRAPTION);
	}

	private static AbstractContraptionEntity assembleCreateCartContraption(ServerLevel level, BlockPos assemblerPos, float cartYaw) {
		Minecart cart = new Minecart(level, assemblerPos.getX() + 0.5D, assemblerPos.getY(), assemblerPos.getZ() + 0.5D);
		cart.setYRot(cartYaw);
		level.addFreshEntity(cart);
		CartAssemblerBlockEntity assemblerBlockEntity = WorldHelper.getBlockEntity(level, assemblerPos, CartAssemblerBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Create cart assembler block entity missing"));
		assemblerBlockEntity.tryAssemble(cart);
		return cart.getPassengers().stream().filter(AbstractContraptionEntity.class::isInstance).map(AbstractContraptionEntity.class::cast).findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"Create cart assembler did not create a mounted contraption" + (assemblerBlockEntity.getLastAssemblyException() == null
								? ""
								: ": " + assemblerBlockEntity.getLastAssemblyException().component.getString())));
	}

	private static Issue23SetupResult setupStorageIssue23Reproduction(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos motorPos = player.blockPosition().offset(10, 0, 0);
		BlockPos bearingPos = motorPos.above();
		BlockPos rootPlankPos = bearingPos.above();
		BlockPos leftChestPos = rootPlankPos.above();
		BlockPos rightChestPos = leftChestPos.east();
		BlockPos movingPsiPos = rightChestPos.east();
		clearIssue23ReproductionArea(level, motorPos);
		for (int x = -5; x <= 12; x++) {
			for (int z = -5; z <= 5; z++) {
				level.setBlock(motorPos.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
			}
		}

		level.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.UP), 3);
		level.setBlock(bearingPos, AllBlocks.MECHANICAL_BEARING.getDefaultState().setValue(BearingBlock.FACING, Direction.UP), 3);
		for (int x = 0; x < 3; x++) {
			level.setBlock(rootPlankPos.east(x), Blocks.OAK_PLANKS.defaultBlockState(), 3);
		}
		level.setBlock(leftChestPos,
				ModBlocks.CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT), 3);
		level.setBlock(rightChestPos,
				ModBlocks.CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT), 3);
		StorageBlockEntity storage = WorldHelper.getBlockEntity(level, rightChestPos, StorageBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Issue #23 double-chest source block entity missing"));
		storage.getStorageWrapper().getInventoryHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
		storage.getStorageWrapper().getInventoryHandler().setStackInSlot(1, new ItemStack(Items.COBBLESTONE, 32));
		storage.setChanged();
		level.setBlock(movingPsiPos, AllBlocks.PORTABLE_STORAGE_INTERFACE.getDefaultState().setValue(PortableStorageInterfaceBlock.FACING, Direction.EAST), 3);
		level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(rootPlankPos, rootPlankPos.east(2))));
		level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(rootPlankPos, leftChestPos)));
		level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(leftChestPos, rightChestPos)));
		level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(rightChestPos, movingPsiPos)));

		MechanicalBearingBlockEntity bearing = WorldHelper.getBlockEntity(level, bearingPos, MechanicalBearingBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Issue #23 mechanical bearing block entity missing"));
		bearing.assemble();
		ControlledContraptionEntity contraptionEntity = Optional.ofNullable(bearing.getMovedContraption())
				.orElseThrow(() -> new IllegalStateException("Issue #23 mechanical bearing did not assemble the contraption"));
		BlockPos mountedStoragePos = findMountedDoubleChestLocalPos(contraptionEntity);
		BlockPos movingPsiLocalPos = mountedStoragePos.east();
		Vec3 movingPsiConnectionPoint = contraptionEntity
				.toGlobalVector(Vec3.atCenterOf(movingPsiLocalPos).add(Vec3.atLowerCornerOf(Direction.EAST.getNormal()).scale(1.85F)), 1);
		BlockPos stationaryPsiPos = BlockPos.containing(movingPsiConnectionPoint);
		Direction movingPsiFacing = Direction.getNearest(contraptionEntity.applyRotation(Vec3.atLowerCornerOf(Direction.EAST.getNormal()), 1));
		BlockPos chutePos = stationaryPsiPos.below();
		BlockPos receiverPos = chutePos.below();
		level.setBlock(stationaryPsiPos,
				AllBlocks.PORTABLE_STORAGE_INTERFACE.getDefaultState().setValue(PortableStorageInterfaceBlock.FACING, movingPsiFacing.getOpposite()), 3);
		level.setBlock(chutePos, AllBlocks.SMART_CHUTE.getDefaultState().setValue(SmartChuteBlock.POWERED, false), 3);
		SmartChuteBlockEntity chute = WorldHelper.getBlockEntity(level, chutePos, SmartChuteBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Issue #23 Smart Chute block entity missing"));
		FilteringBehaviour filtering = chute.getBehaviour(FilteringBehaviour.TYPE);
		filtering.count = 64;
		filtering.upTo = false;
		chute.setChanged();
		level.setBlock(receiverPos,
				ModBlocks.CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.SINGLE), 3);
		setPreviewWoodType(level, receiverPos, WoodType.ACACIA);

		return new Issue23SetupResult(contraptionEntity.getId(), mountedStoragePos, receiverPos);
	}

	private static void clearIssue23ReproductionArea(ServerLevel level, BlockPos motorPos) {
		level.getEntitiesOfClass(Entity.class, new AABB(motorPos).inflate(10), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
		for (int x = -4; x <= 10; x++) {
			for (int y = -2; y <= 4; y++) {
				for (int z = -3; z <= 3; z++) {
					level.setBlock(motorPos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	private static String issue23ReproductionResultJson(ServerPlayer player, Issue23SetupResult setupResult) {
		Entity entity = player.serverLevel().getEntity(setupResult.contraptionEntityId());
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
			throw new IllegalStateException("Issue #23 contraption is no longer present");
		}
		MountedStorageBase mountedStorage = ContraptionHelper.getMountedStorage(contraptionEntity, setupResult.mountedStoragePos());
		if (mountedStorage == null) {
			throw new IllegalStateException("Issue #23 mounted storage is no longer present");
		}
		int sourceSlot0 = countInMountedStorageSlot(mountedStorage, 0, Items.COBBLESTONE);
		int sourceSlot1 = countInMountedStorageSlot(mountedStorage, 1, Items.COBBLESTONE);
		int receiver = countItemsInStorage(player.serverLevel(), setupResult.receiverPos(), Items.COBBLESTONE);
		JsonObject result = new JsonObject();
		result.addProperty("ok", true);
		result.addProperty("sourceSlot0", sourceSlot0);
		result.addProperty("sourceSlot1", sourceSlot1);
		result.addProperty("sourceTotal", sourceSlot0 + sourceSlot1);
		result.addProperty("receiver", receiver);
		result.addProperty("mountedStoragePos", setupResult.mountedStoragePos().toShortString());
		result.addProperty("receiverPos", setupResult.receiverPos().toShortString());
		result.addProperty("contraptionEntityId", setupResult.contraptionEntityId());
		return result.toString();
	}

	private static int countInMountedStorageSlot(MountedStorageBase mountedStorage, int slot, Item item) {
		ItemStack stack = mountedStorage.getStackInSlot(slot);
		return stack.is(item) ? stack.getCount() : 0;
	}

	private static int countItemsInStorage(ServerLevel level, BlockPos pos, Item item) {
		StorageBlockEntity storage = WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class)
				.orElseThrow(() -> new IllegalStateException("Issue #23 receiver storage block entity missing"));
		return countItems(storage.getStorageWrapper().getInventoryHandler(), item);
	}

	private static BlockPos findMountedStorageLocalPos(AbstractContraptionEntity contraptionEntity) {
		return ContraptionHelper.getMountedItemStorages(contraptionEntity).keySet().stream()
				.filter(localPos -> ContraptionHelper.getMountedStorage(contraptionEntity, localPos) != null).findFirst()
				.orElseThrow(() -> new IllegalStateException("Create contraption did not contain a mounted sophisticated storage"));
	}

	private static BlockPos findMountedDoubleChestLocalPos(AbstractContraptionEntity contraptionEntity) {
		return ContraptionHelper.getMountedItemStorages(contraptionEntity).keySet().stream().filter(localPos -> {
			MountedStorageBase mountedStorage = ContraptionHelper.getMountedStorage(contraptionEntity, localPos);
			return mountedStorage != null && mountedStorage.getStorageStack().getItem() instanceof ChestBlockItem
					&& ChestBlockItem.isDoubleChest(mountedStorage.getStorageStack())
					&& mountedStorage.getStorageStack().has(ModCoreDataComponents.STORAGE_UUID);
		}).findFirst().orElseThrow(() -> new IllegalStateException("Create contraption did not contain a mounted double chest"));
	}

	private static ItemStack createStoragePreviewStack(String storageType) {
		ItemStack storageStack = switch (storageType) {
			case "chest" -> new ItemStack(ModBlocks.CHEST_ITEM.get());
			case "shulker" -> new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get());
			case "limited_barrel" -> new ItemStack(ModBlocks.LIMITED_BARREL_1_ITEM.get());
			default -> new ItemStack(ModBlocks.BARREL_ITEM.get());
		};
		if (storageStack.getItem() instanceof WoodStorageBlockItem) {
			WoodStorageBlockItem.setWoodType(storageStack, WoodType.BIRCH);
		}
		return storageStack;
	}

	private static void configureItemDisplayPreviewStorage(StorageBlockEntity storageBlockEntity, DisplaySide displaySide, boolean limitedBarrel) {
		if (limitedBarrel && storageBlockEntity.getBlockState().getBlock() instanceof LimitedBarrelBlock limitedBarrelBlock) {
			int targetInventorySlots = limitedBarrelBlock.getNumberOfInventorySlots();
			int wrapperSlotDiff = targetInventorySlots - storageBlockEntity.getStorageWrapper().getNumberOfInventorySlots();
			if (wrapperSlotDiff != 0) {
				storageBlockEntity.getStorageWrapper().changeSize(wrapperSlotDiff, 0);
			}
			int handlerSlotDiff = targetInventorySlots - storageBlockEntity.getStorageWrapper().getInventoryHandler().getSlots();
			if (handlerSlotDiff != 0) {
				storageBlockEntity.getStorageWrapper().getInventoryHandler().changeSlots(handlerSlotDiff);
			}
		}
		if (storageBlockEntity instanceof WoodStorageBlockEntity woodStorageBlockEntity) {
			woodStorageBlockEntity.setWoodType(WoodType.BIRCH);
		}
		configureItemDisplayPreviewWrapper(storageBlockEntity.getStorageWrapper(), displaySide, limitedBarrel);
		storageBlockEntity.setChanged();
		WorldHelper.notifyBlockUpdate(storageBlockEntity);
	}

	private static void setPreviewWoodType(ServerLevel level, BlockPos pos, WoodType woodType) {
		WoodStorageBlockEntity woodStorageBlockEntity = WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class).orElseThrow();
		woodStorageBlockEntity.setWoodType(woodType);
		woodStorageBlockEntity.setChanged();
		WorldHelper.notifyBlockUpdate(woodStorageBlockEntity);
	}

	private static void configureItemDisplayPreviewWrapper(IStorageWrapper storageWrapper, DisplaySide displaySide, boolean limitedBarrel) {
		storageWrapper.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.IRON_AXE));
		if (limitedBarrel) {
			LimitedBarrelBlockEntity.setFixedSettings(storageWrapper, storageWrapper.getInventoryHandler().getSlots());
		}
		ItemDisplaySettingsCategory itemDisplaySettings = storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
		if (!itemDisplaySettings.getSlots().contains(0)) {
			itemDisplaySettings.selectSlot(0);
		}
		itemDisplaySettings.setDisplaySide(displaySide);
		itemDisplaySettings.itemsChanged();
	}

	private static String openStorageInventory(ServerPlayer player, BlockPos pos) {
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		Direction hitDirection = getStorageOpenHitDirection(player.serverLevel().getBlockState(pos));
		Vec3 hitLocation = Vec3.atCenterOf(pos).add(hitDirection.getStepX() * 0.5D, hitDirection.getStepY() * 0.5D, hitDirection.getStepZ() * 0.5D);
		BlockHitResult hitResult = new BlockHitResult(hitLocation, hitDirection, pos, false);
		player.gameMode.useItemOn(player, player.serverLevel(), player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hitResult);
		return "";
	}

	private static String openMainBackpack(ServerPlayer player) {
		ItemStack mainBackpack = player.getInventory().getItem(0);
		if (!(mainBackpack.getItem() instanceof BackpackItem backpackItem)) {
			return "{\"ok\":false,\"error\":\"No backpack in player inventory slot 0\"}";
		}
		player.getInventory().selected = 0;
		backpackItem.use(player.level(), player, InteractionHand.MAIN_HAND);
		JsonObject result = new JsonObject();
		result.addProperty("ok", true);
		result.addProperty("serverMenu", player.containerMenu.getClass().getName());
		return result.toString();
	}

	private static String openMovingStorageInventory(ServerPlayer player, int entityId) {
		Entity entity = player.serverLevel().getEntity(entityId);
		if (!(entity instanceof IMovingStorageEntity movingStorageEntity)) {
			throw new IllegalStateException("Moving storage entity missing for id " + entityId);
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		if (entity instanceof StorageBoat storageBoat) {
			storageBoat.interactWithContainerVehicle(player);
		} else if (entity instanceof StorageMinecart storageMinecart) {
			storageMinecart.interact(player, InteractionHand.MAIN_HAND);
		} else if (entity instanceof Llama llama) {
			llama.openCustomInventoryScreen(player);
		} else {
			movingStorageEntity.getStorageHolder().openContainerMenu(player);
		}
		return "";
	}

	private static String openCreateContraptionStorage(ServerPlayer player, int entityId, BlockPos localPos) {
		Entity entity = player.serverLevel().getEntity(entityId);
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) {
			throw new IllegalStateException("Create contraption entity missing for id " + entityId);
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		if (!contraptionEntity.handlePlayerInteraction(player, localPos, Direction.UP, InteractionHand.MAIN_HAND)) {
			throw new IllegalStateException("Create contraption did not open mounted storage at " + localPos);
		}
		return "";
	}

	private static Direction getStorageOpenHitDirection(BlockState state) {
		if (state.getBlock() instanceof BarrelBlock barrelBlock && barrelBlock.getFacing(state) == Direction.UP) {
			return Direction.NORTH;
		}
		return Direction.UP;
	}

	private static void waitForClientStorageBlockEntity(BlockPos pos, boolean limitedBarrel) {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> isClientStorageBlockEntityReady(pos, limitedBarrel))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for client storage block entity at " + pos);
	}

	private static boolean isClientStorageBlockEntityReady(BlockPos pos, boolean limitedBarrel) {
		if (Minecraft.getInstance().level == null) {
			return false;
		}
		if (limitedBarrel && !(Minecraft.getInstance().level.getBlockState(pos).getBlock() instanceof LimitedBarrelBlock)) {
			return false;
		}
		return WorldHelper.getBlockEntity(Minecraft.getInstance().level, pos, StorageBlockEntity.class).map(storageBlockEntity -> {
			if (!limitedBarrel) {
				return true;
			}
			if (!(storageBlockEntity.getBlockState().getBlock() instanceof LimitedBarrelBlock limitedBarrelBlock)) {
				return false;
			}
			return storageBlockEntity.getStorageWrapper().getInventoryHandler().getSlots() == limitedBarrelBlock.getNumberOfInventorySlots();
		}).orElse(false);
	}

	private static void waitForClientDecorationTable(BlockPos pos, Item resultItem) {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null
					&& WorldHelper.getBlockEntity(Minecraft.getInstance().level, pos, DecorationTableBlockEntity.class)
							.map(table -> table.getResult().is(resultItem)).orElse(false))) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for client decoration table at " + pos);
	}

	private static void waitForClientDecorationTableScreen() {
		waitForClientScreen("decoration table screen", () -> Minecraft.getInstance().screen instanceof DecorationTableScreen);
	}

	private static JsonObject getDecorationTableRenderBoundsJson() {
		Screen screen = Minecraft.getInstance().screen;
		if (!(screen instanceof DecorationTableScreen decorationTableScreen)) {
			throw new IllegalStateException("Decoration table screen is not open");
		}
		DecorationTableMenu menu = decorationTableScreen.getMenu();
		Slot lastDyeSlot = menu.getSlot(menu.getDyeSlotRange().firstSlot() + menu.getDyeSlotRange().numberOfSlots() - 1);
		Slot resultSlot = menu.getResultSlot();
		int x = decorationTableScreen.getGuiLeft() + lastDyeSlot.x + 26;
		int y = decorationTableScreen.getGuiTop() + lastDyeSlot.y;
		int resultSlotX = decorationTableScreen.getGuiLeft() + resultSlot.x;
		int resultSlotY = decorationTableScreen.getGuiTop() + resultSlot.y;
		Slot topCoreSlot = menu.getSlot(DecorationTableBlockEntity.TOP_CORE_SLOT);
		Slot sideCoreSlot = menu.getSlot(DecorationTableBlockEntity.SIDE_CORE_SLOT);
		Slot bottomCoreSlot = menu.getSlot(DecorationTableBlockEntity.BOTTOM_CORE_SLOT);
		JsonObject result = new JsonObject();
		JsonObject preview = new JsonObject();
		preview.addProperty("x", x);
		preview.addProperty("y", y);
		preview.addProperty("width", 80);
		preview.addProperty("height", resultSlot.y - lastDyeSlot.y + 20);
		result.add("preview", preview);
		JsonObject resultSlotBounds = new JsonObject();
		resultSlotBounds.addProperty("x", resultSlotX);
		resultSlotBounds.addProperty("y", resultSlotY);
		resultSlotBounds.addProperty("width", 16);
		resultSlotBounds.addProperty("height", 16);
		result.add("resultSlot", resultSlotBounds);
		JsonObject coreSlotHoverTargets = new JsonObject();
		coreSlotHoverTargets.add("top", getDecorationTableCoreSlotHoverTargetJson(decorationTableScreen, topCoreSlot));
		coreSlotHoverTargets.add("side", getDecorationTableCoreSlotHoverTargetJson(decorationTableScreen, sideCoreSlot));
		coreSlotHoverTargets.add("bottom", getDecorationTableCoreSlotHoverTargetJson(decorationTableScreen, bottomCoreSlot));
		result.add("coreSlotHoverTargets", coreSlotHoverTargets);
		return result;
	}

	private static JsonObject getDecorationTableCoreSlotHoverTargetJson(DecorationTableScreen screen, Slot slot) {
		JsonObject result = new JsonObject();
		result.addProperty("x", screen.getGuiLeft() + slot.x + 8);
		result.addProperty("y", screen.getGuiTop() + slot.y + 8);
		return result;
	}

	private static void waitForClientBackpackInHotbar() {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().player != null
					&& Minecraft.getInstance().player.getInventory().getItem(0).getItem() instanceof BackpackItem)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for client backpack in hotbar slot 0");
	}

	private static void waitForClientEntity(int entityId) {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().level != null && Minecraft.getInstance().level.getEntity(entityId) != null)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for client entity " + entityId);
	}

	private static void waitForStorageScreen() {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof StorageContainerMenuBase<?>)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for storage screen");
	}

	private static void waitForStorageScreenContents() {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(() -> Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
					&& screen.getMenu() instanceof StorageContainerMenuBase<?> && screen.getMenu().getStateId() > 0)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for storage screen contents");
	}

	private static void waitForStorageScreenAndClickSettingsTab() {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(StoragePreviewScenarios::clickStorageSettingsTabIfReady)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting to click storage settings tab");
	}

	private static boolean clickStorageSettingsTabIfReady() {
		Screen screen = Minecraft.getInstance().screen;
		if (!(screen instanceof AbstractContainerScreen<?> containerScreen) || !(containerScreen.getMenu() instanceof StorageContainerMenuBase<?>)) {
			return false;
		}
		StorageSettingsTab settingsTab = findChild(screen, StorageSettingsTab.class)
				.orElseThrow(() -> new IllegalStateException("Storage settings tab was not present on " + screen.getClass().getSimpleName()));
		settingsTab.mouseClicked(settingsTab.getX() + 9, settingsTab.getY() + 12, 0);
		return true;
	}

	private static String waitForSettingsScreenAndOpenItemDisplayTab() {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5_000L);
		while (System.nanoTime() < deadline) {
			String screenName = AutomationRuntime.runOnClient(StoragePreviewScenarios::openItemDisplaySettingsTabIfReady);
			if (!screenName.isEmpty()) {
				return screenName;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for storage settings screen");
	}

	private static String openItemDisplaySettingsTabIfReady() {
		Screen screen = Minecraft.getInstance().screen;
		if (!(screen instanceof SettingsScreen settingsScreen)) {
			return "";
		}
		for (GuiEventListener child : settingsScreen.getSettingsTabControl().children()) {
			if (child instanceof ItemDisplaySettingsTab itemDisplaySettingsTab) {
				Optional<SettingsTab<?>> openTab = settingsScreen.getSettingsTabControl().getOpenTab();
				if (openTab.map(tab -> tab == itemDisplaySettingsTab).orElse(false)) {
					return screen.getClass().getSimpleName();
				}
				itemDisplaySettingsTab.mouseClicked(itemDisplaySettingsTab.getX() + 9, itemDisplaySettingsTab.getY() + 12, 0);
				return "";
			}
		}
		throw new IllegalStateException("Item display settings tab was not present on " + screen.getClass().getSimpleName());
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

	private static void waitForClientScreen(String description, BooleanSupplier condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnClient(condition::getAsBoolean)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static void waitForServerCondition(String description, Function<ServerPlayer, Boolean> condition) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (AutomationRuntime.runOnServer(condition)) {
				return;
			}
			sleep(50);
		}
		throw new IllegalStateException("Timed out waiting for " + description);
	}

	private static Issue23SetupResult getIssue23SetupResult() {
		if (issue23SetupResult == null) {
			throw new IllegalStateException("Issue #23 reproduction has not been set up");
		}
		return issue23SetupResult;
	}

	private static ItemStack createBackpackStack() {
		ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 80);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 5);
		return backpack;
	}

	private static int countItems(InventoryHandler inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private enum ItemDisplayPreviewTargetType {
		PLACED_STORAGE, BACKPACK, MOVING_STORAGE, CREATE_CONTRAPTION
	}

	private enum MovingStoragePreviewVehicle {
		MINECART, BOAT, LLAMA
	}

	private record ItemDisplayPreviewSetupResult(String scenario, BlockPos menuPos, BlockPos localPos, int entityId, String target, boolean limitedBarrel,
			ItemDisplayPreviewTargetType targetType) {
	}

	private record DecorationTableRenderPreviewSetupResult(String itemName, BlockPos tablePos, Item resultItem) {
	}

	private record Issue23SetupResult(int contraptionEntityId, BlockPos mountedStoragePos, BlockPos receiverPos) {
	}
}
