package bs7nn;

/**
 * Connection between two neurons, holding the source of the singal as a reference
 * to the source neuron and the weight of the connection.
 * @author maika
 *
 */
public class Connection {
	/** Neuron sending the signal through the connection */
	private Neuron source;
	
	/** Weight multiplied by the source signal leads to the signal value of the connection */
	private double weight;
	
	/** momentum for faster weight convergence */
	private double momentum = 0;
	
	/** momentum damping for leading momentum back to 0 */
	private double damping = 0.9;
	
	/**
	 * Constructor receiving the neccessary information 
	 * @param source Source of the signal
	 * @param weight Weight, the singal will be multiplied with
	 */
	public Connection(Neuron source, double weight) {
		this.source = source;
		this.weight = weight;
	}

	/**
	 * The output signal of the connection will be the source signal times the weight
	 * @return Output signal of the connection
	 */
	public double getX() {
		return weight * source.getA();
	}

	/**
	 * Getter of the source neuron
	 * @return source neuron
	 */
	public Neuron getSource() {
		return source;
	}

	/**
	 * Getter of the weight
	 * @return weight
	 */
	public double getWeight() {
		return weight;
	}

	/**
	 * Setter of the weight. This setter is needed for later adaption of 
	 * the weight or for complete new learning cycles.
	 * @param weight
	 */
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	/**
	 * Weight adaption within the delta learn algorithm
	 * @param delta Amount of change of the weight
	 */
	public void moveWeight(double delta) {
		momentum += delta;
		momentum *= damping;
		weight += delta + momentum;
	}

}
