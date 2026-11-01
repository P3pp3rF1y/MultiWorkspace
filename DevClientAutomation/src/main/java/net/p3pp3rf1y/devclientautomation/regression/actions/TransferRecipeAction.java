package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.p3pp3rf1y.devclientautomation.input.VirtualInput;
import net.p3pp3rf1y.devclientautomation.recipeviewer.jei.JeiRecipeViewerAutomation;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Transfers only through JEI's rendered transfer button, never its handler API. */
public final class TransferRecipeAction extends TypedRegressionAction<TransferRecipeAction.Arguments> {
	private static final Pattern RECIPE_RESOURCE_LOCATION = Pattern.compile("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+");
	public record Arguments(Identifier recipeResourceId, boolean maxTransfer) {
	}

	public TransferRecipeAction() {
		super("recipeviewer.transferRecipe", "1", Set.of("minecraft:client", "jei"));
	}

	@Override
	protected Arguments parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("recipeResourceID", "maxTransfer"), description);
		String id = requireString(args, "recipeResourceID", description);
		if (!RECIPE_RESOURCE_LOCATION.matcher(id).matches()) {
			throw new IllegalArgumentException("Argument recipeResourceID on " + description + " must be a resource location");
		}
		Identifier resourceLocation;
		try {
			resourceLocation = Identifier.parse(id);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Argument recipeResourceID on " + description + " must be a resource location", e);
		}
		return new Arguments(resourceLocation, optionalBoolean(args, "maxTransfer", false, description));
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, Arguments arguments) {
		return new TickRegressionActionExecution(context) {
			private Object parentMenu;
			private boolean navigated;
			private boolean clicked;

			@Override
			public RegressionActionProgress tick() {
				if (!navigated) {
					if (!(context.minecraft().gui.screen() instanceof StorageScreenBase<?> screen)) {
						return fail("Storage screen is not active for JEI recipe transfer");
					}
					parentMenu = screen.getMenu();
					try {
						if (!JeiRecipeViewerAutomation.showExactRecipe(arguments.recipeResourceId())) {
							return fail("JEI runtime is not available for recipe transfer");
						}
					} catch (IllegalStateException e) {
						return fail(e.getMessage());
					}
					navigated = true;
					return RegressionActionProgress.WAITING;
				}
				if (!clicked) {
					if (!JeiRecipeViewerAutomation.isRecipeScreenOpen(Minecraft.getInstance().gui.screen())) {
						return RegressionActionProgress.WAITING;
					}
					try {
						if (JeiRecipeViewerAutomation.parentContainer(Minecraft.getInstance().gui.screen()) != parentMenu) {
							return fail("JEI recipe screen did not retain the active storage menu");
						}
						List<IRecipeLayoutDrawable<?>> matching = JeiRecipeViewerAutomation.openedRecipeLayouts(Minecraft.getInstance().gui.screen()).stream()
								.filter(layout -> arguments.recipeResourceId().toString().equals(JeiRecipeViewerAutomation.layoutRecipeId(layout))).toList();
						if (matching.isEmpty()) {
							return RegressionActionProgress.WAITING;
						}
						if (matching.size() != 1) {
							return fail("JEI recipe layout is not exact: " + arguments.recipeResourceId());
						}
						Rect2i layoutRect = matching.get(0).getRect();
						Rect2i buttonRect = matching.get(0).getRecipeTransferButtonArea();
						double centerX = layoutRect.getX() + buttonRect.getX() + buttonRect.getWidth() / 2D;
						double centerY = layoutRect.getY() + buttonRect.getY() + buttonRect.getHeight() / 2D;
						int modifiers = arguments.maxTransfer() ? GLFW.GLFW_MOD_SHIFT : 0;
						boolean handled = VirtualInput.click(Minecraft.getInstance().gui.screen(), centerX, centerY, 0, modifiers);
						observe(arguments.recipeResourceId().toString(),
								Map.of("layout", Map.of("x", layoutRect.getX(), "y", layoutRect.getY()), "button",
										Map.of("x", buttonRect.getX(), "y", buttonRect.getY(), "width", buttonRect.getWidth(), "height",
												buttonRect.getHeight()),
										"center", Map.of("x", centerX, "y", centerY), "mouseButton", 0, "modifiers", modifiers, "handled", handled,
										"parentMenu", parentMenu.getClass().getName()));
						if (!handled) {
							return fail("JEI recipe transfer button did not handle the click");
						}
						clicked = true;
						return RegressionActionProgress.WAITING;
					} catch (ReflectiveOperationException e) {
						return fail("JEI recipe layout reflection failed: " + e.getMessage());
					}
				}
				if (!(context.minecraft().gui.screen() instanceof StorageScreenBase<?> screen) || screen.getMenu() != parentMenu) {
					return fail("JEI transfer did not return to the original storage menu");
				}
				return RegressionActionProgress.PASSED;
			}
		};
	}
}
