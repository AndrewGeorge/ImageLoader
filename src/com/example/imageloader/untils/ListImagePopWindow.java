package com.example.imageloader.untils;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.imageloader.R;
import com.example.imageloader.bean.FolderBean;

public class ListImagePopWindow extends PopupWindow {

	private int mWidth;
	private int mHeight;
	private ListView mListView;
	private List<FolderBean> mDatas;
	private View mConvertView;

	public interface OnDirSelectedListener{
		
		void onSelected(FolderBean folderBean);
	}
	
	public OnDirSelectedListener mListener;
	
	
	public void setOnDirSelectedListener(OnDirSelectedListener mListener) {
		this.mListener = mListener;
	}

	public ListImagePopWindow(Context context, List<FolderBean> datas) {

		calWidthAndHeight(context);
		mConvertView = LayoutInflater.from(context).inflate(R.layout.pop_menu,
				null);
		mDatas = datas;

		setContentView(mConvertView);
		setWidth(mWidth);
		setHeight(mHeight);

		setFocusable(true);
		setTouchable(true);
		setOutsideTouchable(true);
		setBackgroundDrawable(new BitmapDrawable());
		setTouchInterceptor(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {

				if (arg1.getAction() == MotionEvent.ACTION_OUTSIDE) {
					dismiss();
					return true;
				}
				return false;
			}
		});

		initViews(context);
		initEvent();
	}

	private void initViews(Context context) {

		mListView = (ListView) mConvertView.findViewById(R.id.id_pop_listview);
		mListView.setAdapter(new ListDirAdapter(context, mDatas));
	}
	private void initEvent() {

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				if (mListener!=null) {
					
					mListener.onSelected(mDatas.get(arg2));
				}
			}
		});
}

	/**
	 * 计算popwindow的宽高度
	 * 
	 * @param context
	 */
	private void calWidthAndHeight(Context context) {

		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(outMetrics);
		mWidth = outMetrics.widthPixels;
		mHeight = (int) (outMetrics.heightPixels * 0.7);

	}

	private class ListDirAdapter extends ArrayAdapter<FolderBean> {

		private LayoutInflater mInflater;
		private List<FolderBean> mDatats;

		public ListDirAdapter(Context context, List<FolderBean> objects) {
			super(context, 0, objects);
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Viewholder viewholder = null;
			if (convertView == null) {
				viewholder = new Viewholder();
				convertView = mInflater.inflate(R.layout.pop_item, null);
				viewholder.imageView = (ImageView) convertView
						.findViewById(R.id.id_pop_item_image);
				viewholder.select_img = (ImageView) convertView
						.findViewById(R.id.id_pop_item_select);
				viewholder.dir_name = (TextView) convertView
						.findViewById(R.id.id_pop_item_name);
				viewholder.dir_count = (TextView) convertView
						.findViewById(R.id.id_pop_item_count);
				convertView.setTag(viewholder);
			} else {
				viewholder = (Viewholder) convertView.getTag();
			}

			FolderBean mBean = mDatas.get(position);
			viewholder.imageView
					.setImageResource(R.drawable.default_avatar_select);
			ImageLoader.getInstence().LoadImage(mBean.getFirstImagPath(),
					viewholder.imageView);
			viewholder.dir_name.setText(mBean.getName());
			viewholder.dir_count.setText(mBean.getCount()+"");

			return convertView;

		}

	}

	private static class Viewholder {
		ImageView imageView;
		ImageView select_img;
		TextView dir_name;
		TextView dir_count;
	}
}
