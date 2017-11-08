package me.joowon.androidlocalvpn;

import android.content.Intent;
import android.net.VpnService;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent vpnRequest = VpnService.prepare(MainActivity.this);
                if (vpnRequest != null) {
                    startActivityForResult(vpnRequest, 0);
                } else {
                    startService(new Intent(MainActivity.this, LocalVpnService.class));
                }
            }
        });
    }
}
