package usefulTools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

public class DataLoader {
	
	
	
	private static double mNoiseFactor = 25;
	public static double[][][] loadData (String dataPath)
    {
 	    FileInputStream inImage = null;
        FileInputStream inLabel = null;
        
        String inputImagePath =  dataPath + "/train-images.idx3-ubyte";
        String inputLabelPath =  dataPath + "/train-labels.idx1-ubyte";

        try {
    	    inLabel = new FileInputStream(inputLabelPath);
            inImage = new FileInputStream(inputImagePath);
            Random random = new Random();
            
            int magicNumberImages = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read());
            int numberOfImages = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read());
            int numberOfRows  = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read());
            int numberOfColumns = (inImage.read() << 24) | (inImage.read() << 16) | (inImage.read() << 8) | (inImage.read());

            int magicNumberLabels = (inLabel.read() << 24) | (inLabel.read() << 16) | (inLabel.read() << 8) | (inLabel.read());
            int numberOfLabels = (inLabel.read() << 24) | (inLabel.read() << 16) | (inLabel.read() << 8) | (inLabel.read());
            
            int numberOfPixels = numberOfRows * numberOfColumns;
            
            double[][][] images = new double[10][numberOfImages/8][numberOfPixels]; 
            
            for(int i=0; i<10; i++){
            	images[i][0][0] = 0;
            }
            
            for(int i = 0; i < numberOfImages ; i++) {//numberOfImages
            	
                if(i % 10000 == 0) {System.out.println("Number of images extracted: " + i);}
                double[] imgPixels = new double[numberOfPixels];

                for(int p = 0; p < numberOfPixels; p++) {
                    int gray = inImage.read();
                    int noise = clamp(addGNoise(gray, random));
                    gray = noise;
              
                    imgPixels[p] = (gray + 100)/455.0 * 1.01;
                }
                
                int label1 = inLabel.read();
                images[label1][0][0]++;
                images[label1][(int) images[label1][0][0]] = imgPixels;
                 
            }
 /*           for(int i = 0; i< 10; i++) {
              	System.out.println("label " + i + ":");
          		for(int j=1; j <= images[i][0][0]; j++) {
          			for(int k = 0; k < 784; k++) {
          				System.out.print(images[i][j][k]+" ");
          			}
                  	System.out.println();
                  	}
            	}
*/            
            return images;
            
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
	
	private static int addGNoise(int tr, Random random) {  
        int v, ran;  
        ran = (int)Math.round(random.nextGaussian()*mNoiseFactor);  
        
//        System.out.println(ran);
        
        v = tr + ran;  
        return v;   
    }  
  
    public static int clamp(int p) {  
        return p > 355 ? 355 : (p < -100 ? -100 : p);  
    }  
    
//   public static void main(String[] args) {
//		loadData("Dataset");
//	}
  
}


