package com.example.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MessageAdapter extends ArrayAdapter<Message> {
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public MessageAdapter(@NonNull Context context, ArrayList<Message> messages) {
        super(context, 0,messages);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        Message message = (Message) getItem(position);

        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message,parent,false);
        }
        TextView senderTextView = (TextView) convertView.findViewById(R.id.sender);
        TextView messageTextView = (TextView) convertView.findViewById(R.id.message);

        senderTextView.setText(message.getSender());
        messageTextView.setText(message.getMessage());

        return convertView;
    }


}
