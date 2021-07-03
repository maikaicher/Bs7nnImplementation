package bs7nn_image;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;

/**
 * Class for loading data files in the "idx" file format. Structure according to Mr. Yann Lecun, who provided the data:
 * http://yann.lecun.com/exdb/mnist/
 * 
 * Data File (training and test):
 * [offset] [type]          [value]          [description]
 * 0000     32 bit integer  0x00000803(2051) magic number  (MSB first)
 * 0004     32 bit integer  ??               number of images
 * 0008     32 bit integer  28               number of rows
 * 0012     32 bit integer  28               number of columns
 * 0016     unsigned byte   ??               pixel
 * 0017     unsigned byte   ??               pixel
 * ........
 * xxxx     unsigned byte   ??               pixel
 * Pixels are organized row-wise. Pixel values are 0 to 255. 0 means background (white), 255 means foreground (black).
 * This is somewhat "tricky", because if we use e.g. a JPanel (or Canvas) for creating an image, 0x000000 will be black
 * and 0xFFFFFF will be white, so we need an inversion of the numbers if we want to use the NN for identifying own images.
 * 
 * "magic number" can be seen as a kind of signature
 * 
 * Row-wise means that the pixel are given, starting at the most upper left position, increasing
 * to the left until the last column and then next row and so on:
 *  0  1  2  3  4  5  6  7  ... 27
 * 28 29 30 31 32 33 34 35 .... 59
 * 60 .....
 * 
 * Label File (training and test - number of labels must be number of images in corresponding file)
 * [offset] [type]          [value]          [description]
 * 0000     32 bit integer  0x00000801(2049) magic number (MSB first)
 * 0004     32 bit integer  ??               number of items
 * 0008     unsigned byte   ??               label
 * 0009     unsigned byte   ??               label
 * ........
 * xxxx     unsigned byte   ??               label
 */
public class ImageDatasetLoader {
	/** header size of the data set - in MINST this will be 16 */
	public static final int DATA_HEADER_SIZE = 16;
	
	/** header size of the label set - in MINST this will be 8 */
	public static final int LABEL_HEADER_SIZE = 8;
	
	/** width of a single image - could be read from Byte 12...in MINST this will be 28 */
	public static final int IMG_WIDTH = 28;
	
	/** height of a single image - could be read from Byte 8...in MINST this will be 28 */
	public static final int IMG_HEIGHT = 28;
	
	/** size of one image data set */
	private static final int DATASET_SIZE = IMG_WIDTH * IMG_HEIGHT;
	
	/** Path, where the images will be stored, if "extractImages" will be called */
	private static final String OUTPUT_PATH = "C:\\tmp\\";
	
	/** Name base of the stored images, if "extractImages" will be called. The images will be named as described in this method*/
	private static final String IMG_NAME = "mnist";


	/** local variable for holding the data file path */
	private String dataFilePath = "";

	/** local variable for holding the label file path */
	private String labelFilePath = "";

	
	/**
	 * Give the absolute path to the data and label files
	 * @param dataFilePath Path to data file
	 * @param labelFilePath Path to label file
	 */
	public ImageDatasetLoader(String dataFilePath, String labelFilePath) {
		this.dataFilePath = dataFilePath;
		this.labelFilePath = labelFilePath;
	}

	
	/**
	 * Will write the images as *.png files to the path stored in OUTPUT_PATH. The image names
	 * will be structured as followed:
	 * IMG_NAME_i_j.png
	 * where "i" is a subsequent number and "j" is the label according to the image content.
	 * In the MINST dataset this will be 0-9
	 * @param startPosition ID of the first image to extract
	 * @param numberOfImg Number of subsequent images to extract
	 * @return 1 in case of success or -i where i is the image that was outside the data array
	 * @throws IOException
	 */
	public int extractAndSaveImages(int startPosition, int numberOfImg) throws IOException {
		// load the data to byte arrays
		byte[] baData = Files.readAllBytes(Paths.get(dataFilePath));
		byte[] baLabel = Files.readAllBytes(Paths.get(labelFilePath));

		// pointer to the first byte of the current data to extract 
		int bytePosData = 0;
		int bytePosLabel = 0;

		//skip header
		bytePosData += DATA_HEADER_SIZE;
		bytePosLabel += LABEL_HEADER_SIZE;

		// move to position
		bytePosData += DATASET_SIZE * startPosition;
		bytePosLabel += startPosition;

		// iterate through all images to be extracted
		for (int i = 0; i < numberOfImg; i++) {
			
			// prepare array for data of a single image
			byte[][] currentDataSet = new byte[IMG_WIDTH][IMG_HEIGHT];

			// fill the 2-d Array from the data set
			for (int x = 0; x < IMG_WIDTH; x++) {
				for (int y = 0; y < IMG_HEIGHT; y++) {
					// if the current position of the image was out of range
					if (bytePosData < baData.length) {
						currentDataSet[x][y] = baData[bytePosData++];
					} else {
						return -i;
					}
				}
			}

			// convert the data to a buffered image
			BufferedImage myImg = getImage(currentDataSet);

			// write the image to the file system
			File outputfile = new File(OUTPUT_PATH + IMG_NAME + "_" + i + "_" + baLabel[bytePosLabel++] + ".png");
			ImageIO.write(myImg, "png", outputfile);
		}

		return 1;
	}


	/**
	 * Converts the byte data to a buffered image
	 * @param imageData raw image data from the dataset
	 * @return BufferedImage representation
	 */
	private BufferedImage getImage(byte[][] imageData) {
		BufferedImage myImg;
		
		// get graphics context for image creation
		GraphicsConfiguration gfxConf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration();

		// create image with the correct size
		myImg = gfxConf.createCompatibleImage(IMG_WIDTH, IMG_HEIGHT);

		// loop through the raw data and set the pixel accordingly
		for (int x = 0; x < IMG_WIDTH; x++) {
			for (int y = 0; y < IMG_HEIGHT; y++) {
				// convert the data, so that the numbers are black on white background
				int colVal = 255 - imageData[x][y] & 0xFF;

				// set the data. Flip x and y for a 90° mirroring.
				myImg.setRGB(y, x, (new Color(colVal, colVal, colVal, 255)).getRGB());
			}
		}
		return myImg;
	}

	/**
	 * Create a machine readable representation of the data. It will be normalized
	 * to double values between 0.0 and 1.0. 
	 * @return ArrayList of all data or null if an error occurred
	 * @throws IOException
	 */
	public ArrayList<LabeledImage> loadDataSet() throws IOException {
		// load raw data from file system
		byte[] baData = Files.readAllBytes(Paths.get(dataFilePath));
		byte[] baLabel = Files.readAllBytes(Paths.get(labelFilePath));
		ArrayList<LabeledImage> allImages = new ArrayList<>();

		// pointer to the first byte of the current data to extract 
		int bytePosData = 0;
		int bytePosLabel = 0;

		//skip header
		bytePosData += DATA_HEADER_SIZE;
		bytePosLabel += LABEL_HEADER_SIZE;

		// read all data sets from the source file
		while (bytePosData < baData.length) {
			// create array for a single data set
			byte[][] currentDataSet = new byte[IMG_WIDTH][IMG_HEIGHT];

			// extract the data from the data set
			for (int x = 0; x < IMG_WIDTH; x++) {
				for (int y = 0; y < IMG_HEIGHT; y++) {
					if (bytePosData < baData.length) {
						currentDataSet[x][y] = baData[bytePosData++];
					} else {
						return null;
					}
				}
			}

			// insert the data into the list in a "SingleImage" object
			allImages.add(new LabeledImage(Byte.toUnsignedInt(baLabel[bytePosLabel++]), currentDataSet));
		}

		return allImages;
	}
}
