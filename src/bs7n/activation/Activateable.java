package bs7n.activation;

/**
 * Interface for all activation functions supported by the Worker Neurons
 * @author maika
 *
 */
public interface Activateable {
	/**
	 * Activation function
	 * @param input sum of all incomming connection values
	 * @return activation level
	 */
	double f(double input);
	
	/**
	 * derivative of the activation function
	 * @param x sum of all incomming connection values
	 * @return derivative on position x
	 */
	double ddx(double x);
}
