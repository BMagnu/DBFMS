package net.bmagnu.dbfms.util.thumbnails;

import java.io.File;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.JavaFXFrameConverter;
import org.bytedeco.javacv.FrameGrabber.Exception;

import javafx.scene.image.Image;
import net.bmagnu.dbfms.util.Logger;
import net.bmagnu.dbfms.util.Thumbnail;

public class ThumbnailVideo extends Thumbnail{
	
	private final FFmpegFrameGrabber g;
	
	private static final JavaFXFrameConverter conv = new JavaFXFrameConverter();
	
	static {
		avutil.av_log_set_level(avutil.AV_LOG_QUIET);
	}
	
	public ThumbnailVideo(File path) {
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
	
	@Override
	public boolean shouldCache() {
		return false;
	}
}
