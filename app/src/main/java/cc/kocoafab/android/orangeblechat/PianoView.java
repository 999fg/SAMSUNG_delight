package cc.kocoafab.android.orangeblechat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
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
    int whatorange = -1;
    long clicktime;

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
        Bitmap whiteKey, blackKey;
        Bitmap[] colorKey = new Bitmap[8];
        int[] Keysource = {R.drawable.redkey,R.drawable.orangekey,R.drawable.yellowkey,R.drawable.greenkey,R.drawable.bluekey,R.drawable.navykey,R.drawable.purplekey,R.drawable.pinkkey};
        Paint paint = new Paint();
        Paint fade = new Paint();
        Canvas canvas = null;
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
            for(int i = 0; i<colorKey.length; i++){
                if(colorKey[i] == null){
                    colorKey[i] = BitmapFactory.decodeResource(pcontext.getResources(),Keysource[i]);
                    Double dow = colorKey[i].getWidth() * 1.47;
                    ow = dow.intValue();
                    oh = colorKey[i].getHeight();
                }
            }
        }
        public void run(){
            while(true){
                canvas = pholder.lockCanvas();
                try{
                    synchronized (pholder){
                        for (int i = 0; i < 8; i++) {
                            Rect dst = new Rect(8 + i * ww, 0, 8 + (i + 1) * ww, wh + 193);
                            if(i == whatorange){
                                canvas.drawBitmap(colorKey[whatorange], null, dst, paint);
                                int alpha = (int) (System.currentTimeMillis() - clicktime) / 3;
                                alpha = alpha * alpha / 255;
                                if(alpha > 255) {
                                    alpha = 255;
                                    whatorange = -1;
                                }
                                fade.setAlpha(alpha);
                                canvas.drawBitmap(whiteKey, null, dst, fade);
                            }else {
                                canvas.drawBitmap(whiteKey, null, dst, paint);
                            }
                        }
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
        clicktime = System.currentTimeMillis();
    }
}
