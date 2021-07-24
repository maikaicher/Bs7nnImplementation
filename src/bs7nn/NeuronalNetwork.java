package bs7nn;

import java.util.ArrayList;
import java.util.Random;

/**
 * Holds all necessary methods for creating a dense mesh neuronal network including
 * the delta learn rule and the backpropagation algorithm
 */
public class NeuronalNetwork {
	/** All input neurons */
	private ArrayList<InputNeuron> inputs = new ArrayList<>();
	
	/** all output neurons */
	private ArrayList<WorkerNeuron> outputs = new ArrayList<>();
	
	/** all hidden layer (wich then will contain the worker neurons) */
	private ArrayList<NeuronLayer> hiddenLayers = new ArrayList<>();

	/**
	 * Constructor that expects the infor of how many hidden layers should be
	 * created
	 * @param noOfHiddenLayers Number of hidden layers
	 */
	public NeuronalNetwork(int noOfHiddenLayers) {
		// creates empty hidden layers
		for (int i = 0; i < noOfHiddenLayers; i++) {
			hiddenLayers.add(new NeuronLayer());
		}
	}

	/**
	 * Creates a single input neuron, places it into the input neuron list and
	 * returns it for further references
	 * @return The newly created input neuron
	 */
	public InputNeuron createInputNeuron() {
		InputNeuron n = new InputNeuron();
		inputs.add(n);
		return n;
	}

	/**
	 * Creates a single output neuron, places it into the output neuron list and
	 * returns it for further references. Because the output neurons are "worker neurons"
	 * the method needs to know which activation function should be realized.
	 * @param type Type of activation function
	 * @return The newly created worker neuron
	 */
	public WorkerNeuron createOutputNeuron(int type) {
		// the neurons will be built by the worker factory
		WorkerNeuron n = WorkerFactory.getNeuron(type);
		outputs.add(n);
		return n;
	}

	/**
	 * Calculates, how many weights must be created for a dense mesh network. The
	 * method needs to know, if the complete network needs bias neurons. However, it 
	 * is not able to select, which worker neuron should have bias - it will be always
	 * all or none.
	 * @param withBias True, if all Worker Neuron should have a bias input
	 * @return The number of weights (connections) of the network in a dense topology
	 */
	public int getNoOfDenseConnects(boolean withBias) {
		int noOfExpectedWeights = 0;
		// if no hidden layer, only the input and output are connected
		if (hiddenLayers.size() == 0) {
			noOfExpectedWeights = inputs.size() * outputs.size();
			if (withBias) {
				noOfExpectedWeights += outputs.size();
			}

		} else {
			noOfExpectedWeights = inputs.size() * hiddenLayers.get(0).getNoOfNeurons();
			for (int i = 1; i < hiddenLayers.size(); i++) {
				noOfExpectedWeights += hiddenLayers.get(i - 1).getNoOfNeurons() * hiddenLayers.get(i).getNoOfNeurons();
			}
			noOfExpectedWeights += hiddenLayers.get(hiddenLayers.size() - 1).getNoOfNeurons() * outputs.size();
			if (withBias) {
				noOfExpectedWeights += outputs.size();
				for (int i = 0; i < hiddenLayers.size(); i++) {
					noOfExpectedWeights += hiddenLayers.get(i).getNoOfNeurons();
				}
			}
		}
		return noOfExpectedWeights;
	}

	/**
	 * Builds a dense network based on all existing neurons
	 * @param weights	if the weights are predefined, they will be set here as a vector.
	 * The number of weights must be equal to the return value of the getNoOfDenseConnects method.
	 * First the input connections, then (if existing) the hidden layers, then the
	 * bias (first hidden, then output) and at last the output layer. However, usually
	 * the weights should be initialized by random. In this case set the weights vector to null.
	 * @param withBias True, if all worker neurons should get a bias input
	 * @throws InconsistentValueException In case of the number of weights is wrong
	 */
	public void doDenseMesh(double[] weights, boolean withBias, double weightFactor) throws InconsistentValueException {
		int noOfExpectedWeights = getNoOfDenseConnects(withBias);
		if (weights == null) {
			Random myRnd = new Random();
			weights = new double[noOfExpectedWeights];
			for (int i = 0; i < noOfExpectedWeights; i++) {
				weights[i] = myRnd.nextDouble() * weightFactor;
			}
		}

		if (weights.length != noOfExpectedWeights) {
			throw new InconsistentValueException(String.valueOf(weights.length), "= " + noOfExpectedWeights,
					"size of weights", "NeuronalNetwork.doFullMesh");
		}
		int indexPos = 0;
		if (hiddenLayers.size() == 0) {
			for (InputNeuron in : inputs) {
				for (WorkerNeuron wn : outputs) {
					wn.addConnection(new Connection(in, weights[indexPos++]));
				}
			}
			if (withBias) {
				for (WorkerNeuron wn : outputs) {
					wn.addConnection(new Connection(new InputNeuron(), weights[indexPos++]));
				}
			}

		} else {
			for (InputNeuron in : inputs) {
				for (WorkerNeuron wn : hiddenLayers.get(0).getNeuronList()) {
					wn.addConnection(new Connection(in, weights[indexPos++]));
				}
			}
			for (int i = 1; i < hiddenLayers.size(); i++) {
				for (WorkerNeuron wIn : hiddenLayers.get(i - 1).getNeuronList()) {
					for (WorkerNeuron wOut : hiddenLayers.get(i).getNeuronList()) {
						wOut.addConnection(new Connection(wIn, weights[indexPos++]));
					}
				}
			}
			if (withBias) {
				for (int i = 0; i < hiddenLayers.size(); i++) {
					for (WorkerNeuron wn : hiddenLayers.get(i).getNeuronList()) {
						wn.addConnection(new Connection(new InputNeuron(), weights[indexPos++]));
					}
				}
				for (WorkerNeuron wn : outputs) {
					wn.addConnection(new Connection(new InputNeuron(), weights[indexPos++]));
				}
			}

			for (WorkerNeuron wIn : hiddenLayers.get(hiddenLayers.size() - 1).getNeuronList()) {
				for (WorkerNeuron wOut : outputs) {
					wOut.addConnection(new Connection(wIn, weights[indexPos++]));
				}
			}
		}
	}

	/**
	 * Forward propagation method. It will set all input values and calculates the
	 * activation levels of all neurons
	 * @param inputValues Vector of all input values
	 * @throws InconsistentValueException In case of the number of inputValues do not match the
	 * number of input neurons
	 */
	public void setInputValues(double[] inputValues) throws InconsistentValueException {
		if (inputValues.length != inputs.size()) {
			throw new InconsistentValueException(String.valueOf(inputValues.length), "< " + inputs.size(),
					"inputValues", "NeuronalNetwork.setInputValues");
		}

		for (int i = 0; i < inputValues.length; i++) {
			inputs.get(i).setA(inputValues[i]);
		}

		for (int i = 0; i < hiddenLayers.size(); i++) {
			for (WorkerNeuron wn : hiddenLayers.get(i).getNeuronList()) {
				wn.calcA();
			}
		}
		for (WorkerNeuron wn : outputs) {
			wn.calcA();
		}
	}

	/**
	 * Delta learn rule - with back propagation. It adapts the weights of all connections, based
	 * on the reference values in case of output neurons and all hidden neuron connections
	 * based of the back propagated error value
	 * @param references Reference values of the output layer
	 * @param beta Learn step width
	 * @throws InconsistentValueException In case that the number of reference values do not
	 * match the number of output neurons
	 */
	public void deltaLearn(double[] references, double beta) throws InconsistentValueException {
		if (references.length != outputs.size()) {
			throw new InconsistentValueException(String.valueOf(references.length), "!= " + outputs.size(),
					"references", "NeuronalNetwork.deltaLearnRule");
		}
		
		// set all internal error values ("delta") to 0.
		resetMe();
		int refPos = 0;
		
		// calculate the error of the output neurons based on the reference value...
		for (WorkerNeuron w : outputs) {
			w.calcDelta(references[refPos++]);
			
			//... and backpropagate the error to the last hidden layer ("most right")
			w.backPropagate();
		}

		// now backpropagate the error from the last hidden layer towards the the input
		// layer ("from right to left") to the first hidden layer
		for (int i = hiddenLayers.size() - 1; i > 0; i--) {
			hiddenLayers.get(i).backPropagate();
		}

		// now adapt the weights of the output neuron connections
		for (WorkerNeuron w : outputs) {
			w.deltaLearn(beta);
		}

		// and adapt the weights of the hidden layers
		for (NeuronLayer l : hiddenLayers) {
			l.deltaLearn(beta);
		}
	}

	/**
	 * Getter of the hidden layer
	 * @return All hidden layer
	 */
	public ArrayList<NeuronLayer> getHiddenLayers() {
		return hiddenLayers;
	}

	/**
	 * Resets all error values of the neurons
	 */
	public void resetMe() {
		for (WorkerNeuron w : outputs) {
			w.resetMe();
		}
		for (NeuronLayer l : hiddenLayers) {
			l.resetMe();
		}
	}
	
	/**
	 * Sets all weights to new values
	 * @param weights The new weight values
	 * @param withBias Needed for error handling
	 * @throws InconsistentValueException If the number of given weights differs from the needed number of weights
	 */
	public void setWeights(double[] weights, boolean withBias) throws InconsistentValueException {
		int noOfExpectedWeights = getNoOfDenseConnects(withBias);
		if (weights.length != noOfExpectedWeights) {
			throw new InconsistentValueException(String.valueOf(weights.length), String.valueOf(noOfExpectedWeights), "no of weights", "setWeights");
		}
		// take over all values
		int pos = 0;
		for (NeuronLayer l : hiddenLayers) {
			for (WorkerNeuron wn : l.getNeuronList()) {
				for (Connection c : wn.getConnections()) {
					c.setWeight(weights[pos++]);
				}
			}
		}
		
		for (WorkerNeuron wn : outputs) {
			for (Connection c : wn.getConnections()) {
				c.setWeight(weights[pos++]);
			}
		}
	}

}
