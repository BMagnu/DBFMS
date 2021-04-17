package net.bmagnu.dbfms.util.thumbnails;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.scene.image.Image;
import net.bmagnu.dbfms.util.Thumbnail;

public class ThumbnailFileThumbs extends Thumbnail {
	
	private final String path;
	
	private static BufferedImage icon; 
	
	static {
		try {
			icon = ImageIO.read(ThumbnailFileThumbs.class.getResource("icon_file.png"));
		} catch (IOException e) {
			icon = null;
		}
	}
	
	public ThumbnailFileThumbs(String path) {
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