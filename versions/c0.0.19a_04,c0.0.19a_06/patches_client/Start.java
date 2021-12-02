import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.lang.reflect.Field;

import com.mojang.minecraft.Minecraft;
import com.mojang.minecraft.User;

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
      int width = 854;
      int height = 480;
      var6.setPreferredSize(new Dimension(width, height));
      var5.pack();
      var5.setLocationRelativeTo((Component)null);
      Minecraft var7 = new Minecraft(var6, width, height, var3);
      if(var0 != null && var1 != null) {
          var7.user = new User(var0, var1);
      } else {
          var7.user = new User("Player" + System.currentTimeMillis() % 1000L, "");
      }
      var7.user.mpPass = "-";
      if(var2 != null) {
         String[] var9 = var2.split(":");
   	  	 try {
   	  		 Field server = var7.getClass().getDeclaredField("server");
   	  		 server.setAccessible(true);
   	  		 server.set(var7, var9[0]);
   	  		 Field port = var7.getClass().getDeclaredField("port");
   	  		 port.setAccessible(true);
   	  		 port.set(var7, Integer.parseInt(var9[1]));
   	  	 } catch (Exception ex) {
   	  		 ex.printStackTrace();
   	  	 }
      }
      var7.width = width;
      var7.height = height;
      Thread var8 = new Thread(var7, "Minecraft main thread");
      var8.setPriority(10);
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