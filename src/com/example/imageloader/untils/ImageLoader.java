package com.example.imageloader.untils;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import android.R.integer;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

@SuppressLint("NewApi")
public class ImageLoader {
	public static ImageLoader mImageLoader = null;
	/**
	 * ͼͼƬ����ĺ��Ķ���
	 */
	private LruCache<String, Bitmap> mLruCache = null;

	/**
	 * ͼƬ�����̳߳�
	 */
	private ExecutorService mThreadPool = null;
	/**
	 * Ĭ���̳߳صĴ�С
	 */
	private static final int DEFAULT_THREAD_COUNT = 1;
	/**
	 * ���еĵ��ȷ�ʽ
	 */
	private Type mType = Type.LIFO;

	/**
	 * ö������
	 */
	public enum Type {
		// �Ƚ��ȳ�������ȳ�
		FIFO, LIFO;
	}

	/**
	 * �����б�linklist����ṹ���е�һ�������һ��Ԫ�صĻ�ȡ����
	 */
	private LinkedList<Runnable> mTaskQuene = null;
	/**
	 * ��̨��ѯ�߳�
	 */
	private Thread mPoolThread = null;
	private Handler mPoolThreadHandler = null;
	/**
	 * ����UIHandler
	 */
	private Handler mUIHandler = null;

	private Semaphore mPoolThreadHandlerSemaphore = new Semaphore(0);
	private Semaphore mSemaphoreThreadPool;

	private ImageLoader(int threadcount, Type type) {

		init(threadcount, type);

	}

	public static ImageLoader getInstence() {
		// �����ж����Ч��

		if (mImageLoader == null) {

			// ͬ��
			synchronized (ImageLoader.class) {

				if (mImageLoader == null) {
					mImageLoader = new ImageLoader(DEFAULT_THREAD_COUNT,
							Type.LIFO);
				}
			}
		}
		return mImageLoader;

	}
	public static ImageLoader getinstence(int threadcount,Type type) {
		// �����ж����Ч��

		if (mImageLoader == null) {

			// ͬ��
			synchronized (ImageLoader.class) {

				if (mImageLoader == null) {
					mImageLoader = new ImageLoader(threadcount,type);
				}
			}
		}
		return mImageLoader;

	}

	/**
	 * ��ʼ������
	 * 
	 * @param threadcount
	 * @param type
	 */
	private void init(int threadcount, Type type) {
		mPoolThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				mPoolThreadHandler = new Handler() {
					public void handleMessage(android.os.Message msg) {
						// �̳߳�ȥȡ��һ���������ִ��
						mThreadPool.execute(getTask());
						try {
							mSemaphoreThreadPool.acquire();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				// �ͷ�һ���ź���
				mPoolThreadHandlerSemaphore.release();
				Looper.loop();

			}
		});
		mPoolThread.start();
		// ��ȡ���õ�����߳�
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheMemeory = maxMemory / 8;
		mLruCache = new LruCache<String, Bitmap>(maxMemory) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				// TODO Auto-generated method stub
				// ����ÿ��bitmap��ʵ�ʴ�С
				return value.getRowBytes() * value.getHeight();
			}

		};
		// �����̳߳�
		mThreadPool = Executors.newFixedThreadPool(threadcount);
		mTaskQuene = new LinkedList<Runnable>();
		mType = mType;
		mSemaphoreThreadPool = new Semaphore(threadcount);
	}

	/**
	 * ���������ȡ��һ������
	 * 
	 * @return
	 */
	private Runnable getTask() {
		if (mType == Type.FIFO) {
			return mTaskQuene.removeFirst();

		} else if (mType == Type.LIFO) {
			return mTaskQuene.removeLast();
		}
		return null;
	};

	/**
	 * ����path����imageview
	 * 
	 * @param path
	 * @param imageView
	 */
	public void LoadImage(final String path, final ImageView imageView) {

		imageView.setTag(path);
		if (mUIHandler == null) {
			mUIHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					// ��ȡ��ͼƬΪImageview ����ͼƬ
					ImageBeanHolder beanHolder = new ImageBeanHolder();
					beanHolder = (ImageBeanHolder) msg.obj;
					Bitmap bm = beanHolder.bitmap;
					String path = beanHolder.path;
					ImageView imageView = beanHolder.imageView;
					if (imageView.getTag().equals(path)) {
						imageView.setImageBitmap(bm);
					}
				}
			};
		}
		Bitmap bm = getBitmapFromLruCache(path);
		if (bm != null) {
			refreashBitmap(path, imageView, bm);
		} else {
			addTask(new Runnable() {
				@Override
				public void run() {
					// ����ͼƬͼƬѹ��
					// 1 ��ȡͼƬ��Ҫ��ʾ�Ĵ�С
					ImageSize imageSize = getImageViewsize(imageView);
					// 2 ��������ߴ�ѹ��ͼƬ
					Bitmap bm = decodeSampleBitmapePath(path, imageSize.width,
							imageSize.height);
					// 3 ͼƬ���뵽����
					addBitmapTpLrucache(path, bm);
					// UI����
					refreashBitmap(path, imageView, bm);
					mSemaphoreThreadPool.release();
				}
			});
		}
	}

	/**
	 * ��ѹ�����ͼƬ���ص�������
	 * 
	 * @param path
	 * @param bm
	 */
	protected void addBitmapTpLrucache(String path, Bitmap bm) {
		if (getBitmapFromLruCache(path) == null) {
			if (bm != null) {
				addBitmapToLrucache(path, bm);
			}
		}
	}

	/**
	 * ���ͼƬ������
	 * 
	 * @param path
	 * @param bm
	 */
	private void addBitmapToLrucache(String path, Bitmap bm) {
		mLruCache.put(path, bm);
	}

	/**
	 * ����ͼƬ��Ҫ��ʵ�Ŀ�͸߽���ͼƬѹ��
	 * 
	 * @param path
	 * @param imageSize
	 * @return
	 */
	protected Bitmap decodeSampleBitmapePath(String path, int with, int height) {
		// ��ȡbitmap�Ŀ�͸߲�����bitmap���ص��ڴ���
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(path, options);
		options.inSampleSize = caculateSampleSize(options, with, height);
		// ʹ��inSamplesize�ٴν���ͼƬ
		options.inJustDecodeBounds = false;
		Bitmap bm = BitmapFactory.decodeFile(path, options);
		return bm;
	}

	/**
	 * �������Ŀ�͸��Լ�ͼƬʵ�ʵĿ�͸߼���Samplesize
	 * 
	 * @param options
	 * @param with
	 * @param height
	 * @return
	 */
	private int caculateSampleSize(Options options, int reqwith, int reqheight) {

		int width = options.outWidth;
		int height = options.outHeight;
		int inSamplesize = 1;
		if (width > reqwith || height > reqheight) {

			int widthRadio = Math.round(width * 1.0f / reqwith);
			int heightRadio = Math.round(height * 1.0f / reqheight);
			inSamplesize = Math.max(widthRadio, heightRadio);
		}

		return inSamplesize;
	}

	/**
	 * ����ikmageview��ȡ�ʵ��Ŀ�͸�
	 * 
	 * @param imageView
	 * @return
	 */
	protected ImageSize getImageViewsize(ImageView imageView) {
		ImageSize imageSize = new ImageSize();

		DisplayMetrics dis = imageView.getContext().getResources()
				.getDisplayMetrics();

		LayoutParams params = imageView.getLayoutParams();
		int width = imageView.getWidth();// ��ȡimageview����ƿ��
		if (width <= 0) {
			width = params.width;// ��ȡimageview��layput�������ĵ� ���
		}
		if (width <= 0) {
			width = getImageViewFiledValue(imageView, "mMaxHeight");// ������ֵ
		}

		if (width <= 0) {
			width = dis.widthPixels;// ��ȡ��Ļ��С
		}
		int height =  imageView.getHeight();// ��ȡimageview����߶�
		if (height <= 0) {
			height = params.height;// ��ȡimageview��layput�������ĵ� �߶�
		}
		if (height <= 0) {
			height = getImageViewFiledValue(imageView, "mMaxHeight");// ������ֵ
		}

		if (height <= 0) {
			height = dis.heightPixels;// ��ȡ��Ļ�߶�
		}
		imageSize.width = width;
		imageSize.height = height;

		return imageSize;

	}

	/**
	 * ͨ���ŷ����ȡimageview��ĳ��ֵ
	 * 
	 * @param object
	 * @param filename
	 */

	private static int getImageViewFiledValue(Object object, String filename) {

		int value = 0;
		try {
			Field field = ImageView.class.getDeclaredField(filename);
			field.setAccessible(true);
			int fieldValue = field.getInt(object);
			if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
				value = fieldValue;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return value;

	}

	private synchronized void addTask(Runnable runnable) {
		mTaskQuene.add(runnable);
		// ʹ���ź�����ȡmPoolThreadHandler����
		try {
			if (mPoolThreadHandler == null) {
				mPoolThreadHandlerSemaphore.acquire();
			}
			mPoolThreadHandler.sendEmptyMessage(0x110);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * ����path��ȡ�����bit
	 * 
	 * @param path
	 * @return
	 */
	private Bitmap getBitmapFromLruCache(String path) {
		return mLruCache.get(path);
	}

	@SuppressLint("NewApi")
	private void refreashBitmap(final String path, final ImageView imageView,
			Bitmap bm) {
		Message message = Message.obtain();
		ImageBeanHolder beanHolder = new ImageBeanHolder();
		beanHolder.bitmap = bm;
		beanHolder.imageView = imageView;
		beanHolder.path = path;
		message.obj = beanHolder;
		mUIHandler.sendMessage(message);
	}

	// ��ֹ���ִ���
	private class ImageBeanHolder {

		Bitmap bitmap;
		ImageView imageView;
		String path;

	}

	private class ImageSize {
		int height;
		int width;

	}
}
