package bs7n.activation;

/**
 * Identity function as activation function.
 * @author maika
 *
 */
public class Identity implements Activateable {

	@Override
	public double f(double x) {
		return x;
	}
	@Override
	public double ddx(double x) {
		return 1;
	}

} 		
