import org.lwjgl.LWJGLException;
import com.mojang.minecraft.RubyDung;

public class Start
{
   public static void main(String[] args) {
	   try {
		   RubyDung.main(args);
	   } catch (LWJGLException ex) {
		   ex.printStackTrace();
	   }
   }

}