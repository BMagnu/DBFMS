package net.bmagnu.dbfms.util.thumbnails;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.bmagnu.dbfms.util.Thumbnail;

public class ThumbnailDirectory extends Thumbnail {
	
	private final String path;
	
	private static final Font font = new Font("Arial", Font.PLAIN, 20);
	
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
		
		BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = (Graphics2D)image.getGraphics();
	
		g.setColor(Color.BLACK);
		FontMetrics metrics = g.getFontMetrics(font);
	    int x = 150 - (metrics.stringWidth(path) / 2);
	    int y = 250 - (metrics.getHeight() / 2) + metrics.getAscent();
	    g.setFont(font);
	    g.drawString(path, x, y);
	    g.drawImage(icon, 150 - icon.getWidth()/2, 150 - icon.getHeight()/2, null);
		
		return SwingFXUtils.toFXImage(image, null);
	}
	
	@Override
	public boolean shouldCache() {
		return false;
	}
}
