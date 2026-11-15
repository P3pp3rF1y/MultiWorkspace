package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

/** Submits through the same chat-screen input path as a player, including client commands. */
public final class SendChatCommandAction extends TypedRegressionAction<SendChatCommandAction.Arguments> {
	public record Arguments(String command) {
	}

	public SendChatCommandAction() {
		super("input.chatCommand", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("command"), description);
		String command = requireString(args, "command", description);
		if (command.isBlank() || command.startsWith("/") || command.contains("\n") || command.length() > 255) {
			throw new IllegalArgumentException("Expected a single command without a leading slash on " + description);
		}
		return new Arguments(command);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			@Override
			public RegressionActionProgress tick() {
				if (context.minecraft().screen != null) {
					return fail("Close the current screen before submitting a chat command");
				}
				ChatScreen chat = new ChatScreen("/" + arguments.command());
				context.minecraft().setScreen(chat);
				if (!ForgeHooksClient.onScreenKeyPressedPre(chat, GLFW.GLFW_KEY_ENTER, 0, 0)) {
					chat.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
				}
				return context.minecraft().screen == null ? RegressionActionProgress.PASSED : fail("Chat command did not close chat");
			}
		};
	}
}
