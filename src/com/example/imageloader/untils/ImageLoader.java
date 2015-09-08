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
	 * 图图片缓存的核心对象
	 */
	private LruCache<String, Bitmap> mLruCache = null;

	/**
	 * 图片下载线程池
	 */
	private ExecutorService mThreadPool = null;
	/**
	 * 默认线程池的大小
	 */
	private static final int DEFAULT_THREAD_COUNT = 1;
	/**
	 * 队列的调度方式
	 */
	private Type mType = Type.LIFO;

	/**
	 * 枚举类型
	 */
	public enum Type {
		// 先进先出，后进先出
		FIFO, LIFO;
	}

	/**
	 * 任务列表，linklist链表结构。有第一个和最后一个元素的获取方法
	 */
	private LinkedList<Runnable> mTaskQuene = null;
	/**
	 * 后台轮询线程
	 */
	private Thread mPoolThread = null;
	private Handler mPoolThreadHandler = null;
	/**
	 * 更新UIHandler
	 */
	private Handler mUIHandler = null;

	private Semaphore mPoolThreadHandlerSemaphore = new Semaphore(0);
	private Semaphore mSemaphoreThreadPool;

	private ImageLoader(int threadcount, Type type) {

		init(threadcount, type);

	}

	public static ImageLoader getInstence() {
		// 两层判断提高效率

		if (mImageLoader == null) {

			// 同步
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
		// 两层判断提高效率

		if (mImageLoader == null) {

			// 同步
			synchronized (ImageLoader.class) {

				if (mImageLoader == null) {
					mImageLoader = new ImageLoader(threadcount,type);
				}
			}
		}
		return mImageLoader;

	}

	/**
	 * 初始化操作
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
						// 线程池去取出一个任务进行执行
						mThreadPool.execute(getTask());
						try {
							mSemaphoreThreadPool.acquire();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				// 释放一个信号量
				mPoolThreadHandlerSemaphore.release();
				Looper.loop();

			}
		});
		mPoolThread.start();
		// 获取引用的最大线程
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheMemeory = maxMemory / 8;
		mLruCache = new LruCache<String, Bitmap>(maxMemory) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				// TODO Auto-generated method stub
				// 返回每个bitmap的实际大小
				return value.getRowBytes() * value.getHeight();
			}

		};
		// 创建线程池
		mThreadPool = Executors.newFixedThreadPool(threadcount);
		mTaskQuene = new LinkedList<Runnable>();
		mType = mType;
		mSemaphoreThreadPool = new Semaphore(threadcount);
	}

	/**
	 * 从任务队列取出一个方法
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
	 * 根据path设置imageview
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
					// 获取到图片为Imageview 设置图片
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
					// 加载图片图片压缩
					// 1 获取图片需要显示的大小
					ImageSize imageSize = getImageViewsize(imageView);
					// 2 根据所需尺寸压缩图片
					Bitmap bm = decodeSampleBitmapePath(path, imageSize.width,
							imageSize.height);
					// 3 图片加入到缓存
					addBitmapTpLrucache(path, bm);
					// UI更新
					refreashBitmap(path, imageView, bm);
					mSemaphoreThreadPool.release();
				}
			});
		}
	}

	/**
	 * 将压缩后的图片加载到缓存中
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
	 * 添加图片到缓存
	 * 
	 * @param path
	 * @param bm
	 */
	private void addBitmapToLrucache(String path, Bitmap bm) {
		mLruCache.put(path, bm);
	}

	/**
	 * 根据图片需要现实的宽和高进行图片压缩
	 * 
	 * @param path
	 * @param imageSize
	 * @return
	 */
	protected Bitmap decodeSampleBitmapePath(String path, int with, int height) {
		// 获取bitmap的宽和高并不把bitmap加载到内存中
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(path, options);
		options.inSampleSize = caculateSampleSize(options, with, height);
		// 使用inSamplesize再次解析图片
		options.inJustDecodeBounds = false;
		Bitmap bm = BitmapFactory.decodeFile(path, options);
		return bm;
	}

	/**
	 * 格局需求的宽和高以及图片实际的宽和高计算Samplesize
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
	 * 根据ikmageview获取适当的宽和高
	 * 
	 * @param imageView
	 * @return
	 */
	protected ImageSize getImageViewsize(ImageView imageView) {
		ImageSize imageSize = new ImageSize();

		DisplayMetrics dis = imageView.getContext().getResources()
				.getDisplayMetrics();

		LayoutParams params = imageView.getLayoutParams();
		int width = imageView.getWidth();// 获取imageview的设计宽度
		if (width <= 0) {
			width = params.width;// 获取imageview在layput中申明的的 宽度
		}
		if (width <= 0) {
			width = getImageViewFiledValue(imageView, "mMaxHeight");// 检查最大值
		}

		if (width <= 0) {
			width = dis.widthPixels;// 获取屏幕大小
		}
		int height =  imageView.getHeight();// 获取imageview的设高度
		if (height <= 0) {
			height = params.height;// 获取imageview在layput中申明的的 高度
		}
		if (height <= 0) {
			height = getImageViewFiledValue(imageView, "mMaxHeight");// 检查最大值
		}

		if (height <= 0) {
			height = dis.heightPixels;// 获取屏幕高度
		}
		imageSize.width = width;
		imageSize.height = height;

		return imageSize;

	}

	/**
	 * 通过排反射获取imageview的某个值
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
		// 使用信号量获取mPoolThreadHandler对象
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
	 * 根据path获取缓存的bit
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

	// 防止出现错乱
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
