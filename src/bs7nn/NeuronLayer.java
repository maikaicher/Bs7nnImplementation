package bs7nn;

import java.util.ArrayList;

/**
 * Holds a hidden layer (meaning all neurons of it) of the neuronal network.
 * @author maika
 *
 */
public class NeuronLayer {
	/** All hidden neurons of this layer */
	private ArrayList<WorkerNeuron> layerNeurons = new ArrayList<>();

	
	/**
	 * Creates a single hidden neuron, places it into the hidden neuron list and
	 * returns it for further references. Because the neurons are "worker neurons"
	 * the method needs to know which activation function should be realized.
	 * @param type Type of activation function
	 * @return The newly created worker neuron
	 */
	public WorkerNeuron createNeuron(int type) {
		WorkerNeuron n = WorkerFactory.getNeuron(type);
		layerNeurons.add(n);
		return n;
	}

	/**
	 * Getter of the neuron list
	 * @return list of all neurons of this layer
	 */
	public ArrayList<WorkerNeuron> getNeuronList() {
		return layerNeurons;
	}

	/**
	 * Gets the number of neurons of this layer
	 * @return Number of neurons of this layer
	 */
	public int getNoOfNeurons() {
		return layerNeurons.size();
	}

	/**
	 * Calls the reset method of all neurons of this layer
	 */
	public void resetMe() {
		for (WorkerNeuron w : layerNeurons) {
			w.resetMe();
		}
	}

	/**
	 * Calls the backPropagate method of all neurons of this layer
	 */
	public void backPropagate() {
		for (WorkerNeuron w : layerNeurons) {
			w.backPropagate();
		}
	}

	/**
	 * Calls the deltaLearn method of all neurons of this layer
	 */
	public void deltaLearn(double beta) {
		for (WorkerNeuron w : layerNeurons) {
			w.deltaLearn(beta);
		}
	}

}
