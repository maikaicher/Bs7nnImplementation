package bs7nn;

/**
 * For better analysis of potential errors a dedicated exception class was built
 * @author maika
 *
 */
public class InconsistentValueException extends Exception {
	/** Value that caused the problem */
	private String wrongValue;
	
	/** Value range that was expected */
	private String expectedValue;
	
	/** Name of the value for better identification */
	private String valueName;
	
	/** Method (plus class) where the exception was thrown */
	private String causingMethod;

	/**
	 * Constructor expecting all necessary values
	 * @param wrongValue
	 * @param expectedValue
	 * @param valueName
	 * @param causingMethod
	 */
	public InconsistentValueException(String wrongValue, String expectedValue, String valueName, String causingMethod) {
		this.wrongValue = wrongValue;
		this.expectedValue = expectedValue;
		this.valueName = valueName;
		this.causingMethod = causingMethod;
	}

	/** 
	 * Output formatter
	 * @return Readable message
	 */
	public String getExceptionCause() {
		return "Inconsistent Value " + valueName + ":\n" + "received Value: " + wrongValue + "\n" + "expected Value: "
				+ expectedValue + "\n" + "in " + causingMethod + " detected.";
	}
}
