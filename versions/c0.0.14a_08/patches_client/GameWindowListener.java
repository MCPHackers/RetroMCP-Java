import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;

import com.mojang.minecraft.Minecraft;

public final class GameWindowListener extends WindowAdapter
{
   final Minecraft mc;
   final Thread thread;


   public GameWindowListener(Minecraft var1, Thread var2) {
      this.mc = var1;
      this.thread = var2;
   }

   public void windowClosing(WindowEvent var1) {
	  try {
		 Field isRunning = mc.getClass().getDeclaredField("running");
		 isRunning.setAccessible(true);
		 isRunning.set(mc, false);
         this.thread.join();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      System.exit(0);
   }
}
