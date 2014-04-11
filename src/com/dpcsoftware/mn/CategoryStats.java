/*
 *   Copyright 2013, 2014 Daniel Pereira Coelho
 *   
 *   This file is part of the Expenses Android Application.
 *
 *   Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation in version 3.
 *
 *   Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Expenses.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.dpcsoftware.mn;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;


public class CategoryStats extends SherlockActivity {
	private App app;
	private CategoryStatsAdapter adapter;
	private View footer,footer2;
	private Runnable menuCallback = new Runnable() {
		public void run() {
			renderGraph();
		}
	};
	private Date date;
	private boolean isByMonth = true;
	private OnClickListener monthButtonCListener = new OnClickListener() {
		public void onClick(View v) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			switch(v.getId()) {
				case R.id.imageButton1:
					cal.add(Calendar.MONTH, -1);
					break;
				case R.id.imageButton2:
					cal.add(Calendar.MONTH, 1);
					break;
			}
			date = cal.getTime();
			renderGraph();
		}
	};
	
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (App) getApplication();

		setContentView(R.layout.categorystats);
		
		ListView lv = ((ListView) findViewById(R.id.listView1));
		LayoutInflater inflater = LayoutInflater.from(this);
		footer = inflater.inflate(R.layout.categorystats_listitem, null);
		footer.findViewById(R.id.imageView1).setVisibility(View.GONE);
		((TextView) footer.findViewById(R.id.textView1)).setText(R.string.categorystats_c1);
		lv.addFooterView(footer);
		
		footer2 = inflater.inflate(R.layout.categorystats_listitem, null);
		footer2.findViewById(R.id.imageView1).setVisibility(View.GONE);
		((TextView) footer2.findViewById(R.id.textView1)).setText(R.string.categorystats_c2);
		lv.addFooterView(footer2);
		
    	View emptyView = findViewById(R.id.empty);
    	((TextView) emptyView.findViewById(R.id.textView1)).setText(R.string.categorystats_c3);
    	lv.setEmptyView(emptyView);
    	
    	lv.setOnItemClickListener(new OnItemClickListener() {
    		public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
    			if(position != adapter.getCount() && position != (adapter.getCount()+1)) {
	    			Intent it = new Intent(CategoryStats.this, ExpensesList.class);
	    			it.putExtra("FILTER_ID", id);
	    			if(isByMonth)
	    				it.putExtra("FILTER_DATE", date);	    			
	    			startActivity(it);
    			}
    		}
    	});
    	
    	((ImageButton) findViewById(R.id.imageButton1)).setOnClickListener(monthButtonCListener);
    	((ImageButton) findViewById(R.id.imageButton2)).setOnClickListener(monthButtonCListener);
   
		((RadioGroup) findViewById(R.id.radioGroup1)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged (RadioGroup group, int checkedId) {
				LinearLayout monthPicker = (LinearLayout) findViewById(R.id.LinearLayoutMonthPicker);
				if(checkedId == R.id.radio0) {
					isByMonth = true;
					monthPicker.setVisibility(View.VISIBLE);
				}
				else {
					isByMonth = false;
					monthPicker.setVisibility(View.GONE);
				}
				renderGraph();
			}
		});
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	app.new SpinnerMenu(this,menuCallback);
    	
    	return true;
    }
    
	public void renderGraph() {
		SQLiteDatabase db = DatabaseHelper.quickDb(this,0);
		if(date == null)
			date = Calendar.getInstance().getTime();
		
		String queryModifier = "";
		if(isByMonth)
			queryModifier = " AND strftime('%Y-%m'," + Db.Table1.TABLE_NAME + "." + Db.Table1.COLUMN_DATAT + ") = '" + app.dateToDb("yyyy-MM", date) + "'";
		
		Cursor c = db.rawQuery("SELECT " + 
				Db.Table2.TABLE_NAME + "." + Db.Table2._ID + "," + 
				Db.Table2.TABLE_NAME + "." + Db.Table2.COLUMN_NCAT + "," +
				Db.Table2.TABLE_NAME + "." + Db.Table2.COLUMN_CORCAT + "," +
				"SUM(" + Db.Table1.TABLE_NAME + "." + Db.Table1.COLUMN_VALORT + ")" +
				" FROM " +
				Db.Table1.TABLE_NAME + "," +
				Db.Table2.TABLE_NAME +
				" WHERE " +
				Db.Table1.TABLE_NAME + "." + Db.Table1.COLUMN_IDCAT + " = " + Db.Table2.TABLE_NAME + "." + Db.Table2._ID +
				" AND " + Db.Table1.TABLE_NAME + "." + Db.Table1.COLUMN_IDGRUPO + " = " + app.activeGroupId +
				queryModifier + 
				" GROUP BY " + Db.Table1.TABLE_NAME + "." + Db.Table1.COLUMN_IDCAT +
				" ORDER BY " + Db.Table2.COLUMN_NCAT, null);

		float[] values = new float[c.getCount()];
        int[] colors = new int[c.getCount()];
        float total = 0;
        
        while(c.moveToNext()) {
    		values[c.getPosition()] = c.getFloat(3);
    		colors[c.getPosition()] = c.getInt(2);
    		total += c.getFloat(3);
        }		

        BarChart v = new BarChart(this, values, colors);
		v.setPadding(10, 10, 10, 10);
		FrameLayout graphLayout = ((FrameLayout) findViewById(R.id.FrameLayout1));
		if(graphLayout.getChildCount() == 1) graphLayout.removeViewAt(0);
		graphLayout.addView(v);
		
		ListView lv = ((ListView) findViewById(R.id.listView1));
		((TextView) footer.findViewById(R.id.textView2)).setText(app.printMoney(total));
		
		int days = 1;
		if(!isByMonth) {
			SimpleDateFormat dateF = new SimpleDateFormat("yyyy-MM-dd");
			dateF.setTimeZone(TimeZone.getDefault());
			
			Cursor cTemp = db.rawQuery("SELECT "+
					Db.Table1.COLUMN_DATAT +
					" FROM " +
					Db.Table1.TABLE_NAME +
					" WHERE " + Db.Table1.COLUMN_IDGRUPO + " = " + app.activeGroupId +
					" ORDER BY " + Db.Table1.COLUMN_DATAT + " DESC",null);
			try {
				cTemp.moveToFirst();
				Date date2 = dateF.parse(cTemp.getString(0));
				cTemp.moveToLast();
				Date date1 = dateF.parse(cTemp.getString(0));
				
				days = (int)Math.ceil((date2.getTime() - date1.getTime())/(1000.0*24*60*60)) + 1;
				
				App.Log(""+days);
			}
			catch (Exception e) {
				e.printStackTrace();
			}			
		}
		else {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			Calendar now = Calendar.getInstance();
			if(cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR))
				days = now.get(Calendar.DAY_OF_MONTH);
			else
				days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		}
		
		((TextView) footer2.findViewById(R.id.textView2)).setText(app.printMoney(total/days));
		
		((TextView) findViewById(R.id.textViewMonth)).setText(app.dateToUser("MMMM / yyyy", date));
	
		if(adapter == null) {
			adapter = new CategoryStatsAdapter(this,c,total);
			lv.setAdapter(adapter);
		}
		else {
			adapter.changeCursor(c,total);
			adapter.notifyDataSetChanged();			
		}
	}
	
	private class BarChart extends View {
		private Paint[] paintColors;
		private float[] heights, percentages;
		private int[] colors;
		private float left, right, top, chartHeight, total;
		
		public BarChart(Context ctx, float[] dt, int[] clrs) {
			super(ctx);
			Paint paintModel = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintModel.setStyle(Paint.Style.FILL);
					
			heights = new float[dt.length];
			percentages = new float[dt.length];
			paintColors = new Paint[dt.length];
			colors = clrs;
			
			total = 0;
			int i;
			for(float value : dt)
				total += value;
			for(i = 0;i < dt.length;i++) {
				percentages[i] = dt[i]/total;
				paintColors[i] = new Paint(paintModel);
			}
		}
		
		@Override
		public void onSizeChanged(int w, int h, int oldw, int oldh) {
			left = getPaddingLeft();
			right = w - getPaddingRight();
			top = getPaddingTop();
			chartHeight = h - getPaddingTop() - getPaddingBottom();
			
			int i;
			for(i = 0;i < paintColors.length;i++) {
				paintColors[i].setShader(new LinearGradient(-w/2,0,w - getPaddingRight(),0,Color.BLACK,colors[i],Shader.TileMode.REPEAT));
				heights[i] = percentages[i]*chartHeight;
			}
		}
		
		@Override
		public void onDraw(Canvas canvas) {
			int i;
			float topRect = top;
			for(i = 0;i < heights.length;i++) {
				canvas.drawRect(left, topRect, right, topRect + heights[i], paintColors[i]);
				topRect += heights[i];
			}
		}
		

	}
	
    private class CategoryStatsAdapter extends CursorAdapter {
    	private LayoutInflater mInflater;
    	private float total;

    	public CategoryStatsAdapter(Context context, Cursor c, float ttl) {
	        super(context, c, 0);
	        mInflater=LayoutInflater.from(context);
	        total = ttl;
	    }
	    
	    @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.categorystats_listitem,parent,false); 
        }
    	
    	@Override
        public void bindView(View view, Context context, Cursor cursor) {
    		((TextView) view.findViewById(R.id.textView1)).setText(cursor.getString(1));
    		((ImageView) view.findViewById(R.id.imageView1)).getDrawable().setColorFilter(cursor.getInt(2), PorterDuff.Mode.MULTIPLY);
    		((TextView) view.findViewById(R.id.textView2)).setText(app.printMoney(cursor.getFloat(3)) + " (" + String.format("%.1f", cursor.getFloat(3)*100/total) + "%)");
    	}
    	
    	public void changeCursor(Cursor c, float ttl) {
    		changeCursor(c);
    		total = ttl; 
    	}
    }
}