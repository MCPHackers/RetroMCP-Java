import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import com.mojang.minecraft.Minecraft;

public class Start
{

   public static void startMainThread() {
      boolean var3 = false;
      Frame var5 = new Frame("Minecraft");
      Canvas var6 = new Canvas();
      var5.setLayout(new BorderLayout());
      var5.add(var6, "Center");
      int width = 854;
      int height = 480;
      var6.setPreferredSize(new Dimension(width, height));
      var5.pack();
      var5.setLocationRelativeTo((Component)null);
      Minecraft var7 = new Minecraft(var6, width, height, var3);
      Thread var8 = new Thread(var7, "Minecraft main thread");
      var8.setPriority(10);
      var5.setVisible(true);
      var5.addWindowListener(new GameWindowListener(var7, var8));
      var8.start();
   }

   public static void main(String[] var0) {
      startMainThread();
   }

}