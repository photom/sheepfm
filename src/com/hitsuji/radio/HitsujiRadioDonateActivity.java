package com.hitsuji.radio;
import java.math.BigDecimal;

import com.paypal.android.MEP.CheckoutButton;
import com.paypal.android.MEP.PayPal;
import com.paypal.android.MEP.PayPalActivity;
import com.paypal.android.MEP.PayPalReceiverDetails;
import com.paypal.android.MEP.PayPalPayment;
import com.paypal.android.MEP.PayPalResultDelegate;
import com.util.Log;
import com.util.Util;

import de.umass.lastfm.User;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;

public class HitsujiRadioDonateActivity extends Activity 
implements OnClickListener, PayPalResultDelegate, DialogInterface.OnClickListener{
	private static final String TAG = HitsujiRadioDonateActivity.class.getSimpleName();
	private static final int PAYPAL_REQUEST_CODE = 1;
	private PayPal mPpObj;
	private CheckoutButton mCheckout;
	private Button mCancel;
	private Spinner mSpinner;
	
	private static final String [] LANGUAGES = {
			"zh_HK",
			"zh_TW",
			"nl_BE",
			"nl_NL",
			"en_AU",
			"en_BE",
			"en_CA",
			"en_FR",
			"en_DE",
			"en_GB",
			"en_HK",
			"en_IN",
			"en_MX",
			"en_NL",
			"en_PL",
			"en_SG",
			"en_ES",
			"en_CH",
			"en_TW",
			"en_US",
			"fr_BE",
			"fr_CA",
			"fr_FR",
			"fr_CH",
			"de_AT",
			"de_DE",
			"de_CH",
			"it_IT",
			"es_AR",
			"es_MX",
			"es_ES",
			"pl_PL",
			"pt_BR"
			};
	
	private ProgressDialog mDialog;
	private Handler mHandler;
	private Handler mAHandler;
	private HandlerThread mHandlerThread;
	
	@Override
	public void onCreate(Bundle icicle){
		super.onCreate(icicle);
		mHandlerThread = new HandlerThread("donate");
		mHandlerThread.start();
		mHandler = new Handler(mHandlerThread.getLooper());
		mAHandler = new Handler();
		
		this.setContentView(R.layout.donate);
		mCancel = (Button)this.findViewById(R.id.donate_cancel);
		mCancel.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				HitsujiRadioDonateActivity.this.finish();
			}
			
		});
		
		ArrayAdapter adapter = 
				ArrayAdapter.createFromResource(this, R.array.donate_languages,
				android.R.layout.simple_spinner_item);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner = (Spinner) findViewById(R.id.donate_lan_spinner);
		mSpinner.setAdapter(adapter);
		mSpinner.setSelection(19);
		

		
		setTitle(getPackageManager().getApplicationLabel(getApplicationInfo()));
	}
	
	public CheckoutButton getCheckoutButton(Context context, int style, int
			textType){
		CheckoutButton launchPayPalButton = mPpObj.getCheckoutButton(this,
				style, textType);
		
				RelativeLayout.LayoutParams params = new
				RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
				
				LinearLayout l = (LinearLayout)this.findViewById(R.id.donate_lan_layout);
				params.addRule(RelativeLayout.BELOW, l.getId());
				params.bottomMargin = 10;
				launchPayPalButton.setLayoutParams(params);
				((RelativeLayout)findViewById(R.id.donate_layout)).addView(launchPayPalButton);
				
		return launchPayPalButton;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
		if (mSpinner!=null){
			int p = mSpinner.getSelectedItemPosition();
			mPpObj.setLanguage(LANGUAGES[p]);
		}
			
		PayPalPayment newPayment = new PayPalPayment();
		newPayment.setSubtotal( new BigDecimal(1.0f));
		newPayment.setCurrencyType("USD");
		newPayment.setRecipient(Util.VALID_PAYPAL ?
				 "" :
				 "jankth_1326378691_biz@gmail.com");
		newPayment.setMerchantName("Donation");
		newPayment.setPaymentType(PayPal.PAYMENT_TYPE_PERSONAL);
		Intent paypalIntent = PayPal.getInstance().checkout(newPayment, this);
		this.startActivityForResult(paypalIntent, PAYPAL_REQUEST_CODE);

	}

	@Override
	public void onResume(){
		super.onResume();

		
		if (mPpObj == null) {
		    mDialog = new ProgressDialog(this);
		    mDialog.setMessage("connecting PayPal ...");
		    mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		    
		    mDialog.setOnShowListener(new OnShowListener(){

				@Override
				public void onShow(DialogInterface dialog) {
					// TODO Auto-generated method stub
		
					mHandler.post(new Runnable(){

						@Override
						public void run() {
							// TODO Auto-generated method stub
							
							mPpObj = PayPal.initWithAppID(
									HitsujiRadioDonateActivity.this, 
									"APP-80W284485P519543T", 
									Util.VALID_PAYPAL ? 
											PayPal.ENV_LIVE :
											PayPal.ENV_SANDBOX);
							mPpObj.setShippingEnabled(false);
							mPpObj.setDynamicAmountCalculationEnabled(false);	
							mAHandler.post(new Runnable(){

								@Override
								public void run() {
									// TODO Auto-generated method stub
									mCheckout = getCheckoutButton(
											HitsujiRadioDonateActivity.this, 
											PayPal.BUTTON_278x43, 
											CheckoutButton.TEXT_DONATE);
									mCheckout.setOnClickListener(HitsujiRadioDonateActivity.this);
									
									mDialog.dismiss();
								}
								
							});

						}
						
					});

				}
		    	
		    });
		    
		    mDialog.show();
		}
	}
	
	public void end(){
		mAHandler.post(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				HitsujiRadioDonateActivity.this.finish();
			}
			
		});
	}
	
	@Override
	public void onDestroy(){
		//mHandlerThread.stop();
		super.onDestroy();
	}

	@Override
	public void onPaymentCanceled(String arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG,"onpaymentCanceld:"+arg0);
		end();
	}

	@Override
	public void onPaymentFailed(String arg0, String arg1, String arg2,
			String arg3, String arg4) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onpaymentfailed arg0:"+arg0+ " arg2:"+arg2 +" arg3:"+arg3+ " arg4:"+arg4);
		end();
	}

	@Override
	public void onPaymentSucceeded(String arg0, String arg1) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onpaymentsucceeded arg0:"+arg0 +" arg1:"+arg1);
		end();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		Log.d(TAG, "onactivityresult requestcode:"+requestCode+" resultCode:"+resultCode + " data:"+data.toString());
		if (requestCode != PAYPAL_REQUEST_CODE) return;

		AlertDialog.Builder dlg;
		dlg = new AlertDialog.Builder(this);
		dlg.setCancelable(false);
		dlg.setPositiveButton("OK", this);

		switch(resultCode) {
		case Activity.RESULT_OK:
			//The payment succeeded
			String payKey =
			data.getStringExtra(PayPalActivity.EXTRA_PAY_KEY);
			//Tell the user their payment succeeded
			
			dlg.setTitle("Completed");
	        dlg.setMessage("I really appreciate your support!!!");
	        dlg.show();
			break;
		default:
		case Activity.RESULT_CANCELED:
			//The payment was canceled
			//Tell the user their payment was canceled
			end();
			break;
		case PayPalActivity.RESULT_FAILURE:
			//The payment failed -- we get the error from the
			//EXTRA_ERROR_ID and EXTRA_ERROR_MESSAGE
			String errorID =
			data.getStringExtra(PayPalActivity.EXTRA_ERROR_ID);
			String errorMessage =
			data.getStringExtra(PayPalActivity.EXTRA_ERROR_MESSAGE);
			//Tell the user their payment was failed.

			dlg.setTitle("Error: "+errorID);
	        dlg.setMessage(errorMessage);
	        dlg.show();
			break;
		}

		
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		// TODO Auto-generated method stub
		end();
	}
}
