
import java.awt.AWTException;
import java.awt.Color;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.RescaleOp;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.github.sarxos.webcam.Webcam;

import java.util.ArrayList;
/**
 * Finds objects of lime green color and puts red square around them.
 * @author      Inderpreet Pabla
 */
public class HandGesture extends JFrame implements Runnable, MouseListener, KeyListener{
	int width,height;
    Webcam webcam;
    int[] pixelRaster = null;
    BufferedImage initialWebcamImage;
    
    String[][] boardPos = new String[][]{{"A","B","C","D","E","F","G","H","-"},
		 {"1","2","3","4","5","6","7","8","-"}
		}; 
    
    String realTimePath = "C:/Users/Pabla/Desktop/ImageAnalysis/Tests/AdvancedCarModel/real_time.png";
    String rawDataFilename = "raw_data.txt";
    
    ServerSocket serverSocket;
    Socket socket;
    String[] gestureTypes = new String[]{"Ack","Fist","Hand","One","Straight", "Palm", "Thumbs", "None", "Swing"};
    boolean connected = false;
    boolean dataCollectionMode = false;
    int imageNumber= 0;
    int maxImageNumber = 500;
    int imageCounter = 0;
    int resetCount = 1;
    
    boolean clicked = false;
    File rawData;
    PrintWriter writer;
    int gestureIndex = 8;
    int currentGestureIndex = 7;
    ArrayList<Integer[]> countQ = new ArrayList<Integer[]>();
    int[] count = new int[10];
    int countIndex = 0;
    
    
    int move = 7;
    int timer = 0;
    int maxTimer = 50;
    int move1 = 7;
    int move2 = 7;
    int move3 = 7;
    int move4 = 7;
    int moveType = 0;
    int pos1= 8;
    int pos2 = 8;
    
    int actionType = 0;
    Point bottomLeftCorner = null, topRightCorner= null;
    Robot robot;
    public static void main(String[] args) throws Exception {
    	HandGesture gesture = new HandGesture(); //make an image analysis object
        Thread thread = new Thread(gesture); //create thread
        thread.start();//start thread
        
    }

    /**
     * Initializes webcam, buffered image, 2D pixel raster and sets up window.
     */
    public HandGesture() {
    	
    	//get default webcam connected to this computer
        webcam = Webcam.getDefault();
        webcam.open(); //open webcam communication
        
        //get webcam dimensions 
        width = webcam.getViewSize().width; 
        height = webcam.getViewSize().height;

        //initialize image buffer and pixel raster initialized according to buffer size
        initialWebcamImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB); 
        pixelRaster = ((DataBufferInt) initialWebcamImage.getRaster().getDataBuffer()).getData();
        
        //window setups
        setSize((width*3), height+50);
        setTitle("Hand Gesture Detection");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(null);
		setVisible(true);

		addMouseListener(this);
		addKeyListener(this);
		
		//if not data collection mode start server
		if(dataCollectionMode == false){
			try {
				robot = new Robot();
			} catch (AWTException e1) {
				e1.printStackTrace();
			}
			try {
				serverSocket = new ServerSocket(12345);
				socket = serverSocket.accept();
				connected = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			rawData = new File(rawDataFilename);
			
			if(!rawData.exists()){
				try {
					rawData.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			try {
				writer = new PrintWriter(rawData);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
    }

    /**
     * Finds lime green colored objects that are detected infront of the camera and draws 
     * red boxes on top of them. It first finds pixels that are in the lime green spectrum,  
     * followed by counting density of found pixel in a given area. Density of over 40 units
     * is then added to a vector of rectangles and each pixel is checked if it's contained 
     * within the previously found rectangles. If it's not then a new rectangle is added to 
     * the location of the new pixel.   
     * @param g		used to draw buffered image along with red rectangles to the screen.
     */
    public void paint(Graphics graphic) {
    	initialWebcamImage = webcam.getImage(); //get image
    	BufferedImage tempInitialWebcamImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB); 
    	
    	//initilize rasters 
    	pixelRaster = new int[width*height];
    	int[] tempRaster = new int[width*height];
    	
    	int[][] pixelRaster2D = new int [height][width]; //converting pixelRaster to 2D format to check for surrounding pixels 
    	int[][] tempRaster2D = new int [height][width];
    	int[][] densityRaster = new int [height][width]; //raster for density
    	int[][] clusterRaster = new int [height][width]; //raster for density
    	int index = 0; //used to access pixelRaster when running through 2D array
    	
    	//get rasters
    	
    	
    	RescaleOp op = new RescaleOp(2f, 0, null);
    	initialWebcamImage = op.filter(initialWebcamImage, initialWebcamImage);
    	initialWebcamImage.getRGB(0, 0, width, height, pixelRaster, 0, width);
    	initialWebcamImage.getRGB(0, 0, width, height, tempRaster, 0, width);
    	BufferedImage newImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
    	
    	index = 0;
    	for(int i = 0;i<height;i++){
    		for(int j = 0;j<width;j++,index++){
    			tempRaster2D[i][j] = pixelRaster[index];
    			
    			int[] color = hexToRGB(pixelRaster[index]); //convert hex arbg to array with rgb 0-255
        		float r = color[0];
        		float g = color[1];
        		float b = color[2];
        		
        		float[] hsb = new float[3];
        		Color.RGBtoHSB((int)r,(int)g,(int)b,hsb);
        		
        		if(hsb[0]<0.15f && hsb[1]>0.2f && hsb[1]<0.63f){
        			pixelRaster2D[i][j] = 0xFFFFFFFF; //if found turn pixel white
        		}
        		else{
        			pixelRaster2D[i][j] = 0xFF000000; //else turn pixel black
        		}
        	}
    	}
    	
    	Vector<Rectangle> listOfFoundObjects = new Vector<Rectangle>(); //list of found objects
    	
    	for(int col = 0 ;col<height;col++){
    		for(int row = 0 ;row<width;row++){
    			//if current pixel is green
    			if(pixelRaster2D[col][row] == 0xFFFFFFFF){	
    				
    				
    				int max = 10;
					int lowY = col-max>=0?col-max:0;
					int highY = col+max<height?col+max:height-1;
					
					int lowX = row-max>=0?row-max:0;
					int highX = row+max<width?row+max:width-1;
    				
					
					for(int i = lowY; i<=highY;i++){
						for(int j = lowX;j<=highX;j++){
							if(pixelRaster2D[i][j] == 0xFFFFFFFF){
	    						densityRaster[i][j]++; //update desnity if pixel found is white
	    						
							}
						}
    				
					}
    			}	
    		}
    	}
    	
    	int minX = 10000;
    	int maxX = -10000;
    	
    	int minY = 10000;
    	int maxY = -10000;

    	for(int col = 0 ;col<height;col++){
    		for(int row = 0 ;row<width;row++){
    			pixelRaster2D[col][row] = 0xFF000000; //make pixel black 
    			
    			//if denisty at this pixel is greater then 40
    			if(densityRaster[col][row]>60){
    				pixelRaster2D[col][row] = 0xFFFFFFFF; //turn this pixel white

    				boolean intersects = false;
    				
					Rectangle rect = new Rectangle(row-7,col-7,14,14); //this pixel's rectangle 
					for(int i = 0;i<listOfFoundObjects.size();i++){ 
						if(rect.intersects(listOfFoundObjects.get(i)) == true){
							intersects = true; //if a rectangle is found, then this pixel needs to ignored
							break;
						}
					}
					if(!intersects){
						listOfFoundObjects.addElement(rect); //if no rectangles are found, then this rectangle can be added to the list
						if(minX>rect.x)
		        			minX = rect.x;
		        		
		        		if(maxX<rect.x + rect.width)
		        			maxX = rect.x + rect.width;
		        		
		        		if(minY>rect.y)
		        			minY = rect.y;
		        		
		        		if(maxY<rect.y + rect.height)
		        			maxY = rect.y + rect.height;
					}		
    			}
    		}	
    	}
    	
    	Rectangle rec = null;
    	
    	if(listOfFoundObjects.size()>0){
    		if(maxX-minX > 100) {
	    		
	    		int diff = (maxX-minX)-100;
	    		int half = diff/2;
	    		minX += half;
	    		maxX -= half;
	    		
	    	}
	    	else if(maxX-minX<100){
	    		int diff = 100-(maxX-minX);
	    		int half = diff/2;
	    		minX -= half;
	    		maxX += half;
	    	}
	    	
	    	
	    	if(maxY-minY > 100) {
	    		
	    		int diff = (maxY-minY)-100;
	    		int half = diff/2;
	    		minY += half;
	    		maxY -= half;
	    		
	    	}
	    	else if(maxY-minY<100){
	    		int diff = 100-(maxY-minY);
	    		int half = diff/2;
	    		minY -= half;
	    		maxY += half;
	    	}
	    	
	    	if(minX<0)
				minX = 0;
			if(minY<0)
				minY = 0;
			
			if(maxX>=width)
				maxX = width - 1;
			if(maxY>=height)
				maxY = height - 1;
			
			rec = new Rectangle(minX,minY,maxX-minX,maxY-minY);
			
			
			for(int col = minY ;col<maxY;col++){
	    		for(int row = minX ;row<maxX;row++){
	    			if(pixelRaster2D[col][row] == 0xFFFFFFFF){
	    				int max = 5;
						int lowY = col-max>=0?col-max:0;
						int highY = col+max<height?col+max:height-1;
						
						int lowX = row-max>=0?row-max:0;
						int highX = row+max<width?row+max:width-1;
	    				
	    				
						
						for(int i = lowY; i<=highY;i++){
							for(int j = lowX;j<=highX;j++){
								clusterRaster[i][j]++;
							}
						}
	    			}
	    		}
	    	}
    		
			for(int col = minY ;col<maxY;col++){
	    		for(int row = minX ;row<maxX;row++){
	    			if(clusterRaster[col][row]>10 && pixelRaster2D[col][row]==0xFF000000){
		    			int[] color = hexToRGB(tempRaster2D[col][row]); 
	    	    		float r = color[0];
	    	    		float g = color[1];
	    	    		float b = color[2];
	    	    		
	    	    		float[] hsb = new float[3];
	    	    		Color.RGBtoHSB((int)r,(int)g,(int)b,hsb);
		    			
		    			if(hsb[0]<0.4f &&  hsb[1]<1f && hsb[2]<0.7f){
	    					pixelRaster2D[col][row] = 0xFFFFFFFF; 
	    	    		}
	    			}
	    		}
	    	}
			
			index = 0;
	    	for(int i = 0;i<height;i++){
	    		for(int j = 0;j<width;j++,index++){
	    			pixelRaster[index] = pixelRaster2D[i][j];
	    			//if(pixelRaster[index] == 0xFFFFFFFF)
	    				
	    				//pixelRaster[index] = /*pixelRaster2D[i][j]*/tempRaster[index];
	    		}
	    	}
			
			
	    	initialWebcamImage.setRGB(0, 0, width, height, pixelRaster, 0, width);
			BufferedImage crop = cropImage(initialWebcamImage,rec);

			
			
        	Graphics2D g = newImage.createGraphics();
        	g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        	    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        	g.drawImage(crop, (int)0, (int)0, (int)50, (int)50, (int)0, (int)0, crop.getWidth(),
        			crop.getHeight(), null);
        	g.dispose();
    	}
    	
    	
    	if(rec == null){
    		Graphics g2 = newImage.getGraphics();

        	g2.setColor(Color.black);
        	g2.fillRect(0,0,50,50);
        	g2.dispose();
    		
    	}
    	
    	if(dataCollectionMode==true && clicked==true){
    		
    		
    		if(imageNumber>=maxImageNumber){
    			clicked =false;
    			writer.close();
    		}
    		else{
        		if(imageCounter == resetCount){
        			System.out.println(imageNumber);
        			int i = 0;
        			for(;i<gestureIndex;i++){
        				if(i == gestureTypes.length-1)
        					writer.print("0");
        				else
        					writer.print("0 ");
        			}
        			
        			if(i == gestureTypes.length-1){
        				writer.print("1");
        				i++;
        			}
        			else{
        				
        				writer.print("1 ");
        				i++;
        				for(;i<gestureTypes.length;i++){
	        				if(i == gestureTypes.length-1)
	        					writer.print("0");
	        				else
	        					writer.print("0 ");
	        			}
        				
        			}
        			
        			try {
	             		   
            		    File outputfile = new File(imageNumber+".png");
            		    ImageIO.write(newImage, "png", outputfile);
            		} catch (IOException e) {}
        			
        			
        			imageCounter = 0;
        			imageNumber++;
        			
        			if(imageNumber < maxImageNumber){
        				writer.print("\n");
        			}
        			
        			
        			
        		}

        		
        		imageCounter++;
    		}
    	}
    	
    	
    	
    	
    	
    	
    	if(connected == true){
    		try {
      		   
    		    File outputfile = new File(realTimePath);
    		    ImageIO.write(newImage, "png", outputfile);
				
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());

				out.writeFloat(1.23f);
				
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String number = br.readLine();
                
                String[] str = number.split(" ");
                int highestIndex = -1;
                int highestValue = -1;
                for(int i = 0; i<str.length;i++){
                	try{
                		int f  = (int)(100f*Float.parseFloat(str[i]));
                		//System.out.print(f+" ");
                		
                		if(f>highestValue){
                			highestValue = f;
                			highestIndex = i;
                		}
                	}
                	catch(Exception e){
                		System.out.println("asdsd");
                	}
              
                }

                int[] countCheck = new int[9];

                countQ.add(new Integer[]{highestIndex,highestValue});
                if(countQ.size()>10)
                	countQ.remove(0);
                
                float factor = 1f;
                for(int i =countQ.size()-1;i>=0;i--){
                	countCheck[countQ.get(i)[0]]+= (float)countQ.get(i)[1]/factor;
                	factor *= 1f;
                }
                
                int correctIndex = -1;
                int value = 0;
                for(int i =0;i<countCheck.length;i++){
                	if(value<countCheck[i]){
                		correctIndex= i;
                		value = countCheck[i];
                	}
                }
                currentGestureIndex = correctIndex;
                
                Font myFont = new Font ("Courier New", Font.BOLD, 30);
                graphic.setFont (myFont);
                
                graphic.setColor(Color.white);
                graphic.fillRect(0, height+51,5000,5000);
                graphic.setColor(Color.red);
                graphic.drawString(gestureTypes[correctIndex]+" "+(int)(100f*Float.parseFloat(str[highestIndex])), 10, height+50+50);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
    	}

    	
    	//graphic.drawString(boardPos[0][pos1]+""+boardPos[1][pos2], 10, height+50+50+30);
    	
    	tempInitialWebcamImage.setRGB(0, 0, width, height, tempRaster, 0, width);
    	
    	graphic.drawImage(initialWebcamImage, 0, 0, null);
    	graphic.drawImage(tempInitialWebcamImage, width, 0, null);
    	
    	graphic.setColor(Color.green);
    	for(int i = 0;i<listOfFoundObjects.size();i++){
    		Rectangle rect = listOfFoundObjects.get(i);
    		graphic.drawRect(rect.x,rect.y,rect.width,rect.height);
		}

    	graphic.drawImage(newImage, 0, height, null);
    	if(rec!=null){
    		graphic.setColor(Color.red);
    		graphic.drawRect(rec.x,rec.y,rec.width,rec.height);
    	}
    	
    	
    }
    
    
    private BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
        return dest; 
    }
    

	@Override
	public void run() {
		while(true){
			
			try {
				Thread.sleep(25);
				repaint();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}		
			
			if(move!=currentGestureIndex && topRightCorner!=null){
				move = currentGestureIndex;
				
				System.out.println(move);
				if(moveType == 0 && move!=7)
				{
					move1 = move;
					moveType++;
				}
				else if(moveType == 1 && move!=7)
				{
					move1 = move;
				}
				if(moveType == 1 && move==7)
				{
					move2 = move;
					moveType++;
				}
				if(moveType == 2 && move!=7)
				{
					move3 = move;
					moveType++;
				}
				else if(moveType == 3 && move!=7)
				{
					move3 = move;
				}
				if(moveType == 3 && move==7)
				{
					move4 = move;
					moveType++;
				}
				
				
				
				if(moveType == 4){
					moveType = 0;
					if(move1!=7 && move3!=7 && move2==7 && move4==7){
						
						int oldPos1 = pos1;
						int oldPos2 = pos2;
						
						pos1= move1;
				    	pos2 = move3;
				    	
				    	if(pos1==7)
				    		pos1 = 8;
				    	else if (pos1 == 8)
				    		pos1 = 7;
				    	
				    	if(pos2==7)
				    		pos2 = 8;
				    	else if (pos2 == 8)
				    		pos2 = 7;
				    	
				    	
				    	
				    	actionType++;
				    	
				    	if(actionType == 2){
				    		System.out.println(boardPos[0][oldPos1]+""+boardPos[1][oldPos2]+" TO "+boardPos[0][pos1]+""+boardPos[1][pos2]);
				    		actionType = 0;
				    		
				    		int diff = Math.abs(topRightCorner.x-bottomLeftCorner.x);
				    		int size = diff/8;
				    		
				    		
				    		
				    		go(oldPos1*size + (size/2) + bottomLeftCorner.x, bottomLeftCorner.y-(oldPos2*size + (size/2)));
				    		click();
				    		go(pos1*size + (size/2)+ bottomLeftCorner.x, bottomLeftCorner.y- (pos2*size + (size/2)));
				    		click();
				    	}
					}
				}
				
			}
		}
		
	}
	
	
	public void go(int x, int y){
		robot.mouseMove(x, y);
		try {
			Thread.sleep(5);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void click()
	{
		robot.mousePress(InputEvent.BUTTON1_MASK);
		try {
			Thread.sleep(5);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
		try {
			Thread.sleep(5);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
	}
	
	
	/**
	 * Converts hex to integer array which contains red, green, and blue color of 0-255.
     * @param rgbHex integer in format of 0xAARRGGBB, A = alpha, R = red, G = green, B = blue
     */
	public int[] hexToRGB(int argbHex){
		int[] rgb = new int[3];
    	rgb[0] = (argbHex & 0xFF0000) >> 16; //get red
    	rgb[1] = (argbHex & 0xFF00) >> 8; //get green
    	rgb[2] = (argbHex & 0xFF); //get blue
		return rgb;//return array
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		clicked = true;	
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		if(dataCollectionMode == false){
			if(bottomLeftCorner == null){
				bottomLeftCorner= MouseInfo.getPointerInfo().getLocation();
				System.out.println(bottomLeftCorner.x+" "+bottomLeftCorner.y);
			}
			else if(topRightCorner==null){
				topRightCorner= MouseInfo.getPointerInfo().getLocation();
				System.out.println(topRightCorner.x+" "+topRightCorner.y);
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}