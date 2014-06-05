package jp.egcuhi.android.samplecast;

// Chromecastにキャストするサンプルアプリケーション
// Programed by Kazuyuki Eguchi

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;

public class MainActivity extends Activity
{
	private String TAG = "test";
	
	// Google Cast SDK Developer Consoleから取得した Application IDを設定してください
	private static String APPLICATION_ID = "hogehoge";
	
	// Chromecastを探索するのに使う変数類
	private MediaRouter mRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouter.Callback mCB;
	
	// Chromecastをコントロールするための変数類
	private GoogleApiClient mApiClient;
	private CastDevice mSelectedDevice = null;
	private String mSessionId = null;
	private Cast.Listener mCastClientListener = new Cast.Listener(){};
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Chromecastを探す準備
		mRouter = MediaRouter.getInstance(this);
		mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(APPLICATION_ID)).build();
		mCB = new MediaRouter.Callback()
		{
			@Override
			public void onRouteAdded(MediaRouter router, RouteInfo route)
			{
				Log.d(TAG,"onRouteAdded=" + route.getName());
				
				// 最初に見つけたデバイスに接続を行う。
				if(mSelectedDevice == null)
				{
					mSelectedDevice = CastDevice.getFromBundle(route.getExtras());
					
					Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(mSelectedDevice, mCastClientListener);
					apiOptionsBuilder.setVerboseLoggingEnabled(true);
					
					mApiClient = new GoogleApiClient.Builder(getBaseContext())
		            .addApi(Cast.API, apiOptionsBuilder.build())
		            .addConnectionCallbacks(mConnectionCallbacks)
		            .addOnConnectionFailedListener(mConnectionFailedListener)
		            .build();
					
					mApiClient.connect();
				}
			}

			@Override
			public void onRouteRemoved(MediaRouter router, RouteInfo route)
			{
				Log.d(TAG,"onRouteRemoved" + route.getName());
			}
		};
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		// Chromecastを探す
		mRouter.addCallback(mMediaRouteSelector, mCB,MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
	}

	@Override
	protected void onPause()
	{
		if (isFinishing())
		{
		    mRouter.removeCallback(mCB);
		}
		
		// 接続切断する
		peardown();
		
		super.onPause();
	}
	
	// Chromecastに接続確認用コールバック
	private GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks()
	{
		@Override
		public void onConnected(Bundle connectionHint)
		{
			Log.d(TAG,"onConnected");
			
			try
			{
				Cast.CastApi.launchApplication(mApiClient, APPLICATION_ID, false)
				 .setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>()
				{
					@Override
					public void onResult(ApplicationConnectionResult result)
					{
						Status status = result.getStatus();
		                if (status.isSuccess())
		                {
		                	mSessionId = result.getSessionId();
		                	Log.d(TAG,"mSessionID=" + mSessionId);
		                }
					}
				});
			}
			catch(Exception ex)
			{
				Log.d(TAG,ex.toString());
			}			
		}

		@Override
		public void onConnectionSuspended(int cause)
		{
		}
	};

	// Chromecastに接続失敗した場合に呼ばれる
	private GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener()
	{
		@Override
		public void onConnectionFailed(ConnectionResult result)
		{
		}
	};
	
	// Chromecastとの接続を解除する
	private void peardown()
	{		
		if (mApiClient != null)
		{
			if (mApiClient.isConnected())
			{
				try
				{
		            Cast.CastApi.stopApplication(mApiClient, mSessionId);
		        }
				catch (Exception e)
		        {
		        	Log.e(TAG, "Exception while removing channel", e);
		        }
				
				mApiClient.disconnect();
		    }
			
		    mApiClient = null;
		    mSelectedDevice = null;
		    mSessionId = null;
		}
	}

}
