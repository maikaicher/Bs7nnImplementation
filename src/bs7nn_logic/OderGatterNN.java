package bs7nn_logic;

import bs7nn.InconsistentValueException;
import bs7nn.InputNeuron;
import bs7nn.NeuronLayer;
import bs7nn.NeuronalNetwork;
import bs7nn.WorkerFactory;
import bs7nn.WorkerNeuron;

public class OderGatterNN {

	public static void main(String[] args) {
		NeuronalNetwork nn = new NeuronalNetwork(1);

		InputNeuron x0 = nn.createInputNeuron();
		InputNeuron x1 = nn.createInputNeuron();
		
		for (NeuronLayer l : nn.getHiddenLayers()) {
			l.createNeuron(WorkerFactory.RELU);
			l.createNeuron(WorkerFactory.RELU);
		}
		
		WorkerNeuron a = nn.createOutputNeuron(WorkerFactory.RELU);

		double[] weights = null;
		double[][] ref = { { 0 }, { 1 }, { 1 }, { 0 } };
		double[][] inp = { { 0, 0 }, { 0, 1 }, { 1, 0 }, { 1, 1 } };
		double beta = 0.01;
		int noOfEpochs = 1;
		int maxEpochs = 1000000;
		double maxError = 0.001;
		boolean doOptimize = true;

		try {
			nn.doDenseMesh(weights, true, 1);
			while (doOptimize) {
				for (int i = 0; i < inp.length; i++) {
					nn.setInputValues(inp[i]);
					nn.deltaLearn(ref[i], beta);
				}
				double error = 0;
				for (int i = 0; i < inp.length; i++) {
					nn.setInputValues(inp[i]);
					error += Math.pow(ref[i][0] - a.getA(), 2);
				}
				System.out.println(noOfEpochs + " " + error);
				if (error < maxError) {
					System.out.println("Optimum found");
					doOptimize = false;
				} else if (noOfEpochs++ > maxEpochs) {
					System.out.println("Optimum not found");
					doOptimize = false;
				}
				
			}

			for (int i = 0; i < inp.length; i++) {
				nn.setInputValues(inp[i]);
				System.out.println(inp[i][0] + " " + inp[i][1] + " : " + a.getA());
			}
		} catch (InconsistentValueException e) {
			System.out.println(e.getExceptionCause());
			System.exit(1);
		}
	}
}
