package datastore;

public class MusicData {
	private String filename;
	private String title;
	private String disc;
	private int track;
	
	/**
	 * Constructor
	 * @param filename String of Filename
	 * @param title String of Track Title
	 * @param disc String of Disc Name
	 * @param track String of Track Number
	 */
	public MusicData(String filename, String title, String disc, String track) {
		this.filename = filename;
		this.title = title;
		this.disc = disc;
		this.track = Integer.parseInt(track);
	}
	
	/**
	 * Get Filename
	 * @return filename
	 */
	public String getFileName() {
		return this.filename;
	}
	
	/**
	 * Get Track Title
	 * @return Track Title
	 */
	public String getTitle() {
		return this.title;
	}
	
	/**
	 * Get Disc Name
	 * @return Disc Name 
	 */
	public String getDisc() {
		return this.disc;
	}
	
	/**
	 * Get Track Number
	 * @return track number
	 */
	public int getTrack() {
		return this.track;
	}
}