import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

import org.apache.commons.io.FilenameUtils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;


public class ProcessingEngine extends SwingWorker<Integer, Result> {

	private MainWindow uiWindow;
	private String imageFile;
	private ExecutorService executorService;
	private BufferedImage meterImage;
	private boolean bStop = false;
	
	public ProcessingEngine(MainWindow window) {
	
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		uiWindow = window;	
		executorService = Executors.newSingleThreadExecutor();
	}
	
	public void processVideo(String file) {
		
		imageFile = file;
		execute();
	}
	
	public void stopProcessing() {
		bStop = true;
	}
	
	@Override
	protected Integer doInBackground() throws Exception {
		int count = 0;
		ImageIcon icon;
	    Mat src = new Mat();
	    FileWriter fw = null;
	    int r = 0;
	    int g =0;
	    int b = 0;
	    double lastTemperature = 0;
	    double startTemperature = 0;
	    double rgbCount =0;
	    VideoCapture capture = null;
	    
        try {
        		capture = new VideoCapture();
				//capture.open(0);
				capture.open(imageFile);
				
				if(!capture.isOpened()) {
				    System.out.println("could not load video data...");
				    return -1;
				}
				
				int frames_total = (int)capture.get(7);
				//int frame_width = (int)capture.get(3);
				//int frame_height = (int)capture.get(4);
				
				String fileName= FilenameUtils.getBaseName(imageFile) + ".csv";
				String folder = Paths.get(imageFile).getParent().toString() + "/results";
				File file = new File(folder);
				file.mkdir();
				
				String resultFile = Paths.get(folder, fileName).toString();
				fw = new FileWriter(resultFile);
				fw.write(String.format("%s, %s, %s, %s, %s\n", "FrameCount", "Temperature", "R", "G", "B"));
				fw.flush();
	
				//last one second maybe throw away
				while(!bStop && capture.read(src)) {
	
				    try {
							// detect and cut to get the thermal label image so can analyze the color change
							Rect region = new Rect();
							Result result = markBlobsEx(src, region);

							result.file = imageFile;
							result.frameCount = count;
							result.totalFrames = frames_total;

							// not found the label, go to get the next image in the video stream
							if(region.width ==0 || region.height == 0)
							{
								continue;
							}

							// get average of R, G, B value of the thermal label
							double[] average = getPixelValues(src, region);
							
							count += 1;
							
							if(lastTemperature == 0 && result.endTemperature == 1000) {
								//bad start
								count = 0;
								continue;
							}
							
							//check temperature reading is good
							if(lastTemperature != 0 && result.endTemperature > lastTemperature) {
								result.endTemperature = lastTemperature;
							}
							
							lastTemperature  = result.endTemperature;
							
							fw.write(String.format("%d, %.1f, %d, %d, %d\n", count, result.endTemperature, (int)Math.round(average[2]), (int)Math.round(average[1]), (int)Math.round(average[0])));
							fw.flush();
							
							if(count ==1) {
								
								startTemperature = result.endTemperature;
							}
							
							r += average[2];
							g += average[1];
							b += average[0];
							rgbCount++;
							
							//update the UI every 1 second (30 frames)
							if(count == 1 || count %30 == 0) {
								
								result.startTemperature = startTemperature;
								startTemperature = result.endTemperature;
								
								result.rData  = r/rgbCount;
								result.gData  = g/rgbCount;
								result.bData  = b/rgbCount;
								rgbCount = 0;
								r=b=g=0;

								// send result to process function for GUI update
								publish(result);

							}
						
					} catch (Throwable e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}//end of while
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        finally {
        	
        	if(capture != null) {
        		capture.release();
        	}
        	
        	if(fw != null) {
        		
        		fw.close();
        	}
        }
        
        //this should the detected cutoff temperature
        return 0;
	}

	@Override
	// called when data is published in doInBackground() function
	protected void process(List<Result> rsList) {

		// repaint the GUI
		uiWindow.updateUI(rsList);
		
		// TODO Auto-generated method stub
		//super.process(chunks);
	}

	@Override
	protected void done() {
		
		// TODO Auto-generated method stub
		try {
			
			int temperature  = get();

			// repaint the GUI
			uiWindow.completeProcessing();
			super.done();
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	//for the region, average R G B color respectively and return
	private double[] getPixelValues(Mat src, Rect region) {
	
	//get average r, g, b from the region
	double r = 0;
	double b = 0;
	double g = 0;
	double count = 0;
	for (int i = 0; i < region.height; i++) {
		for (int j = 0; j < region.width; j++) {
			
			//pixel[0] is B, pixel[1] is G, pixel[2] is R
			double[] pixel = src.get(region.y + i, region.x + j); 
			//String info = String.format("[%d, %d]: R = %f, G= %f, B=%f", region.y + i, region.x + j, pixel[2], pixel[1], pixel[0]);
			//System.out.println(info);
			if(pixel != null) {
				r += pixel[2];
				g += pixel[1];
				b += pixel[0];
				count++;
			}
		}
	}
	
	double[] average = new double[3];
	average[0] = b/count;
	average[1] = g/count;
	average[2] = r/count;
	
	return average;
}

	//read meter's display (4 digits integer, including 1 decimal)
	private int readMeterEx(Mat src) {

		Mat meter1 = src.clone();

		//debug
		//Imgcodecs.imwrite("/Users/jessie/Documents/meter_org.jpg", src);

		// src is the meter in RGB
		int number;
		Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));

		// get gray scale image from the src (color)
		Mat grayMat = src.clone();
		Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY);
		
		//remove image noises using 3X3 Gaussian kernel
		Imgproc.GaussianBlur(grayMat, grayMat, new Size(3, 3), 0);

		//convert grayscale image to binary image and invert (object is black and background is white)
		Mat binaryMat = new Mat(grayMat.height(),grayMat.width(),CvType.CV_8UC1);
		Imgproc.threshold(grayMat, binaryMat, 20, 255, Imgproc.THRESH_BINARY_INV);
		//Core.bitwise_not(binaryMat, binaryMat);

		//debug
		//Imgcodecs.imwrite("/Users/jessie/Documents/meter_binary.jpg", binaryMat);

		//white background for gaps
		//Imgproc.floodFill(binaryMat, new Mat(), new Point(0, 0), new Scalar(255));

		//debug
		//Imgcodecs.imwrite("/Users/jessie/Documents/meter_floodfill.jpg", binaryMat);

		// debug
		//Imgproc.cvtColor(binaryMat, meter1, Imgproc.COLOR_GRAY2BGRA);
		//Imgcodecs.imwrite("/Users/jessie/Documents/meter.jpg", meter1);

		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		ArrayList<Point> allContours = new ArrayList<Point>();
		Imgproc.findContours(binaryMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		Rect firstRect = new Rect(binaryMat.width()-1, binaryMat.height()-1, 1,1);
		Rect secondRect = new Rect(binaryMat.width()-1, binaryMat.height()-1, 1,1);
		
		for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
		{
			double contourArea = Imgproc.contourArea(contours.get(contourIdx));

			// reject this object if it's contourArea is close to the entire image
			if(contourArea < binaryMat.width()*binaryMat.height()* 0.9) {

				// save the contours if it is not too small. I will get the bounding box for all contours objects)
				if(contourArea < 100)
				{
					//debug: what are these small objects?
					//Imgproc.drawContours(binaryMat, contours, contourIdx, new Scalar(0, 0, 0), -1);
					//Imgproc.drawContours(src, contours, contourIdx, new Scalar(0, 255, 0), 1);
				}
				else
				{
					allContours.addAll(contours.get(contourIdx).toList());
				}
				
				//Imgproc.drawContours(src, contours, contourIdx, new Scalar(0, 255, 0));
				// get the bounding box of the object
				Rect rect = Imgproc.boundingRect(contours.get(contourIdx));

				// the vertical digit segment has the following shape: get the upper and lower segment of digit 1
				// the height of digit 1 at left will be used to crop the image
				if((float)rect.width/binaryMat.width()< 0.2 && (float)rect.height/rect.width >=2.0 && rect.height*rect.width >200) {

					// rect's x and y si the left top corner's coordinates of the rect, the (0,0) of the image
					// is at top-left corner
					int yc = (int)Math.round(rect.y + rect.height/2.0);
					
					if(yc < binaryMat.height()/2.0) {
						if (rect.x < firstRect.x) {
			            	firstRect = rect;
			            }
					}
					else {
						if (rect.x < secondRect.x) {
			            	secondRect = rect;
			            }
					}
				}

			}
		}//end of for
		
		//crop to get the digits part
		MatOfPoint sourceMat = new MatOfPoint();
		// get the big bounding box for all segments
		sourceMat.fromList(allContours);
		Rect crop = Imgproc.boundingRect(sourceMat);
		Mat cropMat = binaryMat.submat(crop); 

		// define the rect ofr cropping, a little twists of the size
		// height is the firstRect's height + secondRect's height + gap between them
		int height = secondRect.y < firstRect.y? firstRect.y + firstRect.height - secondRect.y + 1 : secondRect.y + secondRect.height - firstRect.y +1;
		int width = (int)Math.round(cropMat.width()*0.85);
		int top = Math.min(firstRect.y, secondRect.y);
		int shift = firstRect.x - crop.x - 3; 
		int left = crop.x + shift;
		width -= shift;

		// copy the digits part to empty image for digits recognition
		Mat meterMat = Mat.zeros(new Size(crop.width + 30, crop.height + 30), CvType.CV_8UC1);
		Mat from = binaryMat.submat(new Rect(left, top, width, height));
		Mat to = meterMat.submat(new Rect(15, 15, width, height));	
		from.copyTo(to);

		//debug
		//Mat meter2 = meterMat.clone();
		//Imgproc.cvtColor(meterMat, meter2, Imgproc.COLOR_GRAY2BGRA);
		//Imgcodecs.imwrite("/Users/jessie/Documents/meter_crop.jpg", meter2);

		// get segments of the digits
		List<Rect> list = ConnectBoxes(meterMat, crop.height*crop.width);

		//recognize the digit number: 4 digits
		number = ORCIntegerEx(meterMat, list, height);

		return number;
	}
	
	private Rect merge(Rect r1, Rect r2) {
		int left1 = r1.x;
		int left2 = r2.x;
		int top1 = r1.y;
		int top2 = r2.y;
		int right1 = r1.x + r1.width -1;
		int right2 = r2.x + r2.width -1;
		int bottom1 = r1.y + r1.height -1;
		int bottom2 = r2.y + r2.height -1;
		
		int left  = left1<left2 ? left1:left2;
		int top  = top1<top2 ? top1:top2;
		int right = right1 > right2? right1:right2;
		int bottom = bottom1>bottom2? bottom1:bottom2;
		
		return new Rect(left, top, right-left+1, bottom-top +1);
		
	}
	
	private List<Rect> ConnectBoxes(Mat meterMat, int area) {
		List<Rect> list = new ArrayList<Rect>();
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();

		//debug
		//Mat meter3 = meterMat.clone();
		//Imgproc.cvtColor(meterMat, meter3, Imgproc.COLOR_GRAY2BGRA);

		// find all segments of the digits, the segments are not too small
		Imgproc.findContours(meterMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
		{
			double contourArea = Imgproc.contourArea(contours.get(contourIdx));
			Rect rect = Imgproc.boundingRect(contours.get(contourIdx));
			
			if(contourArea > area*0.002) {
				list.add(rect);

				//debug
				//Imgproc.rectangle(meter3, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 1);

			}
		}//end of for

		//debug
		//Imgcodecs.imwrite("/Users/jessie/Documents/meter_segments.jpg", meter3);

		/*
		https://www.geeksforgeeks.org/listiterator-in-java/
		There is no current element in ListIterator. Its cursor always lies between the previous and next elements.
		The previous() will return to the previous elements and the next() will return to the next element.
		Therefore, for a list of n length, there are n+1 possible cursors.
		*/
		//connect segments that belongs to a digit
		ListIterator<Rect> iter = list.listIterator();
		while(iter.hasNext()){
		    Rect r1 = iter.next();
		    int idx = iter.nextIndex(); 
		    int toBeMerged = -1;
		    
		    for(int i = idx; i< list.size(); i++)
		    {
		    	Rect r2 = list.get(i);
		    	
		    	if(r1.x < r2.x+r2.width-1 && r2.x < r1.x + r1.width-1)
		    	{
		    		toBeMerged = i;
		    		break;
		    	}
		    }
		    
		    if(toBeMerged != -1) {
		    	Rect merge = merge(r1, list.get(toBeMerged));
		    	list.set(toBeMerged,merge);
		    	iter.remove(); // remove this element (r1)
		    }
		}

		//debug
		//for (int i = 0; i < list.size(); i++) {
			//Rect rect = list.get(i);
			//Imgproc.rectangle(meter3, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 1);
		//}
		//Imgcodecs.imwrite("/Users/jessie/Documents/meter_segments_group.jpg", meter3);

		// the list contains boxes for every digit
		return list;
	}

	// determine the digit numeric value
	private int ORCIntegerEx(Mat meterMat, List<Rect> list, int height) {
		String text = "";
		List<Digits> digits = new ArrayList<Digits>();
		
		for (int i = 0; i < list.size(); i++)
		{
			Rect rect = list.get(i);

			int hits = 0x00;
			int digit = -1;
			
			double l = rect.x + 5;
			double r = rect.x + rect.width -5;
			double t = rect.y + 5;
			double b = rect.y + rect.height -5;
			double h = rect.height;
			double w = rect.width;

			// these are test positions for the segments to determine the number
			/*
			       p2
			   p0      p4
			       p6
			   p1      p3
			   	   p5
			*/
			// p0 and p4: upper left and right vertical segments
			Point p0 = new Point(l, t + h/4.0 -5);
			Point p4 = new Point(r, t + h/4.0 -5);

			// p1 and p3: lower left and right vertical segments
			Point p1 = new Point(l, t + 3*h/4.0 -5);
			Point p3 = new Point(r, t + 3*h/4.0 -5);

			// p2 and p5 upper and lower  horizontal segments
			Point p2 = new Point(l + w/2.0-5, b);
			Point p5 = new Point(l + w/2.0-5, t);
			
			//the middle segment
			Point p6 = new Point(l + w/2.0 - 5, t + h/2.0 - 5);
			
			//too small
			if(rect.height < height*0.8) {
				continue;
			}
			
			if((float)rect.height/rect.width > 4.3) {
				
				//this is one
				digit = 1;
			}
			else
			{
				//row-col: y -x
				if(meterMat.get((int)p0.y, (int)p0.x)[0] == 255) {
					hits |= 0x0000001;
				}
				
				if(meterMat.get((int)p1.y, (int)p1.x)[0] == 255) {
					hits |= 0x0000010;
				}
				
				if(meterMat.get((int)p2.y, (int)p2.x)[0] == 255) {
					hits |= 0x0000100;
				}
				
				if(meterMat.get((int)p3.y, (int)p3.x)[0] == 255) {
					hits |= 0x0001000;
				}
				
				if(meterMat.get((int)p4.y, (int)p4.x)[0] == 255) {
					hits |= 0x0010000;
				}
				
				if(meterMat.get((int)p5.y, (int)p5.x)[0] == 255) {
					hits |= 0x0100000;
				}
				
				if(meterMat.get((int)p6.y, (int)p6.x)[0] == 255) {
					hits |= 0x1000000;
				}
				
				switch(hits)
				{
					case 0x1110110:
						digit =2;
						break;
						
					case 0x1111100:
						digit =3;
						break;	
						
					case 0x1011001:
						digit =4;
						break;
						
					case 0x1101101:
						digit =5;
						break;
						
					case 0x1101111:
						digit =6;
						break;
						
					case 0x0111001:
						digit =7;
						break;
						
					case 0x1111111:
						digit =8;
						break;
						
					case 0x1111101:
						digit =9;
						break;
						
					case 0x0111111:
						digit =0;
						break;	
				}
			}//end of else
			
			//System.out.println(digit);
			//text = text.concat(String.format("%d", digit));
			if(digit >=0)
			{
				Digits d = new Digits();
				d.position = rect.x;
				d.digit = digit;
				digits.add(d);
			}
		}//end of for

		// sort digits based on x position
		Collections.sort(digits);

		// the numeric value of the digits
		int sum = 0;
		for(Digits d : digits) {
			sum = sum*10 + d.digit;
		}

		// return the value - we are all set!
		return sum;
		
	}
	
	private Result markBlobsEx(Mat src, Rect region) {
		
		Result result = null;
		double reading = 0;
		
	    try {	
	    		Mat grayMat = src.clone();
				Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY);
				Mat binaryMat = new Mat(grayMat.height(),grayMat.width(),CvType.CV_8UC1);
				Imgproc.threshold(grayMat, binaryMat, 20, 255, Imgproc.THRESH_BINARY);
				
				//remove noises - keep this
				//Imgproc.GaussianBlur(grayMat, grayMat, new Size(3, 3), 0);
				
				Core.bitwise_not(binaryMat, binaryMat);
				
				Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 15));
				Imgproc.dilate(binaryMat,binaryMat,element);

				//debug
				//Imgcodecs.imwrite("/Users/jessie/Documents/binary.jpg", binaryMat);

				Mat cannyEdges = new Mat();
				Imgproc.Canny(binaryMat, cannyEdges, 10, 100);

				//dilate to close edges
				Mat closeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
				Imgproc.dilate(cannyEdges, cannyEdges, closeElement, new Point(-1, -1), 1);

				//debug
				//Mat edgeDrawing = cannyEdges.clone();
				//Imgproc.cvtColor(cannyEdges, edgeDrawing, Imgproc.COLOR_GRAY2BGRA);
				//Imgcodecs.imwrite("/Users/jessie/Documents/edge.jpg", edgeDrawing);

				//get contours
				List<MatOfPoint> contours = new ArrayList<>();
				Mat hierarchy = new Mat();
				// use RETR_EXTERNAL so inner contour is not counted
				Imgproc.findContours(cannyEdges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

				//debug
				//Mat objectDrawing = src.clone();
				//Mat edge = new Mat( cannyEdges.rows(), cannyEdges.cols(), CvType.CV_8UC3);

				for (int idx = 0; idx < contours.size(); idx++)
				{
					 double contourArea = Imgproc.contourArea(contours.get(idx));
					 Rect rect = Imgproc.boundingRect(contours.get(idx));
					 double extent = contourArea/(rect.width*rect.height);

					 // an object that is not too big or small and filling factor is high
					 // will be the meter or the label. label is round and meter is rectangle
					 if(contourArea > 6000 && contourArea < 500000 && extent > 0.7) {

						 //debug
						 //Imgproc.rectangle(objectDrawing, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 1);

						 //debug
						 //Imgproc.drawContours(edge, contours, idx, new Scalar(0, 0, 255), 2);

						 MatOfPoint2f contourPoints = new MatOfPoint2f(contours.get(idx).toArray());
						 double contourlength = Imgproc.arcLength(contourPoints, true);
						 double epsilon = 0.04*contourlength;
					     MatOfPoint2f approx = new MatOfPoint2f();
					     Imgproc.approxPolyDP(contourPoints,approx,epsilon,true);

						 // rectangle has 4 vertex
					     long numOfVetex = approx.total();
					     if( numOfVetex == 4 && contourArea > 20000) {
					        
					    	//this is the meter
							 Rect crop = new Rect(rect.x + 5, rect.y + 5, rect.width -5, rect.height-5);
							 Mat meterMat = new Mat(src, crop); 
							 reading = readMeterEx(meterMat);
							 Imgproc.drawContours(src, contours, idx, new Scalar(0, 0, 255), 2);
					     }
					     else {
					    	
					    	 //this is the thermal label
							 Imgproc.drawContours(src, contours, idx, new Scalar(0, 255, 0), 2);
							 int xc = (int)Math.round(rect.x + rect.width/2.0);
							 int yc = (int)Math.round(rect.y + rect.height/2.0);
							 region.x = (int)Math.round(xc - rect.width/6.0);
							 region.y = (int)Math.round(yc - rect.height/6.0);
							 region.width = (int)Math.round(rect.width/3.0);
							 region.height = (int)Math.round(rect.height/3.0);

					     } 
					 }
				}

				//debug
				//Imgcodecs.imwrite("/Users/jessie/Documents/objects_drawing.jpg", objectDrawing);
			    //Imgcodecs.imwrite("/Users/jessie/Documents/objects_contour.jpg", edge);

			    result = new Result();
			    result.icon = makeImageIcon(src);

			    if(reading >=1000 && reading <= 1999)
			    {
					// divide 10 since we ignored decimal
			    	result.endTemperature = reading/10.0;
			    }
			    else
			    {
			    	result.endTemperature = 1000.0;
			    }
				
			    return result;
	
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    return result;
	
	}

	private ImageIcon makeImageIcon(Mat matrix) {
		
		BufferedImage image  = makeBufferedImage(matrix);
		
		if(image != null) {
			
			 return new ImageIcon(image);
		}
		else {
			
			return null;
		}
    }
 
	private BufferedImage makeBufferedImage(Mat matrix) {
	
		if (matrix != null) {
			
			int type = BufferedImage.TYPE_BYTE_GRAY;
	        
	        if (matrix.channels() > 1) {
	            type = BufferedImage.TYPE_3BYTE_BGR;
	        }
	        
	        int bufferSize = matrix.channels() * matrix.cols() * matrix.rows();
	        
	        byte[] buffer = new byte[bufferSize];
	        matrix.get(0, 0, buffer); //put all pixel to buffer
	        
	        //copy all pixels to buffered image
	        BufferedImage image = new BufferedImage(matrix.cols(), matrix.rows(), type);
	        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
	        
	        return image;
		}
		else {
			
			return null;
		}
    
	}

}
