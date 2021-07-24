package bs7nn_cars;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Holding all information (and helper methods) for the street and generates the image which will be 
 * shown by the JPanel
 */
public class Street {
	/** drawing radius that will produce the street */
	public static final int RADIUS = 50;
	
	/** squared radius for avoiding too many calculations */
	public static final int RADIUSSQ = RADIUS*RADIUS;
	
	/** Color of the fied where the street is located */
	public static final int BACKGROUND_COLOR = Color.WHITE.getRGB();
	
	/** Color of the fied where the street is located */
	public static final Color STREET_COLOR = Color.BLACK;
	
	/** color of the start/stop line */
	public static final int LINE_COLOR = Color.RED.getRGB();
	
	/** The street will be drawn with circles, which have a normed distance from each other */
	private static final double SQUARED_ELEMENT_DISTANCE = (RADIUS * RADIUS / 16);
	
	/** last x position of the mouse */
	private int oldX = -1;
	
	/** last y position of the mouse */
	private int oldY = -1;
	
	/** As long as the user is able to manipulate the street, this value will be true */
	private boolean isEditable = true;

	/** Displayed image */
	private BufferedImage myImg = null;

	/** here all points the user draws with the mouse are selected. They will be reduced afterwards
	 * in order to have an optimized graph */
	public ArrayList<int[]> allPoints = new ArrayList<>();
	
	/** for caluclating the overal distance of a point on the street, the location must be identified by
	 * an iterative approach. This value stores the position of the last iteration */
	private int prelocation = -1;
	
	/** The start line is a line 90 degree to the street where all cars will start. The array holds
	 * the start and endpoints of that line {x0, y0, x1, y1}
	 */
	private int[] startLine = null;

	/** overall length of the street */
	private double streetLength = 0;

	
	/**
	 * Constructor is only used for allowing an empty constructor call
	 */
	public Street() {
	}
	
	/**
	 * Constructor for creating the street from the file system.
	 * @param fileName Complete path
	 * @param dim Dimmension of the image to create
	 */
	public Street(String fileName, Dimension dim) {
		myImg = getImage(dim);
		readFromFile(fileName);
		finalizeStreet();
	}
	
	/**
	 * Getter, if the street still can be manipulated
	 * @return true, if the street can be manipulated
	 */
	public boolean isEditable() {
		return isEditable;
	}

	/**
	 * If a new street should be drawn
	 */
	public void resetStreet() {
		startLine = null;
		allPoints.clear();
		oldX = -1;
		oldY = -1;
		isEditable = true;
		clearImage();
	}
	
	/**
	 * Call this if the street drawing process is finished. The graph will be optimized and the
	 * street will be set to not editable
	 */
	public void finalizeStreet() {
		optimizePoints();
		isEditable = false;
	}
	
	/**
	 * Reads the point data from the file system. The file contais every street point 
	 * as a value pair of x/y in integer values
	 * @param fileName absolute path
	 */
	private void readFromFile(String fileName) {
		FileReader frd = null;
		BufferedReader brd = null;
		// here the point data will be stored
		ArrayList<int[]> fileData = new ArrayList<int[]>();
		try {
			frd = new FileReader(fileName);
			brd = new BufferedReader(frd);
			String sLine = null;
			
			while((sLine = brd.readLine()) != null) {
				if (sLine.length() > 0) {
					// data will be held in colon separated file
					String[] lineColumns = sLine.split(";");
					if (lineColumns.length == 2) {
						fileData.add(new int[] {Integer.parseInt(lineColumns[0]), Integer.parseInt(lineColumns[1])});
					} else {
						System.out.println("Error while reading");
					}
				} 
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		finally {
			if (frd != null) {
				try {
					frd.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		allPoints = fileData;
	}
	
	/**
	 * Writes the current street data (the points) to the file system in integer x/y values
	 * @param filePath Absolute path
	 * @return true if succeeded
	 */
	public boolean writeToFile(String filePath) {
		boolean success = true;
		FileWriter myWriter = null;
		try {
			myWriter = new FileWriter(filePath, false);
			
			for (int[] point : allPoints) {
				String line = point[0] + ";" + point[1] + "\n";
				myWriter.append(line);
			}
			myWriter.flush();
		} catch (IOException e) {
			success = false;
		}
		
		finally {
			if (myWriter != null) {
				try {
					myWriter.close();
				} catch (IOException e) {
					success = false;
				}
			}
		}
		return success;
	}
	
	/**
	 * Reduces the points to the ELEMENT_DISTANCE (and fills it up if the original distances are to small)
	 */
	public void optimizePoints() {
		// helper for getting one point and hand it over to the method that fills up the points
		int[] point = allPoints.get(0); //start with the first point
		fillPoints(point);
		
		// now search for the next point and remove all that are closer than ELEMENT_DISTANCE
		for (int i = 0; i < allPoints.size() - 2; i++) {
			if (getSquaredDistance(allPoints.get(i + 0), allPoints.get(i + 1)) < SQUARED_ELEMENT_DISTANCE &&
				getSquaredDistance(allPoints.get(i + 1), allPoints.get(i + 2)) < SQUARED_ELEMENT_DISTANCE) {
				allPoints.remove(i + 1);
				i--;
			}
		}
		
		// now calculate the street length
		streetLength = 0;
		for (int i = 1; i < allPoints.size(); i++) {
			streetLength = getStreetLength() + Math.sqrt(getSquaredDistance(allPoints.get(i - 1), allPoints.get(i)));
		}
		// update the image to the reduced point list
		reduceImage();
	}
	
	
	/**
	 * Mainly for the closing of the loop there might be too much space between the last and first point.
	 * This method moves through all points and fills them up if the distances are too big. The method is called
	 * in two situations. Either in the optimization process, which means that the idea is to close the loop. 
	 * Therefore the starting point is the first point in the list. The other situation is if a new point is
	 * added. Therefore the starting point is the last point in the list.
	 * @param point Starting point of the analysis
	 */
	public void fillPoints(int[] point) {
		if (allPoints.size() > 0) {
			// the distance is measured between this point and the predecessor, which will be 
			// the last point of the list
			int[] predecessor = allPoints.get(allPoints.size() - 1);
			if (getSquaredDistance(point, predecessor) > SQUARED_ELEMENT_DISTANCE ) {
				// now get the delta of the x and y values for every fill step
				
				// the new positions should be double in order to avoid the cut of using values < 1
				double fillPosX = predecessor[0];
				double fillPosY = predecessor[1];
				
				// this is the number of steps that must be filled
				double numberOfSteps = Math.sqrt(getSquaredDistance(point, predecessor) / SQUARED_ELEMENT_DISTANCE);
				
				// The delta is determined by the overall distance in x and y direction divided by the number of steps
				double deltaX = ((point[0] - predecessor[0]) / numberOfSteps);
				double deltaY = ((point[1] - predecessor[1]) / numberOfSteps);
				
				// fill the list with new points of the delta distance
				do {
					fillPosX += deltaX;
					fillPosY += deltaY;
					
					// the new predecessor will now be closer to the reference point
					predecessor = new int[] {(int)fillPosX, (int)fillPosY};
					allPoints.add(predecessor);
					
					// stop if the predecessor is close enough to the reference point (or allPoints explodes -> just in case...)
				} while (getSquaredDistance(point, predecessor) > SQUARED_ELEMENT_DISTANCE || allPoints.size() > 10000000);
			}
		}
	}
	
	/**
	 * Adds a new point to the list
	 * @param e Mouse event that holds the x and y coordinate
	 */
	public void addPoint(MouseEvent e) {
		// add only a point if the street is editable
		if (!isEditable) {
			return;
		}
		
		// now generate the data format for adding
		int[] point = new int[] {e.getX(), e.getY()};
		
		// check, if the distance to the predecessor is too big and if yes, fill the distance with points
		fillPoints(point);
		
		// now place the new point to the end of the list
		allPoints.add(point);
		
		// only if the new point is not the first, the line can be drawn
		if (oldX != -1) {
			Graphics2D g = myImg.createGraphics();
			g.setColor(Color.black);
			
			// set the stroke quite big in order to match a reasonable size 
			g.setStroke(new BasicStroke(2.0f * Street.RADIUS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
			g.drawLine(oldX, oldY, e.getX(), e.getY());
		}
		
		// remember this point as "the old"
		oldX = e.getX();
		oldY = e.getY();
	}

	/**
	 * Checks, if the start line was created
	 * @return true if start line data is present
	 */
	public boolean hasStartLine() {
		return startLine != null;
	}
	
	/**
	 * Getter of the x start point of the start line
	 * @return x start point of the start line
	 */
	public int getStartX0() {
		return startLine[0];
	}
	
	/**
	 * Getter of the y start point of the start line
	 * @return y start point of the start line
	 */
	public int getStartY0() {
		return startLine[1];
	}
	
	/**
	 * Getter of the x end point of the start line
	 * @return x end point of the start line
	 */
	public int getStartX1() {
		return startLine[2];
	}
	
	/**
	 * Getter of the y end point of the start line
	 * @return y end point of the start line
	 */
	public int getStartY1() {
		return startLine[3];
	}
	
	/**
	 * Getter of the point list
	 * @return all points
	 */
	public ArrayList<int[]> getAllPoints() {
		return allPoints;
	}
	
	/**
	 * Helper for calculate the squared distance of two points
	 * @param point1 x/y value of point 1
	 * @param point2 x/y value of point 2
	 * @return squared distance of the two points
	 */
	private static double getSquaredDistance(int[] point1, int[] point2) {
		return Math.pow(point1[0] - point2[0], 2) + Math.pow(point1[1] - point2[1], 2);
	}
	
	/**
	 * Helper for calculate the squared distance of two points
	 * @param point1 x/y value of point 1
	 * @param point2 x/y value of point 2
	 * @return squared distance of the two points
	 */
	private double getSquaredDistance(double x0, double y0, int[] point) {
		return Math.pow(x0 - point[0], 2) + Math.pow(y0 - point[1], 2);
	}

	/**
	 * Setter of the start line based on the point data
	 */
	public void setStartLine() {
		// we need at least two points
		if (allPoints.size() > 1) {
			double correction = 0.1;//2*RADIUS / laenge;
			double dx = (allPoints.get(1)[1] - allPoints.get(0)[1]) * correction;
			double dy = (allPoints.get(1)[0] - allPoints.get(0)[0]) * correction;
			double x0 = allPoints.get(0)[0] + dx;
			double y0 = allPoints.get(0)[1] + dy;
			double x1 = allPoints.get(0)[0] - dx;
			double y1 = allPoints.get(0)[1] - dy;
			
			while(pointIsOnStreet(x0, y0)) {
				x0 += dx;
				y0 += dy;
			}
			while(pointIsOnStreet(x1, y1)) {
				x1 -= dx;
				y1 -= dy;
			}
			
			startLine = new int[] {(int)x0, (int)y0, (int)x1, (int)y1};
		}
	}

	/**
	 * Get the angle of the line between the first two points in order to align the car to that angle
	 * @return Angle the car should be aligned to
	 */
	public double getStartAngle() {
		int dx = allPoints.get(1)[0] - allPoints.get(0)[0];
		int dy = allPoints.get(1)[1] - allPoints.get(0)[1];
		return Math.atan2(dx, -dy);
	}
	
	/**
	 * Getter of the image. The dimension is for creating an empty image if 
	 * no image was created yet
	 * @param dim for initial image creation
	 * @return the image of the street to display
	 */
	public BufferedImage getImage(Dimension dim) {
		if (myImg == null) {
			generateImage(dim.width, dim.height);
		}
		return myImg;
	}

	/**
	 * Generates a plain image as a starting point
	 * @param width Width of the image to create
	 * @param height Height of the image to create
	 * @return Newly created image for display in the JPanel
	 */
	private void generateImage(int width, int height) {
		GraphicsConfiguration gfxConf = 
				GraphicsEnvironment.getLocalGraphicsEnvironment().
			    getDefaultScreenDevice().getDefaultConfiguration();
		myImg = gfxConf.createCompatibleImage(width, height);
		clearImage();
	}
	
	/**
	 * Deletes all data from the image
	 */
	private void clearImage() {
		Graphics2D g = myImg.createGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, myImg.getWidth(), myImg.getHeight());
		g.dispose();
		allPoints.clear();
	}
	
	/**
	 * Call this method if the point reduction (optimization) has finished in order to draw only
	 * the needed circles of the street
	 */
	private void reduceImage() {
		Graphics2D g = myImg.createGraphics();
		
		// first delete the content
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, myImg.getWidth(), myImg.getHeight());
		g.setColor(STREET_COLOR);
		
		// now draw the circles
		if (getAllPoints().size() != 0) {
			for (int[] point : getAllPoints()) {
				g.fillOval(point[0] - RADIUS, point[1] - RADIUS, 2*RADIUS, 2*RADIUS);
			}
		}
		
		// calculate and display the starting line
		setStartLine();
		g.setColor(Color.red);
		g.setStroke(new BasicStroke(4));
		g.drawLine(getStartX0(), getStartY0(), getStartX1(), getStartY1());
		
		g.dispose();
	}

	/**
	 * Check if the given x/y point is located on the street
	 * @param x x position of the point to check
	 * @param y y position of the point to check
	 * @return true, if the point is on the street
	 */
	public boolean pointIsOnStreet(double x, double y) {
		// error handling - if the image was not created yet - means there is no street
		if (myImg == null) {
			return false;
		}
		
		// now check, if the point is within the image borders
		if (x < 0 || y < 0 || x >= myImg.getWidth() || y >= myImg.getHeight()) {
			return false;
		}
		
		// now check, if the point is located on the street
		return myImg.getRGB((int)x, (int)y) != BACKGROUND_COLOR;
	}
	
	/**
	 * Check if the given x/y point is located on the starting line
	 * @param x x position of the point to check
	 * @param y y position of the point to check
	 * @return true, if the point is on the starting line
	 */
	public boolean pointIsOnStartLine(int x, int y) {
		// error handling - if the image was not created yet - means there is no street and therfore
		// no starting line
		if (myImg == null) {
			return false;
		}
		
		// now check, if the point is within the image borders
		if (x < 0 || y < 0 || x >= myImg.getWidth() || y >= myImg.getHeight()) {
			return false;
		}
		
		// now check, if the point is located on the starting line
		return myImg.getRGB(x, y) == LINE_COLOR;
	}

	/**
	 * Calculates the distance from the starting point + startSearch index of the given point
	 * @param x x value of the point
	 * @param y y value of the point
	 * @param startSearch Street point where to start the calculation
	 * @return distance
	 */
	public double getDistance(double x, double y, int startSearch) {
		// helper variables to avoid too many calculations
		double sizeLoc0 = 0;
		double sizeLoc1 = 0;
		double sizeLoc2 = 0;
		double distance = 0;
		
		// we need at least two points for the calculation
		if (allPoints.size() < startSearch + 3) {
			return -1;
		}
		
		// index of the prececessor
		prelocation = -1;
		
		// calculate only, if point is on the street
		if (pointIsOnStreet(x, y)) {
			// now calculate the distance between the point and the first two points on the street
			sizeLoc0 = getSquaredDistance(x, y, allPoints.get(startSearch));
			sizeLoc1 = getSquaredDistance(x, y, allPoints.get(startSearch + 1));
			sizeLoc2 = Double.MAX_VALUE;
			prelocation = 0;
			// now iterate through the street and check, if the distance increases. If that is the 
			// case we passed the two points where x/y is inbetween
			for (int i = startSearch + 2; i < allPoints.size(); i++) {
				sizeLoc2 = getSquaredDistance(x, y, allPoints.get(i));
				if (sizeLoc0 <= RADIUSSQ) {
					if (sizeLoc2 > sizeLoc0) {
						prelocation = i - 2;
						for (int j = 1; j < prelocation; j++) {
							distance += Math.sqrt(getSquaredDistance(allPoints.get(j-1), allPoints.get(j)));
						}
						return distance + Math.sqrt(sizeLoc0);
					}
				}
				sizeLoc0 = sizeLoc1;
				sizeLoc1 = sizeLoc2;
			}
		}
		
		// If the point is outside the street, an error value is returned
		return -allPoints.size();
	}
	
	/**
	 * Getter of the street length
	 * @return length of the street
	 */
	public double getStreetLength() {
		return streetLength;
	}
	
}
