package bs7nn;

import java.util.ArrayList;

import bs7n.activation.Activateable;

/**
 * Class for modeling a worker (and therfore also an output) neuron
 * @author maika
 *
 */
public class WorkerNeuron extends Neuron {
	/** Activation function of the neuron */
	private Activateable activate;
	
	/** all incomming connections (including connections from a bias neuron) */
	private ArrayList<Connection> connections = new ArrayList<>();
	
	/** sum of all incomming connection signals */
	private double x = 0;
	
	/** error at the output of the neuron */
	private double delta = 0;

	/**
	 * Constructor with activation function
	 * @param activate activation function object
	 */
	public WorkerNeuron(Activateable activate) {
		this.activate = activate;
	}

	/**
	 * For adding a new incomming connection to the neuron
	 * @param newConnection New connection to add
	 */
	public void addConnection(Connection newConnection) {
		connections.add(newConnection);
	}

	/**
	 * Calculation of the sum of all incomming connection values
	 */
	public void calcX() {
		double x = 0;
		for (Connection c : connections) {
			x += c.getX();
		}
		this.x = x;
	}

	/** 
	 * Calculation of the activation level (meaning activation function at point x)
	 */
	public void calcA() {
		calcX();
		a = activate.f(x);
	}

	/**
	 * Adaption of all weights of the incomming connections based on the "delta" error
	 * at the output of the neuron
	 * @param beta Learn step size
	 */
	public void deltaLearn(double beta) {
		double factor = beta * delta * activate.ddx(x);
		for (Connection c : connections) {
			c.moveWeight(factor * c.getSource().getA());
		}
	}

	/**
	 * Delta calculation for the output neurons
	 * @param ref Expected value at the output
	 */
	public void calcDelta(double ref) {
		delta = ref - a;
	}

	/**
	 * Delta calculation for hidden neurons within the backpropagation algorithm. 
	 * This will be the sum of all weighted errors of all connections that use the 
	 * output signal (activation level) of this neuron
	 * @param deltaNplus1 Error adaption
	 */
	public void addDelta(double deltaNplus1) {
		delta += deltaNplus1;
	}

	/**
	 * Resets the delta for the next backpropagation cycle
	 */
	public void resetMe() {
		delta = 0;
	}

	/**
	 * Back propagation propagates the weighted error of this neuron to the neurons that
	 * feed the connections
	 */
	public void backPropagate() {
		for (Connection c : connections) {
			if (c.getSource() instanceof WorkerNeuron) {
				((WorkerNeuron) c.getSource()).addDelta(delta * c.getWeight());
			}
		}
	}
	
	/**
	 * Getter of all connections
	 * @return The connections
	 */
	public ArrayList<Connection> getConnections() {
		return connections;
	}
}
