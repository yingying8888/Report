package com.test.report.wxapi;


import static com.test.report.WXAppLogin.APP_ID;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.ShowMessageFromWX;
import com.tencent.mm.opensdk.modelmsg.WXAppExtendObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.test.report.R;

/**
 * 无界面透明的Activity，让用户感觉不到其存在
 * 只是处理onReq、onResp响应,处理完即finish
 */
public class WXEntryActivity extends AppCompatActivity implements IWXAPIEventHandler {
    private IWXAPI iwxapi;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //WXShare.getInstance(this).handleIntent(getIntent(), this);
        iwxapi = WXAPIFactory.createWXAPI(this, APP_ID, false);
        try {
            Intent intent = getIntent();
            iwxapi.handleIntent(intent, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 只有appdata被点击时可以调用第三方应用的进程，所触发的方法是实现了IWXAPIEventHandler接口的类的onReq方法，
     * 类型是ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX
     * 而在微信中和好友聊天时点击第三方应用图标所所触发的方法是实现了IWXAPIEventHandler接口的类的onReq方法，
     * 类型是ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX
     */
    // 微信发送请求到第三方应用时，会回调到该方法
    @Override
    public void onReq(BaseReq req) {

        switch (req.getType()) {
            // 微信内部调加号内的第三方应用
            case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:

                break;
            // 点击分享的appdata型消息调用
            case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
                goToShowMsg((ShowMessageFromWX.Req) req);
                break;
            default:
                break;
        }
    }

    // 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
    @Override
    public void onResp(BaseResp resp) {
        String result = "default";
        int type = resp.getType();
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                if (type == 2) {
                    //分享成功
                    finish();

                } else if (type == 1) {
                    String code = ((SendAuth.Resp) resp).code;
                    Intent intent = new Intent();
                    intent.putExtra("code", code);
                    intent.setAction("authlogin");
                    WXEntryActivity.this.sendBroadcast(intent);
                }


                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                result = WXEntryActivity.this.getResources().getString(R.string.ERR_USER_CANCEL);
                finish();
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                result = WXEntryActivity.this.getResources().getString(R.string.ERR_AUTH_DENIED);
                finish();
                break;
            case BaseResp.ErrCode.ERR_COMM:
                result = WXEntryActivity.this.getResources().getString(R.string.ERR_COMM);
                break;
            case BaseResp.ErrCode.ERR_SENT_FAILED:
                result = WXEntryActivity.this.getResources().getString(R.string.ERR_SENT_FAILED);
                break;
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                result = WXEntryActivity.this.getResources().getString(R.string.ERR_UNSUPPORT);
                break;
            default:
                result = WXEntryActivity.this.getResources().getString(R.string.unknown_mistake);
                break;
        }

        if (!result.equals("default")) {
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        }
        finish();
    }


    /**
     * 点击微信分享的内容后响应，如跳第三方应用
     *
     * @param showReq
     */
    private void goToShowMsg(ShowMessageFromWX.Req showReq) {
        WXMediaMessage wxMsg = showReq.message;
        WXAppExtendObject obj = (WXAppExtendObject) wxMsg.mediaObject;
        // 具体的响应逻辑
        // 跳完就finish WXEntryActivity,让用户感觉不到WXEntryActivity的存在,返回时直接回到原来的UI
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        iwxapi.handleIntent(intent, this);
    }

}