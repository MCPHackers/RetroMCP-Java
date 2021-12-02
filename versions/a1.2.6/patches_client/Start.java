import net.minecraft.client.Minecraft;
import java.io.File;
import java.lang.reflect.Field;

public class Start
{

  public static void main(String[] args)
  {
      try
      {
          Field f = Minecraft.class.getDeclaredField("minecraftDir");
          Field.setAccessible(new Field[] { f }, true);
          f.set(null, new File("."));
      }
      catch (Exception e)
      {
          e.printStackTrace();
          return;
      }
      Minecraft.main(args);
   }

}
