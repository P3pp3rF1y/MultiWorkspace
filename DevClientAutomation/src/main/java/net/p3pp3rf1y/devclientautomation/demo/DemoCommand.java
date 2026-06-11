package net.p3pp3rf1y.devclientautomation.demo;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DemoCommand {
	private static boolean quiet = false;
	private static VisualMobCatchAction visualMobCatchAction;
	private static PlayerWorldAction playerWorldAction;

	private DemoCommand() {
	}

	public static void init() {
		DemoMouseMotion.init();
		DemoMotionRecorder.init();
		NeoForge.EVENT_BUS.addListener(DemoCommand::registerCommands);
		NeoForge.EVENT_BUS.addListener(DemoCommand::tickPlayerWorldActions);
		NeoForge.EVENT_BUS.addListener(DemoCommand::renderPlayerWorldCamera);
		NeoForge.EVENT_BUS.addListener(DemoCommand::tickVisualActions);
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
								.executes(context -> clearContainer(context.getSource(), context.getSource().getLevel().dimension().identifier().toString(),
										BlockPosArgument.getLoadedBlockPos(context, "pos"), true)))
						.then(Commands.literal("set").then(Commands.argument("slot", IntegerArgumentType.integer(0)).then(Commands
								.argument("item", ItemArgument.item(event.getBuildContext()))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
										.executes(context -> setContainerSlot(
										context.getSource(), context.getSource().getLevel().dimension().identifier().toString(),
												BlockPosArgument.getLoadedBlockPos(context, "pos"), IntegerArgumentType.getInteger(context, "slot"),
												ItemArgument.getItem(context, "item"), IntegerArgumentType.getInteger(context, "count"), false, true))))))
						.then(Commands.literal("only").then(Commands.argument("slot", IntegerArgumentType.integer(0)).then(Commands
								.argument("item", ItemArgument.item(event.getBuildContext()))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
										.executes(context -> setContainerSlot(context.getSource(),
										context.getSource().getLevel().dimension().identifier().toString(),
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

		var mobCatcher = Commands.literal("mobCatcher")
				.then(Commands.literal("catchNearest")
						.then(Commands.argument("selector", StringArgumentType.word())
								.executes(context -> catchNearestMob(context.getSource(), StringArgumentType.getString(context, "selector"), 8D, true))
								.then(Commands.argument("range", DoubleArgumentType.doubleArg(1D, 64D))
										.executes(context -> catchNearestMob(context.getSource(), StringArgumentType.getString(context, "selector"),
												DoubleArgumentType.getDouble(context, "range"), true)))))
				.then(Commands.literal("catchNearby")
						.then(Commands.argument("selector", StringArgumentType.word()).then(Commands.argument("count", IntegerArgumentType.integer(1, 20))
								.executes(context -> catchNearbyMobs(context.getSource(), StringArgumentType.getString(context, "selector"),
										IntegerArgumentType.getInteger(context, "count"), 8D, true))
								.then(Commands.argument("range", DoubleArgumentType.doubleArg(1D, 64D))
										.executes(context -> catchNearbyMobs(context.getSource(), StringArgumentType.getString(context, "selector"),
												IntegerArgumentType.getInteger(context, "count"), DoubleArgumentType.getDouble(context, "range"), true))))))
				.then(Commands.literal("releaseFirst").executes(context -> releaseFirstCapturedMob(context.getSource(), true)));

		var playerLookAt = Commands.literal("lookAt")
				.then(Commands.argument("x", DoubleArgumentType.doubleArg())
						.then(Commands.argument("y", DoubleArgumentType.doubleArg())
								.then(Commands.argument("z", DoubleArgumentType.doubleArg())
										.executes(context -> lookAt(context.getSource(),
												new Vec3(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"),
														DoubleArgumentType.getDouble(context, "z")),
												20, true))
										.then(Commands.argument("ticks", IntegerArgumentType.integer(1, 200))
												.executes(context -> lookAt(context.getSource(),
														new Vec3(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"),
																DoubleArgumentType.getDouble(context, "z")),
														IntegerArgumentType.getInteger(context, "ticks"), true))))));

		var playerLookAtEntity = Commands.literal("lookAtEntity")
				.then(Commands.argument("selector", StringArgumentType.word())
						.executes(context -> lookAtEntity(context.getSource(), StringArgumentType.getString(context, "selector"), 12D, 20, true))
						.then(Commands.argument("range", DoubleArgumentType.doubleArg(1D, 64D))
								.executes(context -> lookAtEntity(context.getSource(), StringArgumentType.getString(context, "selector"),
										DoubleArgumentType.getDouble(context, "range"), 20, true))
								.then(Commands.argument("ticks", IntegerArgumentType.integer(1, 200))
										.executes(context -> lookAtEntity(context.getSource(), StringArgumentType.getString(context, "selector"),
												DoubleArgumentType.getDouble(context, "range"), IntegerArgumentType.getInteger(context, "ticks"), true)))));

		var playerMoveTo = Commands.literal("moveTo")
				.then(Commands.argument("x", DoubleArgumentType.doubleArg())
						.then(Commands.argument("y", DoubleArgumentType.doubleArg())
								.then(Commands.argument("z", DoubleArgumentType.doubleArg())
										.executes(context -> moveTo(context.getSource(),
												new Vec3(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"),
														DoubleArgumentType.getDouble(context, "z")),
												200, true))
										.then(Commands.argument("maxTicks", IntegerArgumentType.integer(1, 1000))
												.executes(context -> moveTo(context.getSource(),
														new Vec3(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"),
																DoubleArgumentType.getDouble(context, "z")),
														IntegerArgumentType.getInteger(context, "maxTicks"), true))))));

		var playerMoveToLookingAt = Commands.literal("moveToLookingAt")
				.then(Commands.argument("x", DoubleArgumentType.doubleArg()).then(Commands.argument("y", DoubleArgumentType.doubleArg())
						.then(Commands.argument("z", DoubleArgumentType.doubleArg()).then(Commands.argument("lookX", DoubleArgumentType.doubleArg())
								.then(Commands.argument("lookY", DoubleArgumentType.doubleArg()).then(Commands.argument("lookZ", DoubleArgumentType.doubleArg())
										.then(Commands.argument("maxTicks", IntegerArgumentType.integer(1, 1000))
												.executes(context -> moveToLookingAt(context.getSource(),
														new Vec3(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"),
																DoubleArgumentType.getDouble(context, "z")),
														new Vec3(DoubleArgumentType.getDouble(context, "lookX"), DoubleArgumentType.getDouble(context, "lookY"),
																DoubleArgumentType.getDouble(context, "lookZ")),
														IntegerArgumentType.getInteger(context, "maxTicks"), true)))))))));

		var playerCommands = Commands.literal("player").then(Commands.literal("clearInventory").executes(context -> clearInventory(context.getSource(), true)))
				.then(Commands.literal("hotbar")
						.then(Commands.literal("set").then(Commands.argument("slot", IntegerArgumentType.integer(0, 8))
								.then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
										.then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
												.executes(context -> setHotbar(context.getSource(), IntegerArgumentType.getInteger(context, "slot"),
														ItemArgument.getItem(context, "item"), IntegerArgumentType.getInteger(context, "count"), true))))))
						.then(Commands.literal("select")
								.then(Commands.argument("slot", IntegerArgumentType.integer(0, 8))
										.executes(context -> selectHotbar(context.getSource(), IntegerArgumentType.getInteger(context, "slot"), true)))))
				.then(playerLookAt).then(playerLookAtEntity)
				.then(Commands.literal("walkForward")
						.then(Commands.argument("ticks", IntegerArgumentType.integer(1, 400))
								.executes(context -> walkForward(context.getSource(), IntegerArgumentType.getInteger(context, "ticks"), true))))
				.then(playerMoveTo).then(playerMoveToLookingAt);

		var segmentCommands = Commands.literal("segment")
				.then(Commands.literal("record")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(context -> recordOnly(context.getSource(), "segment record " + StringArgumentType.getString(context, "name")))))
				.then(Commands.literal("stop").executes(context -> recordOnly(context.getSource(), "segment stop")));

		var motionCommands = Commands.literal("motion").then(Commands.literal("status").executes(context -> motionRecordingStatus(context.getSource())))
				.then(Commands.literal("record")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(context -> startMotionRecording(context.getSource(), StringArgumentType.getString(context, "name")))))
				.then(Commands.literal("recording").then(Commands.literal("stop").executes(context -> stopMotionRecording(context.getSource())))
						.then(Commands.literal("start")
								.then(Commands.argument("name", StringArgumentType.word())
										.executes(context -> startMotionRecording(context.getSource(), StringArgumentType.getString(context, "name"))))))
				.then(Commands.literal("stop").executes(context -> stopMotionRecording(context.getSource())));

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
						.then(Commands.argument("name", StringArgumentType.word()).suggests((context, builder) -> suggestDemoNames(builder))
								.executes(context -> runDemo(context.getSource(), StringArgumentType.getString(context, "name")))))
				.then(Commands.literal("marker")
						.then(Commands.literal("set")
								.then(Commands.argument("name", StringArgumentType.word())
										.executes(context -> recordOnly(context.getSource(), "marker set " + StringArgumentType.getString(context, "name"))))))
				.then(playerCommands).then(Commands.literal("container").then(lookedAtContainer).then(blockContainer))
				.then(Commands.literal("storageTarget").then(nearbyStorageTarget).then(nearestStorageTarget)).then(mobCatcher)
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
				.then(segmentCommands).then(motionCommands));
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

	private static int startMotionRecording(CommandSourceStack source, String name) {
		if (name.equalsIgnoreCase("stop") || name.equalsIgnoreCase("status") || name.equalsIgnoreCase("recording")) {
			source.sendFailure(Component.literal("Use a descriptive motion recording name, not '" + name + "'"));
			return 0;
		}
		if (DemoMotionRecorder.isRecording()) {
			source.sendFailure(Component.literal("Motion recording " + DemoMotionRecorder.recordingName() + " is already active"));
			return 0;
		}
		DemoMotionRecorder.start(name);
		success(source, () -> Component.literal("Started motion recording " + name));
		return 1;
	}

	private static int motionRecordingStatus(CommandSourceStack source) {
		if (!DemoMotionRecorder.isRecording()) {
			source.sendSuccess(() -> Component.literal("No active motion recording"), false);
			return 1;
		}

		source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Recording %s: %d samples, %.1fs", DemoMotionRecorder.recordingName(),
				DemoMotionRecorder.sampleCount(), DemoMotionRecorder.recordingDurationSeconds())), false);
		return 1;
	}

	private static int stopMotionRecording(CommandSourceStack source) {
		try {
			Path path = DemoMotionRecorder.stop();
			success(source, () -> Component.literal("Saved " + DemoMotionRecorder.sampleCount() + " motion samples to " + path));
			return 1;
		} catch (IOException | IllegalStateException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static CompletableFuture<Suggestions> suggestDemoNames(SuggestionsBuilder builder) {
		Path demosDirectory = Minecraft.getInstance().gameDirectory.toPath().resolve("devclientautomation").resolve("demos");
		if (!Files.isDirectory(demosDirectory)) {
			return builder.buildFuture();
		}

		try (var files = Files.list(demosDirectory)) {
			files.filter(path -> path.getFileName().toString().endsWith(".json")).map(path -> path.getFileName().toString())
					.map(fileName -> fileName.substring(0, fileName.length() - ".json".length())).sorted().forEach(builder::suggest);
		} catch (IOException e) {
			// Missing or unreadable demo files should not break command completion.
		}
		return builder.buildFuture();
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
		if (parts.length == 6 && parts[0].equals("player") && parts[1].equals("lookAt")) {
			return lookAt(source, new Vec3(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4])),
					Integer.parseInt(parts[5]), false);
		}
		if (parts.length == 5 && parts[0].equals("player") && parts[1].equals("lookAtEntity")) {
			return lookAtEntity(source, parts[2], Double.parseDouble(parts[3]), Integer.parseInt(parts[4]), false);
		}
		if (parts.length == 3 && parts[0].equals("player") && parts[1].equals("walkForward")) {
			return walkForward(source, Integer.parseInt(parts[2]), false);
		}
		if (parts.length == 6 && parts[0].equals("player") && parts[1].equals("moveTo")) {
			return moveTo(source, new Vec3(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4])),
					Integer.parseInt(parts[5]), false);
		}
		if (parts.length == 9 && parts[0].equals("player") && parts[1].equals("moveToLookingAt")) {
			return moveToLookingAt(source, new Vec3(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4])),
					new Vec3(Double.parseDouble(parts[5]), Double.parseDouble(parts[6]), Double.parseDouble(parts[7])), Integer.parseInt(parts[8]), false);
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
		if (parts.length == 4 && parts[0].equals("mobCatcher") && parts[1].equals("catchNearest")) {
			return catchNearestMob(source, parts[2], Double.parseDouble(parts[3]), false);
		}
		if (parts.length == 5 && parts[0].equals("mobCatcher") && parts[1].equals("catchNearby")) {
			return catchNearbyMobs(source, parts[2], Integer.parseInt(parts[3]), Double.parseDouble(parts[4]), false);
		}
		if (command.equals("mobCatcher releaseFirst")) {
			return releaseFirstCapturedMob(source, false);
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

	static boolean hasRunningAction() {
		return visualMobCatchAction != null || playerWorldAction != null;
	}

	private static void tickPlayerWorldActions(ClientTickEvent.Post event) {
		if (playerWorldAction != null) {
			try {
				if (playerWorldAction.tick()) {
					playerWorldAction.stop();
					playerWorldAction = null;
				}
			} catch (RuntimeException e) {
				playerWorldAction.player.sendSystemMessage(Component.literal(e.getMessage()));
				playerWorldAction.stop();
				playerWorldAction = null;
			}
		}
	}

	private static void renderPlayerWorldCamera(ViewportEvent.ComputeCameraAngles event) {
		if (playerWorldAction != null) {
			playerWorldAction.renderFrame(event);
		}
		if (visualMobCatchAction != null) {
			visualMobCatchAction.renderFrame(event);
		}
	}

	private static void tickVisualActions(ServerTickEvent.Post event) {
		if (visualMobCatchAction != null && visualMobCatchAction.player.level().getServer() == event.getServer()) {
			try {
				if (visualMobCatchAction.tick()) {
					setMovementKeys(false, false, false, false);
					visualMobCatchAction = null;
				}
			} catch (RuntimeException e) {
				visualMobCatchAction.player.sendSystemMessage(Component.literal(e.getMessage()));
				setMovementKeys(false, false, false, false);
				visualMobCatchAction = null;
			}
		}
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
			Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemName))
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
			source.getPlayerOrException().getInventory().setSelectedSlot(slot);
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

	private static int lookAt(CommandSourceStack source, Vec3 target, int ticks, boolean record) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			if (!startPlayerWorldAction(source, new LookAtPositionAction(player, target, ticks))) {
				return 0;
			}
			if (record) {
				DemoSession.get().record(String.format(Locale.ROOT, "player lookAt %.3f %.3f %.3f %d", target.x(), target.y(), target.z(), ticks));
			}
			success(source, () -> Component.literal("Started player lookAt action"));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int lookAtEntity(CommandSourceStack source, String selector, double range, int ticks, boolean record) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			LivingEntity target = findMatchingMobs(player, selector, range).stream().findFirst()
					.orElseThrow(() -> new IllegalArgumentException("No matching entity found for " + selector));
			if (!startPlayerWorldAction(source, new LookAtPositionAction(player, target.getEyePosition(), ticks))) {
				return 0;
			}
			if (record) {
				DemoSession.get().record(String.format(Locale.ROOT, "player lookAtEntity %s %.3f %d", selector, range, ticks));
			}
			success(source, () -> Component.literal("Started player lookAtEntity action"));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int walkForward(CommandSourceStack source, int ticks, boolean record) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			if (!startPlayerWorldAction(source, new WalkForwardAction(player, ticks))) {
				return 0;
			}
			if (record) {
				DemoSession.get().record("player walkForward " + ticks);
			}
			success(source, () -> Component.literal("Started player walkForward action"));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int moveTo(CommandSourceStack source, Vec3 target, int maxTicks, boolean record) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			if (!startPlayerWorldAction(source, new MoveToPositionAction(player, target, null, maxTicks))) {
				return 0;
			}
			if (record) {
				DemoSession.get().record(String.format(Locale.ROOT, "player moveTo %.3f %.3f %.3f %d", target.x(), target.y(), target.z(), maxTicks));
			}
			success(source, () -> Component.literal("Started player moveTo action"));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int moveToLookingAt(CommandSourceStack source, Vec3 target, Vec3 lookTarget, int maxTicks, boolean record) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			if (!startPlayerWorldAction(source, new MoveToPositionAction(player, target, lookTarget, maxTicks))) {
				return 0;
			}
			if (record) {
				DemoSession.get().record(String.format(Locale.ROOT, "player moveToLookingAt %.3f %.3f %.3f %.3f %.3f %.3f %d", target.x(), target.y(),
						target.z(), lookTarget.x(), lookTarget.y(), lookTarget.z(), maxTicks));
			}
			success(source, () -> Component.literal("Started player moveToLookingAt action"));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static boolean startPlayerWorldAction(CommandSourceStack source, PlayerWorldAction action) {
		if (hasRunningAction()) {
			source.sendFailure(Component.literal("A demo action is already running"));
			return false;
		}
		playerWorldAction = action;
		return true;
	}

	private static int clearLookedAtContainer(CommandSourceStack source, boolean record) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			BlockPos pos = getLookedAtBlock(player);
			String dimension = player.level().dimension().identifier().toString();
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
			String dimension = player.level().dimension().identifier().toString();
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
			ResourceHandler<ItemResource> itemHandler = getContainerHandler(level, pos);
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
			ResourceHandler<ItemResource> itemHandler = getContainerHandler(level, pos);
			if (slot >= itemHandler.size()) {
				throw new IllegalArgumentException("Container only has " + itemHandler.size() + " slots");
			}
			Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemName))
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
					+ target.itemHandler().size() + " slots"));
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
			ResourceHandler<ItemResource> itemHandler = target.itemHandler();
			if (slot >= itemHandler.size()) {
				throw new IllegalArgumentException("Storage target only has " + itemHandler.size() + " slots");
			}
			Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemName))
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
			ResourceHandler<ItemResource> itemHandler = (ResourceHandler<ItemResource>) sia.targetItemHandler.invoke(target);
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

	private static float approachDegrees(float current, float target, float maxStep) {
		float delta = Mth.wrapDegrees(target - current);
		if (delta > maxStep) {
			delta = maxStep;
		} else if (delta < -maxStep) {
			delta = -maxStep;
		}
		return current + delta;
	}

	private static float smoothApproachDegrees(float current, float target, float fraction, float maxStep) {
		float delta = Mth.wrapDegrees(target - current);
		float step = Mth.clamp(delta * fraction, -maxStep, maxStep);
		return current + step;
	}

	private static Rotation rotationTo(ServerPlayer player, Vec3 target) {
		return rotationFromEye(currentEyePosition(player), target);
	}

	private static Rotation rotationFromEye(Vec3 eyePosition, Vec3 target) {
		Vec3 toTarget = target.subtract(eyePosition);
		double horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
		return new Rotation((float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90F),
				(float) -Math.toDegrees(Math.atan2(toTarget.y, horizontalDistance)));
	}

	private static Rotation levelRotationTo(ServerPlayer player, Vec3 target) {
		Vec3 eyePosition = currentEyePosition(player);
		return rotationTo(player, new Vec3(target.x(), eyePosition.y, target.z()));
	}

	private static Rotation rotateToward(ServerPlayer player, Vec3 target, float yawStep, float pitchStep) {
		Rotation targetRotation = rotationTo(player, target);
		float yaw = smoothApproachDegrees(currentYaw(player), targetRotation.yaw(), 0.28F, yawStep);
		float pitch = smoothApproachDegrees(currentPitch(player), targetRotation.pitch(), 0.22F, pitchStep);
		rotatePlayer(player, yaw, pitch);
		return targetRotation;
	}

	private static void rotateCameraFrame(ServerPlayer player, ViewportEvent.ComputeCameraAngles event, Rotation targetRotation, float yawFraction,
			float yawMaxStep, float pitchFraction, float pitchMaxStep) {
		float yaw = smoothApproachDegrees(event.getYaw(), targetRotation.yaw(), yawFraction, yawMaxStep);
		float pitch = smoothApproachDegrees(event.getPitch(), targetRotation.pitch(), pitchFraction, pitchMaxStep);
		event.setYaw(yaw);
		event.setPitch(pitch);
		rotatePlayer(player, yaw, pitch);
	}

	private static float interpolateDegrees(float start, float target, float progress) {
		return start + Mth.wrapDegrees(target - start) * progress;
	}

	private static float easeInOut(float progress) {
		float clampedProgress = Mth.clamp(progress, 0F, 1F);
		float remaining = 1F - clampedProgress;
		return 1F - remaining * remaining * remaining * remaining;
	}

	private static double horizontalDistance(Vec3 first, Vec3 second) {
		double x = first.x() - second.x();
		double z = first.z() - second.z();
		return Math.sqrt(x * x + z * z);
	}

	private static void moveToward(ServerPlayer player, Vec3 target, double desiredDistance, float yaw, float pitch) {
		if (isCloseToTarget(player, target, desiredDistance)) {
			rotatePlayer(player, yaw, pitch);
			setMovementKeys(false, false, false, false);
			return;
		}

		rotatePlayer(player, yaw, pitch);
		setMovementKeysForTarget(player, target, yaw);
	}

	private static boolean isCloseToTarget(ServerPlayer player, Vec3 target, double desiredDistance) {
		Vec3 toTarget = target.subtract(currentPosition(player));
		double horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
		return horizontalDistance <= desiredDistance;
	}

	private static Vec3 currentPosition(ServerPlayer player) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null ? minecraft.player.position() : player.position();
	}

	private static Vec3 currentEyePosition(ServerPlayer player) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null ? minecraft.player.getEyePosition() : player.getEyePosition();
	}

	private static float currentYaw(ServerPlayer player) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null ? minecraft.player.getYRot() : player.getYRot();
	}

	private static float currentPitch(ServerPlayer player) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null ? minecraft.player.getXRot() : player.getXRot();
	}

	private static void rotatePlayer(ServerPlayer player, float yaw, float pitch) {
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.setYHeadRot(yaw);
		player.setYBodyRot(yaw);
		Minecraft minecraft = Minecraft.getInstance();
		Runnable applyClientRotation = () -> {
			if (minecraft.player != null) {
				minecraft.player.setYRot(yaw);
				minecraft.player.setXRot(pitch);
				minecraft.player.setYHeadRot(yaw);
				minecraft.player.setYBodyRot(yaw);
			}
		};
		if (minecraft.isSameThread()) {
			applyClientRotation.run();
		} else {
			minecraft.execute(applyClientRotation);
		}
	}

	private static void setForwardKeyDown(boolean down) {
		setMovementKeys(down, false, false, false);
	}

	private static void setMovementKeysForTarget(ServerPlayer player, Vec3 target, float viewYaw) {
		Rotation movementRotation = levelRotationTo(player, target);
		float delta = Mth.wrapDegrees(movementRotation.yaw() - viewYaw);
		boolean forward = Math.abs(delta) <= 67.5F;
		boolean back = Math.abs(delta) >= 112.5F;
		boolean right = delta > 22.5F && delta < 157.5F;
		boolean left = delta < -22.5F && delta > -157.5F;
		setMovementKeys(forward, back, left, right);
	}

	private static void setMovementKeys(boolean forward, boolean back, boolean left, boolean right) {
		Minecraft minecraft = Minecraft.getInstance();
		Runnable applyMovementKeys = () -> {
			minecraft.options.keyUp.setDown(forward);
			minecraft.options.keyDown.setDown(back);
			minecraft.options.keyLeft.setDown(left);
			minecraft.options.keyRight.setDown(right);
		};
		if (minecraft.isSameThread()) {
			applyMovementKeys.run();
		} else {
			minecraft.execute(applyMovementKeys);
		}
	}

	private static int catchNearestMob(CommandSourceStack source, String selector, double range, boolean record) {
		try {
			if (visualMobCatchAction != null) {
				source.sendFailure(Component.literal("A visual mob catcher action is already running"));
				return 0;
			}
			ensureMobCatcherAvailable();

			ServerPlayer player = source.getPlayerOrException();
			LivingEntity target = findMatchingMobs(player, selector, range).stream().findFirst()
					.orElseThrow(() -> new IllegalArgumentException("No matching mob catcher target found for " + selector));
			player.getInventory().setSelectedSlot(0);
			visualMobCatchAction = new VisualMobCatchAction(player, target, player.tickCount, 100);
			if (record) {
				DemoSession.get().record(String.format(Locale.ROOT, "mobCatcher catchNearest %s %.3f", selector, range));
			}
			success(source, () -> Component.literal("Started visual mob catcher capture for " + selector));
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int catchNearbyMobs(CommandSourceStack source, String selector, int count, double range, boolean record) {
		try {
			ServerPlayer player = source.getPlayerOrException();
			player.getInventory().setSelectedSlot(0);
			List<LivingEntity> targets = findMatchingMobs(player, selector, range);
			int captured = 0;
			for (LivingEntity target : targets) {
				if (captured >= count) {
					break;
				}
				player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
				InteractionResult result = invokeMobCatcherCapture(player, target);
				if (result.consumesAction()) {
					captured++;
				}
			}

			if (captured == 0) {
				source.sendFailure(Component.literal("No matching mob catcher targets captured for " + selector));
				return 0;
			}
			if (record) {
				DemoSession.get().record(String.format(Locale.ROOT, "mobCatcher catchNearby %s %d %.3f", selector, count, range));
			}
			int capturedCount = captured;
			success(source, () -> Component.literal("Captured " + capturedCount + " mob(s) matching " + selector));
			return captured;
		} catch (Exception e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static List<LivingEntity> findMatchingMobs(ServerPlayer player, String selector, double range) {
		AABB searchBox = player.getBoundingBox().inflate(range);
		List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
				entity -> entity != player && entity.isAlive() && matchesMobSelector(entity, selector));
		targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
		return targets;
	}

	private static boolean matchesMobSelector(LivingEntity entity, String selector) {
		String normalizedSelector = selector.toLowerCase(Locale.ROOT);
		return switch (normalizedSelector) {
			case "any" -> true;
			case "animal", "animals", "passive" -> !isMobCatcherHostile(entity);
			case "hostile", "hostiles", "monster", "monsters" -> isMobCatcherHostile(entity);
			default -> {
				String selectorId = normalizedSelector.contains(":")
						? normalizedSelector
						: normalizedSelector.contains(".") ? normalizedSelector.replaceFirst("\\.", ":") : "minecraft:" + normalizedSelector;
				String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
				yield entityId.equals(selectorId) || entityId.endsWith(":" + normalizedSelector);
			}
		};
	}

	private static boolean isMobCatcherHostile(LivingEntity entity) {
		try {
			Class<?> handlerClass = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherHandler");
			Method isHostile = handlerClass.getMethod("isHostile", LivingEntity.class);
			return (boolean) isHostile.invoke(null, entity);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to query mob catcher hostility: " + e.getMessage(), e);
		}
	}

	private static void ensureMobCatcherAvailable() {
		try {
			Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherHandler");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Mob catcher upgrade is not available in the installed Sophisticated Backpacks jar", e);
		}
	}

	private static InteractionResult invokeMobCatcherCapture(ServerPlayer player, LivingEntity entity) {
		try {
			Class<?> handlerClass = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherHandler");
			Method tryCapture = handlerClass.getMethod("tryCapture", net.minecraft.world.entity.player.Player.class, InteractionHand.class, LivingEntity.class);
			return (InteractionResult) tryCapture.invoke(null, player, InteractionHand.MAIN_HAND, entity);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause() == null ? e : e.getCause();
			throw new IllegalStateException("Mob catcher capture failed: " + cause.getMessage(), cause);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Mob catcher upgrade is not available at runtime: " + e.getMessage(), e);
		}
	}

	private static InteractionResult interactWithMobCatcherTarget(ServerPlayer player, LivingEntity entity) {
		if (!(player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof BackpackItem)) {
			throw new IllegalStateException("Hold the mob catcher backpack in the main hand before capturing");
		}

		Vec3 localHitPosition = entity.getBoundingBox().getCenter().subtract(entity.position());
		PlayerInteractEvent.EntityInteractSpecific event = new PlayerInteractEvent.EntityInteractSpecific(player, InteractionHand.MAIN_HAND, entity,
				localHitPosition);
		NeoForge.EVENT_BUS.post(event);
		return event.getCancellationResult();
	}

	private static int releaseFirstCapturedMob(CommandSourceStack source, boolean record) {
		Minecraft.getInstance().execute(() -> {
			try {
				releaseFirstCapturedMobOnClient(source);
			} catch (Exception e) {
				source.sendFailure(Component.literal(e.getMessage()));
			}
		});
		if (record) {
			DemoSession.get().record("mobCatcher releaseFirst");
		}
		success(source, () -> Component.literal("Clicked first captured mob release area"));
		return 1;
	}

	private static void releaseFirstCapturedMobOnClient(CommandSourceStack source) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)
				|| !(containerScreen.getMenu() instanceof BackpackContainer backpackContainer)) {
			throw new IllegalStateException("Open a backpack screen before releasing captured mobs");
		}

		List<CapturedMob> capturedMobs = MobCatcherStorage.getCapturedMobs(backpackContainer.getStorageWrapper());
		if (capturedMobs.isEmpty()) {
			throw new IllegalStateException("The open backpack has no captured mobs");
		}

		CapturedMob capturedMob = capturedMobs.get(0);
		if (capturedMob.slot() >= backpackContainer.getNumberOfStorageInventorySlots()) {
			throw new IllegalStateException("Captured mob slot is outside the visible backpack inventory");
		}

		Slot slot = backpackContainer.getSlot(capturedMob.slot());
		int x = containerScreen.getGuiLeft() + slot.x - 1;
		int y = containerScreen.getGuiTop() + slot.y - 1;
		int clickX = x + capturedMob.width() * 9;
		int clickY = y + capturedMob.height() * 9;
		DemoMouseMotion.moveTo(clickX, clickY, 12, 8, () -> {
			MouseButtonEvent event = new MouseButtonEvent(clickX, clickY, new MouseButtonInfo(0, 0));
			containerScreen.mouseClicked(event, false);
			containerScreen.mouseReleased(event);
		});
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
		ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension));
		ServerLevel level = source.getServer().getLevel(dimensionKey);
		if (level == null) {
			throw new IllegalArgumentException("Dimension is not loaded: " + dimension);
		}
		return level;
	}

	private static ResourceHandler<ItemResource> getContainerHandler(ServerLevel level, BlockPos pos) {
		ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
		if (itemHandler == null) {
			throw new IllegalArgumentException("No item handler at " + pos.toShortString());
		}
		return itemHandler;
	}

	private static int clearItemHandler(ResourceHandler<ItemResource> itemHandler) {
		int cleared = 0;
		try (Transaction tx = Transaction.openRoot()) {
			for (int slot = 0; slot < itemHandler.size(); slot++) {
				ItemResource resource = itemHandler.getResource(slot);
				int amount = itemHandler.getAmountAsInt(slot);
				if (!resource.isEmpty() && amount > 0) {
					cleared += itemHandler.extract(resource, amount, tx);
				}
			}
			tx.commit();
		}
		return cleared;
	}

	private static ItemStack setItemHandlerSlot(ResourceHandler<ItemResource> itemHandler, int slot, ItemStack stack) {
		try (Transaction tx = Transaction.openRoot()) {
			ItemResource currentResource = itemHandler.getResource(slot);
			int currentAmount = itemHandler.getAmountAsInt(slot);
			if (!currentResource.isEmpty() && currentAmount > 0) {
				itemHandler.extract(currentResource, currentAmount, tx);
			}
			int inserted = itemHandler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
			tx.commit();
			return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
		}
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

	private record Rotation(float yaw, float pitch) {
	}

	private record ItemSeed(String itemName, int count) {
	}

	private record StorageTarget(int index, String kind, Vec3 position, ResourceHandler<ItemResource> itemHandler) {
	}

	private abstract static class PlayerWorldAction {
		protected final ServerPlayer player;

		private PlayerWorldAction(ServerPlayer player) {
			this.player = player;
		}

		protected abstract boolean tick();

		protected void renderFrame(ViewportEvent.ComputeCameraAngles event) {
			// Default action has no camera component.
		}

		protected void stop() {
			setMovementKeys(false, false, false, false);
			DemoMotionRecorder.clearActionDebug();
		}
	}

	private static class EasedCameraTurn {
		private static final float MAX_FRAME_YAW_STEP = 2F;
		private static final float MAX_FRAME_PITCH_STEP = 2F;

		private Float startYaw;
		private Float startPitch;
		private Float targetYaw;
		private Float targetPitch;
		private long startNanos;
		private float yaw;
		private float pitch;

		private Rotation render(ServerPlayer player, ViewportEvent.ComputeCameraAngles event, Rotation targetRotation, int ticks, int maxTicks) {
			return render(player, event, targetRotation, ticks, maxTicks, false);
		}

		private Rotation render(ServerPlayer player, ViewportEvent.ComputeCameraAngles event, Rotation targetRotation, int ticks, int maxTicks,
				boolean freezeTarget) {
			if (startYaw == null || startPitch == null) {
				startYaw = event.getYaw();
				startPitch = event.getPitch();
				targetYaw = targetRotation.yaw();
				targetPitch = targetRotation.pitch();
				startNanos = System.nanoTime();
				yaw = startYaw;
				pitch = startPitch;
			} else if (!freezeTarget) {
				targetYaw = targetRotation.yaw();
				targetPitch = targetRotation.pitch();
			}

			float durationNanos = Math.max(1F, maxTicks) * 50_000_000F;
			float progress = easeInOut((System.nanoTime() - startNanos) / durationNanos);
			float targetFrameYaw = interpolateDegrees(startYaw, targetYaw, progress);
			float targetFramePitch = Mth.lerp(progress, startPitch, targetPitch);
			yaw = approachDegrees(yaw, targetFrameYaw, MAX_FRAME_YAW_STEP);
			pitch += Mth.clamp(targetFramePitch - pitch, -MAX_FRAME_PITCH_STEP, MAX_FRAME_PITCH_STEP);
			event.setYaw(yaw);
			event.setPitch(pitch);
			rotatePlayer(player, yaw, pitch);
			return new Rotation(yaw, pitch);
		}

		private float yaw() {
			return yaw;
		}

		private void reset() {
			startYaw = null;
			startPitch = null;
			targetYaw = null;
			targetPitch = null;
			startNanos = 0L;
		}
	}

	private static class LookAtPositionAction extends PlayerWorldAction {
		private final Vec3 target;
		private final int maxTicks;
		private final EasedCameraTurn cameraTurn = new EasedCameraTurn();
		private int ticks;

		private LookAtPositionAction(ServerPlayer player, Vec3 target, int maxTicks) {
			super(player);
			this.target = target;
			this.maxTicks = maxTicks;
		}

		@Override
		protected boolean tick() {
			ticks++;
			Rotation targetRotation = rotationTo(player, target);
			boolean closeEnough = Math.abs(Mth.wrapDegrees(targetRotation.yaw() - currentYaw(player))) < 1.5F
					&& Math.abs(targetRotation.pitch() - currentPitch(player)) < 1.5F;
			return closeEnough || ticks >= maxTicks;
		}

		@Override
		protected void renderFrame(ViewportEvent.ComputeCameraAngles event) {
			Rotation targetRotation = rotationTo(player, target);
			Rotation cameraRotation = cameraTurn.render(player, event, targetRotation, ticks, maxTicks);
			DemoMotionRecorder.setActionDebug(new DemoMotionRecorder.ActionDebug("lookAt", "turn", ticks, maxTicks,
					currentEyePosition(player).distanceTo(target), target.x, target.y, target.z, target.x, target.y, target.z, cameraRotation.yaw(),
					cameraRotation.pitch(), targetRotation.yaw(), targetRotation.pitch(), cameraRotation.yaw()));
		}
	}

	private static class WalkForwardAction extends PlayerWorldAction {
		private final int maxTicks;
		private int ticks;

		private WalkForwardAction(ServerPlayer player, int maxTicks) {
			super(player);
			this.maxTicks = maxTicks;
		}

		@Override
		protected boolean tick() {
			ticks++;
			setForwardKeyDown(true);
			return ticks >= maxTicks;
		}
	}

	private static class MoveToPositionAction extends PlayerWorldAction {
		private static final int TRAVEL_TURN_TICKS = 24;
		private static final int LOOK_TARGET_TURN_TICKS = 34;

		private final Vec3 target;
		private final Vec3 lookTarget;
		private final int maxTicks;
		private final double initialHorizontalDistance;
		private final EasedCameraTurn travelCameraTurn = new EasedCameraTurn();
		private final EasedCameraTurn lookCameraTurn = new EasedCameraTurn();
		private boolean lookingAtTarget;
		private int lookStartTick;
		private float movementReferenceYaw;
		private int ticks;

		private MoveToPositionAction(ServerPlayer player, Vec3 target, Vec3 lookTarget, int maxTicks) {
			super(player);
			this.target = target;
			this.lookTarget = lookTarget;
			this.maxTicks = maxTicks;
			initialHorizontalDistance = Math.max(0.001D, horizontalDistance(currentPosition(player), target));
			movementReferenceYaw = currentYaw(player);
		}

		@Override
		protected boolean tick() {
			ticks++;
			if (lookTarget != null && lookingAtTarget) {
				if (!isCloseToTarget(player, target, 0.35D)) {
					setMovementKeysForTarget(player, target, movementReferenceYaw);
				} else {
					setMovementKeys(false, false, false, false);
				}
				return ticks - lookStartTick >= LOOK_TARGET_TURN_TICKS || ticks >= maxTicks;
			}

			if (isCloseToTarget(player, target, 0.35D)) {
				setMovementKeys(false, false, false, false);
				if (lookTarget != null) {
					lookingAtTarget = true;
					lookStartTick = ticks;
					lookCameraTurn.reset();
					return false;
				}
			} else {
				float travelYaw = levelRotationTo(player, target).yaw();
				float yawError = Math.abs(Mth.wrapDegrees(travelYaw - movementReferenceYaw));
				if (yawError > 75F) {
					setMovementKeys(false, false, false, false);
				} else {
					setMovementKeysForTarget(player, target, movementReferenceYaw);
				}
			}
			return lookTarget == null && currentPosition(player).distanceToSqr(target) < 0.25D || ticks >= maxTicks;
		}

		@Override
		protected void renderFrame(ViewportEvent.ComputeCameraAngles event) {
			Rotation travelRotation = levelRotationTo(player, target);
			if (lookTarget == null || !shouldLookAtTargetYet()) {
				Rotation cameraRotation = travelCameraTurn.render(player, event, travelRotation, ticks, Math.min(maxTicks, TRAVEL_TURN_TICKS));
				movementReferenceYaw = cameraRotation.yaw();
				recordActionDebug("travel", cameraRotation, travelRotation, target);
				return;
			}

			if (!lookingAtTarget) {
				lookingAtTarget = true;
				lookStartTick = ticks;
				lookCameraTurn.reset();
			}

			Rotation targetRotation = rotationTo(player, lookTarget);
			Rotation cameraRotation = lookCameraTurn.render(player, event, targetRotation, ticks - lookStartTick, LOOK_TARGET_TURN_TICKS, true);
			movementReferenceYaw = cameraRotation.yaw();
			recordActionDebug("lookTarget", cameraRotation, targetRotation, lookTarget);
		}

		private void recordActionDebug(String phase, Rotation cameraRotation, Rotation targetRotation, Vec3 currentLookTarget) {
			DemoMotionRecorder.setActionDebug(new DemoMotionRecorder.ActionDebug("moveTo", phase, ticks, maxTicks,
					horizontalDistance(currentPosition(player), target), target.x, target.y, target.z, currentLookTarget.x, currentLookTarget.y,
					currentLookTarget.z, cameraRotation.yaw(), cameraRotation.pitch(), targetRotation.yaw(), targetRotation.pitch(), movementReferenceYaw));
		}

		private boolean shouldLookAtTargetYet() {
			double remainingDistance = horizontalDistance(currentPosition(player), target);
			float moveProgress = (float) Mth.clamp(1D - remainingDistance / initialHorizontalDistance, 0D, 1D);
			return moveProgress >= 0.45F;
		}
	}

	private static class VisualMobCatchAction {
		private static final int PRE_CLICK_SNEAK_TICKS = 10;
		private static final int POST_CLICK_SNEAK_TICKS = 10;
		private static final int TRAVEL_TURN_TICKS = 24;
		private static final int LOOK_TARGET_TURN_TICKS = 32;

		private final ServerPlayer player;
		private final LivingEntity target;
		private final int startTick;
		private final int maxTicks;
		private final double initialHorizontalDistance;
		private final EasedCameraTurn travelCameraTurn = new EasedCameraTurn();
		private final EasedCameraTurn lookCameraTurn = new EasedCameraTurn();
		private float movementReferenceYaw;
		private boolean lookingAtTarget;
		private int lookStartTick;
		private int sneakStartTick = -1;
		private int clickTick = -1;

		private VisualMobCatchAction(ServerPlayer player, LivingEntity target, int startTick, int maxTicks) {
			this.player = player;
			this.target = target;
			this.startTick = startTick;
			this.maxTicks = maxTicks;
			initialHorizontalDistance = Math.max(0.001D, horizontalDistance(currentPosition(player), target.position()));
			movementReferenceYaw = currentYaw(player);
		}

		private boolean tick() {
			if (!target.isAlive()) {
				player.setShiftKeyDown(false);
				setMovementKeys(false, false, false, false);
				return true;
			}

			int age = player.tickCount - startTick;
			Vec3 targetPosition = target.position();
			double horizontalDistance = horizontalDistance(currentPosition(player), targetPosition);
			player.getInventory().setSelectedSlot(0);

			if (clickTick >= 0) {
				setMovementKeys(false, false, false, false);
				player.setShiftKeyDown(true);
				if (player.tickCount - clickTick >= POST_CLICK_SNEAK_TICKS) {
					player.setShiftKeyDown(false);
					return true;
				}
				return false;
			}

			boolean closeEnough = horizontalDistance <= 2.35D;
			if (closeEnough && !lookingAtTarget) {
				lookingAtTarget = true;
				lookStartTick = age;
				lookCameraTurn.reset();
			}

			if (!closeEnough) {
				float travelYaw = levelRotationTo(player, targetPosition).yaw();
				float yawError = Math.abs(Mth.wrapDegrees(travelYaw - movementReferenceYaw));
				if (yawError > 75F) {
					setMovementKeys(false, false, false, false);
				} else {
					setMovementKeysForTarget(player, targetPosition, movementReferenceYaw);
				}
			} else {
				setMovementKeys(false, false, false, false);
			}

			boolean lookSettled = lookingAtTarget && age - lookStartTick >= LOOK_TARGET_TURN_TICKS;
			boolean shouldSneak = closeEnough && (lookSettled || age > 20);
			if (shouldSneak) {
				if (sneakStartTick < 0) {
					sneakStartTick = player.tickCount;
				}
				player.setShiftKeyDown(true);
			} else {
				player.setShiftKeyDown(false);
				sneakStartTick = -1;
			}

			boolean sneakedLongEnough = sneakStartTick >= 0 && player.tickCount - sneakStartTick >= PRE_CLICK_SNEAK_TICKS;
			if ((closeEnough && lookSettled && sneakedLongEnough) || age >= maxTicks) {
				rotatePlayer(player, currentYaw(player), currentPitch(player));
				setMovementKeys(false, false, false, false);
				player.setShiftKeyDown(true);
				player.swing(InteractionHand.MAIN_HAND, true);
				InteractionResult result = interactWithMobCatcherTarget(player, target);
				if (!result.consumesAction()) {
					throw new IllegalStateException("Mob catcher sneak right-click did not capture " + target.getType().toShortString());
				}
				clickTick = player.tickCount;
			}
			return false;
		}

		private void renderFrame(ViewportEvent.ComputeCameraAngles event) {
			if (!target.isAlive()) {
				return;
			}

			int age = player.tickCount - startTick;
			double remainingDistance = horizontalDistance(currentPosition(player), target.position());
			if (!lookingAtTarget && remainingDistance > 2.35D) {
				Rotation travelRotation = levelRotationTo(player, target.position());
				Rotation cameraRotation = travelCameraTurn.render(player, event, travelRotation, age, Math.min(maxTicks, TRAVEL_TURN_TICKS));
				movementReferenceYaw = cameraRotation.yaw();
				DemoMotionRecorder.setActionDebug(new DemoMotionRecorder.ActionDebug("mobCatcher", "travel", age, maxTicks, remainingDistance,
						target.position().x, target.position().y, target.position().z, target.position().x, currentEyePosition(player).y, target.position().z,
						cameraRotation.yaw(), cameraRotation.pitch(), travelRotation.yaw(), travelRotation.pitch(), movementReferenceYaw));
				return;
			}

			if (!lookingAtTarget) {
				lookingAtTarget = true;
				lookStartTick = age;
				lookCameraTurn.reset();
			}

			float partialTick = (float) event.getPartialTick();
			Minecraft minecraft = Minecraft.getInstance();
			Vec3 eyePosition = minecraft.player != null ? minecraft.player.getEyePosition(partialTick) : player.getEyePosition(partialTick);
			Vec3 targetEyePosition = target.getEyePosition(partialTick);
			Rotation targetRotation = rotationFromEye(eyePosition, targetEyePosition);
			Rotation cameraRotation = lookCameraTurn.render(player, event, targetRotation, age - lookStartTick, LOOK_TARGET_TURN_TICKS, true);
			movementReferenceYaw = cameraRotation.yaw();
			DemoMotionRecorder.setActionDebug(new DemoMotionRecorder.ActionDebug("mobCatcher", clickTick >= 0 ? "postClick" : "lookTarget", age, maxTicks,
					remainingDistance, target.position().x, target.position().y, target.position().z, targetEyePosition.x, targetEyePosition.y,
					targetEyePosition.z, cameraRotation.yaw(), cameraRotation.pitch(), targetRotation.yaw(), targetRotation.pitch(), movementReferenceYaw));
		}

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
					itemActionHandlerRegistry.getMethod("getEntityHandler", Identifier.class),
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
			player.getInventory().setSelectedSlot(0);
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
			player.getInventory().setSelectedSlot(0);
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
			NeoForge.EVENT_BUS.post(new InputEvent.Key(new KeyEvent(key.getValue(), 0, GLFW.GLFW_PRESS), GLFW.GLFW_MOD_ALT));
			NeoForge.EVENT_BUS.post(new InputEvent.Key(new KeyEvent(key.getValue(), 0, GLFW.GLFW_RELEASE), GLFW.GLFW_MOD_ALT));
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
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			player.getInventory().setItem(slot, ItemStack.EMPTY);
		}
		player.getInventory().setChanged();
		player.containerMenu.broadcastChanges();
	}

	private static ItemStack createConfiguredBackpack(String mode, List<ItemSeed> itemSeeds) {
		String normalizedMode = mode.toLowerCase(Locale.ROOT);
		ItemStack backpack = new ItemStack(normalizedMode.equals("advanced_mob_catcher") || normalizedMode.equals("advanced_mobcatcher")
				? ModItems.NETHERITE_BACKPACK.get()
				: ModItems.DIAMOND_BACKPACK.get());
		backpack.set(ModCoreDataComponents.STORAGE_UUID, UUID.randomUUID());

		IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
		UpgradeHandler upgrades = wrapper.getUpgradeHandler();
		if (normalizedMode.equals("restock")) {
			upgrades.setStackInSlot(0, new ItemStack(ModItems.RESTOCK_UPGRADE.get()));
		} else if (normalizedMode.equals("mob_catcher") || normalizedMode.equals("mobcatcher") || normalizedMode.equals("basic_mob_catcher")) {
			upgrades.setStackInSlot(0, createItemStack("sophisticatedbackpacks:mob_catcher_upgrade", 1));
		} else if (normalizedMode.equals("advanced_mob_catcher") || normalizedMode.equals("advanced_mobcatcher")) {
			upgrades.setStackInSlot(0, createItemStack("sophisticatedbackpacks:advanced_mob_catcher_upgrade", 1));
		} else {
			upgrades.setStackInSlot(0, new ItemStack(ModItems.DEPOSIT_UPGRADE.get()));
		}
		upgrades.saveInventory();

		InventoryHandler inventory = wrapper.getInventoryHandler();
		for (int slot = 0; slot < itemSeeds.size(); slot++) {
			ItemSeed itemSeed = itemSeeds.get(slot);
			Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemSeed.itemName()))
					.orElseThrow(() -> new IllegalArgumentException("Unknown item " + itemSeed.itemName()));
			inventory.setStackInSlot(slot, new ItemStack(item, itemSeed.count()));
		}
		inventory.saveInventory();
		return backpack;
	}

	private static ItemStack createItemStack(String itemName, int count) {
		Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemName))
				.orElseThrow(() -> new IllegalArgumentException("Unknown item " + itemName));
		return new ItemStack(item, count);
	}

	private static List<ItemSeed> defaultBackpackItemSeeds(String mode) {
		String normalizedMode = mode.toLowerCase(Locale.ROOT);
		if (normalizedMode.contains("mob_catcher") || normalizedMode.contains("mobcatcher")) {
			return List.of();
		}
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
