package com.hitsuji.radio.tab;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hitsuji.radio.Auth;
import com.hitsuji.radio.PlayingActivity;
import com.hitsuji.radio.R;
import com.hitsuji.radio.RadioListActivity;
import com.hitsuji.radio.shout.ArtistShout;
import com.hitsuji.radio.shout.Header;
import com.hitsuji.radio.shout.Next;
import com.hitsuji.radio.shout.ShoutItem;
import com.hitsuji.radio.shout.TrackShout;
import com.util.Log;
import com.util.Util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.umass.lastfm.Artist;
import de.umass.lastfm.CallException;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Shout;

public class ShoutFragment extends TabBaseFragment {

	private static final String TAG  = ShoutFragment.class.getSimpleName();
	private static final int INIT = 0;
	private static final int NEXT = 1;
	private static final int MAX_PAGE = 10;
	private static final int SHOUTS_PER_PAGE = 5;
	private static final int EMPTY_ID = 101;

	private List<Shout> mTrackShouts = new ArrayList<Shout>();
	private List<Shout> mArtistShouts = new ArrayList<Shout>();
	private ExecHandler mEHandler;
	private HandlerThread mEHandlerThread;
	private DisplayHandler mDHandler;
	private ListView mListView;
	private ArrayAdapter<ShoutItem> mAdapter;
	private Boolean mLock = false;

	private static final Pattern STANDARD_URL_MATCH_PATTERN = 
			Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+", Pattern.CASE_INSENSITIVE);

	public ShoutFragment() {
		super();
		// TODO Auto-generated constructor stub
	}

	private class ShoutItemArrayAdapter extends ArrayAdapter<ShoutItem> {
		private int resourceId;
		private List<ShoutItem> items;
		private LayoutInflater inflater;

		public ShoutItemArrayAdapter(PlayingActivity context, int resourceId, List<ShoutItem> list) {
			super(context, resourceId);
			this.resourceId = resourceId;
			this.items = list;
			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ShoutItem item = (ShoutItem) getItem(position);
			if (convertView == null) {
				convertView = inflater.inflate(resourceId, null);
			}
			LinearLayout layout = (LinearLayout)convertView.findViewById(R.id.shout_layout);
			LinearLayout.LayoutParams params = 
					new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 
							LayoutParams.WRAP_CONTENT);
			params.leftMargin = 10;

			//final String fm = "<div style=\"color: WhiteSmoke; \">%s</div>";
			final String fm = "<html><head><LINK href=\"shoutbody.css\" type=\"text/css\" rel=\"stylesheet\"/></head><body>%s</body></html>";

			//Pattern regexp1 = Pattern.compile("\\[url=(http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?)\\]");
			//Pattern regexp2 = Pattern.compile("\\[url=LINK\\]\\(http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?\\)\\[\\/url\\]");


			if (item instanceof Header) {
				Header h = (Header)item;
				layout.removeAllViews();
				TextView t = new TextView(mParent);
				t.setText(h.getName());
				layout.setBackgroundColor(Color.DKGRAY);
				layout.addView(t);
			} else if (item instanceof Next) {
				Next n = (Next)item;
				layout.removeAllViews();
				TextView t = new TextView(mParent);
				t.setText(n.getName());
				layout.setBackgroundColor(Color.BLACK);
				layout.addView(t, params);
				layout.setOnClickListener(new NextClickListener(n, position));
			} else if (item instanceof TrackShout || item instanceof ArtistShout) {

				String content = null;
				String author = null;
				if (item instanceof ArtistShout) {
					ArtistShout as = (ArtistShout)item;
					content = as.getBody();
					author = as.getAuthor();
				} else if (item instanceof TrackShout) {
					TrackShout ts = (TrackShout)item;
					content = ts.getBody();
					author = ts.getAuthor();
				} else {
					return convertView;
				}

				TextView a;
				WebView b;
				layout.removeAllViews();
				a = new TextView(mParent);
				b = new WebView(mParent);
				b.setWebViewClient(new WebViewClient(){
					@Override
				    public boolean shouldOverrideUrlLoading(WebView view, String url) {
				        Intent intent = new Intent(Intent.ACTION_VIEW);
				        intent.setData(Uri.parse(url));
				        mParent.startActivity(intent);
				        return true;
				    }
				});
				layout.addView(a, params);
				layout.addView(b, params);

				a.setText(author);
				//a.setTextColor(Color.rgb(0x87, 0xCE, 0xFA));
				a.setTextColor(Color.rgb(135,206,250));
				a.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
				//
				layout.setBackgroundColor(Color.BLACK);
				a.setOnClickListener(new AuthorTouchListener(author));

				b.setBackgroundColor(Color.BLACK);
				Log.d(TAG, content);
				//content = String.format(fm,  as.getBody());
				content = content.replaceAll("\\[url=((http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+\\(\\)]+)\\](.+?)\\[/url\\]","<a href='$1'>$3</a>");
				content = content.replaceAll("\\[tag\\](.+?)\\[/tag\\]", "<a href='"+"http://m.last.fm/tag/$1"+"'>$1</a>");
				content = content.replaceAll("\\[group\\](.+?)\\[/group\\]", "<a href='"+"http://m.last.fm/group/$1"+"'>$1</a>");
				content = content.replaceAll("\\[artist\\](.+?)\\[/artist\\]", "<a href='"+"http://m.last.fm/music/$1"+"'>$1</a>");
				content = content.replaceAll("\\[track artist=(.+?)\\](.+?)\\[/track\\]", "<a href='"+"http://m.last.fm/music/$1/_/$2"+"'>$2</a>");
				b.loadDataWithBaseURL("file:///android_asset/", 
						String.format(fm, content),
						"text/html", "UTF-8", null);
				Log.d(TAG, content);

			}

			return convertView;
		}
	}
	private class AuthorTouchListener implements OnClickListener{

		private String mAuthor;
		private AuthorTouchListener(String a){
			mAuthor = a;
		}


		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			//URLEncoder.encode(
			String url = "http://m.last.fm/"+
					(mAuthor!=null? "user/"+URLEncoder.encode(mAuthor) : "");

			Uri uri = Uri.parse(url);
			Intent i = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(i); 
		}

	}

	private class NextClickListener implements OnClickListener {
		private Next mNext;
		private NextClickListener(Next n, int pos){
			mNext = n;
			mNext.setPos(pos);
		}

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			synchronized (mLock) {
				if (!getLock()){
					setLock(true);
					mEHandler.sendMessage( mEHandler.obtainMessage( NEXT, mNext ) );
				}
			}
		}

	}
	@Override
	public void onCreate(Bundle icicle){
		super.onCreate(icicle);
		Log.d(TAG, "oncreate");
		mEHandlerThread = new HandlerThread("ExecHandlerThread");
		mEHandlerThread.start();
		mEHandler = new ExecHandler(mEHandlerThread.getLooper());
		mDHandler = new DisplayHandler();
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle){
		super.onCreateView(inflater, container, icicle);
		Log.d(TAG, "oncreateview");
		mTrackShouts.clear();
		mArtistShouts.clear();

		mBody = (RelativeLayout)new RelativeLayout(mParent);
		
		mEmpty = (LinearLayout) new LinearLayout(mParent);
		mEmpty.setId(EMPTY_ID);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT,
				mParent.getResources().getDimensionPixelSize(R.dimen.bottom_action_bar_height));
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		mBody.addView(mEmpty, params);
				
		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.ABOVE, EMPTY_ID);
		mListView = new ListView(mParent);
		mBody.addView(mListView, params);
		mAdapter = new ShoutItemArrayAdapter(mParent,  R.layout.shout_list_raw, new ArrayList<ShoutItem>());
		mListView.setAdapter(mAdapter);

		return mBody;
	}

	@Override
	public void onDestroy(){
		//mEHandlerThread.stop();
		super.onDestroy();
	}

	public void initShoutLists(String track, String artist) {
		// TODO Auto-generated method stub
		if (track==null || artist==null) return;
		Bundle bundle = new Bundle();
		bundle.putString("track", track);
		bundle.putString("artist", artist);
		mEHandler.sendMessage(mEHandler.obtainMessage(INIT, bundle));
	}


	private class ExecHandler extends Handler {
		public ExecHandler(Looper l){
			super(l);
		}
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == INIT) {
				Log.d(TAG, "init shout emph:"+ 
						(mEmpty!=null ? mEmpty.getHeight():"null") + 
						" listh:"+(mListView!=null?mListView.getHeight():"null"));
				Bundle b = (Bundle)msg.obj;
				String track = b.getString("track");
				String artist = b.getString("artist");
				if (track == null || artist ==null)return;
				try {
					PaginatedResult<Shout> tracks = 
							de.umass.lastfm.Track.getShouts(artist, track, 0, -1, Auth.LASTFM_API_KEY);

					PaginatedResult<Shout> artists = 
							Artist.getShouts(artist, 0, -1, Auth.LASTFM_API_KEY);

					mDHandler.post(new DisplayInitRunner(track, artist, tracks, artists));
				} catch(CallException e){
					e.printStackTrace();
				}

			} else if (msg.what == NEXT) {
				if (mParent == null) return;
				String track = mParent.getCurrentTitle();
				String artist = mParent.getCurrentArtist();
				if (!(msg.obj instanceof Next)) return;
				Next n = (Next) msg.obj;

				if (n.getType() == Next.ARTIST) {
					PaginatedResult<Shout> artists = 
							Artist.getShouts(artist, n.getNextPage(), -1, Auth.LASTFM_API_KEY);

					mDHandler.post(new DisplayNextRunner(artists, n, track, artist));
				} else if (n.getType() == Next.TRACK) {
					PaginatedResult<Shout> tracks = 
							de.umass.lastfm.Track.getShouts(artist, track, n.getNextPage(), -1, Auth.LASTFM_API_KEY);					
					mDHandler.post(new DisplayNextRunner(tracks, n, track, artist));
				}
			}
		} 

	}

	private class DisplayHandler extends Handler{

	}
	private class DisplayNextRunner implements Runnable{
		private PaginatedResult<Shout> mResults;
		private Next mNext;
		private String mArtist;
		private String mTrack;

		private DisplayNextRunner (
				PaginatedResult<Shout> pr, Next n, String t, String a){
			mResults = pr;
			mNext = n;
			mArtist = a;
			mTrack = t;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Collection<Shout> shouts = mResults.getPageResults();
			int pos = mNext.getPos();

			if (mNext.getType() == Next.ARTIST) {
				for (Shout s : shouts) {
					mAdapter.insert(new ArtistShout(mArtist, s), pos);
					pos++;
				}
			} else if (mNext.getType() == Next.TRACK) {
				for (Shout s : shouts) {
					mAdapter.insert(new TrackShout(mArtist, s), pos);
					pos++;
				}
			}
			if (mResults.getPage() == mResults.getTotalPages()-1){
				mAdapter.remove(mNext);
			} else {
				mNext.setPos(-1);
				mNext.setNextPage(mResults.getPage()+1);
			}
			mAdapter.notifyDataSetChanged();
			synchronized (mLock) {
				setLock(false);
			}
		}
	}
	private class DisplayInitRunner implements Runnable{
		private PaginatedResult<Shout> mTracks;
		private PaginatedResult<Shout> mArtists;
		private String mArtist;
		private String mTrack;

		public DisplayInitRunner(
				String track, String artist,
				PaginatedResult<Shout> tracks, 
				PaginatedResult<Shout> artists){
			mTracks = tracks;
			mArtists = artists;
			mTrack = track;
			mArtist = artist;
		}


		@Override
		public void run() {
			// TODO Auto-generated method stub
			mAdapter.clear();
			mTrackShouts.clear();
			mArtistShouts.clear();
			synchronized (mLock) {
				mLock = false;
			}

			if (!mTracks.isEmpty()){
				Collection<Shout> page = mTracks.getPageResults();
				for (Shout s : page){
					mTrackShouts.add(s);
				}
			}
			if (!mArtists.isEmpty()){
				Collection<Shout> page = mArtists.getPageResults();
				for (Shout s : page){
					mArtistShouts.add(s);
				}
			}

			if (!mTracks.isEmpty()){
				mAdapter.add(new Header("To Track: "+mTrack ));

				for (Shout s: mTrackShouts) {
					mAdapter.add(new TrackShout(mTrack, s));
				}
				//if (mTracks.getTotalPages() > 1) 
				//mAdapter.add(new Next(Next.TRACK, mTracks.getPage()+1));
			}

			if (!mArtists.isEmpty()){
				mAdapter.add(new Header("To Artist: "+ mArtist ));
				for (Shout s : mArtistShouts){
					mAdapter.add(new ArtistShout(mArtist, s));
				}
				//if (mArtists.getTotalPages() > 1) 
				//mAdapter.add(new Next(Next.ARTIST, mArtists.getPage()+1));
			}
			Log.d(TAG, "track page:"+mTracks.getPage() + " track total:"+mTracks.getTotalPages() + " artist page:"+mArtists.getPage() + " artist total:"+mArtists.getTotalPages());
			mAdapter.notifyDataSetChanged();
		}
	}

	public boolean getLock(){
		return mLock;
	}
	public void setLock(boolean b) {
		mLock = b;
	}
}
