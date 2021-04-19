package net.bmagnu.dbfms.util.thumbnails;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import net.bmagnu.dbfms.util.Logger;
import net.bmagnu.dbfms.util.Thumbnail;

public class ThumbnailWeb extends Thumbnail {

	private static final String[] icons = {"shortcut icon", "icon", "apple-touch-icon", "apple-touch-icon-precomposed", "SHORTCUT ICON", "ICON", "APPLE-TOUCH-ICON", "APPLE-TOUCH-ICON-PRECOMPOSED"};
	
	private static final Image iconGeneric = new Image(ThumbnailFileThumbs.class.getResourceAsStream("icon_web.png"));
	
	String url;

	public ThumbnailWeb(String url) {
		this.url = url;
	}

	@Override
	protected Image loadImage() {
		String urlBase = url.split("/")[0] + "//" + url.split("/")[2];

		String title = "";
		
		List<URI> iconURIs = new ArrayList<>();
		
		
		iconURIs.add(URI.create(urlBase + "/favicon.ico"));
		iconURIs.add(URI.create(urlBase + "/apple-touch-icon.png"));
		iconURIs.add(URI.create(urlBase + "/apple-touch-icon-precomposed.png"));

		try {
			URI uri = URI.create(url);

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(uri)
					.build();

			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			String html = response.body();

			String relCollated = "(";
			for(String rel : icons) {
				relCollated += rel + '|';
			}
			relCollated = relCollated.substring(0, relCollated.length() -1 ) + ')';
			
			Pattern link = Pattern.compile("<link[^>]*?rel=\"" + relCollated + "\"[^>]*?>");
			Pattern href = Pattern.compile("href=\"[^\"]*?\"");
			Matcher matcher = link.matcher(html);
			while (matcher.find()) {
				Matcher hrefMatcher = href.matcher(matcher.group());
				if(hrefMatcher.find()) {
					String hrefString = hrefMatcher.group();
					iconURIs.add(makeURIAbsolute(uri, hrefString.substring(6, hrefString.length() - 1)));
				}
			}
			
			Pattern titlePattern = Pattern.compile("<title[^>]*?>[^<]*");
			Matcher titleMatcher = titlePattern.matcher(html);
			if(titleMatcher.find()) {
				String tempTitle = titleMatcher.group();
				title = tempTitle.substring(tempTitle.indexOf('>') + 1);
			}
			

		} catch (IOException | InterruptedException e) {
			Logger.logError(e);
		}

		List<Image> iconFiles = new ArrayList<>();

		HttpClient client = HttpClient.newHttpClient();

		for (URI icon : iconURIs) {

			HttpRequest request = HttpRequest.newBuilder().uri(icon).build();

			try {
				HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
				
				if (response.statusCode() != 200)
					continue;
				
				byte[] data = response.body().readAllBytes();
				Image image = new Image(new ByteArrayInputStream(data));
				
				if (image.isError())
				{
					//Probably a Microsoft icon.
					image = iconGeneric;
				}

				iconFiles.add(image);
			} catch (Exception e) {
				Logger.logInfo("Couldn't download Icon " + icon.toString());
			}
		}

		int max = 0;
		Image largest = null;

		for (Image icon : iconFiles) {
			int localDims = (int) (icon.getWidth() + icon.getHeight());
			if (max > localDims)
				continue;

			max = localDims;
			largest = icon;
		}

		if (largest != null)
			return super.makeIconWithText(SwingFXUtils.fromFXImage(largest, null), title, "", url);
		
		return null;
	}

	@Override
	public boolean shouldCache() {
		return true;
	}

	private URI makeURIAbsolute(URI base, String attachment) {
		URI attach = URI.create(attachment);
		if (attach.isAbsolute())
			return attach;

		return base.resolve(attach);
	}
}
