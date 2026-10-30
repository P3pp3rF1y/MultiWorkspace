package net.p3pp3rf1y.devclientautomation.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccess {
	@Invoker("onPress")
	void pressVirtualButton(long window, int button, int action, int modifiers);

	@Accessor("xpos")
	void setVirtualX(double x);

	@Accessor("ypos")
	void setVirtualY(double y);
}
