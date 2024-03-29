package com.example.dlk_user;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {
// 입력 받은 값에서 각 기능에대해 마스킹을 어떻게 할지 어떤 앱이 켜지는지 어떻게 받아올지 혹은 열수있는 앱을 통제하는 식으로 할건지
    private static final int REQ_CODE_OVERLAY_PERMISSION = 1;//마스킹 뷰 실행을 위한 변수
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    String information[] = new String[10];
    Button finish;
    ImageView check;
    Context context;
    Handler handler;
    Timer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.show();
        actionBar.setTitle("마스킹 실행");

        context = getApplicationContext();
        check = (ImageView)findViewById(R.id.check);

        nfcAdapter = NfcAdapter. getDefaultAdapter (this);
        Intent intent = new Intent(this, getClass()).addFlags(Intent. FLAG_ACTIVITY_SINGLE_TOP );
        pendingIntent = PendingIntent. getActivity (this, 0, intent, 0);

        finish = (Button)findViewById(R.id.go_finish);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent finish = new Intent(MainActivity.this, FinishActivity.class);
                startActivity(finish);
            }
        });

        handler = new Handler();

        Intent end_intent = getIntent();
        String tmp = end_intent.getStringExtra("close");
        if(tmp == "close"){
            close();

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    teskkill();
                }
            };
            timer = new Timer();
            timer.schedule(timerTask, 5000);

//앱완전죽이기
        }

        final Handler handler = new Handler(){
            public void handleMessage(Message msg){
                // 원래 하려던 동작 (UI변경 작업 등)V
                check.setImageResource(R.drawable.uncheck);
                finish.setVisibility(View.INVISIBLE);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch( this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch( this, pendingIntent, null, null);
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter. EXTRA_NDEF_MESSAGES );
        if(rawMsgs != null) {
            NdefMessage msgs =(NdefMessage) rawMsgs[0];
            NdefRecord[] rec = msgs.getRecords();
            byte[] bt = rec[0].getPayload();
            String text = new String(bt);
            Log.e("qh",text);
            information = text.split(",");
            Log.e("qh",information[0]);
            //이 앱을 키고 다른 NFCtag를 읽었을때의 예외 처리
            if(information[0].contains("FLARE_Entrance")){
                Log.e("qh","233");
                popup();
                check.setImageResource(R.drawable.cheked);
                finish.setVisibility(VISIBLE);
                settings();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openView();
                    }
                },3000);
                //openView();
            }else if(information[0].contains("FLARE_Exit")){
                String tp = PreferenceManager.getString(context, "password");
                if(information[1].equals(tp))
                {
                    close();
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {

                            teskkill();
                        }
                    };
                    timer = new Timer();
                    timer.schedule(timerTask, 5000);

                }else{
                    Toast.makeText(context,"입구용 NFC를 먼저 입력해 주십시오.", Toast.LENGTH_LONG).show();
                }
            }
            else{
                Log.e("qh","45435");
                check.setImageResource(R.drawable.uncheck);
                finish.setVisibility(View.GONE);
                //토스트 보내기
            }
        }
    }

    public void popup()
    {
        Intent intent = new Intent(getApplicationContext(), PopopActivity.class);
        startActivityForResult(intent, 1);


    }

    public void settings(){
        PreferenceManager.setString(context,"camera",information[1]);
        PreferenceManager.setString(context, "hifirecorder", information[2]);
        PreferenceManager.setString(context,"kakaotalk", information[3]);
        PreferenceManager.setString(context, "chrome", information[4]);
        PreferenceManager.setString(context,"mms",information[5]);
        PreferenceManager.setString(context,"pp", information[6]);
        PreferenceManager.setString(context,"word", information[7]);
        PreferenceManager.setString(context,"gm", information[8]);
        PreferenceManager.setString(context,"password", information[9]);
    }

    public void openView() {
        // 접근성 권한이 없으면 접근성 권한 설정하는 다이얼로그 띄워주는 부분
        if(!checkAccessibilityPermissions()) {
            setAccessibilityPermissions();
        }

        if (Settings.canDrawOverlays(this))//Overlay사용가능여부 체크

            //startService(new Intent(this, Masking.class));
        {}
        else
            onObtainingPermissionOverlayWindow();//다른 앱위에 그리기 권한 획득 창이 나타나는 함수를 부른다.




    }
//허용안하면 꺼지게 하기
    //안드로이드 6.0 부터 화면 오버레이 권한 설정을 Manifest 입력만으로는 사용 못하게 막아서 아래 코드를 사용한다.
    public void onObtainingPermissionOverlayWindow() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        //Settings.ACTION_MANAGE_OVERLAY_PERMISSION에 현재 패키지 명을 넘겨 설정화면을 노출하게 한다.
        startActivityForResult(intent, REQ_CODE_OVERLAY_PERMISSION);
    }
    // 접근성 권한이 있는지 없는지 확인하는 부분
    // 있으면 true, 없으면 false
    public boolean checkAccessibilityPermissions() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);

        // getEnabledAccessibilityServiceList는 현재 접근성 권한을 가진 리스트를 가져오게 된다
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.DEFAULT);

        for (int i = 0; i < list.size(); i++) {
            AccessibilityServiceInfo info = list.get(i);

            // 접근성 권한을 가진 앱의 패키지 네임과 패키지 네임이 같으면 현재앱이 접근성 권한을 가지고 있다고 판단함
            if (info.getResolveInfo().serviceInfo.packageName.equals(getApplication().getPackageName())) {
                return true;
            }
        }
        return false;
    }

    // 접근성 설정화면으로 넘겨주는 부분
    public void setAccessibilityPermissions() {
        AlertDialog.Builder gsDialog = new AlertDialog.Builder(this);
        gsDialog.setTitle("접근성 권한 설정");
        gsDialog.setMessage("접근성 권한을 켜주세요");
        gsDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // 설정화면으로 보내는 부분
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return;
            }
        }).create().show();
    }

    public void teskkill(){
        //접근성 먼저 끄고 꺼져있으면은 이미지 바꾸고 버튼이랑 그 다음에 앱을 완전히 죽이기
        if(checkAccessibilityPermissions() == false) {
            Message msg = handler.obtainMessage();
            handler.sendMessage(msg);
            timer.cancel();
            moveTaskToBack(true);						// 태스크를 백그라운드로 이동
            finishAndRemoveTask();						// 액티비티 종료 + 태스크 리스트에서 지우기
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
    public void close(){
        if(!close_checkAccessibilityPermissions()) {
            close_setAccessibilityPermissions();
        }
    }
    public boolean close_checkAccessibilityPermissions() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);

        // getEnabledAccessibilityServiceList는 현재 접근성 권한을 가진 리스트를 가져오게 된다
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.DEFAULT);

        for (int i = 0; i < list.size(); i++) {
            AccessibilityServiceInfo info = list.get(i);

            // 접근성 권한을 가진 앱의 패키지 네임과 패키지 네임이 같으면 현재앱이 접근성 권한을 가지고 있다고 판단함
            if (info.getResolveInfo().serviceInfo.packageName.equals(getApplication().getPackageName())) {
                return false;
            }
        }
        return true;
    }

    // 접근성 설정화면으로 넘겨주는 부분
    public void close_setAccessibilityPermissions() {
        AlertDialog.Builder gsDialog = new AlertDialog.Builder(this);
        gsDialog.setTitle("접근성 권한 설정");
        gsDialog.setMessage("접근성 권한을 꺼주세요");
        gsDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // 설정화면으로 보내는 부분
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return;
            }
        }).create().show();
    }

}
