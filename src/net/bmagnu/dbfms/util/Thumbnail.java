package net.bmagnu.dbfms.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import javax.imageio.ImageIO;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.JavaFXFrameConverter;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.util.Pair;

import net.bmagnu.dbfms.database.LocalDatabase;

public abstract class Thumbnail {
	
	public abstract Image loadImage();

	public static Thumbnail getThumbnail(String filePath, String fileThumb) {
		//TODO Cache Thumbs?
		
		if(!fileThumb.isEmpty())
			return new ThumbnailImage(LocalDatabase.thumbDBDir + fileThumb);
		
		String mime = "";
		try {
			mime = Files.probeContentType(Paths.get(filePath));
		} catch (IOException e) {
			Logger.logError(e);
		}
		
		if(mime == null)
			return new ThumbnailDirectory(filePath);
		
		if(!mime.isEmpty() && mime.split("/")[0].equals("image"))
			return new ThumbnailImage(filePath);
		
		if(!mime.isEmpty() && mime.split("/")[0].equals("video"))
			return new ThumbnailVideo(filePath);
		
		return new ThumbnailFileThumbs(filePath);
	}
	
	public static Pair<String, Thumbnail> emplaceThumbnailInCache(String image){
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
				
				Files.copy(Paths.get(image), Paths.get(LocalDatabase.thumbDBDir + hash));
				
				return new Pair<>(hash, new ThumbnailImage(LocalDatabase.thumbDBDir + hash));
			} catch (IOException e1) {
				Logger.logError(e1);
			}
		}

		return new Pair<>("", new ThumbnailNull());
	}
	
	public static Pair<String, Thumbnail> emplaceThumbnailInCache(Thumbnail thumb) {
		//FIXME Implement rendering 300x300 Thumb and emplace in cache
		return null;
	}
	
}

class ThumbnailImage extends Thumbnail{
	

	private final Path path;
	
	public ThumbnailImage(String path) {
		this.path = Paths.get(path);
	}
	
	@Override
	public Image loadImage() {
		return new Image(path.toUri().toString());
	}
	
}

class ThumbnailVideo extends Thumbnail{
	
	private final FFmpegFrameGrabber g;
	
	private static final JavaFXFrameConverter conv = new JavaFXFrameConverter();
	
	static {
		avutil.av_log_set_level(avutil.AV_LOG_QUIET);
	}
	
	public ThumbnailVideo(String path) {
		g = new FFmpegFrameGrabber(path);
	}
	
	@Override
	public Image loadImage() {
		Image image = null;
		
		try {
			g.start();
			g.setTimestamp(g.getLengthInTime() / 3, false);
			
			Frame frame = g.grabImage();
			
			image = conv.convert(frame);
			
			g.stop();
			g.close();
		} catch (Exception e) {
			Logger.logError(e);
		}
		
		return image;
	}
	
}

class ThumbnailFileThumbs extends Thumbnail {
	
	private final String path;
	
	private static final Font font = new Font("Arial", Font.PLAIN, 20);
	
	private static BufferedImage icon; 
	
	static {
		try {
			icon = ImageIO.read(ThumbnailDirectory.class.getResource("icon_file.png"));
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
}

class ThumbnailDirectory extends Thumbnail {
	
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
}

class ThumbnailNull extends Thumbnail {
	
	@Override
	public Image loadImage() {
		return null;
	}
}