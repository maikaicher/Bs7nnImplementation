package bs7nn;

/**
 * Neuron that has no incomming connections. It will be used as an input neuron or
 * as a BIAS neuron
 * @author maika
 *
 */
public class InputNeuron extends Neuron {
	/**
	 * Constructor, initializing the value with 1, so the object can be used
	 * also as a BIAS neuron
	 */
	public InputNeuron() {
		setA(1);
	}
}

