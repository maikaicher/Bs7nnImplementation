package bs7n.activation;

/**
 * Tangens hyperbolicus function as activation function.
 * @author maika
 *
 */
public class TangensHyp implements Activateable {

	@Override
	public double f(double x) {
		return (1-2/(1+Math.pow(Math.E, 2*x)));
	}
	
	@Override
	public double ddx(double x) {
		return 1 - Math.pow(f(x), 2);
	}

} 		

