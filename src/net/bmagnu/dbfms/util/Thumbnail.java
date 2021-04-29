package net.bmagnu.dbfms.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.util.Pair;

import net.bmagnu.dbfms.database.LocalDatabase;
import net.bmagnu.dbfms.util.thumbnails.ThumbnailAudio;
import net.bmagnu.dbfms.util.thumbnails.ThumbnailDirectory;
import net.bmagnu.dbfms.util.thumbnails.ThumbnailFileThumbs;
import net.bmagnu.dbfms.util.thumbnails.ThumbnailImage;
import net.bmagnu.dbfms.util.thumbnails.ThumbnailVideo;
import net.bmagnu.dbfms.util.thumbnails.ThumbnailWeb;

public abstract class Thumbnail {
	
	protected abstract Image loadImage();
	
	public abstract boolean shouldCache();
	
	private Image cache = null;
	
	public Image getImage() {
		if (cache == null)
			cache = loadImage();
		
		return cache;
	}

	public static Thumbnail getThumbnail(String filePath, String fileThumb) {
		if(!fileThumb.isEmpty())
			return new ThumbnailImage(LocalDatabase.thumbDBDir + fileThumb);
		
		try {
			if(filePath.startsWith("http://") || filePath.startsWith("https://"))
				return new ThumbnailWeb(filePath);
			
			String mime = "";
			Path path = Paths.get(filePath);
			mime = Files.probeContentType(path);
			
			if(Files.isDirectory(path))
				return new ThumbnailDirectory(filePath);
			
			if(mime == null)
				return new ThumbnailFileThumbs(filePath);
			
			if(!mime.isEmpty() && mime.split("/")[0].equals("image"))
				return new ThumbnailImage(filePath);
			
			if(!mime.isEmpty() && mime.split("/")[0].equals("video"))
				return new ThumbnailVideo(new File(filePath));
			
			if(!mime.isEmpty() && mime.split("/")[0].equals("audio"))
				return new ThumbnailAudio(filePath);
		} catch (IOException e) {
			Logger.logError(e);
		}
		
		return new ThumbnailFileThumbs(filePath);
	}
	
	private static Pair<String, Thumbnail> emplaceThumbnailInCache(String image){
		String mime = "";
		try {
			mime = Files.probeContentType(Paths.get(image));
		} catch (IOException e) {
			Logger.logError(e);
		}
		
		if(!mime.isEmpty() && mime.split("/")[0].equals("image")) {
			
			String hash = "";
			
			try {
				FileInputStream imageStream = new FileInputStream(image);
				byte[] imageData = imageStream.readAllBytes();

				
				try {
					MessageDigest md = MessageDigest.getInstance("SHA-1");
					Formatter formatter = new Formatter();
					for (byte b : md.digest(imageData)) {
					    formatter.format("%02x", b);
					}
					hash = formatter.toString();
					formatter.close();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} 
				
				imageStream.close();
				
				Files.copy(Paths.get(image), Paths.get(LocalDatabase.thumbDBDir + hash), StandardCopyOption.REPLACE_EXISTING);
				
				return new Pair<>(hash, new ThumbnailImage(LocalDatabase.thumbDBDir + hash));
			} catch (IOException e1) {
				Logger.logError(e1);
			}
		}

		return new Pair<>("", new ThumbnailNull());
	}
	
	private static Dimension getScaledDimension(int original_width, int original_height) {
		return getScaledDimension(original_width, original_height, 300, 300);
	}
	
	private static Dimension getScaledDimension(int original_width, int original_height, int bound_width, int bound_height) {
	    int new_width = original_width;
	    int new_height = original_height;

	    // first check if we need to scale width
	    if (original_width > bound_width) {
	        //scale width to fit
	        new_width = bound_width;
	        //scale height to maintain aspect ratio
	        new_height = (new_width * original_height) / original_width;
	    }

	    // then check if we need to scale even with the new height
	    if (new_height > bound_height) {
	        //scale height to fit instead
	        new_height = bound_height;
	        //scale width to maintain aspect ratio
	        new_width = (new_height * original_width) / original_height;
	    }

	    return new Dimension(new_width, new_height);
	}
	
	public static Pair<String, Thumbnail> emplaceThumbnailInCache(Image thumb) {
		
		java.awt.Image original = SwingFXUtils.fromFXImage(thumb, null);
		Dimension newDim = getScaledDimension(original.getWidth(null), original.getHeight(null));
		java.awt.Image tmp = original.getScaledInstance(newDim.width, newDim.height, java.awt.Image.SCALE_SMOOTH);

		BufferedImage dimg = new BufferedImage(newDim.width, newDim.height, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();
		
		try {
			boolean success = ImageIO.write(dimg, "png", new File(LocalDatabase.programDataDir + "tmp.png"));
			if(!success)
				throw new IOException("Thumbnail rescaling Failed!");
		} catch (IOException e) {
			Logger.logError(e);
			return new Pair<>("", new ThumbnailNull());
		}

		return emplaceThumbnailInCache(LocalDatabase.programDataDir + "tmp.png");
	}
	
	private static final Font font = new Font("Arial MS Unicode", Font.PLAIN, 20);
	private static final Map<?, ?> fontRenderHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
	protected static Image makeIconWithText(BufferedImage icon, String text, String text2, String textTop) {
		BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = (Graphics2D)image.getGraphics();
		if (fontRenderHints == null) {
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		}
		else {
			g.setRenderingHints(fontRenderHints);
		}

		Dimension imageSize = getScaledDimension(icon.getWidth(), icon.getHeight(), 144, 144);
		java.awt.Image tmp = icon.getScaledInstance(imageSize.width, imageSize.height, java.awt.Image.SCALE_SMOOTH);
		g.drawImage(tmp, 150 - tmp.getWidth(null)/2, 150 - tmp.getHeight(null)/2, null); 
		
		g.setColor(Color.BLACK);
		
		FontMetrics metrics = g.getFontMetrics(font);
		
		int maxWidthTop = metrics.stringWidth(textTop);
		float intendedSizeTop = (float)(font.getSize2D() * 290.0f/Math.max(maxWidthTop, 290));
		boolean cutTop = intendedSizeTop < 10.0f;
		Font fontNewTop = font.deriveFont(cutTop ? 10.0f : intendedSizeTop);
		
		int maxWidthBot = Math.max(metrics.stringWidth(text), metrics.stringWidth(text2));
		float intendedSizeBot = (float)(font.getSize2D() * 290.0f/Math.max(maxWidthBot, 290));
		boolean cutBot = intendedSizeBot < 10.0f;
		Font fontNewBot = font.deriveFont(cutBot ? 10.0f : intendedSizeBot);
		
		int lineDistance = metrics.getHeight();
		
		metrics = g.getFontMetrics(fontNewBot);
		g.setFont(fontNewBot);

		//Text1
		while(cutBot && metrics.stringWidth(text) > 290) {
			text = text.substring(0, text.length() - 1);
		}
	    int x = 150 - (metrics.stringWidth(text) / 2);
	    int y = 250 - (metrics.getHeight() / 2) + metrics.getAscent();
	    g.drawString(text, x, y);
	    
	    //Text2
	    if(!text2.isBlank()) {
			while(cutBot && metrics.stringWidth(text2) > 290) {
				text2 = text2.substring(0, text2.length() - 1);
			}
	    	x = 150 - (metrics.stringWidth(text2) / 2);
	    	y += lineDistance;
	    	g.drawString(text2, x, y);
	    }
	    
	    metrics = g.getFontMetrics(fontNewTop);
		g.setFont(fontNewTop);
	    
	    //TextTop
	    if(!textTop.isBlank()) {
			while(cutTop && metrics.stringWidth(textTop) > 290) {
				textTop = textTop.substring(0, textTop.length() - 1);
			}
	    	x = 150 - (metrics.stringWidth(textTop) / 2);
	    	y = 50;
	    	g.drawString(textTop, x, y);
	    }
		
		return SwingFXUtils.toFXImage(image, null);
	}
	
	protected static Image makeIconWithText(BufferedImage icon, String text) {
		return makeIconWithText(icon, text, "", "");
	}
	
}

class ThumbnailNull extends Thumbnail {
	
	@Override
	public Image loadImage() {
		return null;
	}
	
	@Override
	public boolean shouldCache() {
		return false;
	}
}