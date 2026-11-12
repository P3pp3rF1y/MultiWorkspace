package net.p3pp3rf1y.devclientautomation.regression.actions;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public final class CaptureScreenshotAction extends TypedRegressionAction<String> {

	public CaptureScreenshotAction() {
		super("diagnostic.screenshot", "1", Set.of(CLIENT_CAPABILITY));
	}

	@Override
	protected String parseArguments(JsonObject args, String description) {
		requireOnly(args, Set.of("name"), description);
		String name = requireString(args, "name", description);
		if (!name.matches("[a-z0-9_-]{1,80}")) {
			throw new IllegalArgumentException("Invalid screenshot name on " + description);
		}
		return name;
	}

	@Override
	protected RegressionActionExecution start(RegressionExecutionContext context, String name) {
		return new TickRegressionActionExecution(context) {
			private int frames;

			@Override
			public RegressionActionProgress tick() {
				if (++frames < 5) {
					return RegressionActionProgress.WAITING;
				}
				Path destination = context.minecraft().gameDirectory.toPath().resolve("screenshots/regression").resolve(name + ".png");
				try (NativeImage image = Screenshot.takeScreenshot(context.minecraft().getMainRenderTarget())) {
					Files.createDirectories(destination.getParent());
					Files.write(destination, image.asByteArray());
					context.putDiagnostic("screenshot", destination.toAbsolutePath().toString());
					return RegressionActionProgress.PASSED;
				} catch (IOException exception) {
					throw new IllegalStateException("Unable to capture rendered frame", exception);
				}
			}
		};
	}
}
