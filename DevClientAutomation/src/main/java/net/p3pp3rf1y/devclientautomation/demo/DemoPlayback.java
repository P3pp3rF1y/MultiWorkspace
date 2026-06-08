package net.p3pp3rf1y.devclientautomation.demo;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class DemoPlayback {
	private static final Queue<String> COMMANDS = new ArrayDeque<>();
	private static CommandSourceStack source;
	private static int waitTicks = 0;
	private static int executedSteps = 0;
	private static String demoName = "";

	private DemoPlayback() {
	}

	public static void start(CommandSourceStack commandSource, String name, List<String> commands) {
		COMMANDS.clear();
		COMMANDS.addAll(commands);
		source = commandSource;
		waitTicks = 0;
		executedSteps = 0;
		demoName = name;
	}

	public static boolean isRunning() {
		return source != null;
	}

	public static void tick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !isRunning() || source.getServer() != event.getServer()) {
			return;
		}

		if (waitTicks > 0) {
			waitTicks--;
			return;
		}

		if (DemoCommand.hasRunningAction()) {
			return;
		}

		String command = COMMANDS.poll();
		if (command == null) {
			CommandSourceStack completedSource = source;
			int completedSteps = executedSteps;
			String completedDemoName = demoName;
			stop();
			DemoCommand.success(completedSource, () -> Component.literal("Finished demo " + completedDemoName + " after " + completedSteps + " steps"));
			return;
		}

		int wait = DemoCommand.waitTicks(command);
		if (wait >= 0) {
			waitTicks = wait;
			executedSteps++;
			return;
		}

		executedSteps += DemoCommand.executeRecordedCommand(source, command);
	}

	private static void stop() {
		COMMANDS.clear();
		source = null;
		waitTicks = 0;
		executedSteps = 0;
		demoName = "";
	}
}
