import java.awt.Canvas;
import java.awt.Component;
import java.awt.Frame;
import net.minecraft.src.Minecraft;
import net.minecraft.client.MinecraftApplet;
import net.minecraft.src.PanelCrashReport;
import net.minecraft.src.UnexpectedThrowable;

public final class MinecraftImpl extends Minecraft {

   // $FF: synthetic field
   final Frame mcFrame;


   public MinecraftImpl(Component var1, Canvas var2, MinecraftApplet var3, int var4, int var5, boolean var6, Frame var7) {
      super(var1, var2, var3, var4, var5, var6);
      this.mcFrame = var7;
   }

   public void displayUnexpectedThrowable(UnexpectedThrowable var1) {
      this.mcFrame.removeAll();
      this.mcFrame.add(new PanelCrashReport(var1), "Center");
      this.mcFrame.validate();
   }
}
