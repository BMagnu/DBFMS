package net.bmagnu.dbfms.util;

import java.awt.Dimension;
import java.awt.Graphics2D;
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

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.util.Pair;

import net.bmagnu.dbfms.database.LocalDatabase;
import net.bmagnu.dbfms.util.thumbnails.ThumbnailDirectory;
import net.bmagnu.dbfms.util.thumbnails.ThumbnailFileThumbs;
import net.bmagnu.dbfms.util.thumbnails.ThumbnailImage;
import net.bmagnu.dbfms.util.thumbnails.ThumbnailVideo;

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
				return new ThumbnailFileThumbs(filePath); //TODO WebIcon
			
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

	    int bound_width = 300;
	    int bound_height = 300;
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