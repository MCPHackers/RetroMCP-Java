import java.net.URL;

import net.minecraft.client.MinecraftApplet;

public class MinecraftAppletImpl extends MinecraftApplet
{	
	public MinecraftAppletImpl()
	{
	}
	
	public URL getDocumentBase() {
		URL url;
		try {
			url = new URL("http://www.minecraft.net/game/");
		} catch (Exception  ex) {
			url = null;
			ex.printStackTrace();
		}
		return url;
	}
}