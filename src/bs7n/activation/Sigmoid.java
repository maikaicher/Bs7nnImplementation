package bs7n.activation;

/**
 * Sigmoid function as activation function.
 * @author maika
 *
 */
public class Sigmoid implements Activateable {

	@Override
	public double f(double x) {
		return (1/(1+Math.pow(Math.E, -x)));
	}
	
	@Override
	public double ddx(double x) {
		return f(x) * (1 - f(x));
	}
} 		
