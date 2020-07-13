package net.bmagnu.dbfms.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.JavaFXFrameConverter;

import javafx.scene.image.Image;
import javafx.util.Pair;

import net.bmagnu.dbfms.database.LocalDatabase;

public abstract class Thumbnail {
	
	public abstract Image loadImage();

	public static Thumbnail getThumbnail(String filePath, String fileThumb) {
		if(!fileThumb.isEmpty())
			return new ThumbnailImage(LocalDatabase.thumbDBDir + fileThumb);
		
		String mime = "";
		try {
			mime = Files.probeContentType(Paths.get(filePath));
		} catch (IOException e) {
			Logger.logError(e);
		}
		
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
	
	public ThumbnailFileThumbs(String path) {
	}
	
	@Override
	public Image loadImage() {
		//TODO 
		return null;
	}
}

class ThumbnailNull extends Thumbnail {
	
	@Override
	public Image loadImage() {
		return null;
	}
}