package net.bmagnu.dbfms.util.thumbnails;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.scene.image.Image;
import net.bmagnu.dbfms.util.Thumbnail;

public class ThumbnailDirectory extends Thumbnail {
	
	private final String path;
	
	private static BufferedImage icon; 
	
	static {
		try {
			icon = ImageIO.read(ThumbnailDirectory.class.getResource("icon_folder.png"));
		} catch (IOException e) {
			icon = null;
		}
	}
	
	public ThumbnailDirectory(String path) {
		String[] split = path.split("/|\\\\");
		this.path = split[split.length - 1];
	}
	
	@Override
	public Image loadImage() {
		return super.makeIconWithText(icon, path);
	}
	
	@Override
	public boolean shouldCache() {
		return false;
	}
}
