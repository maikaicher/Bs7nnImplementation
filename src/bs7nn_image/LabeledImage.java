package bs7nn_image;

import java.awt.image.BufferedImage;

/**
 * Class holding the image data (unsigned byte values between 0 and 255 as grayscale) and
 * the corresponding label as an integer value
 */
public class LabeledImage {
	/** label for NN interpretation */
	public int label;
	
	/** width of the image */
	private int width = 0;
	
	/** height of the image */
	private int height = 0;
	
	/** image data (grayscale)*/
	public byte[][] data;

	/**
	 * Single image constructorfrom a raw dataset
	 * @param label The label for learing
	 * @param data raw byte data as grayscale
	 */
	public LabeledImage(int label, byte[][] data) {
		this.label = label;
		this.data = data;
		width = data.length;
		if (width > 0) {
			height = data[0].length;
		}
	}

	/**
	 * Single image constructor from a buffered image
	 * @param label The label for testing
	 * @param myImg Buffered image that will be converted to raw data
	 * @param invertColors MINST has an inverted color schema in comparison to our drawing.
	 */
	public LabeledImage(int label, BufferedImage myImg, boolean invertColors) {
		this.label = label;
		this.data = convertImage(myImg, invertColors);
		height = myImg.getHeight();
		width = myImg.getWidth();
	}

	
	/**
	 * Converts a buffered image to raw byte data
	 * @param myImg Image to convert
	 * @param invertColors MINST has an inverted color schema in comparison to our drawing.
	 * @return raw byte data (2 dimensional)
	 */
	public static byte[][] convertImage(BufferedImage myImg, boolean invertColors) {
		byte[][] data = new byte[myImg.getWidth()][myImg.getHeight()];
		
		// note that in the idx dataset is flipped compared to the buffered image format
		// so x and y must be flipped as well
		for (int y = 0; y < myImg.getHeight(); y++) {
			for (int x = 0; x < myImg.getWidth(); x++) {
				data[x][y] = getGrayScale(myImg.getRGB(y, x), invertColors);
			}
		}

		return data;
	}

	
	/**
	 * Converts a color to a grayscale (by average weights of rgb colors) 
	 * @param color Color value as integer (from Color.getRGB())
	 * @param invertColors MNIST has an inverse color schema, so we invert the drawing as well
	 * @return byte value as grayscale value
	 */
	public static byte getGrayScale(int color, boolean invertColors) {
		// extract the rgb colors
		int myColorB = color;
		myColorB &= 0xFF;

		int myColorG = color >> 8;
		myColorG &= 0xFF;

		int myColorR = color >> 16;
		myColorR &= 0xFF;

		if (invertColors) {
			myColorG = 0xFF - myColorG;
			myColorB = 0xFF - myColorB;
			myColorR = 0xFF - myColorR;
		}

		// build average color
		int colorGray = (int) ((myColorR + myColorG + myColorB) / 3);

		return (byte) colorGray;
	}

	/**
	 * converts the data to a double array with values from 0.0 to 1.0
	 * @return normalized double interpretation of the image data
	 */
	public double[] getNormedData() {
		double[] outData = new double[width * height];
		int outPos = 0;
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[x].length; y++) {
				outData[outPos++] = Byte.toUnsignedInt(data[x][y]) / 255.0;
			}
		}
		return outData;
	}
}
