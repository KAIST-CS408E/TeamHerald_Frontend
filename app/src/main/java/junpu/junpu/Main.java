package junpu.junpu;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Main extends AppCompatActivity {

    EditText inputNewUser;
    Button btnSubmitId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.inputNewUser = (EditText) findViewById(R.id.input_new_user);
        this.btnSubmitId = (Button) findViewById(R.id.btn_submit_id);
    }

    // intent to move to ship customization page
    public void moveToCustomization(View view){
        Intent intent = new Intent(this, CustomizeShip.class);
        String userName = this.inputNewUser.getText().toString();
        intent.putExtra("junpu.junpu.USERNAME", userName);
        startActivity(intent);
    }

}
