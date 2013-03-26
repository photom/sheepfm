package com.view;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.view.View;

public class GifView extends View {

	private Movie movie;
	private long moviestart;

	public GifView(Context context) {
		super(context);
	}

	public GifView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GifView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setResouceId(int id) {
		InputStream inputStream = getContext().getResources().openRawResource(id);
		movie = Movie.decodeStream(inputStream);
		moviestart = 0;
	}

	public void setImagePath(String path) {
		try {
			File f = new File(path);
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
			bis.mark((int) f.length());
			movie = Movie.decodeStream(bis);
			bis.close();
			moviestart = 0;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(Color.BLACK);
		if (movie == null) {
			return;
		}
		long now = android.os.SystemClock.uptimeMillis();
		if (moviestart == 0) {
			moviestart = now;
		}
		int relTime = (int) ((now - moviestart) % movie.duration());
		movie.setTime(relTime);
		movie.draw(canvas, 0, 0);
		this.invalidate();
	}
}
