package net.bmagnu.dbfms.util.thumbnails;

import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.scene.image.Image;
import net.bmagnu.dbfms.util.Thumbnail;

public class ThumbnailImage extends Thumbnail{
	

	private final Path path;
	
	public ThumbnailImage(String path) {
		this.path = Paths.get(path);
	}
	
	@Override
	protected Image loadImage() {
		return new Image(path.toUri().toString());
	}
	
	@Override
	public boolean shouldCache() {
		return false;
	}
}
