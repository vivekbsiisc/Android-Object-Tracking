package MeanShift;

import java.awt.FlowLayout;
import java.awt.Image;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class MeanShift_Main {

	
	
public static void main(String args[]) throws InterruptedException
{
	
	String ImgPath="D:/vivek/AIP final project/Java_workspace/ball/scene0";
	System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

	  int Nbins = 6;
	  int[] ROI_Center ={123,222}; 
	  int ROI_Width	= 40;
	  int ROI_Height = 43;
	  Mat img=Imgcodecs.imread(ImgPath+"0093.jpg",0);
	  Mat imPatch = MeanShiftTracker.extract_image_patch_center_size(img, ROI_Center, ROI_Width, ROI_Height); 
	  Mat TargetModel = MeanShiftTracker.color_distribution(imPatch, Nbins);
	 
	  Imgproc.rectangle(img, new Point(ROI_Center[0]-ROI_Width/2,ROI_Center[1]-ROI_Height/2), new Point(ROI_Center[0]+ROI_Width/2,ROI_Center[1]+ROI_Height/2), new Scalar(0,0,2));
	  imshow(img);
	  
	  System.out.println("type img:"+img.type());
	  double s1,s2,s3;
	  for(int i=93;i<=147;i=i+2)
	  {
		  String Filename=String.format("%04d", i)+".jpg";
		  System.out.println(ImgPath+Filename);
		  img=Imgcodecs.imread(ImgPath+Filename,0);
		
		  tcenter info1=MeanShiftTracker.track(img,TargetModel,Nbins,ROI_Center, ROI_Width, ROI_Height);
		  s1=info1.rho;
		  tcenter info2=MeanShiftTracker.track(img,TargetModel,Nbins,ROI_Center, ROI_Width+ROI_Width/10, ROI_Height+ROI_Height/10);
		  s2=info2.rho;
		  tcenter info3=MeanShiftTracker.track(img,TargetModel,Nbins,ROI_Center, ROI_Width-ROI_Width/10, ROI_Height-ROI_Height/10);
		  s3=info3.rho;

		  if(s1>s2)
		  {
			  if(s1>s3)
			  {
				  ROI_Center = info1.x;
			  }
			  else
			  {
				  ROI_Center = info3.x;
			  }
		  }
		  else
		  {
			  if(s2>s3)
			  {
				  ROI_Center = info2.x;
			  }
			  else
			  {
				  ROI_Center = info3.x;
			  }
			  
		  }
		 // System.out.println("roh1:"+ROI_Center[0]);
		//  System.out.println("roh2:"+ROI_Center[1]);
		
		  Imgproc.rectangle(img, new Point(ROI_Center[0]-ROI_Width/2,ROI_Center[1]-ROI_Height/2), new Point(ROI_Center[0]+ROI_Width/2,ROI_Center[1]+ROI_Height/2), new Scalar(0,0,2));
		  imshow(img);
		  Imgcodecs.imwrite(ImgPath+i+".jpg", img);
	
	  }
	 
}

private static void imshow(Mat img) throws InterruptedException {
	// TODO Auto-generated method stub
	displayImage(Mat2BufferedImage(img));
}

public static BufferedImage Mat2BufferedImage(Mat m) {
    // Fastest code
    // output can be assigned either to a BufferedImage or to an Image

    int type = BufferedImage.TYPE_BYTE_GRAY;
    if ( m.channels() > 1 ) {
        type = BufferedImage.TYPE_3BYTE_BGR;
    }
    int bufferSize = m.channels()*m.cols()*m.rows();
    byte [] b = new byte[bufferSize];
    m.get(0,0,b); // get all the pixels
    BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
    final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    System.arraycopy(b, 0, targetPixels, 0, b.length);  
    return image;
}

public static void displayImage(Image img2) throws InterruptedException {

    //BufferedImage img=ImageIO.read(new File("/HelloOpenCV/lena.png"));
    ImageIcon icon=new ImageIcon(img2);
    JFrame frame=new JFrame();
    frame.setLayout(new FlowLayout());        
    frame.setSize(img2.getWidth(null)+50, img2.getHeight(null)+50);     
    JLabel lbl=new JLabel();
    lbl.setIcon(icon);
    frame.add(lbl);
    frame.setVisible(true);
    TimeUnit.MILLISECONDS.sleep(500);
    frame.dispose();
}


}

