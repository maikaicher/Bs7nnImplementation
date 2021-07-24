package bs7nn_cars;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import bs7nn.InconsistentValueException;
import bs7nn.NeuronLayer;
import bs7nn.NeuronalNetwork;
import bs7nn.WorkerFactory;
import bs7nn.WorkerNeuron;

/**
 * Class for building the neurnal network for the car example
 */
public class CarNN {
	/** The inputs will be speed, angle, distance left, distance right, distanc front */
	private static final int NO_INPUT = 5;
	
	/** the outputs will be acceleration and steering */
	private static final int NO_OUTPUT = 2;
	
	/** one hidden layer will be sufficient */
	private static final int NO_HIDDEN = 1;
	
	/** not more hidden neurons than input neurons */
	private static final int NO_HD_NEURONS = NO_INPUT;
	
	/** needed for extracting the output values */
	private WorkerNeuron[] outputs = new WorkerNeuron[NO_OUTPUT];
	
	/** here the weight values will be sent to the nn */
	private CarParamSet param;
	
	/** the nn */
	private NeuronalNetwork nn = new NeuronalNetwork(NO_HIDDEN);
	
	/**
	 * Constructor will create the nn. However, the random values of the nn will be overwritten by
	 * the CarParamSet
	 * @throws InconsistentValueException
	 */
	public CarNN() throws InconsistentValueException {
		for (int i = 0; i < NO_INPUT; i++) {
			nn.createInputNeuron();
		}
		for (NeuronLayer l : nn.getHiddenLayers()) {
			for (int i = 0; i < NO_HD_NEURONS; i++) {
				l.createNeuron(WorkerFactory.RELU);
			}
		}		
		for (int i = 0; i < NO_OUTPUT; i++) {
			outputs[i] = nn.createOutputNeuron(WorkerFactory.TANH);
		}
		// the ramdom values will be overwritten later on
		nn.doDenseMesh(null, true, 1.0);
	}
	
	/**
	 * forward propagation method
	 * @param input Input values of the car (speed, angle and distances)
	 * @return calculated output values
	 * @throws InconsistentValueException
	 */
	public double[] setInput(double[] input) throws InconsistentValueException {
		nn.setInputValues(input);
		
		// extract the output values to an array
		double[] output = new double[NO_OUTPUT];
		for (int i = 0; i < NO_OUTPUT; i++) {
			output[i] = outputs[i].getA();
		}
		return output;
	}
	
	/**
	 * The weight values will be extracted from the parameter set
	 * @param param the wrapper for the weight values
	 * @throws InconsistentValueException
	 */
	public void setNewWeights(CarParamSet param) throws InconsistentValueException {
		// will be stored for later access
		this.param = param;
		// here the weights will be extracted
		nn.setWeights(param.getWeights(), true);
	}

	/**
	 * Getter of the param set
	 * @return the param set
	 */
	public CarParamSet getParams() {
		return param;
	}
	
	/**
	 * Needed for external weight optimization
	 * @return the number of weights
	 */
	public int getNoOfWeights() {
		return nn.getNoOfDenseConnects(true);
	}
	
	/**
	 * For debug and optimization reasons the parameter set can be written to a file
	 * @param fileName
	 */
	public void writeParamSet(String fileName) {
		FileWriter fwr = null;
		
		try {
			fwr = new FileWriter(fileName, false);
			for (double entry : param.getWeights()) {
				String line = entry + "\n";
				fwr.append(line);
			}
			fwr.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (fwr != null) {
				try {
					fwr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * For optimization reansons a written parameterset can be reloaded to the software
	 * @param fileName
	 * @throws InconsistentValueException 
	 */
	public void readParamSet(String fileName) throws InconsistentValueException {
		FileReader frd = null;
		BufferedReader brd = null;
		ArrayList<Double> fileData = new ArrayList<Double>();
		try {
			frd = new FileReader(fileName);
			brd = new BufferedReader(frd);
			String sLine = null;
			
			while((sLine = brd.readLine()) != null) {
				if (sLine.length() > 0) {
					fileData.add(Double.parseDouble(sLine));
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
		
		double[] weights = new double[fileData.size()];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = fileData.get(i);
		}
		param = new CarParamSet(weights, 0);
		setNewWeights(param);
	}
	
}
