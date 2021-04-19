package net.bmagnu.dbfms.util.thumbnails;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.FrameGrabber.Exception;

import net.bmagnu.dbfms.util.Logger;
import net.bmagnu.dbfms.util.Thumbnail;

public class ThumbnailAudio extends Thumbnail{
	
	private final FFmpegFrameGrabber g;
	private final String path;
	
	private static final Java2DFrameConverter conv = new Java2DFrameConverter();
	
	private static BufferedImage icon; 
	
	static {
		try {
			icon = ImageIO.read(ThumbnailFileThumbs.class.getResource("icon_music.png"));
		} catch (IOException e) {
			icon = null;
		}
		
		avutil.av_log_set_level(avutil.AV_LOG_QUIET);
	}
	
	public ThumbnailAudio(String path) {
		String[] split = path.split("/|\\\\");
		this.path = split[split.length - 1];
		
		g = new FFmpegFrameGrabber(new File(path));
	}
	
	@Override
	public Image loadImage() {
		String title = "";
		String artist = "";
		
		BufferedImage thumbImage = icon;

		try {
			g.start();
			
			Map<String, String> metadata = g.getMetadata();
			
			for(Entry<String, String> entry : metadata.entrySet()) {
				String key = entry.getKey().toLowerCase(Locale.ENGLISH);
				if(key.equals("title"))
					title = entry.getValue();
				else if (key.equals("artist"))
					artist = entry.getValue();
			}
			
			if (g.hasVideo()) {
				Frame thumb = g.grabImage();
				thumbImage = conv.convert(thumb);
			}
						
			g.stop();
			g.close();
		} catch (Exception e) {
			Logger.logError(e);
		} 
		
		return super.makeIconWithText(thumbImage, title, artist, path);
	}
	
	@Override
	public boolean shouldCache() {
		return false;
	}
}
