import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import net.minecraft.client.MinecraftApplet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Session;

public class Start
{
   public static void startMainThread1(String var0, String var1) {
      startMainThread(var0, var1, (String)null);
   }

   public static void startMainThread(String var0, String var1, String var2) {
      boolean var3 = false;
      Frame var5 = new Frame("Minecraft");
      Canvas var6 = new Canvas();
      var5.setLayout(new BorderLayout());
      var5.add(var6, "Center");
      var6.setPreferredSize(new Dimension(854, 480));
      var5.pack();
      var5.setLocationRelativeTo((Component)null);
      Minecraft var7 = new Minecraft(var6, new MinecraftAppletImpl(), 854, 480, var3);
      Thread var8 = new Thread(var7, "Minecraft main thread");
      var8.setPriority(10);
      var7.appletMode = false;
      var7.minecraftUri = "www.minecraft.net";
      if(var0 != null && var1 != null) {
         var7.session = new Session(var0, var1);
      } else {
         var7.session = new Session("Player" + System.currentTimeMillis() % 1000L, "");
      }

      if(var2 != null) {
         String[] var9 = var2.split(":");
         var7.setServer(var9[0], Integer.parseInt(var9[1]));
      }

      var5.setVisible(true);
      var5.addWindowListener(new GameWindowListener(var7, var8));
      var8.start();
   }

   public static void main(String[] var0) {
      String var1 = "Player" + System.currentTimeMillis() % 1000L;
      if(var0.length > 0) {
         var1 = var0[0];
      }

      String var2 = "-";
      if(var0.length > 1) {
         var2 = var0[1];
      }

      startMainThread1(var1, var2);
   }

}