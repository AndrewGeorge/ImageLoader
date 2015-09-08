package com.example.imageloader;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Text;

import com.example.imageloader.bean.FolderBean;
import com.example.imageloader.untils.ImageAdapter;
import com.example.imageloader.untils.ImageLoader;
import com.example.imageloader.untils.ImageLoader.Type;
import com.example.imageloader.untils.ListImagePopWindow;
import com.example.imageloader.untils.ListImagePopWindow.OnDirSelectedListener;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private GridView mGridView;
	private List<String> mImags;
	private ImageAdapter mImageAdapter;

	private RelativeLayout mBottomLy;
	private TextView mDirName;
	private TextView mDirConunt;
	private static final int DATA_LOADED = 0x110;

	private File mCurrentDir;
	private int mMaxCount;
	private ProgressDialog mProgressDialog;

	private ListImagePopWindow mImagePopWindow;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == DATA_LOADED) {

				// mProgressDialog.dismiss();
				// 绑定数据到View中
				datatToView();
				initPopwindow();
			}

		};
	};

	private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		initDatdas();
		initEvent();

	}

	protected void initPopwindow() {

		mImagePopWindow = new ListImagePopWindow(this, mFolderBeans);

		mImagePopWindow.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss() {
				// 当窗口消失后其余变亮
				lightOn();
			}
		});

		mImagePopWindow.setOnDirSelectedListener(new OnDirSelectedListener() {

			@Override
			public void onSelected(FolderBean folderBean) {
				// TODO Auto-generated method stub
				mCurrentDir = new File(folderBean.getDir());
				mImags = Arrays.asList(mCurrentDir.list(new FilenameFilter() {

					@Override
					public boolean accept(File arg0, String filename) {

						if (filename.endsWith(".jpg")
								|| filename.endsWith(".jpeg")
								|| filename.endsWith(".png")) {
							return true;
						} else {
							return false;
						}
					}
				}));

				mImageAdapter = new ImageAdapter(MainActivity.this, mImags,
						mCurrentDir.getAbsolutePath());
				mGridView.setAdapter(mImageAdapter);
				
				mDirConunt.setText(mImags.size()+"");
				mDirName.setText(folderBean.getName());
				mImagePopWindow.dismiss();
			}
		});

	}

	/**
	 * 内容区域变亮
	 */
	protected void lightOn() {

		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.alpha = 1.0f;
		getWindow().setAttributes(lp);

	}

	protected void datatToView() {
		if (mCurrentDir == null) {
			Toast.makeText(MainActivity.this, "未扫描到任何图片", Toast.LENGTH_SHORT)
					.show();
			return;
		}
		mImags = Arrays.asList(mCurrentDir.list());
		System.out.println("扫描到的照片数量：" + mImags.size() + "");
		mImageAdapter = new ImageAdapter(this, mImags,
				mCurrentDir.getAbsolutePath());
		mGridView.setAdapter(mImageAdapter);
		mDirConunt.setText(mMaxCount + "");
		mDirName.setText(mCurrentDir.getName());

	}

	private void initEvent() {

		mBottomLy.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				mImagePopWindow.setAnimationStyle(R.style.dir_popwindow_anim);
				mImagePopWindow.showAsDropDown(mBottomLy, 0, 0);
				lightOff();

			}
		});
	}

	/**
	 * 内容区域变暗
	 */
	protected void lightOff() {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.alpha = 0.3f;
		getWindow().setAttributes(lp);
	}

	/**
	 * 利用contentProvader来扫描手机中的图片
	 */
	private void initDatdas() {

		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Toast.makeText(this, "存储卡不可用", Toast.LENGTH_SHORT).show();
			return;
		}
		// mProgressDialog = ProgressDialog.show(MainActivity.this, null,
		// "扫面中....");
		new Thread(new Runnable() {
			@Override
			public void run() {
				Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				ContentResolver cr = MainActivity.this.getContentResolver();
				Cursor cursor = cr.query(mImageUri, null,
						MediaStore.Images.Media.MIME_TYPE + "=?or "
								+ MediaStore.Images.Media.MIME_TYPE + "=?",
						new String[] { "image/jpeg", "image/png" },
						MediaStore.Images.Media.DATE_MODIFIED);
				Set<String> mDirPaths = new HashSet<String>();
				while (cursor.moveToNext()) {
					String path = cursor.getString(cursor
							.getColumnIndex(MediaStore.Images.Media.DATA));

					File parentFile = new File(path).getParentFile();
					if (parentFile == null)
						continue;
					String dirPath = parentFile.getAbsolutePath();
					FolderBean folderBean = null;
					if (mDirPaths.contains(dirPath)) {
						continue;
					} else {
						mDirPaths.add(dirPath);
						folderBean = new FolderBean();
						folderBean.setDir(dirPath);
						folderBean.setFirstImagPath(path);
					}
					if (parentFile.list() == null) {
						continue;
					}
					int picSize = parentFile.list(new FilenameFilter() {
						@Override
						public boolean accept(File arg0, String filename) {

							if (filename.endsWith(".jpg")
									|| filename.endsWith(".jpeg")
									|| filename.endsWith(".png")) {
								return true;
							} else {
								return false;
							}
						}
					}).length;
					folderBean.setCount(picSize);
					mFolderBeans.add(folderBean);
					if (picSize > mMaxCount) {
						mMaxCount = picSize;
						mCurrentDir = parentFile;
					}
				}

				cursor.close();
				// 通知handler扫面完毕
				mHandler.sendEmptyMessage(DATA_LOADED);
			}
		}).start();

	}

	private void initView() {

		mGridView = (GridView) findViewById(R.id.id_griedview);
		mBottomLy = (RelativeLayout) findViewById(R.id.id_bottom_ly);
		mDirName = (TextView) findViewById(R.id.id_dir_name);
		mDirConunt = (TextView) findViewById(R.id.id_dir_count_);

	}

}
