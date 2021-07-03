package bs7n.activation;

/**
 * rectified linear unit function as activation function.
 * @author maika
 *
 */
public class ReLU implements Activateable {

	@Override
	public double f(double x) {
		return Math.max(0, x);
	}
	
	@Override
	public double ddx(double x) {
		if (x < 0) {
			return 0;
		}
		return 1;
	}

}		

