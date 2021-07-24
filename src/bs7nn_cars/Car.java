package bs7nn_cars;

import java.awt.Polygon;
import java.io.FileWriter;
import java.io.IOException;


import bs7nn.InconsistentValueException;


/**
 * Class holds all necessary information of a single car
 */
public class Car implements Comparable<Car> {
	/** half the length of the car (to avoid unnecessary divisions) */
	public static final int HALF_LENGTH = 10;
	
	/** half the width of the car (to avoid unnecessary divisions) */
	public static final int HALF_WIDTH = 5;
	
	/** distance, the car looks ahead */
	public static final int MAX_SEEK_DIST = 200;
	
	/** maximal speed of the car */
	public static final double MAX_SPEED = 0.02;
	
	/** maximal steering angle of the car */
	public static final double MAX_STEER = 0.2;
	
	/** the cars are allowed to stay on the road for this time */
	public static final int MAX_TRAVEL_TIME = 200000;
	
	/** every car gets an id for logging reasons */
	private int carId = 0;
	
	/** x position of the car */
	public double x = 0;
	
	/** y position of the car */
	public double y = 0;
	
	/** angle of the car */
	public double a = 0;
	
	/** velocity of the car input[0] */
	public double v = 0;  
	
	/** steering angle of the car input[1] */
	public double s = 0;  
	
	/** cosinus of the car angle (for avoiding to calculate cos to often) */
	private double ca = 0;
	
	/** sinus of the car angle (for avoiding to calculate sin to often) */
	private double sa = 0;
	
	/** street distance (45° to the left) input[2] */
	private double dl = 0;  
	
	/** street distance (45° to the right) input[3] */
	private double dr = 0;  
	
	/** street distance (45° to the left) input[4] */
	private double df = 0;
	
	/** for calculating the average speed */
	private double speedSum = 0;
	
	/** for tagging the best car in every epoch */
	private boolean best = false;
	
	/** x position of the front left corner */
	public int x0 = 0;
	
	/** y position of the front left corner */
	public int y0 = 0;
	
	/** x position of the front right corner */
	public int x1 = 0;
	
	/** y position of the front right corner */
	public int y1 = 0;
	
	/** x position of the back right corner */
	public int x2 = 0;
	
	/** y position of the back right corner */
	public int y2 = 0;
	
	/** x position of the back left corner */
	public int x3 = 0;
	
	/** y position of the back left corner */
	public int y3 = 0;
	
	/** distance the car drove */
	private double distance = 0;
	
	/** time the car is on the road */
	private double time = 0;

	/** average speed is needed for quality identification */
	private double avgSpeed = 0;
	
	/** reference to the street */
	private Street street;
	
	/** reference to the nn for reading the values */
	private CarNN nn = null;;
	
	/** reference to the parameter set */
	private CarParamSet params = null;
	
	/** true, if the car moves */
	private boolean isMoving = true;
	
	/** lap counter for calculating the overal distance */
	private int lap = 0;
	
	/** for avoiding lap doublecount */
	private boolean frontPassedLine = false;

	/** polygon representing the car for later drawing */
	private Polygon p = new Polygon();

	/**
	 * Constructor hands over the important references
	 * @param street The street
	 * @param nn the controlling nn
	 * @param carId for logging
	 */
	public Car(Street street, CarNN nn, int carId) {
		this.carId = carId;
		this.street = street;
		this.nn = nn;
		this.params = nn.getParams();
		setStartingPosition();
	}

	/**
	 * Sets the initial position based on the starting point of the street
	 */
	public void setStartingPosition() {
		if (street.getAllPoints().size() > 0) {
			this.x = street.getAllPoints().get(0)[0];
			this.y = street.getAllPoints().get(0)[1];
			this.a = street.getStartAngle();
		}
	}
	
	/** 
	 * If the car crashed or went wrong the result data must be written
	 */
	public void carCrashed() {
		isMoving = false;
		setResultData();
	}
	
	/**
	 * Getter for car movement
	 * @return true, if the car is still moving
	 */
	public boolean isMoving() {
		return isMoving;
	}
	
	
	/**
	 * New parameter means that the car must be resetted.
	 * @param params new parameter
	 * @throws InconsistentValueException
	 */
	public void setNewParamSet(CarParamSet params) throws InconsistentValueException {
		this.params = params;
		resetValues();
	}
	
	/**
	 * The status of the car must be resetted to inital state and the nn must
	 * take over the parameter.
	 * @throws InconsistentValueException
	 */
	public void resetValues() throws InconsistentValueException {
		time = 0;
		isMoving = true;
		v = 0;
		df = 0;
		dl = 0;
		dr = 0;
		s = 0;
		speedSum = 0;
		lap = 0;
		frontPassedLine = false;
		best = false;
		nn.setNewWeights(params);
		setStartingPosition();
	}
	
	/**
	 * If the parameter of the car will be needed for later analysis
	 * @param fileName
	 */
	public void writeParams(String fileName) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(fileName, true);
			fw.write("\n" + carId);
			
			for (double w : params.getWeights()) {
				fw.write(";" + w);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Constructor providing all necessary information for placing the car
	 * @param x X position of the car
	 * @param y Y position of the car
	 * @param a Angle of the car
	 * @param street reference to the street
	 */
	public Car(double x, double y, double a, Street street) {
		this.x = x;
		this.y = y;
		this.a = a;
		this.street = street;
	}
	
	/**
	 * Getter of the parameter
	 * @return
	 */
	public CarParamSet getParams() {
		return params;
	}
	
	/**
	 * Getter of x position
	 * @return
	 */
	public int getX() {
		return (int)x;
	}
	
	/**
	 * Getter of y position
	 * @return
	 */
	public int getY() {
		return (int)y;
	}
	
	/**
	 * Accelerate the car by the given acceleration value
	 * @param a Acceleration value
	 */
	public void accelerate(double a) {
		// reduction to a reasonable value
		a/=10000; 
		
		// increase of the speed
		v += a;
		
		// limitation to maximum speed and avoid backward movement
		v = Math.max(0, v);
		v = Math.min(MAX_SPEED, v);
	}

	/**
	 * Steering wheel change in degrees
	 * @param d steering wheel change
	 */
	public void steer(double d) {
		// reduction to a reasonable value
		d/=100;
		
		// increase of the steering wheel angle
		s += d;
		
		// limitation to +- MAX_STEER
		s = Math.max(-MAX_STEER, s);
		s = Math.min(MAX_STEER, s);
	}
	
	/**
	 * physical movement of the car
	 * @param t time between to calls (will be fixed to 1 for simulation)
	 * @return true, if the car did not crash
	 * @throws InconsistentValueException
	 */
	public boolean move(double t) throws InconsistentValueException {
		// overal time on the road
		time += t;

		// get steering information from the nn based on the input values
		double[] outputs = nn.setInput(new double[] {v, s, dl, dr, df});

		// accelerate and steer the car
		accelerate(outputs[0]);
		steer(outputs[1]);
		
		// cos and sin will be used several times - so the results will be stored
		ca = Math.cos(a);
		sa = Math.sin(a);

		// the car angle change will depend on the speed of the car
		a += s*v;
		normA();

		// now calculate the position of the car
		x += sa * v * t;
		y -= ca * v * t;
		
		// this sum is needed for average speed calculation
		speedSum += v;
		
		// now calculate the positions of the 4 corners of the car
		x0 = (int)(x - ca * HALF_WIDTH + sa * HALF_LENGTH);
		y0 = (int)(y - sa * HALF_WIDTH - ca * HALF_LENGTH);
		
		x1 = (int)(x + ca * HALF_WIDTH + sa * HALF_LENGTH);
		y1 = (int)(y + sa * HALF_WIDTH - ca * HALF_LENGTH);
		
		x2 = (int)(x + ca * HALF_WIDTH - sa * HALF_LENGTH);
		y2 = (int)(y + sa * HALF_WIDTH + ca * HALF_LENGTH);
		x3 = (int)(x - ca * HALF_WIDTH - sa * HALF_LENGTH);
		y3 = (int)(y - sa * HALF_WIDTH + ca * HALF_LENGTH);

		// calculate the distances of the street edge 
		setDistFront();
		setDistLeft();
		setDistRight();
		
		// now check if the car passed the start line - which would 
		// trigger the lap counter. The lap counter will increased, if 
		// in one call of this method the front corner passes the line 
		// and in a subsequent call the back corner passes the line. 
		if (street.pointIsOnStartLine(x0, y0)) {
			frontPassedLine = true;
		} 
		if (street.pointIsOnStartLine(x2, y2) && frontPassedLine) {
			frontPassedLine = false;
			lap++;
		}

		// now check if at least one corner of the car left the street - which means "crash"
		if (!street.pointIsOnStreet(x0, y0)) {
			return false;
		}
		if (!street.pointIsOnStreet(x1, y1)) {
			return false;
		}
		if (!street.pointIsOnStreet(x2, y2)) {
			return false;
		}
		if (!street.pointIsOnStreet(x3, y3)) {
			return false;
		}
		
		// after this time no car is allowed to continue driving. So at the end
		// the speed of the car will be the relevant measure
		if(time > MAX_TRAVEL_TIME) {
			return false;
		}

		// now check, if the car managed to come to a reasonable average speed.
		// This is necessary in order to kick out cars that will not accelerate at all
		if(time > 100) {
			if (speedSum / time < 0.00005) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Avoids negative angle values
	 */
	private void normA() {
		if (a < 0) {
			a+= 2*Math.PI;
		}
	}
	
	/**
	 * Calculate the distance of the street in front of the car
	 * @return Array holding the start and end point of the line car(front) to the point it touches the street edge
	 */
	public int[] setDistFront() {
		double dx = x + sa * HALF_LENGTH;
		double dy = y - ca * HALF_WIDTH;
		
		while(street.pointIsOnStreet((int)dx, (int)dy)) {
			dx += sa;
			dy -= ca;
		}
		// Now limit the street distance to MAX_SEEK_DIST
		df = (MAX_SEEK_DIST - Math.min(getDistance(x + sa * HALF_LENGTH, y - ca * HALF_WIDTH, dx, dy), MAX_SEEK_DIST)) / MAX_SEEK_DIST;
		return new int[] {(int)x, (int)y, (int)dx, (int)dy};
	}
	
	/**
	 * Calculate the distance of the street 45° left front the car
	 * @return Array holding the start and end point of the line car(left) to the point it touches the street edge
	 */
	public int[] setDistLeft() {
		double dx = x0;
		double dy = y0;
		
		while(street.pointIsOnStreet((int)dx, (int)dy)) {
			dx -= -sa + ca;
			dy -= ca + sa;
		}
		// Now limit the street distance to MAX_SEEK_DIST
		dl = (MAX_SEEK_DIST - Math.min(getDistance(x0 , y0, dx, dy), MAX_SEEK_DIST)) / MAX_SEEK_DIST;

		return new int[] {(int)x0, (int)y0, (int)dx, (int)dy};
	}

	
	/**
	 * Calculate the distance of the street 45° right front of the car
	 * @return Array holding the start and end point of the line car(right) to the point it touches the street edge
	 */
	public int[] setDistRight() {
		double dx = x1;
		double dy = y1;
		
		while(street.pointIsOnStreet((int)dx, (int)dy)) {
			dx -= -sa - ca;
			dy -= ca - sa;
		}
		// Now limit the street distance to MAX_SEEK_DIST
		dr = (MAX_SEEK_DIST - Math.min(getDistance(x1 , y1, dx, dy), MAX_SEEK_DIST)) / MAX_SEEK_DIST;
		return new int[] {(int)x1, (int)y1, (int)dx, (int)dy};
	}
	
	/**
	 * Get the distance of two points
	 * @param x0 x of start point
	 * @param y0 y of start point
	 * @param x1 x of end point
	 * @param y1 y of end point
	 * @return distance between start and end point
	 */
	private static double getDistance(double x0, double y0, double x1, double y1) {
		return Math.sqrt(Math.pow(x0 - x1, 2) + Math.pow(y0 - y1, 2));
	}

	/**
	 * Resets the polygon to the current calculated values and returns it
	 * @return polygon representation of the car
	 */
	public Polygon getPolygon() {
		p.reset();
		p.addPoint(x0, y0);
		p.addPoint(x1, y1);
		p.addPoint(x2, y2);
		p.addPoint(x3, y3);
		return p;
	}
	
	/**
	 * Result data is distance and average speed
	 */
	public void setResultData() {
		// the distance is the current distance within one lap and the complete street length times the lap counter
		distance = street.getDistance(x, y, 0) + lap * street.getStreetLength();
		avgSpeed =speedSum / (time/1000);
	}

	/**
	 * Getter of the best flag
	 * @return best flag
	 */
	public boolean isBest() {
		return best;
	}

	/**
	 * Setter of the best flag
	 * @param best true if car is best
	 */
	public void setBest(boolean best) {
		this.best = best;
	}
	
	/**
	 * The sort criteria is mainly the distance. However, if two cars are equal, the avg speed will
	 * also count
	 * @return sort criteria for the comparable interface method
	 */
	public double getSortCriteria() {
		return distance + avgSpeed;
	}
	
	/**
	 * Getter of the time
	 * @return elapsed time
	 */
	public double getTime() {
		return time;
	}
	
	/**
	 * Getter of the distance
	 * @return The distance driven by the car
	 */
	public double getDistance() {
		return distance;
	}

	
	@Override
	public int compareTo(Car o) {
		if (getSortCriteria() == o.getSortCriteria()) {
			return 0;
		}
		if (getSortCriteria() > o.getSortCriteria()) {
			return -1;
		}
		return 1;
	}
}
