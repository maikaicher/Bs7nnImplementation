package bs7nn_cars;

import bs7nn.InconsistentValueException;
import bs7nn_cars.StreetBuilder.DrawField;

/**
 * Threadable class for the cyclic calculation
 */
public class PhysicTimer implements Runnable {
	/** for stopping the timer */
	private boolean doRun = false;
	
	/** for calling the drawField to calculate the next step */
	private DrawField drawField;
	
	/**
	 * Constructor
	 * @param df Reference to the class that coordinates the display and recalculations
	 */
	public PhysicTimer(DrawField df) {
		drawField = df;
	}
	
	@Override
	public void run() {
		doRun = true;
		while (doRun) {
			try {
				try {
					// calculate faster :-)
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// the next step should be prepared
				drawField.doStep(10);
			} catch (InconsistentValueException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stop the timer
	 */
	public void doStop() {
		doRun = false;
	}
	
	/**
	 * Getter of doRun
	 * @return true if timer is running
	 */
	public boolean isRunning() {
		return doRun;
	}

}
