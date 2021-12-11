import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import net.minecraft.src.Minecraft;

public final class GameWindowListener extends WindowAdapter {

   // $FF: synthetic field
   final Minecraft mc;
   // $FF: synthetic field
   final Thread thread;


   public GameWindowListener(Minecraft var1, Thread var2) {
      this.mc = var1;
      this.thread = var2;
   }

   public void windowClosing(WindowEvent var1) {
      this.mc.shutdown();

      try {
         this.thread.join();
      } catch (InterruptedException var3) {
         var3.printStackTrace();
      }

      System.exit(0);
   }
}
