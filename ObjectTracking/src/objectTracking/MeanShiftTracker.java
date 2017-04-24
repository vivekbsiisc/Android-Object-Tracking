package objectTracking;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


public class MeanShiftTracker {


	
	public static Mat color_distribution(Mat imPatch,int Nbins)
	{
		// get image size
		Size imgsize=imPatch.size();
		int rows = (int)imgsize.height;
		int cols =  (int)imgsize.width;
		Scalar b=new Scalar(256/Nbins);
		Mat  matrix_bin = new Mat(rows,cols,CvType.CV_8UC1);
	    Core.divide(imPatch, b, matrix_bin);
	    //Core.add(matrix_bin, Scalar.all(1), matrix_bin);
		
	    //System.out.println("matrix_bin:\n"+matrix_bin.dump());

		int c_row= (int) Math.round(imgsize.height/2);
		int  c_col= (int) Math.round(imgsize.width/2);
		Mat imagePatch_new= new Mat(rows,cols,CvType.CV_8UC1,new Scalar(1));
		imagePatch_new.put(c_row-1,c_col-1, 0);
		Mat d_Center =new Mat();
		Imgproc.distanceTransform(imagePatch_new, d_Center,Imgproc.CV_DIST_L2, Imgproc.CV_DIST_MASK_PRECISE);
		
		//System.out.println("D_centre:\n"+d_Center.dump());
		
		// distance transformation
		Mat dN_Center = new Mat(rows,cols,CvType.CV_32F); 
		Core.MinMaxLocResult MinMaxResult= Core.minMaxLoc(d_Center);
		Mat num =new Mat();
		Core.subtract(d_Center, Scalar.all(MinMaxResult.minVal), num);
		Core.divide(num, Scalar.all(MinMaxResult.maxVal - MinMaxResult.minVal), dN_Center);
		
		//System.out.println("dN_Center:\n"+dN_Center.dump());
		
		// kernel profile
		Mat kernel_profile =new Mat(rows,cols,CvType.CV_32F);
		Core.pow(dN_Center, 2, kernel_profile);
		Core.subtract(kernel_profile ,Scalar.all(1), kernel_profile);
		double C= -(2/Math.PI);
		Core.multiply(kernel_profile, Scalar.all(C), kernel_profile);
		
		//System.out.println("kernel_profile:\n"+kernel_profile.dump());
		
		Mat h= new Mat(Nbins,1,CvType.CV_32F);
		for(int i=1;i<=Nbins;i++)
		{
			Mat result = new Mat(rows,cols,CvType.CV_8U);
			Core.compare(matrix_bin, Scalar.all(i), result, Core.CMP_EQ);
			
			Mat mask = new Mat(rows,cols,CvType.CV_32F);
			Core.divide(result, Scalar.all(255), mask);
			
			Mat temp =new Mat(rows,cols,kernel_profile.type());
			mask.convertTo(mask, kernel_profile.type());
			Core.multiply(mask, kernel_profile, temp);
			h.put(i-1,0,Core.sumElems(temp).val[0]);
			//System.out.println("temp:\n"+temp.dump());
		}
		Core.divide(h, Scalar.all(Core.sumElems(h).val[0]), h);
		return h;
	}
	
	
	
	public static double compute_bhattacharyya_coefficient(Mat a,Mat b)
	{
	    //k = sum(sqrt(p.*q))
		Mat  p_q=new Mat();
		Core.multiply(a,b, p_q);
		Core.pow(p_q, 0.5, p_q);
		return Core.sumElems(p_q).val[0];
	}
	
	public static Mat compute_weights_NG(Mat imPatch,Mat TargetModel,Mat ColorModel,int Nbins)
	{
		Size imgsize=imPatch.size();
		int rows = (int)imgsize.height;
		int cols =  (int)imgsize.width;
		Scalar b=new Scalar(256/Nbins);
		Mat  matrix_bin = new Mat(rows,cols,CvType.CV_8UC1);
	    Core.divide(imPatch, b, matrix_bin);
	   
	    //System.out.println("temp:\n"+matrix_bin.dump());
		
	    Mat w= new Mat(rows,cols,CvType.CV_32F,new Scalar(0));
	    for(int i=1;i<=Nbins;i++)
	    {
	    	if(ColorModel.get(i-1, 0)[0]!=0)
			{
				double p_q = (TargetModel.get(i-1, 0)[0]/ColorModel.get(i-1, 0)[0]);
		    	p_q = Math.pow(p_q,0.5);
		    	
				Mat result = new Mat(rows,cols,CvType.CV_8U);
				Core.compare(matrix_bin, Scalar.all(i), result, Core.CMP_EQ);
				
				Mat mask = new Mat(rows,cols,CvType.CV_32F);
				Core.divide(result, Scalar.all(255), mask);
				mask.convertTo(mask, CvType.CV_32F);
				//System.out.println("temp:\n"+p_q);
				//System.out.println("mask:\n"+mask.dump());
				Core.multiply(mask, Scalar.all(p_q),mask );
			   // System.out.println("mask:\n"+mask.dump());
				Core.add(w,mask,w);
			}
	    }
		return w;
	}
	
	public static int[] compute_meanshift_vector(Mat imPatch, int[] prev_center,Mat weights)
	{
		Size imgsize=imPatch.size();
		int height= (int)imgsize.height;
		int width =  (int)imgsize.width;
		int cordinate[] = new int[2];
		cordinate[0] = prev_center[0]-width/2 + 1;
		cordinate[1] = prev_center[1]-height/2 + 1;
		MeshXY meshxy =new MeshXY();
		meshxy=MeshXY.meshgrid(cordinate[0], width+cordinate[0]-1,cordinate[1],height+cordinate[1]-1);

		//System.out.println("x:"+meshxy.x.dump());
		//System.out.println("y:"+meshxy.y.dump());
		
		Mat temp=new Mat();
		double w_sum=Core.sumElems(weights).val[0];
		meshxy.x.convertTo(meshxy.x,weights.type() );
		Core.multiply(meshxy.x, weights, temp);
		temp.convertTo(temp, CvType.CV_32F);
		Core.divide(temp,Scalar.all(w_sum), temp);
		//System.out.println("x.*w/s_w:"+temp.dump());
		double x_mass = Core.sumElems(temp).val[0];
		
		meshxy.y.convertTo(meshxy.y,weights.type());
		Core.multiply(meshxy.y, weights, temp);
		temp.convertTo(temp, CvType.CV_32F);
		Core.divide(temp,Scalar.all(w_sum), temp);
		double y_mass = Core.sumElems(temp).val[0];
		
		int[] z = new int[2];
		z[0] = (int)x_mass;
		z[1] = (int)y_mass;
		return z;
	}
	
	public static Mat extract_image_patch_center_size(Mat I,int[] center,int w,int h )
	{
		Size imgsize=I.size();
		int height= (int)imgsize.height;
		int width =  (int)imgsize.width;
		int y = center[1] - h/2;
		int x = center[0] - w/2;
		
		int r  = Math.max(y, 0);
		int r2 = Math.min(height-1,y+h-1);
		
		int c  = Math.max(x, 0);
		int c2 = Math.min(width-1,x+w-1);
		
		return I.submat(r, r2, c, c2);
	}
	 
	public static int[] track(Mat I,Mat TargetModel,int Nbins,int[] prev_center,int ROI_Width,int ROI_Height)
	{
		//Mean-Shift Algorithm 
		//figure('name', 'Mean Shift Algorithm', 'units', 'normalized', 'outerposition', [0 0 1 1]);
		//prev_center = ROI_Center;
		//disp(prev_center);
		int[] new_center=new int[2];
		Mat ColorModel =new Mat();
		    for(int iters=1;iters<10;iters++)
		    {	// STEP 1
		    	// calculate the pdf of the previous position
		    	Mat imPatch = extract_image_patch_center_size(I, prev_center, ROI_Width, ROI_Height);
		    	ColorModel= color_distribution(imPatch, Nbins);
		    	// evaluate the Bhattacharyya coefficient
		     	double rho = compute_bhattacharyya_coefficient(TargetModel, ColorModel);
		    
		    	// STEP 2, 3
		    	// derive the weights
		    	Mat weights = compute_weights_NG(imPatch, TargetModel, ColorModel, Nbins);
		    	// compute the mean-shift vector
		    	// using Epanechnikov kernel, it reduces to a weighted average
		    	new_center = compute_meanshift_vector(imPatch, prev_center, weights);
		    	prev_center[0] = new_center[0];
		    	prev_center[1] = new_center[1]; 
		    	/*
		        // STEP 4, 5
		        Mat imPatch2 = extract_image_patch_center_size(I, new_center, ROI_Width, ROI_Height);
		    	Mat ColorModel2 = color_distribution(imPatch2, Nbins);
		    	// evaluate the Bhattacharyya coefficient
		     	double rho2 = compute_bhattacharyya_coefficient(TargetModel, ColorModel2);
		     	while(rho2<rho)
		        {
		        	new_center[0] = (prev_center[0]+new_center[0])/2;
		        	new_center[1] = (prev_center[1]+new_center[1])/2;
		        	
		            imPatch2 = extract_image_patch_center_size(I, new_center, ROI_Width, ROI_Height);
		            ColorModel2 = color_distribution(imPatch2, Nbins);
		            // evaluate the Bhattacharyya coefficient
		            rho2 = compute_bhattacharyya_coefficient(TargetModel, ColorModel2);
				}

		        // STEP 6
		        double norm1_centerdiff =Math.abs(new_center[0] - prev_center[0]) + Math.abs(new_center[1] - prev_center[1]);
		        if( norm1_centerdiff  < 0.0001)
		        {
		        	break;
		        }*/
		    }
		 
			
		    return new_center;
	}
}
