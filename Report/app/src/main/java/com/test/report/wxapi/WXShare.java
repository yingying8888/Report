package com.test.report.wxapi;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXAppExtendObject;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.test.report.R;

import java.io.ByteArrayOutputStream;


/**
 * 微信分享核心类
 *
 * @author Andy
 *
 */
public class WXShare {

	/** 在微信开放平台注册后拿到的appid */
	private static final String APP_ID = "wx6df0b100928c56e9";
	private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001; // 4.2以上支持发送到朋友圈
	private static WXShare instance = null;
	private static IWXAPI api = null;
	private Context mContext;
	private WXShare(Context context) {
		this.mContext = context;
	}

	/**
	 * 注册微信
	 * @return
	 */
	public static WXShare getInstance(Context context) {
		if (instance == null) {
			//微信分享初始化， 通过WXAPIFactory工厂，获取IWXAPI的实例
			api = WXAPIFactory.createWXAPI(context, APP_ID, false);
			api.registerApp(APP_ID);
			instance = new WXShare(context);
		}

		return instance;
	}

	/**
	 * 分享WXAppExtendObject类型的数据，只能分享给好友
	 *
	 * @param title
	 *            标题
	 * @param imgPath
	 *            图片路径
	 * @param description
	 *            内容的简洁描述
	 * @param extInfo
	 *            其他信息，包含跳转传值及跳转目标类名，字符串 限制2KB大小
	 */
	public void shareAppDataToFriend(String title, String imgPath, String description, String extInfo) {
		try {
			WXAppExtendObject appdata = new WXAppExtendObject();
			//appdata.filePath:Local directory of the file provided for applications
			//NOTE: The length should be within 10KB and content size within 10MB.
			//appdata.filePath与appdata.fileData用其一
			String path = imgPath;
			appdata.fileData = Util.readFromFile(path, 0, -1);
			appdata.extInfo = extInfo;

			final WXMediaMessage msg = new WXMediaMessage();
			// 如有必要，图片同样要做缩放处理，可参考图片分享
			msg.setThumbImage(Util.extractThumbNail(path, 150, 150, true));
			msg.title = title;
			msg.description = description;
			msg.mediaObject = appdata;

			SendMessageToWX.Req req = new SendMessageToWX.Req();
			req.transaction = buildTransaction("appdata");
			req.message = msg;
			req.scene = SendMessageToWX.Req.WXSceneSession;
			if (api != null) {
				api.sendReq(req);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 微信分享文本消息
	 *
	 * @param isTimeLine
	 *            是否分享到朋友圈，false为分享给好友
	 * @param text
	 *            文本内容
	 */
	public void shareTextMessage(boolean isTimeLine, String text, String description) {
		try {
			if (text == null || text.length() == 0) {
				return;
			}
			// 初始化一个WXTextObject对象
			WXTextObject textObj = new WXTextObject();
			textObj.text = text;

			// 用WXTextObject对象初始化一个WXMediaMessage对象
			WXMediaMessage msg = new WXMediaMessage();
			msg.mediaObject = textObj;
			// 发送文本类型的消息时，title字段不起作用
			// msg.title = "Will be ignored";
			msg.description = description;

			// 构造一个Req
			SendMessageToWX.Req req = new SendMessageToWX.Req();
			req.transaction = buildTransaction("text"); // transaction字段用于唯一标识一个请求
			req.message = msg;
			if (isTimeLine == false) {
				req.scene = SendMessageToWX.Req.WXSceneSession;
			} else {
				if (isSupportTimeLine()) {
					req.scene = SendMessageToWX.Req.WXSceneTimeline;
				} else {
					Toast.makeText(mContext, "您的微信版本不支持分享到朋友圈", Toast.LENGTH_SHORT).show();
					return;
				}
			}
			// 调用api接口发送数据到微信
			api.sendReq(req);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 微信分享图片消息
	 * https://developers.weixin.qq.com/doc/oplatform/Mobile_App/Share_and_Favorites/Android.html
	 *
	 * @param isTimeLine
	 *            是否分享到朋友圈，false为分享给好友
	 * @param bMap
	 *            图片
	 */
	public void shareImgMessage(boolean isTimeLine, Bitmap bMap) {
		try {
			if(bMap == null){
				return;
			}
			// 为保证能较为清晰的分享，先按长宽比例压缩大小，再压缩质量，直到缩略图小于32k
			final int thumb_size_height = 800;
			Bitmap bmp = bMap;
			int bitmapHeight = bmp.getHeight();
			int bitmapWidth = bmp.getWidth();
			final int thumb_size_width = bitmapWidth * thumb_size_height / bitmapHeight;
			WXImageObject imgObj = new WXImageObject(bmp);

			WXMediaMessage msg = new WXMediaMessage();
			msg.mediaObject = imgObj;
			//msg.description = "这是图片";

			Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, thumb_size_width, thumb_size_height, true);
			bmp.recycle();

			byte[] outByteArray = Util.compressImage2ByteArray(thumbBmp, true);
//			if (outByteArray.length / 1024 >= 32) {
//				Toast.makeText(mContext, mContext.getString(R.string.imageSoBigNotShare), Toast.LENGTH_SHORT).show();
//				return;
//			}
			msg.thumbData = outByteArray;// 设置缩略图  NOTE: The file size should be within 32KB.
			SendMessageToWX.Req req = new SendMessageToWX.Req();
			req.transaction = buildTransaction("img");
			req.message = msg;
			if (isTimeLine == false) {
				req.scene = SendMessageToWX.Req.WXSceneSession;
			} else {
				if (isSupportTimeLine()) {
					req.scene = SendMessageToWX.Req.WXSceneTimeline;
				} else {
					Toast.makeText(mContext, mContext.getString(R.string.wechatVersionLowNotShare), Toast.LENGTH_SHORT).show();
					return;
				}
			}
			api.sendReq(req);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 微信分享网页消息
	 *
	 * @param isTimeLine
	 *            是否分享到朋友圈，false为分享给好友
	 * @param webPageUrl
	 *            网页的网址
	 * @param title
	 *            标题
	 * @param description
	 *            内容描述
	 * @param bMap
	 *            网页分享中的左侧小图片
	 */
	public void shareWebPage(boolean isTimeLine, String webPageUrl, String title, String description, Bitmap bMap) {
		try {
			WXWebpageObject webpage = new WXWebpageObject();
			webpage.webpageUrl = webPageUrl;
			WXMediaMessage msg = new WXMediaMessage(webpage);
			msg.title = title;
			msg.description = description;
			Bitmap thumb = bMap;
			byte[] outByteArray = Util.compressImage2ByteArray(thumb, true);
			if (outByteArray.length / 1024 >= 32) {
				Toast.makeText(mContext, "抱歉，由于图片太大，分享失败！", Toast.LENGTH_SHORT).show();
				return;
			}
			msg.thumbData = outByteArray;

			SendMessageToWX.Req req = new SendMessageToWX.Req();
			req.transaction = buildTransaction("webpage");
			req.message = msg;
			if (isTimeLine == false) {
				req.scene = SendMessageToWX.Req.WXSceneSession;
			} else {
				if (isSupportTimeLine()) {
					req.scene = SendMessageToWX.Req.WXSceneTimeline;
				} else {
					Toast.makeText(mContext, "您的微信版本不支持分享到朋友圈", Toast.LENGTH_SHORT).show();
					return;
				}
			}
			api.sendReq(req);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 分享 Excel 链接到微信
	 * @param title 标题
	 * @param description 描述
	 * @param url Excel 下载链接
	 * @param shareToTimeline 是否分享到朋友圈（true）还是好友（false）
	 */
	public void shareExcelLink(String title, String description, String url, boolean shareToTimeline) {
		WXWebpageObject webpage = new WXWebpageObject();
		webpage.webpageUrl = url;

		WXMediaMessage msg = new WXMediaMessage(webpage);
		msg.title = title;
		msg.description = description;

		// 可选：缩略图（必须小于32KB）
		Bitmap thumb = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher); // 你自己的图标
		Bitmap thumbScaled = Bitmap.createScaledBitmap(thumb, 120, 120, true);
		msg.thumbData = bmpToByteArray(thumbScaled, true); // 压缩图像

		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = buildTransaction("webpage");
		req.message = msg;
		req.scene = shareToTimeline ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;

		api.sendReq(req);
	}

	private byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 85, output);
		if (needRecycle) {
			bmp.recycle();
		}
		return output.toByteArray();
	}

	/**
	 * Transaction ID corresponding to this request.
	 * @param type
	 * @return
	 */
	private String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
	}

	/**
	 * 启动微信
	 *
	 * @return
	 */
	public boolean lanuchWX() {
		return api.openWXApp();
	}

	/**
	 * 是否支持发送到朋友圈
	 *
	 * @return
	 */
	public boolean isSupportTimeLine() {
		int wxSdkVersion = api.getWXAppSupportAPI();
		if (wxSdkVersion >= TIMELINE_SUPPORTED_VERSION) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断微信App是否已安装
	 *
	 * @return
	 */
	public boolean isWXAppInstalled() {
		try {
			return api.isWXAppInstalled();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 处理onReq或onResp响应 在WXEntryActivity的onCreate中调用
	 *
	 * @param intent
	 * @param handler
	 */
	public void handleIntent(Intent intent, IWXAPIEventHandler handler) {
		if (api != null) {
			api.handleIntent(intent, handler);
		}
	}
}
