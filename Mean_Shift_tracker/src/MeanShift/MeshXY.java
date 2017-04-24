package MeanShift;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class MeshXY {
    public Mat x;
    public Mat y ;
    public MeshXY() {
        x = new Mat();
        y = new Mat();
    }
    public static MeshXY meshgrid(int startx,int endx,int starty,int endy)
	{
		
		Mat tempx = new Mat(1,endx-startx+1,CvType.CV_8U);
		Mat tempy = new Mat(endy-starty+1,1,CvType.CV_8U);


		for(int i=0;i<endx-startx+1;i++)
		{
			tempx.put(0, i, i+startx);
		}
		for(int i=0;i<endy-starty+1;i++)
		{
			tempy.put(i,0, i+starty);
		}

		MeshXY Meshxy = new MeshXY();
		Core.repeat(tempx,endy-starty+1, 1, Meshxy.x);
		Core.repeat(tempy,1,endx-startx+1, Meshxy.y);
		return Meshxy;
	}
} 
