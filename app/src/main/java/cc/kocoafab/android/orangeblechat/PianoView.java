package cc.kocoafab.android.orangeblechat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import cc.kocoafab.orangeblechat.R;

/**
 * Created by shinjaemin on 2016. 2. 18..
 */
public class PianoView extends SurfaceView implements SurfaceHolder.Callback {

    Context pcontext;
    SurfaceHolder pholder;
    PianoThread pthread;
    int whatorange = 0;

    public PianoView(Context context, AttributeSet attrs){
        super(context, attrs);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        pholder = holder;
        pcontext = context;
        pthread = new PianoThread();
    }
    public void surfaceCreated(SurfaceHolder holder){
        pthread.start();

    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    class PianoThread extends Thread{
        Bitmap whiteKey, blackKey, orangeKey;
        Paint paint = new Paint();
        int ww = 0, wh = 0, bw = 0, bh = 0, ow = 0, oh = 0;

        public PianoThread(){
            if (whiteKey == null) {
                whiteKey = BitmapFactory.decodeResource(pcontext.getResources(), R.drawable.whitekey);
                Double dww = whiteKey.getWidth() * 1.47;
                ww = dww.intValue();
                wh = whiteKey.getHeight();
            }
            if (blackKey == null) {
                blackKey = BitmapFactory.decodeResource(pcontext.getResources(), R.drawable.blackkey);
                Double dbw = blackKey.getWidth() * 0.5;
                bw = dbw.intValue();
                bh = blackKey.getHeight();
            }
            if (orangeKey == null) {
                orangeKey = BitmapFactory.decodeResource(pcontext.getResources(), R.drawable.orangekey);
                Double dow = orangeKey.getWidth() * 1.47;
                ow = dow.intValue();
                oh = orangeKey.getHeight();
            }
        }
        public void run(){
            Canvas canvas = null;
            while(true){
                canvas = pholder.lockCanvas();
                try{
                    synchronized (pholder){
                        for (int i = 0; i < 8; i++) {
                            Rect dst = new Rect(8 + i * ww, 0, 8 + (i + 1) * ww, wh + 193);
                            if(i == whatorange){
                                canvas.drawBitmap(orangeKey, null, dst, paint);

                            }else {
                                canvas.drawBitmap(whiteKey, null, dst, paint);
                            }
                        }
                        whatorange = (whatorange+1)%2;
                        for (int i = 0; i < 8; i++) {
                            if (i != 2 && i != 6) {
                                Rect dst = new Rect(95 + i * ww, 0, 45 + (i + 1) * ww, bh + 80);
                                canvas.drawBitmap(blackKey, null, dst, paint);
                            }
                        }
                    }
                }finally{
                    pholder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    public void setWhatorange(int w) {
        whatorange = w;
    }
}
