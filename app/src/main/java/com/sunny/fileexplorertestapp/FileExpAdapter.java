package com.sunny.fileexplorertestapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sunny.fileexplorertestapp.ServerCommUtility.FileExpItem;

import java.util.ArrayList;
import java.util.List;

public class FileExpAdapter extends ArrayAdapter<FileExpItem> {
    private List<FileExpItem> objects;
    private Context context;
    public FileExpAdapter(Context context, List<FileExpItem> objects) {
        super(context, R.layout.custom_list_item, objects);
        this.context = context;
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater myInflater = LayoutInflater.from(getContext());
        View customView = myInflater.inflate(R.layout.custom_list_item, parent, false);

        FileExpItem item = getItem(position);
        TextView fileNameText = (TextView) customView.findViewById(R.id.fileNameText);
        TextView fileInfoText = (TextView) customView.findViewById(R.id.fileInfoText);
        ImageView fileImageView = (ImageView) customView.findViewById(R.id.fileImageView);

        fileNameText.setText(item.getName());
        fileInfoText.setText(item.getFileInfo());
        if(item.isDirectory()){
            fileImageView.setImageResource(R.drawable.folder_icon);
        }
        else{
            if(item.getFileType()!=null && item.getFileType().startsWith("video/"))
                fileImageView.setImageResource(R.drawable.video_icon);
            else
                fileImageView.setImageResource(R.drawable.file_custom_icon);
        }
        return customView;
    }

    public void changeListItems(ArrayList<FileExpItem> listItems) {
        objects = listItems;
        this.notifyDataSetChanged();
    }
}
