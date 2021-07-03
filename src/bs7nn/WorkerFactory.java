package bs7nn;

import bs7n.activation.Identity;
import bs7n.activation.ReLU;
import bs7n.activation.Sigmoid;
import bs7n.activation.TangensHyp;

/**
 * Helper class for simple neuron generation
 * 
 * @author maika
 *
 */
public class WorkerFactory {

	/** ID of neurons with an identity activation function */
	public static final int IDNT = 0;

	/** ID of neurons with a sigmoid activation function */
	public static final int SIGM = 1;

	/** ID of neurons with a tangens hyperbolicus activation function */
	public static final int TANH = 2;

	/** ID of neurons with a relu activation function */
	public static final int RELU = 3;

	/**
	 * Returns a new activation function object of the given type
	 * @param type Type of worker neuron
	 * @return Worker neuron instance. Default is identity
	 */
	public static WorkerNeuron getNeuron(int type) {
		switch (type) {
		case SIGM:
			return new WorkerNeuron(new Sigmoid());
		case TANH:
			return new WorkerNeuron(new TangensHyp());
		case RELU:
			return new WorkerNeuron(new ReLU());
		}
		return new WorkerNeuron(new Identity());
	}
}
