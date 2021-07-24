package bs7nn_cars;

import java.util.ArrayList;
import java.util.Collections;


import bs7nn.InconsistentValueException;
import bs7nn_cars.StreetBuilder.DrawField;

public class CarBuilder {
	/** set to true in order to log the parameter to the file system */
	private static final boolean DO_LOG_PARAMS = false;
	
	/** file name, where the parameters will be logged to */
	private static final String LOG_PATH = "C:\\temp\\Params.csv";
	
	/** The best cars will placed into an ArrayList and the new weight basis will derived from this
	 * list by building the average value of every single weight. If set to 1 the weights from the 
	 * best car will be used 1:1 as the basis for all new param sets */
	private static final int NO_OF_BEST = 1;
	
	/** number of cars that will run during one process */
	public static final int NO_OF_CARS = 50;
	
	/** the random value of the initial weight will be set to (nextDouble() + PARAM_OFFSET) * INITIAL_SPREAD */
	private static final double INITIAL_SPREAD = 1;
	
	/** the random value of the new weight will be set to (nextDouble() + PARAM_OFFSET) * INITIAL_SPREAD */
	private static final double PARAM_OFFSET = -0.5;
	
	/** if the process gets stuck, the spread will multiplied wir a factor in order to have more variation 
	 * in the random values. This constant defines the delta of this change*/
	private static final double SPREAD_FACTOR_INCREASE = 0.2;
	
	/** if the process gets stuck, the spread will multiplied wir a factor in order to have more variation 
	 * in the random values. This constant defines the maximum value of that factor. If this value is exceeded,
	 * the algorithm will try to increase now the number of changes instead */
	private static final double MAX_SPREAD_FACTOR = 2.0;
	
	/** the subsequent param sets will be modified by a dynamic spread value. It will be derived by the
	 * driven length. The value pair {0.7, 0.1} means that if the best car went less than 70% of the complete
	 * lap length, the spread of the parameter change will be 0.1. The parameter will be processed from top
	 * to the bottom, whereas the last one will be the "else" branch*/
	private static final double[][] CHANGE_VALUES = {
			{0.1, 1}, 
			{0.5, 0.5}, 
			{0.7, 0.1}, 
			{1, 0.07}, 
			{99999, 0.04}};
	
	/** Position of the length value in the CHANGE_VALUES array */
	private static final int POS_LENGTH_VALUE = 0;
	
	/** Position of the spread value in the CHANGE_VALUES array */
	private static final int POS_SPREAD_VALUE = 1;
	
	/** amount of weights that will be changed in one car of the next generation can change, if 
	 * the process get stuck. This will be the start value*/
	private static final int NO_OF_CHANGES_START = 1;
	
	/** an enhancement is detected, if the increase of distance is above this value */
	private static final double ENHANCEMENT_THRESHOLD = 1;
	
	/** here all cars will be held */
	private ArrayList<Car> cars = new ArrayList<Car>();
	
	/** reference to the street object */
	private Street street;
	
	/** reference to the canvas for setting the cars to draw */
	private DrawField drawField;
	
	/** helper variable to hold the number of weights of the used nn */
	private int noOfWeights = 0;
	
	/** for counting the generations */
	private int generation = 0;
	
	/** amount of weights that will be changed in one car of the next generation. The values will be
	 * identified by random. If the process get stuck, the number will increase */
	private int noOfChanges = NO_OF_CHANGES_START;

	/** for pausing the paint process - not very elegant but it will work for the demo program */
	private boolean calculating = false;
	
	/** If the delta is not able to move the cars out of a deadlock, the spread will be increased if no 
	 * enhancement can be measured. With lastBestDistance we can identify enhancements */
	private double lastBestDistance = 0;
	
	/** If the delta is not able to move the cars out of a deadlock, the spread will be increased if no 
	 * enhancement can be measured. With spreadFactor the spread will be increased */
	private double spreadFactor = 1;
	
	/**
	 * Constructor for the initial generation of the builder
	 * @param street The street the cars will run on
	 * @param drawField Here the cars will be set to draw
	 * @throws InconsistentValueException In case of wrong nn parameters
	 */
	public CarBuilder(Street street, DrawField drawField) throws InconsistentValueException {
		this.street = street;
		this.drawField = drawField;
		buildCars();
	}
	
	/**
	 * Creates all cars with the nn and an inital parameter set
	 * @throws InconsistentValueException
	 */
	private void buildCars() throws InconsistentValueException {
		// remove all cars to draw
		drawField.resetCars();
		
		// for every car that should be created
		for (int i = 0; i < NO_OF_CARS; i++) {
			// in carnn the structure of the nn is held (number of intput/output/hidden neurons)
			CarNN nn = new CarNN();
			
			// for later usage
			noOfWeights = nn.getNoOfWeights();
			
			// Creates the paramterset (a wrapper for the weight values)
			CarParamSet newParamSet = new CarParamSet(noOfWeights, INITIAL_SPREAD, PARAM_OFFSET, i);
			
			// place weights to the nn
			nn.setNewWeights(newParamSet);

			// now create the car and set the relevant informations
			Car car = new Car(street, nn, i);
			
			// add the car to the list for sequential 
			cars.add(car);
			drawField.addCar(car);
		}
	}
	
	/**
	 * If the "reset" Button is pressed, the complete NN should be recreated - meaning all weights should
	 * be set to a new random value
	 * @throws InconsistentValueException
	 */
	public void resetNNData() throws InconsistentValueException {
		resetCarData();
		for (int i = 0; i < cars.size(); i++) {
			// in carnn the structure of the nn is held (number of intput/output/hidden neurons)
			cars.get(i).getParams().resetWeightData(INITIAL_SPREAD, PARAM_OFFSET);
		}
	}
	
	/**
	 * Resets the car inner values, like speed, direction etc.
	 * @throws InconsistentValueException
	 */
	public void resetCarData() throws InconsistentValueException {
		for (Car car : cars) {
			car.resetValues();
		}
	}
	
	
	/**
	 * sorts the car array according the the sort criteria (mainly distance)
	 * @return distance of the best car
	 */
	private double findBestCars() {
		Collections.sort(cars);
		
		if (DO_LOG_PARAMS) {
			for (int i = 0; i < cars.size(); i++) {
				cars.get(i).getParams().writeParams(LOG_PATH, i);
			}
		}
		
		return cars.get(0).getDistance();
	}

	/** 
	 * Determines the change value of the weights based on the driven distance of the best car
	 * @param currentDistance Distance of the best car
	 * @param streetLength Length of the street in order to allow relative measurement
	 * @return spread value for the next epoch
	 */
	private double getSpreadValue(double currentDistance, double streetLength) {
		// the information for the changes are in the CHANGE_VALUES array
		for (int i = 0; i < CHANGE_VALUES.length - 1; i++) {
			if (currentDistance < streetLength * CHANGE_VALUES[i][POS_LENGTH_VALUE]) {
				return CHANGE_VALUES[i][POS_SPREAD_VALUE];
			}
		}
		return CHANGE_VALUES[CHANGE_VALUES.length - 1][POS_SPREAD_VALUE];
	}
	
	/**
	 * After the last car terminated, the next generation of cars will be prepared
	 * @throws InconsistentValueException If the nn received wrong parameters
	 */
	public void prepareNextGeneration() throws InconsistentValueException {
		// for blocking the GUI
		calculating = true;

		// for output
		generation++;
		
		// the best car will be on position 0 of the cars ArrayList
		double distanceOfBestCar = findBestCars();
		
		// now get the spread value based on the distance of the best car
		double spreadValue = getSpreadValue(distanceOfBestCar, street.getStreetLength());
		
		// now check, if no enhancement was measured
		if (distanceOfBestCar <= lastBestDistance + ENHANCEMENT_THRESHOLD) {
			// if so, check if the spread factor was already increased to the maximum
			if (spreadFactor >= MAX_SPREAD_FACTOR) {
				// if so, increase the number of weights to change
				if (noOfChanges < noOfWeights) {
					noOfChanges++;
				}
			} else {
				// if not, increase the spread factor
				spreadFactor += SPREAD_FACTOR_INCREASE;
			}
		} else {
			spreadFactor = 1;
			noOfChanges = NO_OF_CHANGES_START;
		}
		spreadValue *= spreadFactor;
		lastBestDistance = distanceOfBestCar;
		
		// if the NO_OF_BEST value will be > 1 the method will use the average value of every
		// weight of the best cars. If NO_OF_BEST is 1 the best params will be taken 1:1
		double[] avgValues = new double[noOfWeights];
		
		// calculate the average values
		for (int i = 0; i < noOfWeights; i++) {
			for (int j = 0; j < NO_OF_BEST; j++) {
				avgValues[i] += cars.get(j).getParams().getWeights()[i];
			}
			avgValues[i] /= NO_OF_BEST;
		}
		
		// now reset the best car. the values will not change in order to have a reference in the next run (display in red)
		cars.get(0).resetValues();
		cars.get(0).setBest(true);
		
		// now change the parameter of all subsequent cars
		for (int i = 1; i < NO_OF_CARS; i++) {
			cars.get(i).getParams().changeCarParamSet(avgValues, spreadValue, PARAM_OFFSET, noOfChanges);
			cars.get(i).setBest(false);
			cars.get(i).resetValues();
		}
		String info = "gen: " + generation + " best: " + getRoundedValue(cars.get(0).getDistance(), 2) + " sprdv: " + getRoundedValue(spreadValue,2) + " sprdf: " + getRoundedValue(spreadFactor, 2) + " noCh:" + noOfChanges;
		System.out.println(info);
		drawField.setTextInfo(info);
		calculating = false;
	}

	/**
	 * For cleaner logging output
	 * @param value Value to round
	 * @param noOfDig No of digits after decimal point
	 * @return rounded value for output
	 */
	private String getRoundedValue(double value, int noOfDig) {
		value = Math.round(value * Math.pow(10, noOfDig));
		return String.valueOf(value / Math.pow(10, noOfDig));
	}
	
	/**
	 * returns true, if the processing of the next generation is done
	 * @return true if no conflicts will arise
	 */
	public boolean okToPrint() {
		return !calculating;
	}
	
	/**
	 * For avoiding double calls from the timer thread
	 * @return true if the next generation is currently calculated
	 */
	public boolean iAmBusy() {
		return calculating;
	}
	
	/**
	 * Getter of the car list
	 * @return list of all cars
	 */
	public ArrayList<Car> getCars() {
		return cars;
	}
}
