package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;

/** Checks actual chat UI Tab cycling, including the displayed suggestion order. */
public final class AssertChatCompletionAction extends TypedRegressionAction<AssertChatCompletionAction.Arguments> {
	public record Arguments(String input, String first, @Nullable String second) {
	}

	public AssertChatCompletionAction() {
		super("assert.chatCompletion", "1", Set.of("minecraft:client"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("input", "first", "second"), description);
		return new Arguments(requireString(args, "input", description), requireString(args, "first", description),
				args.has("second") ? requireString(args, "second", description) : null);
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private ChatScreen chat;

			@Override
			public RegressionActionProgress tick() {
				if (chat == null) {
					if (context.minecraft().screen != null) {
						return fail("Close the current screen before checking chat completion");
					}
					chat = new ChatScreen(arguments.input());
					context.minecraft().setScreen(chat);
					return RegressionActionProgress.WAITING;
				}
				EditBox input = chat.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
				pressTab();
				// With an initial chat value, vanilla's first Tab opens the dropdown without filling it.
				if (input.getValue().equals(arguments.input())) {
					pressTab();
				}
				String first = input.getValue();
				String second = "";
				if (arguments.second() != null) {
					pressTab();
					second = input.getValue();
				}
				context.putDiagnostic("chatCompletion", Map.of("input", arguments.input(), "first", first, "second", second));
				chat.onClose();
				return first.equals(arguments.first()) && (arguments.second() == null || second.equals(arguments.second()))
						? RegressionActionProgress.PASSED
						: fail("Chat completion order mismatch: " + first + " ; " + second);
			}

			private void pressTab() {
				if (!ForgeHooksClient.onScreenKeyPressedPre(chat, GLFW.GLFW_KEY_TAB, 0, 0)) {
					chat.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
				}
			}
		};
	}
}
