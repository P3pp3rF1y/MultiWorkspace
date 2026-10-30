package net.p3pp3rf1y.devclientautomation.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccess {
	@Invoker("onButton")
	void pressVirtualButton(long window, MouseButtonInfo button, int action);

	@Accessor("xpos")
	void setVirtualX(double x);

	@Accessor("ypos")
	void setVirtualY(double y);
}
