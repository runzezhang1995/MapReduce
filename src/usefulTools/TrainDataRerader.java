package usefulTools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;



public class TrainDataRerader {
	
	static public int[][] transformInputData(String dataPath) {
		FileInputStream inImage = null;
        FileInputStream inLabel = null;
        java.io.PrintStream outFile = null; 
        
        String inputImagePath =  dataPath + "/train-images.idx3-ubyte";
        String inputLabelPath =  dataPath + "/train-labels.idx1-ubyte";

        String outputPath = "./Image";

        int[] hashMap = new int[10]; 

        try {
    	    inLabel = new FileInputStream(inputLabelPath);
            inImage = new FileInputStream(inputImagePath);
            outFile = new java.io.PrintStream( "inputData.txt" );
            
            
            
            int magicNumberImages = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read());
            int numberOfImages = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read());
            int numberOfRows  = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read());
            int numberOfColumns = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read());

            int magicNumberLabels = (inLabel.read() << 24) | (inLabel.read() << 16) | (inLabel.read() << 8) | (inLabel.read());
            int numberOfLabels = (inLabel.read() << 24) | (inLabel.read() << 16) | (inLabel.read() << 8) | (inLabel.read());

            BufferedImage image = new BufferedImage(numberOfColumns, numberOfRows, BufferedImage.TYPE_INT_ARGB);
            
            int numberOfPixels = numberOfRows * numberOfColumns;
            int[] imgPixels = new int[numberOfPixels];
            for(int i = 0; i < numberOfImages; i++) {
                for(int p = 0; p < numberOfPixels; p++) {
                    int gray = 255 - inImage.read();
                    outFile.print(gray + ",");
                }
                int label = inLabel.read();
                outFile.println(label);
                

            }
            return null;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (inImage != null) {
                try {
                    inImage.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (inLabel != null) {
                try {
                    inLabel.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
		return null;
		
		
		
		
	}
	
	public static void main(String[] args)
	{
		transformInputData("DataSet");
	}
	
}
