import javax.swing.ImageIcon;

public class Result {
	
	public ImageIcon icon;
	public Double time;
	public Double rData;
	public Double gData;
	public Double bData;
	public Double startTemperature;
	public Double endTemperature;
	public String file;
	
	public Double cutoffData;
	public Double cutoffTemperatureData;
	
	public int totalFrames;
	public int frameCount;
	
	public Result() {
		
		clear();
	}
	
	public void clear() {
		icon = null;
		time = 0.0;
		rData = 0.0;
		gData = 0.0;
		bData = 0.0;
		startTemperature = 0.0;
		endTemperature = 0.0;
		
		totalFrames = 0;
		frameCount = 0;	
		
		cutoffData = 0.0;
		cutoffTemperatureData = 0.0;
		
		file = "";
	}

}