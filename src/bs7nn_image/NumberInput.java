package bs7nn_image;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bs7nn.InconsistentValueException;

/**
 * Allows the drawing of an image and process the result with the MnistNN Network
 */
public class NumberInput extends JFrame implements ActionListener {
	/** JPanel where to draw the image */
	private DrawField drawField;
	
	/** Button for checking the image against NN */
	private JButton checkImage;
	
	/** clear drawn image */
	private JButton clearImage;
	
	/** Button for starting the learn algorithm NN */
	private JButton startLearnNN;
	
	/** Info, which digit the NN has identified */
	private JLabel guessedDigit; 
	
	/** NN which processes the image */
	private MnistNN nn;
	
	public static void main(String[] args) throws IOException {
		new NumberInput();
	}
	
	/**
	 * Preparation of the GUI
	 */
	public NumberInput() throws IOException {
		nn = new MnistNN();
		
		initalize();
		setElements();
		
		setVisible(true);
	}
	
	/**
	 * Placement of all elements including draw field
	 */
	private void setElements() {
		add(guessedDigit = new JLabel(" "),      getConstraints(0, 0, 8, 1, 1, 1));
		add(clearImage = new JButton("cl"),      getConstraints(1, 0, 1, 1, 1, 1));
		add(startLearnNN = new JButton("lrn"),   getConstraints(2, 0, 1, 1, 1, 1));
		add(checkImage = new JButton("ck"),      getConstraints(3, 0, 1, 1, 1, 1));
		add(drawField = new DrawField(),         getConstraints(0, 1, 11, 4, 4, 1));
		
		clearImage.addActionListener(this);
		startLearnNN.addActionListener(this);
		checkImage.addActionListener(this);
	}
	
	/** 
	 * all necessary JFrame initialisations 
	 */
	private void initalize() {
		setTitle("draw number");
		setSize(new Dimension(350, 400));
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new GridBagLayout());
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
	private GridBagConstraints getConstraints(
			int gridx, 
			int gridy, 
			double weightx, 
			double weighty, 
			int gridwidth, 
			int gridheight
	) {
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
		// to clear the draw area
		if (e.getSource() == clearImage) {
			drawField.clearImage();
			
		// to check the image drawn by the user
		} else if (e.getSource() == checkImage) {
			// the labeled image is created. The file path is only for writing
			// the image to the file system and later check by the user
			LabeledImage myImg = drawField.getImage("C:/tmp/nn/myDrawing.png");
			if (myImg != null) {
				try {
					// NN tries to identify the drawn image
					guessedDigit.setText(" Guess:" + nn.getGuess(myImg));
				} catch (InconsistentValueException e1) {
					e1.printStackTrace();
					guessedDigit.setText("Fehler nn");
				}
			} else {
				guessedDigit.setText("Fehler inp.");
			}
			
		// starts to learn based on the MNIST data. It is not placed into an own
		// thread, so the GUI will be freezed for the process
		} else if (e.getSource() == startLearnNN) {
			try {
				// learn until a quality of 97% is reached
				nn.doTrainNN(0.97);
			} catch (InconsistentValueException e1) {
				e1.printStackTrace();
			}
			// info, that the training process is done
			guessedDigit.setText("Done!");
		}
	}
	
	/**
	 * JPanel field for drawing and extracting the image
	 */
	public static class DrawField extends JPanel implements MouseMotionListener, MouseListener {
		/** draw radius of the mouse pointer */
		private static final int RADIUS = 10;
		
		/** Displayed image */
		private BufferedImage myImg = null;
		
		/** last x position of the mouse */
		private int oldX = 0;
		
		/** last y position of the mouse */
		private int oldY = 0;
		
		/** x position of the display circle */
		private int circleX = -RADIUS;
		
		/** y position of the display circle */
		private int circleY = -RADIUS;
		
		/**
		 * Basic preparations
		 */
		public DrawField() {
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
		}
		
		/**
		 * Clears the drawing field
		 */
		public void clearImage() {
			myImg = null;
			repaint();
		}
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			// in case of the first call of this method, the
			// buffered image must be initialized with the width and height of
			// the JPanel
			if (myImg == null) { 
				Dimension dim = getSize();
				myImg = generateImage(dim.width, dim.height);
			}

			// now draw the image
			Graphics2D g2D = (Graphics2D) g;
			g2D.drawImage(myImg, null, 0, 0);
			
			// here the mouse pointer will be set to a circle
			g2D.setColor(Color.red);
			g2D.drawOval(circleX - RADIUS, circleY - RADIUS, 2*RADIUS, 2*RADIUS);
		}
		
		/**
		 * Helper method for creating a blank image with the given size
		 * @param width width of the image
		 * @param height height of the image
		 * @return created image
		 */
		private BufferedImage generateImage(int width, int height) {
			BufferedImage myImage;
			GraphicsConfiguration gfxConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
			myImage = gfxConfig.createCompatibleImage(width, height);
			Graphics2D g = myImage.createGraphics();
			g.setBackground(Color.white);
			g.clearRect(0, 0, width, height);
			g.dispose();
			return myImage;
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// remember the point in order to start the first line element here
			oldX = e.getX();
			oldY = e.getY();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// place the circle at the border of the JPanel
			circleX = e.getX();
			circleY = e.getY();
			repaint();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// hide the circle
			circleX = -RADIUS;
			circleY = -RADIUS;
			repaint();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			// now draw the line according to the mouse movement to the buffered image
			Graphics2D g = myImg.createGraphics();
			g.setColor(Color.black);
			g.setStroke(new BasicStroke(2.0f * RADIUS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			// draw the line from the old position to the current position
			g.drawLine(oldX, oldY, e.getX(), e.getY());
			
			
			// remember the old position for the next partial line
			oldX = e.getX();
			oldY = e.getY();
			
			// replace the circle
			circleX = e.getX();
			circleY = e.getY();
			repaint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			// replace the circle
			circleX = e.getX();
			circleY = e.getY();
			repaint();
		}

		/**
		 * Reads the displayed image, blurs it , resizes it to IMG_SIZE and returns it.
		 * Will be null in case of error
		 * @param imgName Name in case of saving the file
		 * @return LabeledImage Labeled image for later processing
		 */
		public LabeledImage getImage(String imgName) {
			// first find the borders of the image
			int maxX = 0;
			int minX = myImg.getWidth();
			int maxY = 0;
			int minY = myImg.getHeight();
			
			// basic values for manipulation
			int whiteColor = Color.WHITE.getRGB();
			int offset = 30;
			
			// find minX
			for (int y = 0; y < myImg.getHeight(); y++) {
				for (int x = 0; x < minX; x++) {
					if (myImg.getRGB(x, y) < whiteColor) {
						minX = x;
					}
				}
			}
			
			// find maxX
			for (int y = 0; y < myImg.getHeight(); y++) {
				for (int x = myImg.getWidth() - 1; x >= maxX; x--) {
					if (myImg.getRGB(x, y) < whiteColor) {
						maxX = x;
					}
				}
			}
			
			// find minY
			for (int x = 0; x < myImg.getWidth(); x++) {
				for (int y = 0; y < minY; y++) {
					if (myImg.getRGB(x, y) < whiteColor) {
						minY = y;
					}
				}
			}
			
			// find maxY
			for (int x = 0; x < myImg.getWidth(); x++) {
				for (int y = myImg.getHeight() - 1; y >= maxY; y--) {
					if (myImg.getRGB(x, y) < whiteColor) {
						maxY = y;
					}
				}
			}
			
			// simple error handling
			if (minX >= maxX) {
				return null;
			}
			if (minY >= maxY) {
				return null;
			}
			
			// now the new image will be prepared. Therefore the image must be
			// centered and setup with an offset
			int newWidth = maxX - minX;
			int newHeight = maxY - minY;
			
			// Place an offset to the size
			int newSize = Math.max(newWidth, newHeight) + 2*offset;
			
			// now the identified image is cut out of the existing image
			BufferedImage subImg = myImg.getSubimage(minX, minY, newWidth, newHeight);
			BufferedImage tmpImg = generateImage(newSize, newSize);
			Graphics2D g = tmpImg.createGraphics();
			
			// calculate the offset x/y in order to center the image
			int posX = (newSize - newWidth) / 2;
			int posY = (newSize - newHeight) / 2;
			
			// place the new image to the center of the newly created image
			g.drawImage(subImg,  null,  posX,  posY);
			g.dispose();
			
			// now write the image, so the user can use the file for test as well
			File outFile = new File(imgName);
			try {
				ImageIO.write(scaleImage(blurImage(tmpImg, 3), 28, 28), "png", outFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new LabeledImage(-1, scaleImage(blurImage(tmpImg, 3), 28, 28), true);
		}

		/**
		 * Scale the image to a new size
		 * @param original Original image to scale
		 * @param newWidth New width of the image
		 * @param newHeight New height of the image
		 * @return Scaled image
		 */
		private BufferedImage scaleImage(BufferedImage original, int newWidth, int newHeight) {
			// create a new image with a new size of the same type than the original
			BufferedImage resized = new BufferedImage(newWidth, newHeight, original.getType());
			Graphics2D g = resized.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			
			// now draw a resized image of the old imagt to the new one
			g.drawImage(original, 0, 0, newWidth, newHeight, 0, 0, original.getWidth(), original.getHeight(), null);
			g.dispose();
			return resized;
		}
		
		/**
		 * Blur effect in order to align the behavior to the MINST data set.
		 * Be aware that the edge will not be blurred, so the image needs an offset.
		 * @param original Image to blur
		 * @param radius Blur radius
		 * @return blurred image
		 */
		public BufferedImage blurImage(BufferedImage original, int radius) {
			// standard procedure for usage of ConvolveOp
			int size = radius * 2 + 1;
			float weight = 1.0f / (size * size);
			float[] data = new float[size * size];
			for (int i = 0; i < data.length; i++) {
				data[i] = weight;
			}
			Kernel kernel = new Kernel(size, size, data);
			ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
			BufferedImage blurImg = op.filter(original, null);
			
			for (int y = 0; y < blurImg.getHeight(); y++) {
				for (int x = 0; x < blurImg.getWidth(); x++) {
					if (blurImg.getRGB(x, y) == 0xfffefefe) {
						blurImg.setRGB(x, y, 0xffffffff);
					}
				}
			}
			return blurImg;
		}
	}
}