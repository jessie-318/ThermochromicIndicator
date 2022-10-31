import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;

import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.Font;
import java.awt.Image;

import javax.swing.border.BevelBorder;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.awt.event.ActionEvent;

public class MainWindow extends JFrame {

	private JPanel contentPane;
	private JSplitPane splitPane;
	private JSplitPane chartSplitPane;
	private JScrollPane scrollPaneRGB;
	private JScrollPane scrollPaneTemp;
	private JScrollPane imageScrollPane;
	private JMenuBar menuBar;
	private JMenu mnNewMenu;
	private JMenuItem mntmNewMenuItem;
	private JMenuItem mntmNewMenuItem_1;
	private JMenuItem mntmNewMenuItem_2;
	private JLabel statusBar;
	private JLabel imageViewer;
	private JFileChooser fileChooser;
	private int IMAGE_WIDTH =550; //750;
	private int IMAGE_HEIGHT = 700;//1000;
	private int CHART_WIDTH =1000;
	private int RGBCHART_HEIGHT = 400; //540;
	private int RESULTCHART_HEIGHT = 275; //360;
	private XYChart rgbChart;
	private XYChart resultChart;
	private XChartPanel<XYChart> rgbChartPanel;
	private XChartPanel<XYChart> resultChartPanel;
	private List<Double> timeData;
	private List<Double> rData;
	private List<Double> gData;
	private List<Double> bData;
	private List<Double> temperatureData;
    private Preferences prefs;
    private ProcessingEngine engine;
    private List<String> series;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow frame = new MainWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private void setup() {
		
		timeData = new ArrayList<Double>();
		series = new ArrayList<String>();
	
		rData = new ArrayList<Double>();
		gData = new ArrayList<Double>();
		bData = new ArrayList<Double>();
		temperatureData = new ArrayList<Double>();
		
		//so it remembers the last directory it opens
		prefs = Preferences.userRoot().node(getClass().getName());
		fileChooser = new JFileChooser(prefs.get("LAST_USED_FOLDER", new File(".").getAbsolutePath()));

		double[] xData = new double[] {0};
	    double[] yData = new double[] {0};
	    
		rgbChart = new XYChartBuilder().width(CHART_WIDTH).height(RGBCHART_HEIGHT).title("RGB vs. Temperature Chart").build();
		rgbChart.setXAxisTitle("Temperature (F)");
		rgbChart.setYAxisTitle("RGB");
		rgbChart.getStyler().setYAxisMin(0.0);
		rgbChart.getStyler().setYAxisMax(255.0);
		rgbChart.getStyler().setXAxisMin(100.0);
		rgbChart.getStyler().setXAxisMax(225.0);
		rgbChart.getStyler().setLegendPosition(LegendPosition.InsideNE);
	    
		XYSeries seriesR = rgbChart.addSeries("R", xData, yData);
	    seriesR.setMarker(SeriesMarkers.NONE); //or SeriesMarkers.CIRCLE
	    seriesR.setMarkerColor(Color.red);
	    seriesR.setLineColor(Color.red);//fill color
	   
	    XYSeries seriesG = rgbChart.addSeries("G", xData, yData); 
	    seriesG.setMarker(SeriesMarkers.NONE); 
	    seriesG.setMarkerColor(Color.green);
	    seriesG.setLineColor(Color.green);
	    
	    XYSeries seriesB = rgbChart.addSeries("B", xData, yData); 
	    seriesB.setMarker(SeriesMarkers.NONE); 
	    seriesB.setMarkerColor(Color.blue);
	    seriesB.setLineColor(Color.blue);
	  
//	    XYSeries seriesT = rgbChart.addSeries("Temperature", xData, yData); 
//	    seriesT.setMarker(SeriesMarkers.NONE); 
//	    seriesT.setMarkerColor(Color.orange);
//	    seriesT.setLineColor(Color.orange);
	    
	    rgbChartPanel = new XChartPanel<XYChart>(rgbChart);
		scrollPaneRGB.setViewportView(rgbChartPanel);

		resultChart = new XYChartBuilder().width(CHART_WIDTH).height(RESULTCHART_HEIGHT).title("Color vs. Temperature Chart").build();
		resultChart.setXAxisTitle("Temperature (F)");
		resultChart.getStyler().setAntiAlias(false); //not to show vertical white lines
		//resultChart.getStyler().setYAxisTicksVisible(false);
		resultChart.setYAxisTitle("Value");
		resultChart.getStyler().setYAxisMin(0.0);
		resultChart.getStyler().setYAxisMax(100.0);
		resultChart.getStyler().setXAxisMin(100.0);
		resultChart.getStyler().setXAxisMax(225.0);
		resultChart.getStyler().setLegendVisible(false);
//	    
//		for(int i=100; i<255; i++) {
//			double[] color = new double[] {85, 85};
//			double[] temperature = new double[2];
//			temperature[0] = i+ 0.5;
//			temperature[1] = i +1.5;
//			XYSeries s = resultChart.addSeries(Integer.toString(i), temperature, color);
//		    s.setFillColor(new Color(i,0,i));
//		    s.setLineColor(new Color(i,0,i));
//		    s.setMarker(SeriesMarkers.NONE);
//		    s.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);	
//		}
//		
	    resultChartPanel = new XChartPanel<XYChart>(resultChart);
	    scrollPaneTemp.setViewportView(resultChartPanel);
	}
	
	/**
	 * constructor: Create the frame.
	 */
	public MainWindow() {
		
		setTitle("Video Processor");
	
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
		setBounds(100, 100, 1024, 768);
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		mnNewMenu = new JMenu("File");
		mnNewMenu.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		menuBar.add(mnNewMenu);
		
		mntmNewMenuItem = new JMenuItem("Process Video");
		mntmNewMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int option = fileChooser.showOpenDialog(MainWindow.this);
				
				if(option == JFileChooser.APPROVE_OPTION) {
					
					prefs.put("LAST_USED_FOLDER", fileChooser.getSelectedFile().getParent());
					//clear data
					timeData.clear();
					rData.clear();
					gData.clear();
					bData.clear();
					temperatureData.clear();
					for(String s : series) {
						
						resultChart.removeSeries(s);
					}
					series.clear();
					
					File file = fileChooser.getSelectedFile();
					engine  = new ProcessingEngine(MainWindow.this);
					engine.processVideo(file.getAbsolutePath());
				}
			}
		});
		//mntmNewMenuItem.setIcon(new ImageIcon(MainWindow.class.getResource("/sun/print/resources/oneside.png")));
		mntmNewMenuItem.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		mnNewMenu.add(mntmNewMenuItem);
		
		mntmNewMenuItem_1 = new JMenuItem("Process Batch");
		mntmNewMenuItem_1.setEnabled(false);
		//mntmNewMenuItem_1.setIcon(new ImageIcon(MainWindow.class.getResource("/sun/print/resources/duplex.png")));
		mntmNewMenuItem_1.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		mnNewMenu.add(mntmNewMenuItem_1);
		
		mntmNewMenuItem_2 = new JMenuItem("Stop");
		mntmNewMenuItem_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(engine!= null) {
					engine.stopProcessing();
				}
			}
		});
		//mntmNewMenuItem_2.setIcon(new ImageIcon(MainWindow.class.getResource("/javax/swing/plaf/metal/icons/Error.gif")));
		mntmNewMenuItem_2.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		mnNewMenu.add(mntmNewMenuItem_2);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		splitPane = new JSplitPane();
		splitPane.setDividerSize(3);
		splitPane.setResizeWeight(0.28);//actual size depend on the child size (imageScrollPane.setPreferredSize)
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		imageScrollPane = new JScrollPane();
		imageScrollPane.setPreferredSize(new Dimension(IMAGE_WIDTH, IMAGE_HEIGHT));
		splitPane.setLeftComponent(imageScrollPane);
		
		imageViewer = new JLabel();
		imageViewer.setHorizontalAlignment(SwingConstants.CENTER);
		imageScrollPane.setViewportView(imageViewer);
		
		chartSplitPane = new JSplitPane();
		chartSplitPane.setDividerSize(3);
		chartSplitPane.setResizeWeight(0.6);
		chartSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane.setRightComponent(chartSplitPane);
		
		scrollPaneRGB = new JScrollPane();
		scrollPaneRGB.setPreferredSize(new Dimension(CHART_WIDTH, RGBCHART_HEIGHT));
		chartSplitPane.setTopComponent(scrollPaneRGB);
		
		scrollPaneTemp = new JScrollPane();
		scrollPaneTemp.setPreferredSize(new Dimension(CHART_WIDTH, RESULTCHART_HEIGHT));
		chartSplitPane.setBottomComponent(scrollPaneTemp);
		
		statusBar = new JLabel("Ready");
		statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		statusBar.setFont(new Font("Tahoma", Font.PLAIN, 16));
		contentPane.add(statusBar, BorderLayout.SOUTH);
		
		setup();
	}

	public void updateUI(List<Result> rsList) {
		
		for(Result result: rsList) {
			
			if(result.icon != null) {
				
				Image img = result.icon.getImage();
			
				int height  = result.icon.getIconHeight();
				int width = result.icon.getIconWidth();
				double ratio = ((double)height)/width;
			
				img = img.getScaledInstance(IMAGE_WIDTH, (int)Math.round(IMAGE_WIDTH*ratio), Image.SCALE_DEFAULT);
				result.icon.setImage(img);
			
				imageViewer.setIcon(result.icon);

				rData.add(result.rData);
				gData.add(result.gData);
				bData.add(result.bData);
				
				temperatureData.add(result.endTemperature);
				
				rgbChart.updateXYSeries("R", temperatureData, rData, null);
				rgbChart.updateXYSeries("G", temperatureData, gData, null);
				rgbChart.updateXYSeries("B", temperatureData, bData, null);
				
				//lower graph
				double[] color = new double[] {100, 100};
				double[] temperature = new double[2];
				if(result.startTemperature < result.endTemperature) {
					temperature[0] = result.startTemperature;
					temperature[1] = result.endTemperature;
				}
				else{
					temperature[1] = result.startTemperature;
					temperature[0] = result.endTemperature;
				}
				XYSeries s = resultChart.addSeries(Integer.toString(result.frameCount), temperature, color);
				series.add(Integer.toString(result.frameCount));
			    s.setFillColor(new Color((int)Math.round(result.rData), (int)Math.round(result.gData), (int)Math.round(result.bData)));
			    s.setLineColor(new Color((int)Math.round(result.rData), (int)Math.round(result.gData), (int)Math.round(result.bData)));
			    s.setMarker(SeriesMarkers.NONE);
			    s.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);	
			    
			    //status bar
			    statusBar.setText(String.format("processing \"%s\" [%d/%d]", result.file, result.frameCount, result.totalFrames));
			    
				rgbChartPanel.repaint();
				resultChartPanel.repaint();
			}
		}
	}
	
	public void completeProcessing() {
	
		statusBar.setText("Ready");
	}
}
