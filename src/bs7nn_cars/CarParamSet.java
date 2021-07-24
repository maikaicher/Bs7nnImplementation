package bs7nn_cars;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Class wraps the weights for the nn
 */
public class CarParamSet {
	/** number of needed weights for the nn */
	private int noOfWeights = 0;
	
	/** all weights for the nn */
	private double[] weights;
	
	/** needed for the random generation of initial and delta weights */
	private Random myRnd = new Random();

	/** for debug and logging reasons every parameterset gets an id */
	private int id = 0;
	
	/**
	 * Constructor with all necessary information
	 * @param noOfWeights Number of weights
	 * @param spread Spread for the initial value definition 
	 * @param offset Offset for the initial value definition
	 * @param id Id of the param set
	 */
	public CarParamSet(int noOfWeights, double spread, double offset, int id) {
		this.noOfWeights = noOfWeights;
		this.id = id;
		// here the weights are initially created
		weights = new double[noOfWeights];
		resetWeightData(spread, offset);
	}
	
	/**
	 * Resets the weight data to complete new random values
	 * @param spread Spread for the initial value definition 
	 * @param offset Offset for the initial value definition
	 */
	public void resetWeightData(double spread, double offset) {
		for (int i = 0; i < noOfWeights; i++) {
			// offset shifts the random number (which is between 0.0 and 1.0)
			// and the spread allows to set the highest number. For example, if we need
			// a random number between -1.0 and 1.0 the offset is -0.5 and the
			// spread is 2
			weights[i] = (myRnd.nextDouble() + offset) * spread;
		}
	}
	
	/**
	 * Constructor in case of creating the parameterset from the file
	 * @param weightsIn weights read from the file
	 * @param id id read from the file
	 */
	public CarParamSet(double[] weightsIn, int id) {
		this.noOfWeights = weightsIn.length;
		this.id = id;
		weights = weightsIn;
	}

	/**
	 * During the optimization the best parameter will be set and be moved from its current position
	 * @param bestWeightsIn Weights of the best car parameter set 
	 * @param spread spread of the delta change
	 * @param offset offset of the delta change
	 * @param noOfChanges number of single values to be changed
	 */
	public void changeCarParamSet(double[] bestWeightsIn, double spread, double offset, int noOfChanges) {
		// first identify the (random) positions of the values to be changed
		ArrayList<Integer> changePositions = new ArrayList<>(noOfWeights);
		for (int i = 0; i < noOfWeights; i++) {
			// build an arraylist in order to avoid double change of one value
			changePositions.add(i);
			
			// take over the weights from the best values
			weights[i] = bestWeightsIn[i];
		}
		
		// now change the values
		for (int i = 0; i < noOfChanges; i++) {
			// one element of the arraylist will used only once, since it will be
			// removed after usage. The position of this element will be identified
			// by random
			int posToChange = myRnd.nextInt(changePositions.size());
			
			// the position in the weights array will be derived from the arraylist
			int valueToChange = changePositions.get(posToChange);
			
			// now remove the element in order to avoid double usage
			changePositions.remove(posToChange);
			
			// now change the respective weight 
			weights[valueToChange] += (myRnd.nextDouble() + offset) * spread;
		}
	}
	
	/**
	 * For debug reasons
	 * @return pseudo fingerprint of the element 
	 */
	public String getFingerprint() {
		double val = 0;
		for (double d : weights) {
			val += d;
		}
		return "(" + id + ")" + val;
	}
	
	/**
	 * Getter of the id
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * For persisting the params to the file system as colon separated file. The file
	 * will be concatinated
	 * @param path Path to the file 
	 * @param pos position of the respective car in the list
	 */
	public void writeParams(String path, int pos) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(path, true);
			fw.write("\n" + pos);
			
			for (double w : weights) {
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
	 * Getter of the weights for setting them to the nn
	 * @return the weights
	 */
	public double[] getWeights() {
		return weights;
	}
}
