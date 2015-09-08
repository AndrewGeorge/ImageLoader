package com.example.imageloader.untils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.example.imageloader.R;
import com.example.imageloader.untils.ImageLoader.Type;
import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {
	private String mDirPath;
	private List<String> mDatas;
	private Context mContext;
	private LayoutInflater mInflater;
	private static Set<String> mSelelctImg = new HashSet<String>();
	private int mScreenWidth;

	public ImageAdapter(Context context, List<String> datas, String dirPath) {
		this.mContext = context;
		this.mDatas = datas;
		this.mDirPath = dirPath;
		mInflater = LayoutInflater.from(context);
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(outMetrics);
		mScreenWidth = outMetrics.widthPixels;

	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mDatas.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return mDatas.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	@Override
	public View getView(final int poistion, View convertView, ViewGroup arg2) {
		final ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater
					.inflate(R.layout.item_gridview, null, false);
			holder = new ViewHolder();
			holder.mImageButton = (ImageButton) convertView
					.findViewById(R.id.id_item_select);
			holder.mImageView = (ImageView) convertView
					.findViewById(R.id.id_item_image);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// 重置状态
		holder.mImageView.setImageResource(R.drawable.default_avatar_select);
		holder.mImageButton.setImageResource(R.drawable.check_normal);
		holder.mImageView.setColorFilter(null);

		holder.mImageView.setMaxWidth(mScreenWidth / 3);

		ImageLoader.getinstence(3, Type.LIFO).LoadImage(
				mDirPath + "/" + mDatas.get(poistion), holder.mImageView);
		final String filePath = mDirPath + "/" + mDatas.get(poistion);
		holder.mImageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				// 已经被选择了
				if (mSelelctImg.contains(filePath)) {
					mSelelctImg.remove(filePath);
					holder.mImageView.setColorFilter(null);
					holder.mImageButton
							.setImageResource(R.drawable.check_normal);
				} else {// 未被选择过
					mSelelctImg.add(filePath);
					holder.mImageView.setColorFilter(Color
							.parseColor("#77000000"));
					holder.mImageButton
							.setImageResource(R.drawable.checkbox_photo_checked_true);
				}
			}
		});
		if (mSelelctImg.contains(filePath)) {

			holder.mImageView.setColorFilter(Color.parseColor("#77000000"));
			holder.mImageButton
					.setImageResource(R.drawable.checkbox_photo_checked_true);
		}

		return convertView;
	}

	private static class ViewHolder {
		ImageView mImageView;
		ImageButton mImageButton;
	}
}
