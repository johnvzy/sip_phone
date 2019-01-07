/* Copyright (C) 2010-2011, Mamadou Diop.
 *  Copyright (C) 2011, Doubango Telecom.
 *
 * Contact: Mamadou Diop <diopmamadou(at)doubango(dot)org>
 *
 * This file is part of imsdroid Project (http://code.google.com/p/imsdroid)
 *
 * imsdroid is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * imsdroid is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.doubango.imsdroid.Screens;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.doubango.imsdroid.Engine;
import org.doubango.imsdroid.Main;
import org.doubango.imsdroid.R;
import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnSipSession.ConnectionState;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class ScreenHome extends BaseScreen {
    private static String TAG = ScreenHome.class.getCanonicalName();

    private static final int MENU_EXIT = 0;
    private static final int MENU_SETTINGS = 1;

    private GridView mGridView;

    private final INgnSipService mSipService;

    private BroadcastReceiver mSipBroadCastRecv;

    private boolean passSystem;
    protected static final int REFRESH_DATA = 0x00000001;
    private String NursePhoneCode = "";//主護理師的號碼
    private String NurseName = "";


    //動態改變SIP Server Config
    public String CONFIG_SIP_SERVER_RELAMIP = "";
    public String CONFIG_SIP_SERVER_IP = "";
    public String CONFIG_SIP_SERVER_PORT = "";
    public String CONFIG_SIP_SERVER_STUNIP = "";
    public String CONFIG_SIP_SERVER_STUNPORT = "";
    //最後註冊的BedId
    public static String LastRegBedId = "";
    public Boolean IsGetSipConfig = false;

    //2016-12-17
    private TextView IdView;
    WebView mWebView = null;
    List<String> stList = new ArrayList<String>();
    //是否已登入，但不包含sip登入是否成功，sip判斷應用imsdoid的isregister，這邊是給其它如點滴服務使用
    //主要預防sip登不不成功，其它功能也無法使用之問題
    public boolean isKHPass = false;
    private ListView callView;
    List<String> callList = new ArrayList<String>();
    ArrayList<sClass> sList = new ArrayList<sClass>();
    boolean isPlaySound = false;
    //病床清單
    List<String> bedList = new ArrayList<String>();
    ArrayList<BedClass> bedObjList = new ArrayList<BedClass>();
    //小叮嚀訊息
    List<String> textExpList = new ArrayList<String>();

    //待辦事項 0不顯示 1顯示
    public int TextMessageToggle = 0;
    public boolean GetFocus = false;

    List<String> tipList = new ArrayList<String>();
    ArrayList<tipClass> tipObjList = new ArrayList<tipClass>();

    public boolean isSafeExecute = true;

    public ScreenHome() {
        super(SCREEN_TYPE.HOME_T, TAG);
        mSipService = getEngine().getSipService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IdView.setText(LastRegBedId);
        if (mSipService.isRegistered() == false) {
            mSipService.register(ScreenHome.this);
        } else {
            if (Main.callNurse == true) {
                Main.callNurse = false;
                final Engine mEngine;
                final INgnConfigurationService mConfigurationService;
                mEngine = (Engine) Engine.getInstance();
                mConfigurationService = mEngine.getConfigurationService();
                String paraBedId = mConfigurationService.getString(NgnConfigurationEntry.IDENTITY_IMPI, "");
                //showAlert("讀取護理師資訊",paraBedId);
                NursePhoneCode = "";
                NurseName = "";
                new LongOperation().execute(paraBedId);
            }
        }
    }

    public static String padLeftZeros(String str, int n) {
        return String.format("%1$" + n + "s", str).replace(' ', '0');
    }

    public static String padLeftEmpty(String str, int n) {
        return String.format("%1$" + n + "s", str).replace(" ", "  ");
    }

    String msSubString(String myString, int start, int length) {
        return myString.substring(start, Math.min(start + length, myString.length()));
    }

    public void GetCallLog() {
        if (isSafeExecute == true) {
            new GetCallOperation(this).execute(LastRegBedId);
        }
    }

    public void GetBedListWork() {
        //載入病床(Runtime)
        Log.v("載入病床RunTime->", LastRegBedId);
        new GetBedListOperation(this).execute(LastRegBedId);
    }

    public void GetTipWork() {
        Log.v("載入小叮嚀記錄RunTime->", LastRegBedId);
        new GetTipOperation(this).execute(LastRegBedId);
    }

    public void BeginFunctionWork() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (true) {
                    try {
                        Message msg = new Message();
                        msg.what = 1;
                        mHandler.sendMessage(msg);
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        mHandler = new Handler() {
            //int i = 0;
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        GetCallLog();
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    public void onBackPressed() {
        EditText editMessage = (EditText) findViewById(R.id.txtSendMessage);
        Button btnSendMessage = (Button) findViewById(R.id.BtnSend);
        Button btnSendMessage2 = (Button) findViewById(R.id.BtnSend2);

        if (editMessage.isFocused() == false) {
            //btnSendMessage.setVisibility(View.VISIBLE);
            //btnSendMessage2.setVisibility(View.GONE);
        }
        return;
    }

    public void InitiSpinnerIndex(Spinner sp, String gpName) {
        if (gpName.length() >= 4) {
            String gp = msSubString(gpName, 4, 2);
            Log.v("綁定Group訊息:", gpName + ":" + gp);
            switch (gp) {
                case "00":
                    sp.setSelection(0);
                    break;

                case "01":
                    sp.setSelection(1);
                    break;

                case "02":
                    sp.setSelection(2);
                    break;

                case "03":
                    sp.setSelection(3);
                    break;

                case "04":
                    sp.setSelection(4);
                    break;

                case "05":
                    sp.setSelection(5);
                    break;

                case "06":
                    sp.setSelection(6);
                    break;

                case "07":
                    sp.setSelection(7);
                    break;
            }
        }
    }

    public void ChangeWebView() {
        if (mWebView != null) {
            String postData = "";
            try {
                postData = "Account=" + URLEncoder.encode("Admin", "UTF-8") + "&Password=" + URLEncoder.encode("1234", "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String nurseStation = msSubString(LastRegBedId, 0, 4);
            String AccountName = "kmuh_7a";
            String AccountPassword = "Kmuh_7a";

            nurseStation = nurseStation.trim();
            Log.v("最後註冊的帳號:", LastRegBedId);
            Log.v("NurseStation的資訊:", nurseStation);

            if (nurseStation.equals("0701")) {
                AccountName = "kmuh_7a";
                AccountPassword = "Kmuh_7a";
            }

            if (nurseStation.equals("0919")) {
                AccountName = "kmuh_9es";
                AccountPassword = "Kmuh_9es";
            }

            if (nurseStation.equals("0914")) {
                AccountName = "kmuh_9e";
                AccountPassword = "Kmuh_9e";
            }

            if (nurseStation.equals("1001")) {
                AccountName = "kmuh_10a";
                AccountPassword = "Kmuh_10a";
            }

            if (nurseStation.equals("1019")) {
                AccountName = "kmuh_10es";
                AccountPassword = "Kmuh_10es";
            }

            if (nurseStation.equals("1314")) {
                AccountName = "kmuh_13en";
                AccountPassword = "Kmuh_13en";
            }

            if (nurseStation.equals("1319")) {
                AccountName = "kmuh_13es";
                AccountPassword = "Kmuh_13es";
            }

            if (nurseStation.equals("2214")) {
                AccountName = "kmuh_22en";
                AccountPassword = "Kmuh_22en";
            }

            //2017
            if (nurseStation.equals("2119")) {
                AccountName = "kmuh_21es";
                AccountPassword = "Kmuh_21es";
            }


            Log.v("WebView的資訊:", "http://192.168.161.48/Login/Logins?Account=" + AccountName + "&Password=" + AccountPassword);

            mWebView.loadUrl("http://192.168.161.48/Login/Logins?Account=" + AccountName + "&Password=" + AccountPassword);

            //mWebView.loadUrl("http://122.117.112.216:8080/Login");
            //mWebView.loadUrl("http://192.168.161.48/Login/Logins?Account=Admin&Password=1234");
            //mWebView.postUrl("http://122.117.112.216:8080/Login", postData.getBytes());
            //mWebView.postUrl("http://192.168.161.48/Login/Logins?Account=Admin&Password=1234",postData.getBytes());
            //mWebView.loadUrl("http://bungcheng.meworks.co/page.aspx?no=158276");
            //mWebView.loadUrl("http://admin,1234@122.117.112.216:8080");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_home);

        //textExpList.add("禁止飲食");
        //textExpList.add("禁止飲水");
        //textExpList.add("禁止飲食、禁止飲水");
        //textExpList.add("請記得按時用藥");
        //textExpList.add("禁止下床走動");
        //textExpList.add("禁止飲食");
        //textExpList.add("禁止飲食");
        textExpList.add("明日早上抽血，半夜12點以後請禁水、禁食。");
        textExpList.add("明日早上手術，半夜12點以後請禁水、禁食。");
        textExpList.add("明日早上檢查，半夜12點以後請禁水、禁食。");
        BindExpMessage();

        String pBedId = readLogFile(getApplicationContext());
        //
        if (mSipService.isRegistered() == false) {
            //mSipService.register(ScreenHome.this);
            //2016-12-17大修，要到最下才查
            //new ConfigSipServer().execute("-1");

            //String pBedId=readFromFile();
            if (pBedId.length() == 0 || pBedId.trim().equals("")) {
                isKHPass = false;
                Spinner spGroup = (Spinner) findViewById(R.id.sGroup);
                spGroup.setEnabled(true);
            } else {
                isKHPass = true;
                Spinner spGroup = (Spinner) findViewById(R.id.sGroup);
                spGroup.setEnabled(false);
                //InitiSpinnerIndex(spGroup,pBedId);
            }
            //pBedId="07011503";
            LastRegBedId = pBedId;
            //newSettingCommit(pBedId);//2016-12-17註解
            new ConfigSipServer().execute("-1");
        }


        if (pBedId.length() == 0 || pBedId.trim().equals("")) {
            isKHPass = false;
        } else {
            mHandler.removeCallbacks(null);
            Spinner spGroup = (Spinner) findViewById(R.id.sGroup);
            spGroup.setEnabled(false);
            isKHPass = true;
            new GetCallOperation(this).execute(LastRegBedId);
            //開始取得點滴記錄
            BeginFunctionWork();
        }//end of pBedId.length() check

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                showDialog("綁定床號", "請輸入指令綁定SIP床號");
                return true;
            }
        });

        EditText editMessage = (EditText) findViewById(R.id.txtSendMessage);
        Button btnSendMessage = (Button) findViewById(R.id.BtnSend);
        Button btnSendMessage2 = (Button) findViewById(R.id.BtnSend2);

        editMessage.clearFocus();
        editMessage.setFocusable(true);
        editMessage.setFocusableInTouchMode(true);
        editMessage.setFocusable(true);
        /*
        editMessage.post(new Runnable() {
            @Override
            public void run() {
                if (editMessage.isFocused()==true)
                {
                    btnSendMessage.setVisibility(View.GONE);
                    btnSendMessage2.setVisibility(View.VISIBLE);
                }else{
                    btnSendMessage.setVisibility(View.VISIBLE);
                    btnSendMessage2.setVisibility(View.GONE);
                }
            }
        });*/

        editMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.v("文字框焦點已改變------------>", Boolean.toString(editMessage.isFocused()));

                if (editMessage.isFocused() == true) {
                    //btnSendMessage.setVisibility(View.GONE);
                    //btnSendMessage2.setVisibility(View.VISIBLE);
                    GetFocus = true;
                } else {
                    //btnSendMessage.setVisibility(View.VISIBLE);
                    //btnSendMessage2.setVisibility(View.GONE);
                    GetFocus = false;
                }
            }
        });

        btnSendMessage.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isKHPass == true) {
                    if (editMessage.getText().length() <= 0) {
                        Toast.makeText(ScreenHome.this, "請輸入要發送的小叮嚀訊息", Toast.LENGTH_SHORT).show();
                    } else {
                        String wText = editMessage.getText().toString();
                        Spinner spBedList = (Spinner) findViewById(R.id.sBedList);
                        if (spBedList.getSelectedItemPosition() >= 0) {
                            BedClass bClass = bedObjList.get(spBedList.getSelectedItemPosition());
                            String pPatientId = bClass.bChar;
                            Log.v("病歷號->", bClass.bChar);
                            String pPatientIdEight = GetKHBedCode(bClass.bId, 1);//完整8碼
                            Log.v("完整8碼：", pPatientIdEight);

                            String pPatientIdFooterTwo = msSubString(pPatientIdEight, 6, 2);//後2碼
                            Log.v("pPatientIdFooterTwo->", pPatientIdFooterTwo);
                            String pPatientMidTwo = msSubString(pPatientIdEight, 2, 2);//1中間2碼
                            String pPatientIdHeadTwo = msSubString(pPatientIdEight, 0, 2);//前2碼
                            String pPatientSecondMidTwo = msSubString(pPatientIdEight, 4, 2);//2中間2碼

                            int iHead = Integer.parseInt(pPatientIdHeadTwo);
                            String repHead = Integer.toString(iHead);

                            int iMid = Integer.parseInt(pPatientMidTwo);
                            String repMid = "";
                            switch (iMid) {
                                case 1:
                                    repMid = "A";
                                    break;

                                case 14:
                                    repMid = "EN";
                                    break;

                                case 19:
                                    repMid = "ES";
                                    break;
                            }

                            String FinalId = repHead + repMid + pPatientSecondMidTwo;// +pPatientIdFooterTwo;　
                            Log.v("最後的Id->", FinalId);
                            Log.v("寫入小叮嚀資訊：", pPatientId + "," + FinalId);

                            new WriteTextMessage().execute(bClass.bChar, FinalId, pPatientIdFooterTwo, wText);

                            editMessage.setText("");
                            btnSendMessage.setEnabled(false);
                        } else {
                            showAlert("無法送出小叮嚀訊息", "目前您的組別內沒有病床");
                        }
                    }
                } else {
                    Toast.makeText(ScreenHome.this, "您目前為「登出」狀態，「登入」後才可以發送小叮嚀訊息", Toast.LENGTH_SHORT).show();
                }
            }
        });

        editMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                Log.v("文字改變1:", Integer.toString(editMessage.getText().length()));
                if (editMessage.getText().length() != 0) {
                    btnSendMessage.setEnabled(true);
                } else {
                    btnSendMessage.setEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                Log.v("文字改變2:", Integer.toString(editMessage.getText().length()));
                if (editMessage.getText().length() != 0) {
                    btnSendMessage.setEnabled(true);
                } else {
                    btnSendMessage.setEnabled(false);
                }
            }
        });

        /*
        Button btnCallNurse1 = (Button) findViewById(R.id.BtnCallNurse);
        Button btnCallNurse2 = (Button) findViewById(R.id.BtnCallNurseBackup);
        Button btnCallBCService = (Button) findViewById(R.id.BtnCallBCService);

        btnCallNurse1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSipService.isRegistered()) {
                    ScreenAV.makeCall("9991", NgnMediaType.AudioVideo);
                }
            }
        });

        btnCallNurse2.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSipService.isRegistered()) {
                    ScreenAV.makeCall("9992", NgnMediaType.AudioVideo);
                }
            }
        });

        btnCallBCService.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSipService.isRegistered()) {
                    ScreenAV.makeCall("888", NgnMediaType.AudioVideo);
                }
            }
        });


        //btnCallNurse1.setEnabled(false);
        //btnCallNurse2.setEnabled(false);
        //btnCallBCService.setEnabled(false);
        */

        Button btnEnter = (Button) findViewById(R.id.BtnEnter);
        Button btnLogout = (Button) findViewById(R.id.BtnLogout);

        Log.v("KHPass->", Boolean.toString(isKHPass));

        if (isKHPass == false) {
            btnEnter.setEnabled(true);
            btnLogout.setEnabled(false);

            btnEnter.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);

        } else {
            btnEnter.setEnabled(false);
            btnLogout.setEnabled(true);

            btnEnter.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        }


        btnEnter.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                Spinner spStation = (Spinner) findViewById(R.id.sStation);
                Spinner spGroup = (Spinner) findViewById(R.id.sGroup);
                String sText = spStation.getSelectedItem().toString();
                String gText = padLeftZeros(Integer.toString(spGroup.getSelectedItemPosition()), 2);

                if (sText.contains("A") == true) {
                    sText = sText.replace("A", "00");
                }

                if (sText.contains("B") == true) {
                    sText = sText.replace("B", "50");
                }

                if (sText.contains("C") == true) {
                    sText = sText.replace("C", "03");
                }

                if (sText.contains("ES") == true) {
                    sText = sText.replace("ES", "19");
                }

                if (sText.contains("EN") == true) {
                    sText = sText.replace("EN", "14");
                }

                if (sText.length() == 3) {
                    sText = padLeftZeros(sText, 4);
                }

                sText = ReadTmpFile().replace("\n", "");
                Log.v("ReadTmpFile->>>>>>>>", sText);

                Log.v("sip帳號", sText + gText);
                writeLogFile(sText + gText, getApplicationContext());
                newSettingCommit(sText + gText);
                isKHPass = true;
                btnEnter.setEnabled(false);
                btnLogout.setEnabled(true);

                btnEnter.setVisibility(View.GONE);
                btnLogout.setVisibility(View.VISIBLE);
                BeginFunctionWork();
                //new GetBedListOperation(this).execute(LastRegBedId);
                GetBedListWork();
                GetTipWork();
                ChangeWebView();

                spGroup.setEnabled(false);
            }
        });

        btnLogout.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.v("存檔帳號",readLogFile(getApplicationContext()));
                writeLogFile("", getApplicationContext());
                passSystem = true;
                mSipService.stopStack();
                mSipService.unRegister();
                isKHPass = false;
                btnEnter.setEnabled(true);
                btnLogout.setEnabled(false);
                btnEnter.setVisibility(View.VISIBLE);
                btnLogout.setVisibility(View.GONE);
                mHandler.removeCallbacks(null);
                ListView mList = (ListView) findViewById(R.id.callList);
                mList.setAdapter(null);
                Spinner spGroup = (Spinner) findViewById(R.id.sGroup);

                spGroup.setEnabled(true);
            }
        });


        Button btnSIP = (Button) findViewById(R.id.BtnCall);
        Button btnTextMessage = (Button) findViewById(R.id.BtnTextMessage);
        Button btnBoard = (Button) findViewById(R.id.BtnBoard);
        Button btnDD = (Button) findViewById(R.id.BtnDD);
        Button btnLogin = (Button) findViewById(R.id.BtnLogin);

        LinearLayout layoutDD = (LinearLayout) findViewById(R.id.DDLayout);
        LinearLayout layoutBoard = (LinearLayout) findViewById(R.id.BoardLayout);
        LinearLayout layoutTextMessage = (LinearLayout) findViewById(R.id.TextMessageLayout);
        LinearLayout layoutLogin = (LinearLayout) findViewById(R.id.LoginLayout);
        LinearLayout layoutEdit = (LinearLayout) findViewById(R.id.EditMessageLayout);

        layoutDD.setVisibility(View.GONE);
        layoutBoard.setVisibility(View.GONE);
        layoutTextMessage.setVisibility(View.GONE);
        layoutLogin.setVisibility(View.GONE);
        layoutEdit.setVisibility(View.GONE);

        btnSIP.setTextColor(Color.BLUE);
        btnTextMessage.setTextColor(Color.WHITE);
        btnBoard.setTextColor(Color.WHITE);
        btnDD.setTextColor(Color.WHITE);
        btnLogin.setTextColor(Color.WHITE);

        btnSIP.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutDD.setVisibility(View.GONE);
                layoutBoard.setVisibility(View.GONE);
                layoutTextMessage.setVisibility(View.GONE);
                layoutLogin.setVisibility(View.GONE);
                mGridView.setVisibility(View.VISIBLE);
                layoutEdit.setVisibility(View.GONE);

                btnSIP.setTextColor(Color.BLUE);
                btnTextMessage.setTextColor(Color.WHITE);
                btnBoard.setTextColor(Color.WHITE);
                btnDD.setTextColor(Color.WHITE);
                btnLogin.setTextColor(Color.WHITE);
                TextMessageToggle = 0;
            }
        });

        btnBoard.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutDD.setVisibility(View.GONE);
                layoutBoard.setVisibility(View.VISIBLE);
                layoutTextMessage.setVisibility(View.GONE);
                layoutLogin.setVisibility(View.GONE);
                mGridView.setVisibility(View.GONE);
                layoutEdit.setVisibility(View.GONE);

                btnSIP.setTextColor(Color.WHITE);
                btnTextMessage.setTextColor(Color.WHITE);
                btnBoard.setTextColor(Color.BLUE);
                btnDD.setTextColor(Color.WHITE);
                btnLogin.setTextColor(Color.WHITE);

                TextMessageToggle = 0;
            }
        });

        btnTextMessage.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutDD.setVisibility(View.GONE);
                layoutBoard.setVisibility(View.GONE);
                layoutTextMessage.setVisibility(View.VISIBLE);
                layoutLogin.setVisibility(View.GONE);
                mGridView.setVisibility(View.GONE);
                layoutEdit.setVisibility(View.GONE);

                btnSIP.setTextColor(Color.WHITE);

                if (TextMessageToggle == 0) {
                    TextMessageToggle = 1;
                    layoutTextMessage.setVisibility(View.VISIBLE);
                    layoutEdit.setVisibility(View.GONE);
                    btnTextMessage.setTextColor(Color.BLUE);
                } else {
                    TextMessageToggle = 0;
                    layoutTextMessage.setVisibility(View.GONE);
                    layoutEdit.setVisibility(View.VISIBLE);
                    btnTextMessage.setTextColor(Color.RED);
                }

                GetTipWork();

                btnSIP.setTextColor((Color.WHITE));
                btnBoard.setTextColor(Color.WHITE);
                btnDD.setTextColor(Color.WHITE);
                btnLogin.setTextColor(Color.WHITE);
            }
        });

        btnDD.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutDD.setVisibility(View.VISIBLE);
                layoutBoard.setVisibility(View.GONE);
                layoutTextMessage.setVisibility(View.GONE);
                layoutLogin.setVisibility(View.GONE);
                mGridView.setVisibility(View.GONE);
                layoutEdit.setVisibility(View.GONE);

                btnSIP.setTextColor(Color.WHITE);
                btnTextMessage.setTextColor(Color.WHITE);
                btnBoard.setTextColor(Color.WHITE);
                btnDD.setTextColor(Color.BLUE);
                btnLogin.setTextColor(Color.WHITE);

                TextMessageToggle = 0;
            }
        });

        btnLogin.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutDD.setVisibility(View.GONE);
                layoutBoard.setVisibility(View.GONE);
                layoutTextMessage.setVisibility(View.GONE);
                layoutLogin.setVisibility(View.VISIBLE);
                mGridView.setVisibility(View.GONE);
                layoutEdit.setVisibility(View.GONE);

                btnSIP.setTextColor(Color.WHITE);
                btnTextMessage.setTextColor(Color.WHITE);
                btnBoard.setTextColor(Color.WHITE);
                btnDD.setTextColor(Color.WHITE);
                btnLogin.setTextColor(Color.BLUE);

                TextMessageToggle = 0;
            }
        });

        mWebView = (WebView) findViewById(R.id.webview);
        WebViewClient mWebViewClient = new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        };

        mWebView.setWebViewClient(mWebViewClient);
        //mWebView.setInitialScale(3);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setJavaScriptEnabled(true);

        ChangeWebView();

        //初始化，取得清單工作
        new GetBedListOperation(this).execute(LastRegBedId);
        new GetGrouupOperation(this).execute("");
        Spinner spGroup = (Spinner) findViewById(R.id.sGroup);
        final String[] gpName = {"護理站", "第一組", "第二組", "第三組", "第四組", "第五組", "預備組一", "預備組二"};
        ArrayAdapter<String> gList = new ArrayAdapter<>(ScreenHome.this,
                R.layout.spinnerlayout,
                gpName);
        spGroup.setAdapter(gList);
        InitiSpinnerIndex(spGroup, LastRegBedId);
        spGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(ScreenHome.this, "你選的是" + gpName[position], Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        passSystem = false;

        //2016-12-17
        IdView = (TextView) findViewById(R.id.txtAccountId);
        //IdView.setText("test123");
        mGridView = (GridView) findViewById(R.id.screen_home_gridview);
        //mGridView.setVisibility(View.GONE);
        mGridView.setAdapter(new ScreenHomeAdapter(this));
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ScreenHomeItem item = (ScreenHomeItem) parent.getItemAtPosition(position);
                if (item != null) {
                    if (position == ScreenHomeItem.ITEM_SIGNIN_SIGNOUT_POS) {
                        if (mSipService.getRegistrationState() == ConnectionState.CONNECTING || mSipService.getRegistrationState() == ConnectionState.TERMINATING) {
                            mSipService.stopStack();
                        } else if (mSipService.isRegistered()) {
                            passSystem = false;
                            showDialog("登出帳號", "請輸入密碼來登出");
                            if (passSystem == true) {
                                mSipService.unRegister();
                            }
                        } else {
                            mSipService.register(ScreenHome.this);
                        }
                    } else if (position == ScreenHomeItem.ITEM_EXIT_POS) {
                        ((Main) (getEngine().getMainActivity())).exit();
                    } else if (position == ScreenHomeItem.ITEM_CallLucky_POS) {
                        //護理站
                        if (mSipService.isRegistered()) {
                            String NewCode = GetKHBedCode(LastRegBedId, 2);
                            //ScreenAV.makeCall("303", NgnMediaType.Audio);
                            ScreenAV.makeCall(NewCode, NgnMediaType.Audio);
                        }
                    } else if (position == ScreenHomeItem.ITEM_Call3_POS) {
                        if (mSipService.isRegistered()) {
                            ScreenAV.makeCall("888", NgnMediaType.Audio);
                        }
                    } else if (position == ScreenHomeItem.ITEM_SIGNIN_SETTING_POS) {
                        showDialog("綁定床號", "請輸入指令綁定SIP床號");
                    } else {
                        mScreenService.show(item.mClass, item.mClass.getCanonicalName());
                    }
                }
            }
        });

        mSipBroadCastRecv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                // Registration Event
                if (NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT.equals(action)) {
                    NgnRegistrationEventArgs args = intent.getParcelableExtra(NgnEventArgs.EXTRA_EMBEDDED);
                    if (args == null) {
                        Log.e(TAG, "Invalid event args");
                        return;
                    }
                    switch (args.getEventType()) {
                        case REGISTRATION_NOK:
                        case UNREGISTRATION_OK:
                        case REGISTRATION_OK:
                        case REGISTRATION_INPROGRESS:
                        case UNREGISTRATION_INPROGRESS:
                        case UNREGISTRATION_NOK:
                        default:
                            ((ScreenHomeAdapter) mGridView.getAdapter()).refresh();
                            break;
                    }
                }
            }
        };
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT);
        registerReceiver(mSipBroadCastRecv, intentFilter);
    }//end of oncreate

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private String readLogFile(Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("Login.txt");

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    public void writeLogFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("Login.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "寫入文字檔案失敗" + e.toString());
        }

        WriteOnSD(context, "share.txt", data);

        //給不選護理站登入的暫存，暫存前4碼
        if (data.equals("") == false) {
            String newData = data;
            newData = msSubString(newData, 0, 4);
            WriteOnSD(context, "tmp.txt", newData);
        }
    }

    public void WriteOnSD(Context context, String sFileName, String sBody) {
        try {
            File gpxfile = new File(Environment.getExternalStorageDirectory(), sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
            //Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String ReadTmpFile() {
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, "tmp.txt");
        StringBuilder text = new StringBuilder();
        Log.v("開始讀取:", "tmp.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            Log.v("讀取時發生錯誤：", e.toString());
            //You'll need to add proper error handling here
        }

        return text.toString();
    }

    Handler mHandler = new Handler() {

        @Override

        public void handleMessage(Message msg) {

            switch (msg.what) {

                // 顯示網路上抓取的資料

                case REFRESH_DATA:

                    String result = null;

                    if (msg.obj instanceof String)

                        result = (String) msg.obj;

                    if (result != null) {
                        // 印出網路回傳的文字
                        Toast.makeText(ScreenHome.this
                                , result, Toast.LENGTH_LONG).show();
                        if (mSipService.isRegistered() == false) {
                            if (result.equals("")) {
                                showAlert("綁定床號失敗", "無法存取Android Id");
                            } else {
                                showAlert("綁定床號成功", "床號：" + result);
                                newSettingCommit(result);
                            }
                        }
                    }
                    this.removeMessages(0);
                    //mHandler.removeMessages(0);
                    break;
            }
        }
    };

    public void BindStation() {
        if (stList.size() > 0) {
            Spinner spStation = (Spinner) findViewById(R.id.sStation);
            final String[] gpStation = new String[stList.size()];
            stList.toArray(gpStation);

            ArrayAdapter<String> gList = new ArrayAdapter<>(ScreenHome.this,
                    R.layout.spinnerlayout,
                    gpStation);
            spStation.setAdapter(gList);
            //spStation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        }
    }

    public void BindExpMessage() {
        if (textExpList.size() > 0) {
            Spinner spMessage = (Spinner) findViewById(R.id.sExpressMessage);
            final String[] expMessage = new String[textExpList.size()];
            textExpList.toArray(expMessage);

            ArrayAdapter<String> expList = new ArrayAdapter<>(ScreenHome.this,
                    R.layout.spinnerlayout,
                    expMessage);
            spMessage.setAdapter(expList);
            spMessage.setSelection(0, false);//avoid firing
            spMessage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //Toast.makeText(ScreenHome.this, "你選的訊息範例是" + textExpList.get(position), Toast.LENGTH_SHORT).show();
                    EditText iMessage = (EditText) findViewById(R.id.txtSendMessage);
                    iMessage.setText(textExpList.get(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

        } else {
            Spinner spMessage = (Spinner) findViewById(R.id.sExpressMessage);
            spMessage.setAdapter(null);
        }
    }

    public void BindBedList() {
        if (bedList.size() > 0) {
            Spinner spBedList = (Spinner) findViewById(R.id.sBedList);
            final String[] sBedList = new String[bedList.size()];
            bedList.toArray(sBedList);

            ArrayAdapter<String> sList = new ArrayAdapter<>(ScreenHome.this,
                    R.layout.spinnerlayout,
                    sBedList);
            spBedList.setAdapter(sList);
        } else {
            Spinner spBedList = (Spinner) findViewById(R.id.sBedList);
            spBedList.setAdapter(null);
        }
    }

    public void BindTip() {
        Log.v("開始綁定小叮嚀資料", "->" + Integer.toString(tipList.size()));
        if (tipList.size() > 0) {
            ListView mList = (ListView) findViewById(R.id.MessageList);
            String[] mNames = new String[tipList.size()];
            //callList.toArray(mNames);
            int counter = 0;
            Iterator<tipClass> iter = tipObjList.iterator();
            while (iter.hasNext()) {
                tipClass tObj = iter.next();
                mNames[counter] = padLeftEmpty("[" + tObj.bId + "]", 10) + padLeftEmpty(tObj.bContent, 20) + "  於  " + tObj.bDate;
                counter++;
            }

            ListAdapter mAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1,
                    mNames) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView ListItemShow = (TextView) view.findViewById(android.R.id.text1);
                    if (tipObjList.size() > 0) {
                        tipClass tmpObj = tipObjList.get(position);
                        ListItemShow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        ListItemShow.setTextColor(Color.BLACK);
                    }
                    return view;
                }
            };

            mList.setAdapter(mAdapter);

            mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    if (tipObjList.size() > 0) {
                        tipClass tmpObj = tipObjList.get(position);
                    }
                    /*Toast.makeText(getApplicationContext(), "你選擇小叮嚀記錄是" + tmpObj.bId + ":" + tmpObj.bDate
                            + ":" + tmpObj.bRealId, Toast.LENGTH_SHORT).show();*/
                    //new UpdateNurseCall().execute(tmpObj.bRealId, Integer.toString(tmpObj.bStatus + 1));
                    //sList.get(position).bStatus=sList.get(position).bStatus+1;
                    //BindCall();

                }
            });
        }
    }

    public void BindCall() {

        //if (callList.size() > 0) {
        if (sList.size() > 0) {
            //如果一次有1個以上的更新，也只響一次
            isPlaySound = false;

            ListView mList = (ListView) findViewById(R.id.callList);
            //String[] mNames = new String[callList.size()];
            String[] mNames = new String[sList.size()];
            //callList.toArray(mNames);
            int counter = 0;

            //ConcurrentModificationException
            /*
            for(sClass sObj : sList)
            {
                mNames[counter]=padLeftEmpty("[" + sObj.bId + "]",10) + "  於  " + sObj.bDate;
                counter++;
            }*/

            Iterator<sClass> iter = sList.iterator();
            while (iter.hasNext()) {
                sClass sObj = iter.next();
                mNames[counter] = padLeftEmpty("[" + sObj.bId + "]", 10) + "  於  " + sObj.bDate;
                counter++;
            }

            /*
            ListAdapter mAdapter = new ArrayAdapter<String>(this,
                    R.layout.liststyle,R.id.title,
                    mNames);*/
            //View view = getView(position, convertView, parent);
            //View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            //TextView ListItemShow = (TextView) findViewById(android.R.id.text1);
            //ListItemShow.setTextColor(Color.parseColor("#fe00fb"));

            ListAdapter mAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1,
                    mNames) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView ListItemShow = (TextView) view.findViewById(android.R.id.text1);
                    if (sList.size() > 0) {
                        sClass tmpObj = sList.get(position);
                        if (tmpObj.bStatus == 0) {
                            ListItemShow.setTextColor(Color.RED);
                        }

                        if (tmpObj.bStatus == 1) {
                            ListItemShow.setTextColor(Color.BLUE);
                        }

                        if (tmpObj.bStatus >= 2) {
                            ListItemShow.setTextColor(Color.BLACK);
                        }
                        //ListItemShow.setTextColor(Color.parseColor("#fe00fb"));

                        Date mm = ConvertDate(tmpObj.bDate);
                        Date today = new Date();
                        long diff = today.getTime() - mm.getTime();

                        double mSceond = diff / 1000.0;
                        Log.v("今天", today.toString());
                        Log.v("項目時間", mm.toString());
                        Log.v("項目時間BDate->", tmpObj.bDate);
                        Log.v("秒數差：", Double.toString(mSceond) + " 秒");
                        if (tmpObj.bStatus == 0 && mSceond <= 15) {
                            try {
                                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                                if (isPlaySound == false) {
                                    isPlaySound = true;
                                    r.play();
                                    //Intent i = new Intent();
                                    //i.setAction(Intent.ACTION_MAIN);
                                    //i.addCategory(Intent.CATEGORY_LAUNCHER);
                                    //startActivity(i);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return view;
                }
            };

            mList.setAdapter(mAdapter);
            //mList.setBackgroundColor(Color.GRAY);
            mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (sList.size() > 0) {
                        sClass tmpObj = sList.get(position);
                    /*Toast.makeText(getApplicationContext(), "你選擇的是" + tmpObj.bId + ":" + tmpObj.bDate
                            + ":" + tmpObj.bRealId , Toast.LENGTH_SHORT).show();*/
                        new UpdateNurseCall().execute(tmpObj.bRealId, Integer.toString(tmpObj.bStatus + 1));
                        sList.get(position).bStatus = sList.get(position).bStatus + 1;
                        BindCall();
                    }
                }
            });
        } else {
            ListView mList = (ListView) findViewById(R.id.callList);
            mList.setAdapter(null);
        }
    }

    public Date ConvertDate(String pDate) {
        String dateString = pDate;
        //童綜用hh，12:00會有問題(變為00)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date convertedDate = new Date();
        try {
            convertedDate = dateFormat.parse(dateString);
        } catch (ParseException e) {

            e.printStackTrace();
        }
        return convertedDate;
    }

    public String readFromFile() {

        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, "bedlog.txt");

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                //text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }

        return text.toString();
    }

    private String sendPostDataToInternet(String strTxt) {

        String result = "";
        String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        HttpClient client = new DefaultHttpClient();

        //android_id="19fd402896c86565";
        HttpGet get = new HttpGet("http://192.168.254.5/api/profile/" + android_id);

        HttpResponse response = null;
        try {
            response = client.execute(get);
        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpEntity resEntity = response.getEntity();

        try {
            result = EntityUtils.toString(resEntity);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            JSONObject json = new JSONObject(result);
            result = json.getString("bedName");
            result = result.replace("-", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    class sendPostRunnable implements Runnable {

        String strTxt = null;
        // 建構子，設定要傳的字串

        public sendPostRunnable(String strTxt) {

            this.strTxt = strTxt;

        }


        @Override

        public void run() {
            String result = sendPostDataToInternet(strTxt);
            mHandler.obtainMessage(REFRESH_DATA, result).sendToTarget();
        }

    }


    @Override
    protected void onDestroy() {
        if (mSipBroadCastRecv != null) {
            unregisterReceiver(mSipBroadCastRecv);
            mSipBroadCastRecv = null;
        }

        /*
        //2016新增
        if (mSipService.isRegistered() == true) {
            mSipService.stopStack();
            mSipService.unRegister();
        }
*/
        super.onDestroy();
    }

    @Override
    public boolean hasMenu() {
        return true;
    }

    @Override
    public boolean createOptionsMenu(Menu menu) {
        //menu.add(0, ScreenHome.MENU_SETTINGS, 0, "Settings");
        /*MenuItem itemExit =*/
        //menu.add(0, ScreenHome.MENU_EXIT, 0, "Exit");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
        switch(item.getItemId()){
			case ScreenHome.MENU_EXIT:
				((Main)getEngine().getMainActivity()).exit();
				break;
			case ScreenHome.MENU_SETTINGS:
				mScreenService.show(ScreenSettings.class);
				break;
		}*/
        //showDialog("綁定床號","請輸入指令綁定SIP床號");

        return true;
    }

    private void showAlert(String pTitle, String pMessage) {
        AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(this);
        MyAlertDialog.setTitle(pTitle);
        MyAlertDialog.setMessage(pMessage);
        MyAlertDialog.show();
    }


    private void showDialog(String pTitle, String pMessage) {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle(pTitle)
                .setMessage(pMessage)
                .setView(input)
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // 在此處理 input
                        String strResult = input.getText().toString();
                        if (strResult.trim().length() > 0) {
                            String[] cmdArray = strResult.split(":");
                            if (cmdArray.length > 1) {
                                if (cmdArray[0].toString().toLowerCase().equals("bind")) {
                                    if (cmdArray[1].toString().trim().equals("")) {
                                        showAlert("錯誤", "參數有誤或為空值");
                                    } else {
                                        newSettingCommit(cmdArray[1].toString());
                                    }
                                } else if (cmdArray[0].toString().toLowerCase().equals("logout") &&
                                        cmdArray[1].toString().toLowerCase().equals("bcadmin")) {
                                    passSystem = true;
                                    mSipService.unRegister();
                                } else if (cmdArray[0].toString().toLowerCase().equals("setting") &&
                                        cmdArray[1].toString().toLowerCase().equals("bcadmin")) {
                                    mScreenService.show(ScreenIdentity.class, ScreenIdentity.class.getCanonicalName());
                                } else if (cmdArray[0].toString().toLowerCase().equals("network") &&
                                        cmdArray[1].toString().toLowerCase().equals("bcadmin")) {
                                    mScreenService.show(ScreenNetwork.class, ScreenNetwork.class.getCanonicalName());
                                } else if (cmdArray[0].toString().toLowerCase().equals("sipserver")) {
                                    String tmpPara = cmdArray[1].toString();
                                    new ConfigSipServer().execute(tmpPara);
                                } else {
                                    showAlert("錯誤", "輸入的指令錯誤");
                                }
                            } else {
                                showAlert("錯誤", "輸入錯誤或過多的:");
                            }
                        } else {
                            showAlert("錯誤", "輸入的指令錯誤或空白");
                        }
                    }
                })
                .show();
    }

    public boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String GetKHBedCode(String pId, int Mode) {
        //Mode=1 病人轉換  Mode=2 護理站
        String newBedId = pId;
        //2016-12-17 綁床規則
        if (newBedId.isEmpty() == true) {
            return newBedId;
        }

        String isNum = newBedId.substring(0, 2);

        if (tryParseInt(isNum) == false) {

            if (newBedId.trim().length() == 4 || newBedId.trim().length() == 5 ||
                    newBedId.trim().length() == 6
                    || newBedId.trim().length() == 7) {
                if (newBedId.trim().length() == 7 && newBedId.contains("E") == true) {
                    newBedId = "0" + newBedId;
                } else {
                    newBedId = "0" + newBedId;
                }
            }
        }

        if (newBedId.trim().length() == 6 || newBedId.trim().length() == 5) {
            newBedId = newBedId + "01";
        }


        if (newBedId.contains("A") == true) {
            newBedId = newBedId.replace("A", "00");
        }

        if (newBedId.contains("B") == true) {
            newBedId = newBedId.replace("B", "50");
        }

        if (newBedId.contains("C") == true) {
            newBedId = newBedId.replace("C", "03");
        }

        if (newBedId.contains("ES") == true) {
            newBedId = newBedId.replace("ES", "19");
        }

        if (newBedId.contains("EN") == true) {
            newBedId = newBedId.replace("EN", "14");
        }
        //2016-12-17
        if (Mode == 2) {
            newBedId = newBedId.substring(0, 4) + "00";
        }

        return newBedId;
    }

    private void newSettingCommit(String newBedId) {
        final Engine mEngine;
        final INgnConfigurationService mConfigurationService;
        mEngine = (Engine) Engine.getInstance();
        mConfigurationService = mEngine.getConfigurationService();

        mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_DISPLAY_NAME,
                newBedId);

        //newBedId=GetKHBedCode(newBedId,1);

        if (IsGetSipConfig == true) {
            mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU,
                    "sip:" + newBedId + "@" + CONFIG_SIP_SERVER_IP);
            mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI,
                    newBedId);
            mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD,
                    "bcadmin");
            mConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM,
                    "sip:" + CONFIG_SIP_SERVER_RELAMIP);

            mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST,
                    CONFIG_SIP_SERVER_IP);

            Log.v("新 SIP SERVER:", CONFIG_SIP_SERVER_IP);

        } else {

            //Default Setting
            mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU,
                    "sip:" + newBedId + "@122.117.67.226");
            mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI,
                    newBedId);
            mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD,
                    "bcadmin");
            mConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM,
                    "sip:122.117.67.226");

            mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST,
                    "122.117.67.226");

            Log.v("預設的中央設定", "122.117.67.226");

        }


        mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_EARLY_IMS,
                true);

        mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_WIFI,
                true);
        mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_3G,
                true);
        // Compute
        if (!mConfigurationService.commit()) {
            //Log.e(TAG, "Failed to Commit() configuration");
        }

        //mConfigurationService.commit();


        //showAlert("設定成功", "綁定床號 [ " + newBedId + "] 成功");

        LastRegBedId = newBedId;
        IdView.setText(LastRegBedId);
        writeLogFile(LastRegBedId, getApplicationContext());
        Spinner spGroup = (Spinner) findViewById(R.id.sGroup);
        if (spGroup != null) {
            InitiSpinnerIndex(spGroup, LastRegBedId);
            spGroup.setEnabled(false);
        }

        isKHPass = true;
        BeginFunctionWork();
        GetBedListWork();
        GetTipWork();

        Button btnEnter = (Button) findViewById(R.id.BtnEnter);
        Button btnLogout = (Button) findViewById(R.id.BtnLogout);

        btnEnter.setEnabled(false);
        btnLogout.setEnabled(true);

        btnEnter.setVisibility(View.GONE);
        btnLogout.setVisibility(View.VISIBLE);

        if (mSipService.isRegistered() == true) {
            mSipService.stopStack();
            mSipService.unRegister();
            mSipService.register(ScreenHome.this);
        } else {
            mSipService.register(ScreenHome.this);
        }
    }



/*
    private void newSettingCommit(String newBedId) {
        final Engine mEngine;
        final INgnConfigurationService mConfigurationService;
        mEngine = (Engine) Engine.getInstance();
        mConfigurationService = mEngine.getConfigurationService();

        mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_DISPLAY_NAME,
                newBedId);
        mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU,
                "sip:" + newBedId + "@122.117.67.226:5788");
        mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI,
                newBedId);
        mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD,
                newBedId);
        mConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM,
                "sip:122.117.67.226:5788");
        mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST,
                "122.117.67.226");
        mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT,
                5788);
        mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_EARLY_IMS,
                true);
        // Compute
        if (!mConfigurationService.commit()) {
            Log.e(TAG, "Failed to Commit() configuration");
        }
        mConfigurationService.commit();
        showAlert("設定成功", "綁定床號 [ " + newBedId + "] 成功");

        if (mSipService.isRegistered() == true) {
            mSipService.stopStack();
            mSipService.unRegister();
            mSipService.register(ScreenHome.this);
        }
    }
*/

    /**
     * ScreenHomeItem
     */
    static class ScreenHomeItem {
        static final int ITEM_SIGNIN_SIGNOUT_POS = -2;
        static final int ITEM_SIGNIN_SETTING_POS = -1;

        //static final int ITEM_Call1_POS = 0;
        //static final int ITEM_Call2_POS = 1;
        static final int ITEM_CallLucky_POS = 4;
        static final int ITEM_Call3_POS = 1;

        static final int ITEM_EXIT_POS = 999;//BC-Custom
        final int mIconResId;
        final String mText;
        final Class<? extends Activity> mClass;

        private ScreenHomeItem(int iconResId, String text, Class<? extends Activity> _class) {
            mIconResId = iconResId;
            mText = text;
            mClass = _class;
        }
    }

    /**
     * ScreenHomeAdapter
     */
    static class ScreenHomeAdapter extends BaseAdapter {
        //static final int ALWAYS_VISIBLE_ITEMS_COUNT = 6;//BC-Custom
        static final int ALWAYS_VISIBLE_ITEMS_COUNT = 3;
        static final ScreenHomeItem[] sItems = new ScreenHomeItem[]{
                /*
                // always visible
                new ScreenHomeItem(R.drawable.sign_in_48, "登入SIP Server", null),
                //new ScreenHomeItem(R.drawable.exit_48, "離開", null),
                new ScreenHomeItem(R.drawable.options_48, "設定", null),
                //new ScreenHomeItem(R.drawable.about_48, "About", ScreenAbout.class),
                // visible only if connected
                new ScreenHomeItem(R.drawable.dialer_48, "撥號2", ScreenTabDialer.class),
                //new ScreenHomeItem(R.drawable.eab2_48, "Address Book", ScreenTabContacts.class),
                new ScreenHomeItem(R.drawable.history_48, "歷史記錄", ScreenTabHistory.class),
                //new ScreenHomeItem(R.drawable.history_48, "歷史記錄", ScreenSettings.class),
                //new ScreenHomeItem(R.drawable.chat_48, "Messages", ScreenTabMessages.class),
               */
                /*
                new ScreenHomeItem(R.drawable.sign_in_48, "", null),
                new ScreenHomeItem(R.drawable.options_48, "", null),
                new ScreenHomeItem(R.drawable.dialer_48, "", ScreenTabDialer.class),
                new ScreenHomeItem(R.drawable.history_48, "", ScreenTabHistory.class),

                new ScreenHomeItem(R.drawable.call1, "", null),
                new ScreenHomeItem(R.drawable.call2, "", null),
                new ScreenHomeItem(R.drawable.calllucky, "", null),//303
                new ScreenHomeItem(R.drawable.call3, "", null),
                */
                //new ScreenHomeItem(R.drawable.sign_in_48, "", null),
                //new ScreenHomeItem(R.drawable.options_48, "", null),

                /*2016-12-17
                new ScreenHomeItem(R.drawable.call1, "", null),
                new ScreenHomeItem(R.drawable.call2, "", null),
                new ScreenHomeItem(R.drawable.dialer_48, "", ScreenTabDialer.class),
                new ScreenHomeItem(R.drawable.call3, "", null),
                new ScreenHomeItem(R.drawable.calllucky, "", null),//303
                new ScreenHomeItem(R.drawable.history_48, "", ScreenTabHistory.class),
                */

                //new ScreenHomeItem(R.drawable.call1, "", null),
                //new ScreenHomeItem(R.drawable.call2, "", null),
                new ScreenHomeItem(R.drawable.dialer_48, "", ScreenTabDialer.class),
                new ScreenHomeItem(R.drawable.call3, "", null),
                //new ScreenHomeItem(R.drawable.calllucky, "", null),//303
                new ScreenHomeItem(R.drawable.history_48, "", ScreenTabHistory.class),

        };

        private final LayoutInflater mInflater;
        private final ScreenHome mBaseScreen;

        ScreenHomeAdapter(ScreenHome baseScreen) {
            mInflater = LayoutInflater.from(baseScreen);
            mBaseScreen = baseScreen;
        }

        void refresh() {
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mBaseScreen.mSipService.isRegistered() ? sItems.length : ALWAYS_VISIBLE_ITEMS_COUNT;
        }

        @Override
        public Object getItem(int position) {
            return sItems[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            final ScreenHomeItem item = (ScreenHomeItem) getItem(position);

            if (item == null) {
                return null;
            }

            if (view == null) {
                view = mInflater.inflate(R.layout.screen_home_item, null);
            }

            if (position == ScreenHomeItem.ITEM_SIGNIN_SIGNOUT_POS) {
                if (mBaseScreen.mSipService.getRegistrationState() == ConnectionState.CONNECTING || mBaseScreen.mSipService.getRegistrationState() == ConnectionState.TERMINATING) {
                    //((TextView) view.findViewById(R.id.screen_home_item_text)).setText("取消動作");
                    ((TextView) view.findViewById(R.id.screen_home_item_text)).setText("");
                    ((ImageView) view.findViewById(R.id.screen_home_item_icon)).setImageResource(R.drawable.sign_inprogress_48);
                    //((ImageView) view .findViewById(R.id.screen_home_item_icon)).setVisibility(View.INVISIBLE);
                } else {
                    if (mBaseScreen.mSipService.isRegistered()) {
                        // ((TextView) view.findViewById(R.id.screen_home_item_text)).setText("登出");
                        ((TextView) view.findViewById(R.id.screen_home_item_text)).setText("");
                        ((ImageView) view.findViewById(R.id.screen_home_item_icon)).setImageResource(R.drawable.sign_out_48);
                        //((ImageView) view .findViewById(R.id.screen_home_item_icon)).setVisibility(View.INVISIBLE);

                    } else {
                        //((TextView) view.findViewById(R.id.screen_home_item_text)).setText("登入");
                        ((TextView) view.findViewById(R.id.screen_home_item_text)).setText("");
                        ((ImageView) view.findViewById(R.id.screen_home_item_icon)).setImageResource(R.drawable.sign_in_48);
                        //((ImageView) view .findViewById(R.id.screen_home_item_icon)).setVisibility(View.INVISIBLE);

                    }
                }
            } else {
                ((TextView) view.findViewById(R.id.screen_home_item_text)).setText(item.mText);
                ((ImageView) view.findViewById(R.id.screen_home_item_icon)).setImageResource(item.mIconResId);
            }

            return view;
        }

    }

    private class LongOperation extends AsyncTask<String, Void, Void> {

        private final HttpClient Client = new DefaultHttpClient();
        private String Content;
        private String Error = null;

        protected void onPreExecute() {

        }

        protected Void doInBackground(String... urls) {
            try {

                HttpGet httpget = new HttpGet("http://122.117.112.216:5288/Sipquery.aspx?bedid=" + urls[0]);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                Content = Client.execute(httpget, responseHandler);
                /*
                 * [0]->BedId
                 * [1]->主護名稱
                 * [2]->主護號碼
                 */
                NursePhoneCode = Content.split("\\$")[2].toString();
                NurseName = Content.split("\\$")[1].toString();
                Log.v("NursePhoneCode=>", NursePhoneCode);


            } catch (ClientProtocolException e) {
                Error = e.getMessage();
                cancel(true);
            } catch (IOException e) {
                Error = e.getMessage();
                cancel(true);
            }

            return null;
        }

        protected void onPostExecute(Void unused) {
            showAlert("呼叫服務", "撥號給「主護」：" + NurseName);
            ScreenAV.makeCall(NursePhoneCode, NgnMediaType.Audio);
        }
    }

    private class ddOperation extends AsyncTask<String, Void, Void> {

        private final HttpClient Client = new DefaultHttpClient();
        private String Content;
        private String Error = null;
        private Boolean isPass = false;

        protected Void doInBackground(String... urls) {
            try {
                HttpGet httpget = new HttpGet("http://122.117.112.216:5388/newNurseCall.aspx?BedId=" + urls[0]);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                Content = Client.execute(httpget, responseHandler);
                if (Content.trim() != "" && Content.equals("Complete")) {
                    isPass = true;
                }

            } catch (ClientProtocolException e) {
                Error = e.getMessage();
                cancel(true);
            } catch (IOException e) {
                Error = e.getMessage();
                cancel(true);
            }

            return null;
        }

        protected void onPostExecute(Void unused) {
            if (isPass == true) {
                showAlert("點滴滴空服務", "您的點滴滴空服務，已經通知護理師，請稍後...");
            }
        }
    }

    private class ConfigSipServer extends AsyncTask<String, Void, Void> {

        private final HttpClient Client = new DefaultHttpClient();
        private String Content;
        private String Error = null;

        protected void onPreExecute() {
            IsGetSipConfig = false;
        }

        protected Void doInBackground(String... urls) {
            try {
                HttpGet httpget = new HttpGet("http://122.117.67.226:5388/SipServerQuery.aspx?Id=" + urls[0]);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                Content = Client.execute(httpget, responseHandler);
                /*
                 * [0]->Sip Relam Ip
                 * [1]->Sip Server Ip
                 * [2]->Sip Server Port
                 * [3]->Sip Stun Ip
                 * [4]->Sip Stun Port
                 */
                CONFIG_SIP_SERVER_RELAMIP = Content.split(",")[0].toString();
                CONFIG_SIP_SERVER_IP = Content.split(",")[1].toString();
                CONFIG_SIP_SERVER_PORT = Content.split(",")[2].toString();
                CONFIG_SIP_SERVER_STUNIP = Content.split(",")[3].toString();
                CONFIG_SIP_SERVER_STUNPORT = Content.split(",")[4].toString();

                Log.v("Config Relam", CONFIG_SIP_SERVER_RELAMIP);
                Log.v("Config Server", CONFIG_SIP_SERVER_IP);
                Log.v("Config Port", CONFIG_SIP_SERVER_PORT);
                Log.v("Config Stun", CONFIG_SIP_SERVER_STUNIP);
                Log.v("Config Stun Port", CONFIG_SIP_SERVER_STUNPORT);


            } catch (ClientProtocolException e) {
                Error = e.getMessage();
                cancel(true);
            } catch (IOException e) {
                Error = e.getMessage();
                cancel(true);
            }

            return null;
        }

        protected void onPostExecute(Void unused) {
            IsGetSipConfig = true;
            if (LastRegBedId.equals("") == false) {
                Log.v("向新的Server註冊:", LastRegBedId);
                newSettingCommit(LastRegBedId);
            } else {
                final Engine mEngine;
                final INgnConfigurationService mConfigurationService;
                mEngine = (Engine) Engine.getInstance();
                mConfigurationService = mEngine.getConfigurationService();
                String oldBed = mConfigurationService.getString(NgnConfigurationEntry.IDENTITY_IMPI, "123");
                newSettingCommit(oldBed);
            }
        }
    }

    private class GetGrouupOperation extends AsyncTask<String, Void, Void> {

        private final HttpClient Client = new DefaultHttpClient();
        private String Content;
        private String Error = null;
        public ScreenHome activity;

        public GetGrouupOperation(ScreenHome a) {
            this.activity = a;
        }

        protected void onPreExecute() {

        }

        protected Void doInBackground(String... urls) {
            try {
                stList.clear();
                HttpGet httpget = new HttpGet("http://122.117.112.216:5388/GetStation.aspx" + urls[0]);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                Content = Client.execute(httpget, responseHandler);
                if (Content.length() > 0) {
                    String[] token1 = Content.split("\\|");
                    for (String t1 : token1) {
                        stList.add(t1);
                    }
                }
            } catch (ClientProtocolException e) {
                Error = e.getMessage();
                cancel(true);
            } catch (IOException e) {
                Error = e.getMessage();
                cancel(true);
            }

            return null;
        }

        protected void onPostExecute(Void unused) {
            activity.BindStation();
        }
    }

    private class GetCallOperation extends AsyncTask<String, Void, Void> {

        private final HttpClient Client = new DefaultHttpClient();
        private String Content;
        private String Error = null;
        public ScreenHome activity;

        public GetCallOperation(ScreenHome a) {
            this.activity = a;
        }

        protected void onPreExecute() {
            //
            isSafeExecute = false;
        }

        protected Void doInBackground(String... urls) {
            try {
                callList.clear();
                sList.clear();
                HttpGet httpget = new HttpGet("http://122.117.112.216:5388/GetNurseCallTung.aspx?SGid=" + urls[0]);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                Content = Client.execute(httpget, responseHandler);
                Log.v("收到的點滴記錄", Content);
                if (Content.length() > 0) {
                    String[] token1 = Content.split("\\|");
                    for (String t1 : token1) {
                        t1 += "1,2,3,4";
                        String[] token2 = t1.split(",");
                        callList.add(token2[1].toString() + "  於  " + token2[3].toString());
                        sClass tmpClass = new sClass(token2[0], token2[1], Integer.parseInt(token2[2]), token2[3]);
                        sList.add(tmpClass);
                    }
                }
            } catch (ClientProtocolException e) {
                Error = e.getMessage();
                cancel(true);
            } catch (IOException e) {
                Error = e.getMessage();
                cancel(true);
            }
            isSafeExecute = true;
            return null;
        }

        protected void onPostExecute(Void unused) {
            //
            isSafeExecute = true;
            activity.BindCall();
        }
    }

    public class sClass {
        String bRealId;
        String bId;
        Integer bStatus;
        String bDate;

        public sClass(String pRealId, String pId, Integer pStatus, String pDate) {
            this.bRealId = pRealId;
            this.bId = pId;
            this.bStatus = pStatus;
            this.bDate = pDate;
        }
    }

    private class GetTipOperation extends AsyncTask<String, Void, Void> {

        private final HttpClient Client = new DefaultHttpClient();
        private String Content;
        private String Error = null;
        public ScreenHome activity;

        public GetTipOperation(ScreenHome a) {
            this.activity = a;
        }

        protected void onPreExecute() {
            //
        }

        protected Void doInBackground(String... urls) {
            try {
                tipList.clear();
                tipObjList.clear();
                HttpGet httpget = new HttpGet("http://122.117.112.216:5388/GetTextMessage.aspx?BedBegin=" + urls[0]);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                Content = Client.execute(httpget, responseHandler);
                Log.v("收到的小叮嚀記錄", Content);
                tipList.clear();
                tipObjList.clear();
                if (Content.length() > 0) {
                    String[] token1 = Content.split("\\|");
                    Log.v("小叮嚀Token1的數量:", Integer.toString(token1.length));
                    for (String t1 : token1) {
                        String[] token2 = t1.split("\\$");
                        Log.v("小叮嚀Token2的數量:", Integer.toString(token2.length));
                        if (token2.length > 4) {
                            tipList.add(token2[2].toString() + token2[3].toString() + token2[1].toString() + "  於  " + token2[4].toString());
                            tipClass tmpClass = new tipClass(token2[0], token2[2], token2[3], token2[1], token2[4]);
                            tipObjList.add(tmpClass);
                        }
                    }

                    Log.v("跑完Loop後tipList的數量:", Integer.toString(tipList.size()));
                    Log.v("跑完Loop後tipObjList的數量:", Integer.toString(tipObjList.size()));
                }
            } catch (ClientProtocolException e) {
                Error = e.getMessage();
                cancel(true);
            } catch (IOException e) {
                Error = e.getMessage();
                cancel(true);
            }

            return null;
        }

        protected void onPostExecute(Void unused) {
            //
            activity.BindTip();
        }
    }

    public class tipClass {
        String bRealId;
        String bId;
        String bPatientName;
        String bContent;
        String bDate;

        public tipClass(String pRealId, String pPatientName, String pId, String pContent, String pDate) {
            this.bRealId = pRealId;
            this.bPatientName = pPatientName;
            this.bId = pId;
            this.bContent = pContent;
            this.bDate = pDate;
        }
    }

    //護士物件，目前未使用
    public class nurseClass {
        String NurseName;
        String bed;

        public nurseClass(String pNurseName, String pBed) {
            this.NurseName = pNurseName;
            this.bed = pBed;
        }
    }

    private class UpdateNurseCall extends AsyncTask<String, Void, Void> {

        private final HttpClient Client = new DefaultHttpClient();
        private String Content;
        private String Error = null;
        private String newStatus = "";

        protected void onPreExecute() {

        }

        protected Void doInBackground(String... urls) {
            try {
                newStatus = urls[1];
                HttpGet httpget = new HttpGet("http://122.117.112.216:5388/SetNurseCall.aspx?Id=" + urls[0] + "&Status=" + urls[1]);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                Content = Client.execute(httpget, responseHandler);

            } catch (ClientProtocolException e) {
                Error = e.getMessage();
                cancel(true);
            } catch (IOException e) {
                Error = e.getMessage();
                Log.v("發生錯誤：", Error);
                cancel(true);
            }

            return null;
        }

        protected void onPostExecute(Void unused) {
            if (Integer.parseInt(newStatus) == 1) {
                Toast.makeText(getApplicationContext(),
                        "已通知病患將處理[點滴滴空]", Toast.LENGTH_SHORT).show();
            }

            if (Integer.parseInt(newStatus) == 2) {
                Toast.makeText(getApplicationContext(),
                        "已完成處理", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class GetBedListOperation extends AsyncTask<String, Void, Void> {

        private final HttpClient Client = new DefaultHttpClient();
        private String Content;
        private String Error = null;
        public ScreenHome activity;

        public GetBedListOperation(ScreenHome a) {
            this.activity = a;
        }

        protected void onPreExecute() {
            int i = 0;
        }

        protected Void doInBackground(String... urls) {
            try {
                bedList.clear();
                bedObjList.clear();
                HttpGet httpget = new HttpGet("http://122.117.112.216:5388/GetBedListData.aspx?BedBegin=" + urls[0]);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                Content = Client.execute(httpget, responseHandler);
                Log.v("收到的病床記錄", Content);
                if (Content.length() > 0) {
                    String[] token1 = Content.split("\\|");
                    for (String t1 : token1) {
                        String[] token2 = t1.split(",");
                        if (token2.length >= 3) {
                            bedList.add(
                                    padLeftEmpty(token2[0], 8) +
                                            padLeftEmpty(token2[1], 5) +
                                            padLeftEmpty(token2[2], 10));
                            bedObjList.add(new BedClass(token2[0], token2[1], token2[2]));
                        }
                        //bedList.add("AA");
                    }
                }
            } catch (ClientProtocolException e) {
                Error = e.getMessage();
                cancel(true);
            } catch (IOException e) {
                Error = e.getMessage();
                cancel(true);
            }

            return null;
        }

        protected void onPostExecute(Void unused) {
            //
            //activity.BindCall();
            activity.BindBedList();
        }
    }

    public class BedClass {
        String bId;
        String bName;
        String bChar;

        public BedClass(String pId, String pName, String pChar) {
            this.bId = pId;
            this.bName = pName;
            this.bChar = pChar;
        }
    }

    public static String convertURL(String str) {

        String url = null;
        try {
            url = new String(str.trim().replace(" ", "%20").replace("&", "%26")
                    .replace(",", "%2c").replace("(", "%28").replace(")", "%29")
                    .replace("!", "%21").replace("=", "%3D").replace("<", "%3C")
                    .replace(">", "%3E").replace("#", "%23").replace("$", "%24")
                    .replace("'", "%27").replace("*", "%2A").replace("-", "%2D")
                    .replace(".", "%2E").replace("/", "%2F").replace(":", "%3A")
                    .replace(";", "%3B").replace("?", "%3F").replace("@", "%40")
                    .replace("[", "%5B").replace("\\", "%5C").replace("]", "%5D")
                    .replace("_", "%5F").replace("`", "%60").replace("{", "%7B")
                    .replace("|", "%7C").replace("}", "%7D"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    private class WriteTextMessage extends AsyncTask<String, Void, Void> {

        private final HttpClient Client = new DefaultHttpClient();
        private String Content;
        private String Error = null;

        protected void onPreExecute() {

        }

        protected Void doInBackground(String... urls) {
            try {
                HttpGet httpget = new HttpGet("http://122.117.112.216:5388/WriteTextMessage.aspx?Id=" + urls[0]
                        + "&Ward=" + urls[1] + "&Code=" + urls[2] + "&Msg=" + URLEncoder.encode(urls[3], "UTF-8"));
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                Content = Client.execute(httpget, responseHandler);

            } catch (ClientProtocolException e) {
                Error = e.getMessage();
                cancel(true);
            } catch (IOException e) {
                Error = e.getMessage();
                Log.v("發生錯誤：", Error);
                cancel(true);
            }

            return null;
        }

        protected void onPostExecute(Void unused) {
            if (Content.trim().equals("complete") == true) {
                Toast.makeText(getApplicationContext(),
                        "小叮嚀訊息已成功發送", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "小叮嚀訊息發送失敗", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
