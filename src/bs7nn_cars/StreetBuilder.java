package bs7nn_cars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import bs7nn.InconsistentValueException;


/**
 * GUI class for testing the CarNN. It allows to draw a street and send self learning "cars"
 * through this street. The programm can also save and load streets. A street is defined by
 * points that connect to a closed loop
 */
public class StreetBuilder extends JFrame implements ActionListener {
	/** a street is defined as "usable" if it has at least this amount of points */
	public static final int MIN_NO_OF_STREET_POINTS = 4;
	
	/** JPanel where to draw the image */
	private DrawField drawField;
	
	/** Button to open a file dialog for loading street data from the file system */
	private JButton openStreet;
	
	/** Button to open a file dialog for saving street data to the file system */
	private JButton saveStreet;

	/** removes the street from the drawing area */
	private JButton clearStreet;
	
	/** starts or stopps the driving/learning process */
	private JButton startStoppProcess;
	
	/** resets the nn and the car values */
	private JButton resetValues;

	/** here some learning parameters are displayed */
	private JLabel infoField;

	/** for opening/saving files */
	private JFileChooser fileChooser;

	/** 
	 * Start the dialog
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new StreetBuilder();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Preparation of the GUI and the NN
	 * @throws IOException 
	 * @throws InconsistentValueException 
	 */
	public StreetBuilder() throws IOException, InconsistentValueException {
		// init gui
		initialize();
		setElements();
		setVisible(true);
	}
	
	/** 
	 * all necessary JFrame initializations 
	 */
	private void initialize() {
		setTitle("car NN");
		setSize(new Dimension(1200, 800)); 
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new GridBagLayout());
		prepareFileChooser();
	}
	
	/**
	 * Creates the file chooser and sets the filter selection to csv files
	 */
	private void prepareFileChooser() {
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Strassendaten", "csv");
		fileChooser = new JFileChooser();
		fileChooser.addChoosableFileFilter(filter);
	}
	
	/**
	 * Placement of all elements including draw field
	 * @throws InconsistentValueException 
	 */
	private void setElements() throws InconsistentValueException {
		// placement of the infos
		add(infoField =   new JLabel(" "),             getConstraints(0, 0, 5,  1, 1, 1));
		add(openStreet =  new JButton("open"),         getConstraints(1, 0, 1,  1, 1, 1));
		add(saveStreet =  new JButton("save"),         getConstraints(2, 0, 1,  1, 1, 1));
		add(clearStreet = new JButton("clear"),         getConstraints(3, 0, 1,  1, 1, 1));
		add(startStoppProcess = new JButton("start"),  getConstraints(4, 0, 1,  1, 1, 1));
		add(resetValues = new JButton("reset"),        getConstraints(5, 0, 1,  1, 1, 1));
		add(drawField = new DrawField(this),           getConstraints(0, 1, 8, 80, 6, 1));

		// this class is also the action listener
		openStreet.addActionListener(this);
		saveStreet.addActionListener(this);
		clearStreet.addActionListener(this);
		startStoppProcess.addActionListener(this);
		resetValues.addActionListener(this);
		
		// disable the buttons that are not useful at the beginning
		startStoppProcess.setEnabled(false);
		resetValues.setEnabled(false);
		saveStreet.setEnabled(false);
	}
	
	/**
	 * Setter of the display text
	 * @param value display text
	 */
	public void setDisplayValue(String value) {
		infoField.setText(value);
	}
	
	/**
	 * Helper for selecting a file and sending the absolute path to the caller
	 * @return Absolute path of the selected file or ""
	 */
	public String openFile() {
		int openResult = fileChooser.showOpenDialog(this);
		if (openResult == JFileChooser.APPROVE_OPTION) {
			File myFile = fileChooser.getSelectedFile();
			return myFile.getAbsolutePath();
		}
		return "";
	}
	
	/**
	 * Helper for saving a file and sending the absolute path to the caller
	 * @return Absolute path of the file or ""
	 */
	public String saveFile() {
		int openResult = fileChooser.showSaveDialog(this);
		if (openResult == JFileChooser.APPROVE_OPTION) {
			File myFile = fileChooser.getSelectedFile();
			return myFile.getAbsolutePath();
		}
		return "";
	}
	
	/**
	 * Adds a car to the display field car list
	 * @param car new car
	 */
	public void addCar(Car car) {
		drawField.addCar(car);
	}
	

	/**
	 * Helper method for easier GridBag Constraints handling
	 * @param gridx Position x
	 * @param gridy Position y
	 * @param weightx weight of x column
	 * @param weighty weight of y row
	 * @param gridwidth width of current column
	 * @param gridheight height of current row
	 * @return Constraints with fill set to "both"
	 */
	private GridBagConstraints getConstraints(int gridx, int gridy, double weightx, 
			double weighty, int gridwidth, int gridheight) {
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = gridx;
			gbc.gridy = gridy;
			gbc.weightx = weightx;
			gbc.weighty = weighty;
			gbc.gridwidth = gridwidth;
			gbc.gridheight = gridheight;
			gbc.fill = GridBagConstraints.BOTH;
			return gbc;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		// if the drawn street should be removed
		if (e.getSource() == clearStreet) {
			try {
				// clear the drawing field
				drawField.clearImage();
			} catch (InconsistentValueException e1) {
				e1.printStackTrace();
			}
		// if the current street should be saved to disk
		} else if (e.getSource() == saveStreet) {
			drawField.writeToFile(saveFile());
		// if the nn values should be resetted
		} else if (e.getSource() == resetValues) {
			try {
				drawField.carBuilder.resetNNData();
				repaint();
			} catch (InconsistentValueException e1) {
				e1.printStackTrace();
			}
		// if the process should be started/stopped
		} else if (e.getSource() == startStoppProcess) {
			// if the timer is running, the timer can be stopped
			if (drawField.timerIsRunning()) {
				// the button has now the "start" functionality
				startStoppProcess.setText("start");
				resetValues.setEnabled(true);
				drawField.stoppTimer();
				activateStreetButtons(false);
			} else {
				// if the timer is not running, it should be started
				if(drawField.startTimer()) {
					// the button has now the "stop" functionality
					startStoppProcess.setText("stop");
					resetValues.setEnabled(false);
					activateStreetButtons(true);
				}
			}
		// if a previously saved street should be openeed
		} else if (e.getSource() == openStreet) {
			String filePath = openFile();
			if (!filePath.equals("")) {
				drawField.readFromFile(filePath);
				try {
					drawField.finalizeStreet();
					startStoppProcess.setEnabled(true);
				} catch (InconsistentValueException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * Sets the open/clear/save buttons active/inactive. A street can only be saved, if the number of points are above MIN_NO_OF_STREET_POINTS
	 * @param isActive
	 */
	private void activateStreetButtons(boolean isActive) {
		clearStreet.setEnabled(!isActive);
		openStreet.setEnabled(!isActive);
		saveStreet.setEnabled(!isActive && drawField.street.getAllPoints().size() > MIN_NO_OF_STREET_POINTS);
	}
	
	/**
	 * Public method for enabling the start stop button 
	 * @param doActivate true if button should be enabled
	 */
	public void activateStartStoppButton(boolean doActivate) {
		startStoppProcess.setEnabled(doActivate);
	}
	/**
	 * JPanel field for drawing and extracting the image
	 */
	public static class DrawField extends JPanel implements MouseMotionListener, MouseListener {
		/** x position of the display circle */
		private int circleX = -10;
		
		/** y position of the display circle */
		private int circleY = -10;
		
		/** reference to the street object */
		private Street street;
		
		/** physic timer triggers the movement and learing process */
		private PhysicTimer timer = null;
		
		/** reference to the JFrame */
		private StreetBuilder parent;
		
		/** the car builder cares for the car and parameter creation */
		public CarBuilder carBuilder = null;

		/** all cars that should be drawn must be placed in this array. Do not use the car array
		 * of the car builder because it will be reordered for finding the best car */
		private ArrayList<Car> carsToDraw = new ArrayList<>();

		/**
		 * Adds a new car to the list
		 * @param car new car
		 */
		public void addCar(Car car) {
			carsToDraw.add(car);
		}
		
		/**
		 * Removes all cars from the display list
		 */
		public void resetCars() {
			carsToDraw.clear();
		}
		
		/**
		 * Setter of the text info - will delegate the call to the JFrame
		 * @param info New text info
		 */
		public void setTextInfo(String info) {
			parent.setDisplayValue(info);
		}
		
		/**
		 * Basic preparations
		 * @throws InconsistentValueException 
		 */
		public DrawField(StreetBuilder parent) throws InconsistentValueException {
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
			street = new Street();
			this.parent = parent;
		}
		
		/**
		 * This method cares for the timer creation and the start of the thread
		 * @return false if the timer could not have been started
		 */
		public boolean startTimer() {
			// if the street is still editable, it should be finalized first
			if (street.isEditable()) {
				// if there are too less points, the process should not start
				if (street.getAllPoints().size() > MIN_NO_OF_STREET_POINTS) {
					try {
						finalizeStreet();
					} catch (InconsistentValueException e) {
						e.printStackTrace();
					}
				} else {
					return false;
				}
			}
			// now start the timer
			if (timer == null) {
				timer = new PhysicTimer(this);
			}
			new Thread(timer).start();
			return true;
		}
		
		/**
		 * Stopps the timer class
		 */
		public void stoppTimer() {
			timer.doStop();
		}
		
		/**
		 * Checks, if the timer is running
		 * @return true if the timer is still running
		 */
		public boolean timerIsRunning() {
			return timer != null && timer.isRunning();
		}
		
		/**
		 * The paintComponent will check if myImg is null. If so, it will be created with this method.
		 * @throws InconsistentValueException 
		 */
		public void clearImage() throws InconsistentValueException {
			street.resetStreet();
			carBuilder.resetCarData();
			repaint();
		}
		
		/**
		 * Triggers the street optimization (reduction of points and closing of the loop). It will also create a
		 * car builder if it is not already existent. If so, the car data will be reseted so the cars will start
		 * at the beginning of the loop
		 * @throws InconsistentValueException
		 */
		public void finalizeStreet() throws InconsistentValueException {
			street.finalizeStreet();
			if (carBuilder == null) {
				carBuilder = new CarBuilder(street, this);
			} else {
				carBuilder.resetCarData();
			}
			repaint();
		}


		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2D = (Graphics2D) g;
			
			g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			
			// draw the street 
			g2D.drawImage(street.getImage(getSize()), null, 0, 0);

			// now place the middle points of the street
			g2D.setColor(Color.white);
			for (int[] point : street.getAllPoints()) {
				g2D.setColor(Color.white);
				g2D.drawOval(point[0] - 2, point[1] - 2, 2, 2);
			}

			// the start line is drawn as small line. The street has also a bigger one in red.
			if (street.hasStartLine()) {
				g2D.drawLine(street.getStartX0(), street.getStartY0(), street.getStartX1(), street.getStartY1());
			}
			
			// as long as the user wants to edit the street, the draw circle must be shown
			if (street.isEditable()) {
				// now draw the display circle for better orientation
				g2D.setColor(Color.red);
				g2D.drawOval(circleX - Street.RADIUS, circleY - Street.RADIUS, 2 * Street.RADIUS, 2 * Street.RADIUS);
			} else {
				// if the street is not editable, the cars must be shown
				Car bestCar = null;
				for (Car car : carsToDraw) {
					// the best car will be drawn in red
					if (car.isBest()) {
						bestCar = car;
					} else {
						g2D.setColor(Color.yellow);
						g2D.fillPolygon(car.getPolygon());
						g2D.setColor(Color.red);
						g2D.drawPolygon(car.getPolygon());
					}
					// uncomment, if you want to see the seek lines
					/*
					g2D.setColor(Color.red);
					int[] line = car.setDistLeft();
					g2D.drawLine(line[0], line[1], line[2], line[3]);
					line = car.setDistFront();
					g2D.drawLine(line[0], line[1], line[2], line[3]);
					line = car.setDistRight();
					g2D.drawLine(line[0], line[1], line[2], line[3]);
					*/
				}
				
				// the last car to be drawn is the "best car" in red
				if (bestCar != null) {
					g2D.setColor(Color.red);
					g2D.fillPolygon(bestCar.getPolygon());
					g2D.drawPolygon(bestCar.getPolygon());
					
				}
			}
		}
		
		
		@Override
		public void mouseDragged(MouseEvent e) {
			// The drawing circle position is always on the mouse position
			circleX = e.getX();
			circleY = e.getY();
			
			// now add the point to the street. The later optimization process might remove it again.
			street.addPoint(e);
			
			// the start/stop button is determined on the fact that the number of points should not be below MIN_NO_OF_STREET_POINTS
			parent.activateStartStoppButton(street.getAllPoints().size() > MIN_NO_OF_STREET_POINTS);
			repaint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			// The drawing circle position is always on the mouse position
			circleX = e.getX();
			circleY = e.getY();
			repaint();
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// here the new line will begin - the rest is processed in the "mouseDragged" 
			// method
			street.addPoint(e);
			parent.activateStartStoppButton(street.getAllPoints().size() > MIN_NO_OF_STREET_POINTS);
			repaint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// start displaying the circle where the mouse arrow enters the JPanel
			circleX = e.getX();
			circleY = e.getY();
			repaint();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// place the circle to a blind spot when the mouse exited
			circleX = -Street.RADIUS;
			circleY = -Street.RADIUS;
		}

		/**
		 * Triggers a single step (movement including repaint)
		 * @param time Time will be called with a fixed value because it is a simulation
		 * @throws InconsistentValueException
		 */
		public void doStep(double time) throws InconsistentValueException {
			// if the car builder is existing and not calculating new values the process can begin
			if (carBuilder != null && !carBuilder.iAmBusy()) {
				// flag for indicating that there are no cars active
				boolean nextGeneration = true;
				// now check all cars
				for (Car car : carBuilder.getCars()) {
					// if at least one car is moving, the next generation can not be built
					if (car.isMoving()) {
						nextGeneration = false;
						// Because it is a simluation the loop time is fixed to 10 ms
						if (!car.move(10)) {
							// if the move method returns false, the car is considered to be crashed
							car.carCrashed();
						}
					}
				}
				// print only, if the car is not calculating new values
				if (carBuilder.okToPrint()) {
					repaint();
				}
				
				// if the next generation can be built the car builder will be triggered
				if (nextGeneration) {
					carBuilder.prepareNextGeneration();
				}
			}
		}	
		
		/**
		 * Stopps the loop
		 */
		public void doStop() {
			timer.doStop();
		}

		/**
		 * for delegating the call to the street
		 * @param fileName absolute path
		 */
		public void writeToFile(String fileName) {
			street.writeToFile(fileName);
		}
		
		/**
		 * For delgating the call to the street
		 * @param fileName absolute path
		 */
		public void readFromFile(String fileName) {
			street = new Street(fileName, getSize());
			repaint();
		}
	}

}
