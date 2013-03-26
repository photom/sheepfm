package com.hitsuji.radio.manager;

import com.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.view.KeyEvent;

public class RemoteControlEventReceiver extends BroadcastReceiver {
	private static final String TAG = RemoteControlEventReceiver.class.getSimpleName();
    private static Long REC_TIME = -1L;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        Log.d(TAG, "receive action:"+intentAction+" id:"+this.hashCode());
        synchronized (REC_TIME) {
        	long past = REC_TIME;
        	REC_TIME = System.currentTimeMillis(); 
        	if (REC_TIME - past < 500) return;
        }
        
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
        	//do nothing
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = (KeyEvent)
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            
            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            long eventtime = event.getEventTime();
            Log.d(TAG, "receive keycode:"+keycode);
            // single quick press: pause/resume. 
            // double press: next track
            // long press: start auto-shuffle mode.
            
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
    		        Intent i = new Intent(context, PlayManager.class);
    		        i.setAction(PlayManager.STOP_ACTION);
    		        context.startService(i);    
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                	//donothing
                	break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                	Intent j = new Intent(context, PlayManager.class);
    		        j.setAction(PlayManager.STOP_ACTION);
    		        context.startService(j); 
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                	Intent k = new Intent(context, PlayManager.class);
    		        k.setAction(PlayManager.NEXT_ACTION);
    		        context.startService(k);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    //do nothing
                    break;
                default:
                 	break;
            }

        }
    }
}
