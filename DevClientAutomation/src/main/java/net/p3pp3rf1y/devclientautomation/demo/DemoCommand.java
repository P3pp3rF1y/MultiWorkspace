package net.p3pp3rf1y.devclientautomation.demo;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class DemoCommand {
	private static boolean quiet = false;

	private DemoCommand() {
	}

	public static void init() {
		NeoForge.EVENT_BUS.addListener(DemoCommand::registerCommands);
		NeoForge.EVENT_BUS.addListener(DemoPlayback::tick);
	}

	private static void registerCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		var lookedAtContainer = Commands.literal("lookedAt")
				.then(Commands.literal("clear").executes(context -> clearLookedAtContainer(context.getSource(), true)))
				.then(Commands.literal("set").then(Commands.argument("slot", IntegerArgumentType.integer(0))
						.then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
										.executes(context -> setLookedAtContainerSlot(context.getSource(), IntegerArgumentType.getInteger(context, "slot"),
												ItemArgument.getItem(context, "item"), IntegerArgumentType.getInteger(context, "count"), false, true))))))
				.then(Commands.literal("only")
						.then(Commands.argument("slot", IntegerArgumentType.integer(0))
								.then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
										.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
												.executes(context -> setLookedAtContainerSlot(context.getSource(),
														IntegerArgumentType.getInteger(context, "slot"), ItemArgument.getItem(context, "item"),
														IntegerArgumentType.getInteger(context, "count"), true, true))))));

		var blockContainer = Commands.literal("block")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.literal("clear")
								.executes(context -> clearContainer(context.getSource(), context.getSource().getLevel().dimension().location().toString(),
										BlockPosArgument.getLoadedBlockPos(context, "pos"), true)))
						.then(Commands.literal("set").then(Commands.argument("slot", IntegerArgumentType.integer(0)).then(Commands
								.argument("item", ItemArgument.item(event.getBuildContext()))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
										.executes(context -> setContainerSlot(
												context.getSource(), context.getSource().getLevel().dimension().location().toString(),
												BlockPosArgument.getLoadedBlockPos(context, "pos"), IntegerArgumentType.getInteger(context, "slot"),
												ItemArgument.getItem(context, "item"), IntegerArgumentType.getInteger(context, "count"), false, true))))))
						.then(Commands.literal("only").then(Commands.argument("slot", IntegerArgumentType.integer(0)).then(Commands
								.argument("item", ItemArgument.item(event.getBuildContext()))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
										.executes(context -> setContainerSlot(context.getSource(),
												context.getSource().getLevel().dimension().location().toString(),
												BlockPosArgument.getLoadedBlockPos(context, "pos"), IntegerArgumentType.getInteger(context, "slot"),
												ItemArgument.getItem(context, "item"), IntegerArgumentType.getInteger(context, "count"), true, true)))))));

		var nearbyStorageTarget = Commands.literal("nearby").then(Commands.literal("list").executes(context -> listNearbyStorageTargets(context.getSource())))
				.then(Commands.argument("index", IntegerArgumentType.integer(0))
						.then(Commands.literal("clear")
								.executes(context -> clearNearbyStorageTarget(context.getSource(), IntegerArgumentType.getInteger(context, "index"), true)))
						.then(Commands.literal("set").then(Commands.argument("slot", IntegerArgumentType.integer(0)).then(Commands
								.argument("item", ItemArgument.item(event.getBuildContext()))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
										.executes(context -> setNearbyStorageTargetSlot(context.getSource(), IntegerArgumentType.getInteger(context, "index"),
												IntegerArgumentType.getInteger(context, "slot"), ItemArgument.getItem(context, "item"),
												IntegerArgumentType.getInteger(context, "count"), false, true))))))
						.then(Commands.literal("only").then(Commands.argument("slot", IntegerArgumentType.integer(0)).then(Commands
								.argument("item", ItemArgument.item(event.getBuildContext()))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
										.executes(context -> setNearbyStorageTargetSlot(context.getSource(), IntegerArgumentType.getInteger(context, "index"),
												IntegerArgumentType.getInteger(context, "slot"), ItemArgument.getItem(context, "item"),
												IntegerArgumentType.getInteger(context, "count"), true, true)))))));

		var nearestStorageTarget = Commands.literal("nearest")
				.then(Commands.argument("x", DoubleArgumentType.doubleArg())
						.then(Commands.argument("y", DoubleArgumentType.doubleArg()).then(Commands
								.argument("z", DoubleArgumentType.doubleArg()).then(Commands.literal("clear").executes(context -> clearNearestStorageTarget(
										context.getSource(),
										new Vec3(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"),
												DoubleArgumentType.getDouble(context, "z")),
										true)))
								.then(Commands.literal("set").then(Commands.argument("slot", IntegerArgumentType.integer(0)).then(Commands.argument("item",
										ItemArgument.item(event.getBuildContext())).then(
												Commands.argument("count", IntegerArgumentType.integer(1, 64))
														.executes(context -> setNearestStorageTargetSlot(context.getSource(),
																new Vec3(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"),
																		DoubleArgumentType.getDouble(context, "z")),
																IntegerArgumentType.getInteger(context, "slot"), ItemArgument.getItem(context, "item"),
																IntegerArgumentType.getInteger(context, "count"), false, true))))))
								.then(Commands.literal("only").then(Commands.argument("slot", IntegerArgumentType.integer(0))
										.then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
												.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
														.executes(context -> setNearestStorageTargetSlot(context.getSource(),
																new Vec3(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"),
																		DoubleArgumentType.getDouble(context, "z")),
																IntegerArgumentType.getInteger(context, "slot"), ItemArgument.getItem(context, "item"),
																IntegerArgumentType.getInteger(context, "count"), true, true)))))))));

		dispatcher.register(Commands.literal("demo")
				.then(Commands.literal("new")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(context -> newDemo(context.getSource(), StringArgumentType.getString(context, "name")))))
				.then(Commands.literal("save").executes(context -> saveDemo(context.getSource())))
				.then(Commands.literal("quiet").then(Commands.literal("on").executes(context -> setQuiet(context.getSource(), true)))
						.then(Commands.literal("off").executes(context -> setQuiet(context.getSource(), false))))
				.then(Commands.literal("wait")
						.then(Commands.argument("ticks", IntegerArgumentType.integer(0))
								.executes(context -> wait(context.getSource(), IntegerArgumentType.getInteger(context, "ticks"), true))))
				.then(Commands.literal("run")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(context -> runDemo(context.getSource(), StringArgumentType.getString(context, "name")))))
				.then(Commands.literal("marker")
						.then(Commands.literal("set")
								.then(Commands.argument("name", StringArgumentType.word())
										.executes(context -> recordOnly(context.getSource(), "marker set " + StringArgumentType.getString(context, "name"))))))
				.then(Commands.literal("player").then(Commands.literal("clearInventory").executes(context -> clearInventory(context.getSource(), true)))
						.then(Commands.literal("hotbar").then(Commands.literal("set").then(Commands.argument("slot", IntegerArgumentType.integer(0, 8))
								.then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
										.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
												.executes(context -> setHotbar(context.getSource(), IntegerArgumentType.getInteger(context, "slot"),
														ItemArgument.getItem(context, "item"), IntegerArgumentType.getInteger(context, "count"), true))))))
								.then(Commands.literal("select")
										.then(Commands.argument("slot", IntegerArgumentType.integer(0, 8)).executes(
												context -> selectHotbar(context.getSource(), IntegerArgumentType.getInteger(context, "slot"), true))))))
				.then(Commands.literal("container").then(lookedAtContainer).then(blockContainer))
				.then(Commands.literal("storageTarget").then(nearbyStorageTarget).then(nearestStorageTarget))
				.then(Commands.literal("backpack").then(Commands.literal("giveConfigured").then(Commands.argument("mode", StringArgumentType.word())
						.executes(context -> giveConfiguredBackpack(context.getSource(), StringArgumentType.getString(context, "mode"), List.of(), true))
						.then(Commands.literal("items")
								.then(Commands.argument("items", StringArgumentType.greedyString())
										.executes(context -> giveConfiguredBackpack(context.getSource(), StringArgumentType.getString(context, "mode"),
												parseItemSeeds(StringArgumentType.getString(context, "items")), true))))))
						.then(Commands.literal("open").executes(context -> openBackpack(context.getSource(), true))))
				.then(Commands.literal("step").then(Commands.literal("closeScreen").executes(context -> closeScreen(context.getSource(), true)))
						.then(Commands.literal("keybind")
								.then(Commands.argument("action", StringArgumentType.word()).executes(
										context -> triggerKeybindAction(context.getSource(), StringArgumentType.getString(context, "action"), true)))))
				.then(Commands.literal("segment")
						.then(Commands.literal("record")
								.then(Commands.argument("name", StringArgumentType.word()).executes(
										context -> recordOnly(context.getSource(), "segment record " + StringArgumentType.getString(context, "name")))))
						.then(Commands.literal("stop").executes(context -> recordOnly(context.getSource(), "segment stop")))));
	}

	static void success(CommandSourceStack source, Supplier<Component> message) {
		if (!quiet) {
			source.sendSuccess(message, false);
		}
	}

	private static int setQuiet(CommandSourceStack source, boolean enabled) {
		quiet = enabled;
		if (!enabled) {
			source.sendSuccess(() -> Component.literal("Demo messages enabled"), false);
		}
		return 1;
	}

	private static int newDemo(CommandSourceStack source, String name) {
		DemoSession.get().start(name);
		success(source, () -> Component.literal("Started demo " + name));
		return 1;
	}

	private static int saveDemo(CommandSourceStack source) {
		try {
			Path path = DemoSession.get().save();
			success(source, () -> Component.literal("Saved " + DemoSession.get().size() + " demo steps to " + path));
			return 1;
		} catch (IOException | IllegalStateException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int runDemo(CommandSourceStack source, String name) {
		try {
			if (DemoPlayback.isRunning()) {
				source.sendFailure(Component.literal("A demo is already running"));
				return 0;
			}
			List<String> commands = DemoSession.get().load(name);
			DemoPlayback.start(source, name, commands);
			success(source, () -> Component.literal("Started demo playback " + name + " with " + commands.size() + " steps"));
			return commands.size();
		} catch (IOException e) {
			source.sendFailure(Component.literal("Failed to load demo " + name + ": " + e.getMessage()));
			return 0;
		}
	}

	static int executeRecordedCommand(CommandSourceStack source, String command) {
		String[] parts = command.split(" ");
		if (parts.length == 0) {
			return 0;
		}
		if (command.equals("player clearInventory")) {
			return clearInventory(source, false);
		}
		if (parts.length == 6 && parts[0].equals("player") && parts[1].equals("hotbar") && parts[2].equals("set")) {
			return setHotbar(source, Integer.parseInt(parts[3]), parts[4], Integer.parseInt(parts[5]), false);
		}
		if (parts.length == 4 && parts[0].equals("player") && parts[1].equals("hotbar") && parts[2].equals("select")) {
			return selectHotbar(source, Integer.parseInt(parts[3]), false);
		}
		if (parts.length == 7 && parts[0].equals("container") && parts[1].equals("block") && parts[6].equals("clear")) {
			return clearContainer(source, parts[2], new BlockPos(Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5])), false);
		}
		if (parts.length == 10 && parts[0].equals("container") && parts[1].equals("block") && parts[6].equals("set")) {
			return setContainerSlot(source, parts[2], new BlockPos(Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5])),
					Integer.parseInt(parts[7]), parts[8], Integer.parseInt(parts[9]), false, false);
		}
		if (parts.length == 10 && parts[0].equals("container") && parts[1].equals("block") && parts[6].equals("only")) {
			return setContainerSlot(source, parts[2], new BlockPos(Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5])),
					Integer.parseInt(parts[7]), parts[8], Integer.parseInt(parts[9]), true, false);
		}
		if (parts.length == 4 && parts[0].equals("storageTarget") && parts[1].equals("nearby") && parts[3].equals("clear")) {
			return clearNearbyStorageTarget(source, Integer.parseInt(parts[2]), false);
		}
		if (parts.length == 7 && parts[0].equals("storageTarget") && parts[1].equals("nearby") && parts[3].equals("set")) {
			return setNearbyStorageTargetSlot(source, Integer.parseInt(parts[2]), Integer.parseInt(parts[4]), parts[5], Integer.parseInt(parts[6]), false,
					false);
		}
		if (parts.length == 7 && parts[0].equals("storageTarget") && parts[1].equals("nearby") && parts[3].equals("only")) {
			return setNearbyStorageTargetSlot(source, Integer.parseInt(parts[2]), Integer.parseInt(parts[4]), parts[5], Integer.parseInt(parts[6]), true,
					false);
		}
		if (parts.length == 6 && parts[0].equals("storageTarget") && parts[1].equals("nearest") && parts[5].equals("clear")) {
			return clearNearestStorageTarget(source, new Vec3(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4])), false);
		}
		if (parts.length == 9 && parts[0].equals("storageTarget") && parts[1].equals("nearest") && parts[5].equals("set")) {
			return setNearestStorageTargetSlot(source, new Vec3(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4])),
					Integer.parseInt(parts[6]), parts[7], Integer.parseInt(parts[8]), false, false);
		}
		if (parts.length == 9 && parts[0].equals("storageTarget") && parts[1].equals("nearest") && parts[5].equals("only")) {
			return setNearestStorageTargetSlot(source, new Vec3(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4])),
					Integer.parseInt(parts[6]), parts[7], Integer.parseInt(parts[8]), true, false);
		}
		if (parts.length == 3 && parts[0].equals("backpack") && parts[1].equals("giveConfigured")) {
			return giveConfiguredBackpack(source, parts[2], List.of(), false);
		}
		if (parts.length >= 6 && parts[0].equals("backpack") && parts[1].equals("giveConfigured") && parts[3].equals("items")) {
			return giveConfiguredBackpack(source, parts[2], parseItemSeeds(parts, 4), false);
		}
		if (command.equals("backpack open")) {
			return openBackpack(source, false);
		}
		if (command.equals("step closeScreen")) {
			return closeScreen(source, false);
		}
		if (parts.length == 3 && parts[0].equals("step") && parts[1].equals("keybind")) {
			return triggerKeybindAction(source, parts[2], false);
		}
		return 1;
	}

	static int waitTicks(String command) {
		String[] parts = command.split(" ");
		if (parts.length == 2 && parts[0].equals("wait")) {
			return Integer.parseInt(parts[1]);
		}
		return -1;
	}

	private static int wait(CommandSourceStack source, int ticks, boolean record) {
		if (record) {
			DemoSession.get().record("wait " + ticks);
		}
		success(source, () -> Component.literal("Recorded wait for " + ticks + " ticks"));
		return 1;
	}

	private static int recordOnly(CommandSourceStack source, String command) {
		DemoSession.get().record(command);
		success(source, () -> Component.literal("Recorded demo step: " + command));
		return 1;
	}

	private static int clearInventory(CommandSourceStack source, boolean record) {
		try {
			clearPlayerInventory(source.getPlayerOrException());
			if (record) {
				DemoSession.get().record("player clearInventory");
			}
			success(source, () -> Component.literal("Cleared player inventory"));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int setHotbar(CommandSourceStack source, int slot, ItemInput itemInput, int count, boolean record) throws CommandSyntaxException {
		try {
			ItemStack stack = itemInput.createItemStack(count, false);
			String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			ServerPlayer player = source.getPlayerOrException();
			player.getInventory().setItem(slot, stack);
			player.getInventory().setChanged();
			if (record) {
				DemoSession.get().record("player hotbar set " + slot + " " + itemName + " " + count);
			}
			success(source, () -> Component.literal("Set hotbar slot " + slot + " to " + count + "x " + itemName));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int setHotbar(CommandSourceStack source, int slot, String itemName, int count, boolean record) {
		try {
			Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemName))
					.orElseThrow(() -> new IllegalArgumentException("Unknown item " + itemName));
			ServerPlayer player = source.getPlayerOrException();
			player.getInventory().setItem(slot, new ItemStack(item, count));
			player.getInventory().setChanged();
			if (record) {
				DemoSession.get().record("player hotbar set " + slot + " " + itemName + " " + count);
			}
			success(source, () -> Component.literal("Set hotbar slot " + slot + " to " + count + "x " + itemName));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int selectHotbar(CommandSourceStack source, int slot, boolean record) {
		try {
			source.getPlayerOrException().getInventory().selected = slot;
			if (record) {
				DemoSession.get().record("player hotbar select " + slot);
			}
			success(source, () -> Component.literal("Selected hotbar slot " + slot));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int clearLookedAtContainer(CommandSourceStack source, boolean record) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			BlockPos pos = getLookedAtBlock(player);
			String dimension = player.serverLevel().dimension().location().toString();
			return clearContainer(source, dimension, pos, record);
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int setLookedAtContainerSlot(CommandSourceStack source, int slot, ItemInput itemInput, int count, boolean clearFirst, boolean record)
			throws CommandSyntaxException {
		try {
			ServerPlayer player = source.getPlayerOrException();
			BlockPos pos = getLookedAtBlock(player);
			ItemStack stack = itemInput.createItemStack(count, false);
			String dimension = player.serverLevel().dimension().location().toString();
			String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			return setContainerSlot(source, dimension, pos, slot, itemName, count, clearFirst, record);
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int setContainerSlot(CommandSourceStack source, String dimension, BlockPos pos, int slot, ItemInput itemInput, int count, boolean clearFirst,
			boolean record) throws CommandSyntaxException {
		try {
			ItemStack stack = itemInput.createItemStack(count, false);
			String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			return setContainerSlot(source, dimension, pos, slot, itemName, count, clearFirst, record);
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int clearContainer(CommandSourceStack source, String dimension, BlockPos pos, boolean record) {
		try {
			ServerLevel level = getLevel(source, dimension);
			IItemHandler itemHandler = getContainerHandler(level, pos);
			int cleared = clearItemHandler(itemHandler);
			markContainerChanged(level, pos);
			if (record) {
				DemoSession.get().record(containerCommand(dimension, pos, "clear"));
			}
			success(source, () -> Component.literal("Cleared " + cleared + " items from container at " + pos.toShortString()));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int setContainerSlot(CommandSourceStack source, String dimension, BlockPos pos, int slot, String itemName, int count, boolean clearFirst,
			boolean record) {
		try {
			ServerLevel level = getLevel(source, dimension);
			IItemHandler itemHandler = getContainerHandler(level, pos);
			if (slot >= itemHandler.getSlots()) {
				throw new IllegalArgumentException("Container only has " + itemHandler.getSlots() + " slots");
			}
			Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemName))
					.orElseThrow(() -> new IllegalArgumentException("Unknown item " + itemName));
			if (clearFirst) {
				clearItemHandler(itemHandler);
			}
			ItemStack stack = new ItemStack(item, count);
			ItemStack remaining = setItemHandlerSlot(itemHandler, slot, stack);
			markContainerChanged(level, pos);
			if (record) {
				DemoSession.get().record(containerCommand(dimension, pos, clearFirst ? "only" : "set") + " " + slot + " " + itemName + " " + count);
			}
			success(source, () -> Component.literal("Set container at " + pos.toShortString() + " slot " + slot + " to " + count + "x " + itemName
					+ (remaining.isEmpty() ? "" : " with " + remaining.getCount() + " remaining")));
			return remaining.isEmpty() ? 1 : 0;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int listNearbyStorageTargets(CommandSourceStack source) {
		try {
			List<StorageTarget> targets = getNearbyStorageTargets(source.getPlayerOrException());
			if (targets.isEmpty()) {
				source.sendFailure(Component.literal("No SIA storage targets found nearby"));
				return 0;
			}

			for (int i = 0; i < targets.size(); i++) {
				StorageTarget target = targets.get(i);
				success(source, () -> Component.literal("[" + target.index() + "] " + target.kind() + " at " + formatPosition(target.position()) + " with "
						+ target.itemHandler().getSlots() + " slots"));
			}
			return targets.size();
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int clearNearbyStorageTarget(CommandSourceStack source, int index, boolean record) {
		try {
			StorageTarget target = getNearbyStorageTarget(source, index);
			return clearStorageTarget(source, target, record ? targetCommand(target.position(), "clear") : null);
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int clearNearestStorageTarget(CommandSourceStack source, Vec3 position, boolean record) {
		try {
			StorageTarget target = getNearestStorageTarget(source, position);
			return clearStorageTarget(source, target, record ? targetCommand(position, "clear") : null);
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int clearStorageTarget(CommandSourceStack source, StorageTarget target, String recordedCommand) {
		int cleared = clearItemHandler(target.itemHandler());
		if (recordedCommand != null) {
			DemoSession.get().record(recordedCommand);
		}
		success(source, () -> Component.literal("Cleared " + cleared + " items from storage target [" + target.index() + "] " + target.kind()));
		return 1;
	}

	private static int setNearbyStorageTargetSlot(CommandSourceStack source, int index, int slot, ItemInput itemInput, int count, boolean clearFirst,
			boolean record) throws CommandSyntaxException {
		try {
			ItemStack stack = itemInput.createItemStack(count, false);
			String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			return setNearbyStorageTargetSlot(source, index, slot, itemName, count, clearFirst, record);
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int setNearestStorageTargetSlot(CommandSourceStack source, Vec3 position, int slot, ItemInput itemInput, int count, boolean clearFirst,
			boolean record) throws CommandSyntaxException {
		try {
			ItemStack stack = itemInput.createItemStack(count, false);
			String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			return setNearestStorageTargetSlot(source, position, slot, itemName, count, clearFirst, record);
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int setNearbyStorageTargetSlot(CommandSourceStack source, int index, int slot, String itemName, int count, boolean clearFirst,
			boolean record) {
		try {
			StorageTarget target = getNearbyStorageTarget(source, index);
			return setStorageTargetSlot(source, target, slot, itemName, count, clearFirst,
					record ? targetCommand(target.position(), clearFirst ? "only" : "set") + " " + slot + " " + itemName + " " + count : null);
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int setNearestStorageTargetSlot(CommandSourceStack source, Vec3 position, int slot, String itemName, int count, boolean clearFirst,
			boolean record) {
		try {
			StorageTarget target = getNearestStorageTarget(source, position);
			return setStorageTargetSlot(source, target, slot, itemName, count, clearFirst,
					record ? targetCommand(position, clearFirst ? "only" : "set") + " " + slot + " " + itemName + " " + count : null);
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int setStorageTargetSlot(CommandSourceStack source, StorageTarget target, int slot, String itemName, int count, boolean clearFirst,
			String recordedCommand) {
		try {
			IItemHandler itemHandler = target.itemHandler();
			if (slot >= itemHandler.getSlots()) {
				throw new IllegalArgumentException("Storage target only has " + itemHandler.getSlots() + " slots");
			}
			Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemName))
					.orElseThrow(() -> new IllegalArgumentException("Unknown item " + itemName));
			if (clearFirst) {
				clearItemHandler(itemHandler);
			}
			ItemStack remaining = setItemHandlerSlot(itemHandler, slot, new ItemStack(item, count));
			if (recordedCommand != null) {
				DemoSession.get().record(recordedCommand);
			}
			success(source, () -> Component.literal("Set storage target [" + target.index() + "] " + target.kind() + " slot " + slot + " to " + count + "x "
					+ itemName + (remaining.isEmpty() ? "" : " with " + remaining.getCount() + " remaining")));
			return remaining.isEmpty() ? 1 : 0;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static StorageTarget getNearbyStorageTarget(CommandSourceStack source, int index) throws CommandSyntaxException {
		List<StorageTarget> targets = getNearbyStorageTargets(source.getPlayerOrException());
		if (index >= targets.size()) {
			throw new IllegalArgumentException("Only found " + targets.size() + " SIA storage targets nearby");
		}
		return targets.get(index);
	}

	private static StorageTarget getNearestStorageTarget(CommandSourceStack source, Vec3 position) throws CommandSyntaxException {
		return getNearbyStorageTargets(source.getPlayerOrException()).stream()
				.min(Comparator.comparingDouble(target -> target.position().distanceToSqr(position)))
				.orElseThrow(() -> new IllegalArgumentException("No SIA storage targets found nearby"));
	}

	private static List<StorageTarget> getNearbyStorageTargets(ServerPlayer player) {
		try {
			SiaReflection sia = SiaReflection.get();
			List<StorageTarget> targets = new ArrayList<>();
			List<?> blockEntities = (List<?>) sia.getBlockEntitiesInRange.invoke(null, player.level(), player.blockPosition(), 10);
			for (Object object : blockEntities) {
				if (!(object instanceof BlockEntity blockEntity)) {
					continue;
				}
				Level storageLevel = blockEntity.getLevel() == null ? player.level() : blockEntity.getLevel();
				Optional<?> handlerOptional = (Optional<?>) sia.getBlockHandlerFor.invoke(null, storageLevel, blockEntity.getBlockPos(), blockEntity,
						sia.depositAction);
				if (handlerOptional.isEmpty() || !(boolean) sia.mayInteract.invoke(null, player, player.level(), blockEntity.getBlockPos())) {
					continue;
				}
				Object handler = handlerOptional.get();
				BlockPos interactionPos = (BlockPos) sia.getInteractionPosToActOn.invoke(handler, storageLevel, blockEntity.getBlockPos(), blockEntity,
						sia.depositAction);
				addStorageTargets(targets, sia, sia.getBlockStorageItemHandlerTargets.invoke(handler, player, interactionPos), "block");
			}

			player.level().getEntities(player, player.getBoundingBox().inflate(10), entity -> true)
					.forEach(entity -> addEntityStorageTargets(targets, sia, entity));

			targets.sort(Comparator.comparingDouble(target -> distanceToPlayer(sia, player, target.position())));
			List<StorageTarget> indexedTargets = new ArrayList<>();
			for (int i = 0; i < targets.size(); i++) {
				StorageTarget target = targets.get(i);
				indexedTargets.add(new StorageTarget(i, target.kind(), target.position(), target.itemHandler()));
			}
			return indexedTargets;
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Sophisticated Item Actions is not loaded; storageTarget commands require SIA at runtime", e);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to query SIA storage targets: " + e.getMessage(), e);
		}
	}

	private static void addEntityStorageTargets(List<StorageTarget> targets, SiaReflection sia, Entity entity) {
		try {
			Optional<?> handlerIdOptional = (Optional<?>) sia.getEntityHandlerIdFor.invoke(null, entity);
			if (handlerIdOptional.isEmpty()) {
				return;
			}
			Optional<?> handlerOptional = (Optional<?>) sia.getEntityHandler.invoke(null, handlerIdOptional.get());
			if (handlerOptional.isEmpty()) {
				return;
			}
			addStorageTargets(targets, sia, sia.getEntityStorageItemHandlerTargets.invoke(handlerOptional.get(), entity), entity.getType().toShortString());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to query SIA entity storage target: " + e.getMessage(), e);
		}
	}

	private static void addStorageTargets(List<StorageTarget> targets, SiaReflection sia, Object storageTargets, String kind)
			throws ReflectiveOperationException {
		if (!(storageTargets instanceof List<?> list)) {
			return;
		}
		for (Object target : list) {
			Vec3 position = (Vec3) sia.targetPosition.invoke(target);
			IItemHandler itemHandler = (IItemHandler) sia.targetItemHandler.invoke(target);
			targets.add(new StorageTarget(-1, kind, position, itemHandler));
		}
	}

	private static double distanceToPlayer(SiaReflection sia, ServerPlayer player, Vec3 position) {
		try {
			return (double) sia.distanceSquared.invoke(null, player.level(), player.position(), position);
		} catch (ReflectiveOperationException e) {
			return player.position().distanceToSqr(position);
		}
	}

	private static String formatPosition(Vec3 position) {
		return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", position.x(), position.y(), position.z());
	}

	private static String targetCommand(Vec3 position, String action) {
		return String.format(Locale.ROOT, "storageTarget nearest %.3f %.3f %.3f %s", position.x(), position.y(), position.z(), action);
	}

	private static BlockPos getLookedAtBlock(ServerPlayer player) {
		Vec3 eyePosition = player.getEyePosition(1F);
		Vec3 lookPosition = eyePosition.add(player.getViewVector(1F).scale(8D));
		HitResult hitResult = player.level().clip(new ClipContext(eyePosition, lookPosition, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
			throw new IllegalArgumentException("Look at a container block within 8 blocks");
		}
		return blockHitResult.getBlockPos();
	}

	private static ServerLevel getLevel(CommandSourceStack source, String dimension) {
		ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension));
		ServerLevel level = source.getServer().getLevel(dimensionKey);
		if (level == null) {
			throw new IllegalArgumentException("Dimension is not loaded: " + dimension);
		}
		return level;
	}

	private static IItemHandler getContainerHandler(ServerLevel level, BlockPos pos) {
		IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
		if (itemHandler == null) {
			throw new IllegalArgumentException("No item handler at " + pos.toShortString());
		}
		return itemHandler;
	}

	private static int clearItemHandler(IItemHandler itemHandler) {
		int cleared = 0;
		if (itemHandler instanceof IItemHandlerModifiable modifiable) {
			for (int slot = 0; slot < modifiable.getSlots(); slot++) {
				cleared += modifiable.getStackInSlot(slot).getCount();
				modifiable.setStackInSlot(slot, ItemStack.EMPTY);
			}
			return cleared;
		}

		for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
			ItemStack extracted;
			do {
				extracted = itemHandler.extractItem(slot, 64, false);
				cleared += extracted.getCount();
			} while (!extracted.isEmpty());
		}
		return cleared;
	}

	private static ItemStack setItemHandlerSlot(IItemHandler itemHandler, int slot, ItemStack stack) {
		if (itemHandler instanceof IItemHandlerModifiable modifiable) {
			modifiable.setStackInSlot(slot, stack);
			return ItemStack.EMPTY;
		}

		while (!itemHandler.getStackInSlot(slot).isEmpty()) {
			ItemStack extracted = itemHandler.extractItem(slot, 64, false);
			if (extracted.isEmpty()) {
				break;
			}
		}
		return itemHandler.insertItem(slot, stack, false);
	}

	private static void markContainerChanged(ServerLevel level, BlockPos pos) {
		if (level.getBlockEntity(pos) != null) {
			level.getBlockEntity(pos).setChanged();
		}
		level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
	}

	private static String containerCommand(String dimension, BlockPos pos, String action) {
		return "container block " + dimension + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + action;
	}

	private record ItemSeed(String itemName, int count) {
	}

	private record StorageTarget(int index, String kind, Vec3 position, IItemHandler itemHandler) {
	}

	private record SiaReflection(Method getBlockEntitiesInRange, Method mayInteract, Method distanceSquared, Method getBlockHandlerFor,
			Method getEntityHandlerIdFor, Method getEntityHandler, Method getInteractionPosToActOn, Method getBlockStorageItemHandlerTargets,
			Method getEntityStorageItemHandlerTargets, Method targetPosition, Method targetItemHandler, Object depositAction) {
		private static SiaReflection get() throws ClassNotFoundException, NoSuchMethodException {
			Class<?> subLevelCompatHelper = Class.forName("net.p3pp3rf1y.sophisticateditemactions.common.SubLevelCompatHelper");
			Class<?> itemActionHandlerRegistry = Class.forName("net.p3pp3rf1y.sophisticateditemactions.common.ItemActionHandlerRegistry");
			Class<?> blockItemActionHandler = Class.forName("net.p3pp3rf1y.sophisticateditemactions.common.IBlockItemActionHandler");
			Class<?> blockItemAction = Class.forName("net.p3pp3rf1y.sophisticateditemactions.common.IBlockItemActionHandler$Action");
			Class<?> entityItemActionHandler = Class.forName("net.p3pp3rf1y.sophisticateditemactions.common.IEntityItemActionHandler");
			Class<?> storageItemHandlerTarget = Class.forName("net.p3pp3rf1y.sophisticateditemactions.common.StorageItemHandlerTarget");

			return new SiaReflection(subLevelCompatHelper.getMethod("getBlockEntitiesInRange", Level.class, BlockPos.class, int.class),
					subLevelCompatHelper.getMethod("mayInteract", net.minecraft.world.entity.player.Player.class, Level.class, BlockPos.class),
					subLevelCompatHelper.getMethod("distanceSquared", Level.class, Vec3.class, Vec3.class),
					itemActionHandlerRegistry.getMethod("getBlockHandlerFor", Level.class, BlockPos.class, BlockEntity.class, blockItemAction),
					itemActionHandlerRegistry.getMethod("getEntityHandlerIdFor", Entity.class),
					itemActionHandlerRegistry.getMethod("getEntityHandler", ResourceLocation.class),
					blockItemActionHandler.getMethod("getInteractionPosToActOn", Level.class, BlockPos.class, BlockEntity.class, blockItemAction),
					blockItemActionHandler.getMethod("getStorageItemHandlerTargets", ServerPlayer.class, BlockPos.class),
					entityItemActionHandler.getMethod("getStorageItemHandlerTargets", Entity.class), storageItemHandlerTarget.getMethod("position"),
					storageItemHandlerTarget.getMethod("itemHandler"), Enum.valueOf((Class<Enum>) blockItemAction.asSubclass(Enum.class), "DEPOSIT"));
		}
	}

	private static int giveConfiguredBackpack(CommandSourceStack source, String mode, List<ItemSeed> itemSeeds, boolean record) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			clearPlayerInventory(player);
			ItemStack backpack = createConfiguredBackpack(mode, itemSeeds.isEmpty() ? defaultBackpackItemSeeds(mode) : itemSeeds);
			player.getInventory().setItem(0, backpack);
			player.getInventory().selected = 0;
			player.getInventory().setChanged();
			player.containerMenu.broadcastChanges();
			if (record) {
				DemoSession.get().record("backpack giveConfigured " + mode.toLowerCase(Locale.ROOT) + itemSeedsCommand(itemSeeds));
			}
			success(source, () -> Component.literal("Gave configured " + mode + " backpack"));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int openBackpack(CommandSourceStack source, boolean record) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			ItemStack backpack = player.getInventory().getItem(0);
			if (!(backpack.getItem() instanceof BackpackItem backpackItem)) {
				source.sendFailure(Component.literal("No backpack in hotbar slot 0"));
				return 0;
			}
			player.getInventory().selected = 0;
			backpackItem.use(player.level(), player, InteractionHand.MAIN_HAND);
			if (record) {
				DemoSession.get().record("backpack open");
			}
			success(source, () -> Component.literal("Opened backpack"));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int closeScreen(CommandSourceStack source, boolean record) {
		Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(null));
		if (record) {
			DemoSession.get().record("step closeScreen");
		}
		success(source, () -> Component.literal("Closed screen"));
		return 1;
	}

	private static int triggerKeybindAction(CommandSourceStack source, String action, boolean record) {
		String actionName = action.toLowerCase(Locale.ROOT);
		KeyMapping keyMapping;
		try {
			keyMapping = getSiaKeyMapping(actionName);
		} catch (ReflectiveOperationException | ClassCastException | IllegalArgumentException e) {
			source.sendFailure(Component.literal("Could not find SIA " + actionName + " keybind at runtime: " + e.getMessage()));
			return 0;
		}

		if (keyMapping.isUnbound()) {
			source.sendFailure(Component.literal("SIA " + actionName + " keybind is unbound"));
			return 0;
		}

		InputConstants.Key key = keyMapping.getKey();
		Minecraft.getInstance().execute(() -> {
			if (Minecraft.getInstance().player == null) {
				return;
			}
			KeyMapping.set(key, true);
			KeyMapping.click(key);
			NeoForge.EVENT_BUS.post(new InputEvent.Key(key.getValue(), 0, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_ALT));
			NeoForge.EVENT_BUS.post(new InputEvent.Key(key.getValue(), 0, GLFW.GLFW_RELEASE, GLFW.GLFW_MOD_ALT));
			KeyMapping.set(key, false);
		});
		if (record) {
			DemoSession.get().record("step keybind " + actionName);
		}
		success(source, () -> Component.literal("Triggered SIA " + actionName + " keybind with hotbar modifier"));
		return 1;
	}

	private static KeyMapping getSiaKeyMapping(String action) throws ReflectiveOperationException {
		String fieldName = switch (action) {
			case "deposit" -> "ITEM_DEPOSIT_KEYBIND";
			case "restock" -> "ITEM_RESTOCK_KEYBIND";
			default -> throw new IllegalArgumentException("Unsupported SIA keybind action " + action);
		};
		Class<?> clientEventHandler = Class.forName("net.p3pp3rf1y.sophisticateditemactions.client.ClientEventHandler");
		Field field = clientEventHandler.getField(fieldName);
		return (KeyMapping) field.get(null);
	}

	private static void clearPlayerInventory(ServerPlayer player) {
		player.getInventory().items.clear();
		player.getInventory().armor.clear();
		player.getInventory().offhand.clear();
		player.getInventory().setChanged();
		player.containerMenu.broadcastChanges();
	}

	private static ItemStack createConfiguredBackpack(String mode, List<ItemSeed> itemSeeds) {
		ItemStack backpack = new ItemStack(ModItems.DIAMOND_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());
		backpack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 80);
		backpack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 5);

		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		wrapper.setSlotNumbers(80, 5);
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		if (mode.equalsIgnoreCase("restock")) {
			upgrades.setStackInSlot(0, new ItemStack(ModItems.RESTOCK_UPGRADE.get()));
		} else {
			upgrades.setStackInSlot(0, new ItemStack(ModItems.DEPOSIT_UPGRADE.get()));
		}
		upgrades.saveInventory();

		InventoryHandler inventory = wrapper.getInventoryHandler();
		for (int slot = 0; slot < itemSeeds.size(); slot++) {
			ItemSeed itemSeed = itemSeeds.get(slot);
			Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemSeed.itemName()))
					.orElseThrow(() -> new IllegalArgumentException("Unknown item " + itemSeed.itemName()));
			inventory.setStackInSlot(slot, new ItemStack(item, itemSeed.count()));
		}
		inventory.saveInventory();
		return backpack;
	}

	private static List<ItemSeed> defaultBackpackItemSeeds(String mode) {
		int count = mode.equalsIgnoreCase("restock") ? 16 : 32;
		return List.of(new ItemSeed("minecraft:diamond", count), new ItemSeed("minecraft:emerald", count), new ItemSeed("minecraft:copper_ingot", count),
				new ItemSeed("minecraft:redstone", count), new ItemSeed("minecraft:gold_ingot", count));
	}

	private static String itemSeedsCommand(List<ItemSeed> itemSeeds) {
		if (itemSeeds.isEmpty()) {
			return "";
		}

		StringBuilder command = new StringBuilder(" items");
		for (ItemSeed itemSeed : itemSeeds) {
			command.append(' ').append(itemSeed.itemName()).append(' ').append(itemSeed.count());
		}
		return command.toString();
	}

	private static List<ItemSeed> parseItemSeeds(String itemSeeds) {
		return parseItemSeeds(itemSeeds.split(" "), 0);
	}

	private static List<ItemSeed> parseItemSeeds(String[] parts, int offset) {
		if ((parts.length - offset) % 2 != 0) {
			throw new IllegalArgumentException("Backpack items must be item/count pairs");
		}

		List<ItemSeed> itemSeeds = new ArrayList<>();
		for (int i = offset; i < parts.length; i += 2) {
			itemSeeds.add(new ItemSeed(parts[i], Integer.parseInt(parts[i + 1])));
		}
		return itemSeeds;
	}
}
