package bs7nn_image;

import java.io.IOException;
import java.util.List;

import bs7nn.InconsistentValueException;
import bs7nn.InputNeuron;
import bs7nn.NeuronLayer;
import bs7nn.NeuronalNetwork;
import bs7nn.WorkerFactory;
import bs7nn.WorkerNeuron;

/**
 * Class for processing the MNIST dataset in a neuronal network with 1 Layer of 100 hidden neurons.
 * The following files must be provided:
 * C:\\tmp\\NN\\train-images.idx3-ubyte
 * C:\\tmp\\NN\\train-labels.idx1-ubyte
 * C:\\tmp\\NN\\t10k-images.idx3-ubyte
 * C:\\tmp\\NN\\t10k-labels.idx1-ubyte
 */
public class MnistNN {
	/** list of training data */
	public List<LabeledImage> digits;
	
	/** list of test data */
	public List<LabeledImage> digitsTest;
	
	/** all input neurons in an array. This is mainly for debugging reasons, because the 
	 * handling of the input data is maintained within the NeuronalNetwork class */
	public InputNeuron[] inputs = new InputNeuron[28*28];
	
	/** all output neurons for testing the result quality of the nn */
	public WorkerNeuron[] outputs = new WorkerNeuron[10];
	
	/** the Neuronal Network with one hidden layer */
	public NeuronalNetwork nn = new NeuronalNetwork(1);
	
	public static void main(String[] args) {
		MnistNN mnistNN = new MnistNN();
		try {
			// the training will be continued until we have at least 95% correct identifications
			mnistNN.doTrainNN(0.95);
		} catch (InconsistentValueException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructor reads the data into the data lists and prepares the neuronal network
	 */
	public MnistNN() {
		try {
			// read the data into the lists. Error handling has no focus on this project
			getData();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		// build the input neurons
		for (int i = 0; i < inputs.length; i++) {
			inputs[i] = nn.createInputNeuron();
		}
		
		// build the 100 hidden neurons with a sigmoid activation function
		NeuronLayer l = nn.getHiddenLayers().get(0);
		for (int i = 0; i < 100; i++) {
			l.createNeuron(WorkerFactory.SIGM);
		}

		// build the output neurons with a sigmoid activation function
		for (int i = 0; i < outputs.length; i++) {
			outputs[i] = nn.createOutputNeuron(WorkerFactory.SIGM);
		}
		
		try {
			// build the network with a dense mesh, with bias neurons and a weight reduction to 1/(28*28) in order
			// to avoid too high input values for the activation functions
			nn.doDenseMesh(null, true, 1.0/(28*28));
		} catch (InconsistentValueException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reads the data from the file system.
	 * @throws IOException
	 */
	public void getData() throws IOException {
		// training data will be placed into the labeled images
		ImageDatasetLoader myTrainingLoader = new ImageDatasetLoader("C:\\tmp\\NN\\train-images.idx3-ubyte", "C:\\tmp\\NN\\train-labels.idx1-ubyte");
		digits = myTrainingLoader.loadDataSet();
		
		// test data will be placed into the labeled images
		ImageDatasetLoader myTestLoader = new ImageDatasetLoader("C:\\tmp\\NN\\t10k-images.idx3-ubyte", "C:\\tmp\\NN\\t10k-labels.idx1-ubyte");
		digitsTest = myTestLoader.loadDataSet();
	}

	/**
	 * Trains the network until it reaches the quality of "correctThreshold" percent
	 * @param correctThreshold Value between 0.0 and 1.0 that defines the required identification quality
	 * @throws InconsistentValueException 
	 */
	public void doTrainNN(double correctThreshold) throws InconsistentValueException {
		// because we use simgoid activation this will be a good starting point for the learn step speed
		double beta = 0.01;
		
		// continue until the required quality is reached
		while(correctThreshold > testNN()) {
			// all training images will be processed (MNIST: 60 000)
			for (LabeledImage currImg : digits) {
				// place the data into the input neurons
				nn.setInputValues(currImg.getNormedData());
				
				// the target values must be placed into an array for comparison. If e.g. the image displays 
				// the digit "4", the array must be {0, 0, 0, 0, 1, 0, 0, 0, 0, 0};
				double[] shouldValues = new double[10];
				shouldValues[currImg.label] = 1;
				nn.deltaLearn(shouldValues, beta);
			}
		}
	}
	
	/**
	 * Tests the qualtity of the NN based on the 10 000 test data sets
	 * @return Percentage of how many identifications were correct
	 * @throws InconsistentValueException
	 */
	public double testNN() throws InconsistentValueException {
		int correct = 0;
		int incorrect = 0;
		
		// process every test image
		for (LabeledImage currImg : digitsTest) {
			
			// if the image label is equal to the guess the NN produces, the identfication was correct
			if (currImg.label == getGuess(currImg)) {
				correct++;
			} else {
				incorrect++;
			}
		}
		
		// the output percentage ist the number of correct values divided by the number of tested images
		double percentage = (double)correct / (double)(correct + incorrect);
		System.out.println(percentage);
		
		return percentage;
	}
	
	/**
	 * Returns the position of the output neuron based on the given image
	 * @param myImg Image which should be processed
	 * @return Position of the output neuron with the hightest value
	 * @throws InconsistentValueException
	 */
	public int getGuess(LabeledImage myImg) throws InconsistentValueException {
		// process the data
		nn.resetMe();
		nn.setInputValues(myImg.getNormedData());
		// identify the hightest position
		return getHighestOutputPos();
	}
	
	/**
	 * Identifies the output neuron with the hightest value and returns the position
	 * @return Position of the output neuron with the highest value
	 */
	public int getHighestOutputPos() {
		double maxVal = outputs[0].getA();
		int pos = 0;
		for (int i = 1; i < outputs.length; i++) {
			if (outputs[i].getA() > maxVal) {
				pos = i;
				maxVal = outputs[pos].getA();
			}
		}
		return pos;
	}

}
