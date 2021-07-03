package bs7nn;

/**
 * Holds the "must have" of a neuron
 * @author maika
 *
 */
public abstract class Neuron {
	/** neuron activation level */
	protected double a;

	/**
	 * Getter of activation level
	 * @return
	 */
	public double getA() {
		return a;
	}

	/**
	 * Setter of activation level. This is only needed for
	 * the input neurons. Worker Neurons will calculate the activation level
	 * by the incomming connections and the activation function
	 * @param a
	 */
	public void setA(double a) {
		this.a = a;
	}
}

