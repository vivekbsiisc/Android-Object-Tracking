package objectTracking;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.samples.imagemanipulations.R;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;

public class ImageManipulationsActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";

    public static final int      ObjectSelect      = 0;
    public static final int      ObjectTrack      = 1;
    public static final int      ObjectTrackStop     = 2;


    private MenuItem             mItemObjSelect;
    private MenuItem             mItemObjTrack;
    private MenuItem             mItemObjTrackStop;
    private CameraBridgeViewBase mOpenCvCameraView;



    public static int           viewMode = ObjectSelect;
    
    // tracker
    private int Nbins = 8;
	private int[] ROI_Center ={0,0}; 
	private int ROI_Width	= 0;
	private int ROI_Height = 0;
	private Mat imPatch;
	private Mat TargetModel;
	private boolean Track_init=false;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ImageManipulationsActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.image_manipulations_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemObjSelect  = menu.add("Select Target");
        mItemObjTrack  = menu.add("Start Track");
        mItemObjTrackStop = menu.add("Stop Track");
//        mItemPreviewSepia = menu.add("Sepia");
//        mItemPreviewSobel = menu.add("Sobel");
//        mItemPreviewZoom  = menu.add("Zoom");
//        mItemPreviewPixelize  = menu.add("Pixelize");
//        mItemPreviewPosterize = menu.add("Posterize");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemObjSelect)
            viewMode = ObjectSelect;
        if (item == mItemObjTrack)
            viewMode = ObjectTrack;
        else if (item == mItemObjTrackStop)
            viewMode = ObjectTrackStop;
//        else if (item == mItemPreviewSepia)
//            viewMode = VIEW_MODE_SEPIA;
//        else if (item == mItemPreviewSobel)
//            viewMode = VIEW_MODE_SOBEL;
//        else if (item == mItemPreviewZoom)
//            viewMode = VIEW_MODE_ZOOM;
//        else if (item == mItemPreviewPixelize)
//            viewMode = VIEW_MODE_PIXELIZE;
//        else if (item == mItemPreviewPosterize)
//            viewMode = VIEW_MODE_POSTERIZE;
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
       
        
        
    }

    public void onCameraViewStopped() {

    }

    Mat mRgba;
    Mat PRgb;
    int x1=0,y1=0;
    int x2=0,y2=0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
         int x = (int)event.getX();
         int y = (int)event.getY();
        if(viewMode==ObjectSelect)
        {
        	switch (event.getAction()) {
	            case MotionEvent.ACTION_DOWN:
	    		x1 =x;
	    		y1=y;
	    		break;
	            case MotionEvent.ACTION_MOVE:
	            x2 =x;
	            y2 =y;
	            break;
	            case MotionEvent.ACTION_UP:
	        	x2=x;
	        	y2=y;
	        	break;
	        }
        }
    return false;
    }
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	mRgba = inputFrame.rgba();
    	
    	Mat grayimg = new Mat();
    	
    	switch(viewMode)
    	{
    	case ObjectSelect:
    		
    		ROI_Width  = Math.abs(x2-x1);
    		ROI_Height = Math.abs(y2-y1);
    		ROI_Center[0] = Math.max(x1,x2) - ROI_Width/2;
    		ROI_Center[1] = Math.max(y1,y2) - ROI_Height/2;
    		PRgb=mRgba.clone();
    		Core.circle(mRgba, new Point(ROI_Center[0],ROI_Center[1]), 10, new Scalar(0, 255, 0, 255));	
    		Core.rectangle(mRgba, new Point(x1,y1), new Point(x2,y2),  new Scalar(0, 255, 0, 255));
    		
    		break;
    	case ObjectTrack:
    	
    		if(!Track_init)
    		{   
    			Imgproc.cvtColor(PRgb, grayimg, Imgproc.COLOR_RGB2GRAY);
    			
    			imPatch = MeanShiftTracker.extract_image_patch_center_size(grayimg, ROI_Center, ROI_Width, ROI_Height); 
    			TargetModel = MeanShiftTracker.color_distribution(imPatch, Nbins);
    			Track_init=true;
    		}
    		
    			Imgproc.cvtColor(mRgba, grayimg, Imgproc.COLOR_RGB2GRAY);
    			ROI_Center=MeanShiftTracker.track(grayimg,TargetModel,Nbins,ROI_Center, ROI_Width, ROI_Height);
    			
    		Core.circle(mRgba, new Point(ROI_Center[0],ROI_Center[1]), 10, new Scalar(0, 255, 0, 255));	
    		Core.rectangle(mRgba, new Point(ROI_Center[0]-ROI_Width/2,ROI_Center[1]-ROI_Height/2), new Point(ROI_Center[0]+ROI_Width/2,ROI_Center[1]+ROI_Height/2),  new Scalar(0, 255, 0, 255));
    		
    		break;
    		
    	case ObjectTrackStop:
    		Track_init=false;
    		
    		break;
    	}
    	return mRgba;
    }
}
