package junpu.junpu;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class CustomizeShip extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_ship);

        Intent intent = getIntent();
        String userName = intent.getStringExtra("junpu.junpu.USERNAME");
        TextView textView = (TextView) findViewById(R.id.show_username);
        textView.setText(userName);
    }
}
