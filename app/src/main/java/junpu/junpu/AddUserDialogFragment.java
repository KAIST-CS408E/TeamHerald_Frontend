package junpu.junpu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;


public class AddUserDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        Bundle bundle = getArguments();
        final String username = bundle.getString("USERNAME", "");

        builder.setView(inflater.inflate(R.layout.fragment_dialog_add_user, null))
                // Add action buttons
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        Dialog dialogView = ((Dialog) dialog);
                        String friendUsername = ((EditText) dialogView.findViewById(R.id.friend_username)).getText().toString();
                        Log.v("JunpuUsername: ", username);
                        Log.v("JunpuFriend: ", friendUsername);
                    }
                });
        return builder.create();
    }
}