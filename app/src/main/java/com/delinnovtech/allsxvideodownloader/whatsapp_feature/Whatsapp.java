package com.delinnovtech.allsxvideodownloader.whatsapp_feature;

import android.Manifest;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.delinnovtech.allsxvideodownloader.MainActivity;
import com.delinnovtech.allsxvideodownloader.R;
import com.delinnovtech.allsxvideodownloader.VDApp;
import com.delinnovtech.allsxvideodownloader.VDFragment;
import com.delinnovtech.allsxvideodownloader.download_feature.DownloadPermissionHandler;
import com.delinnovtech.allsxvideodownloader.download_feature.lists.CompletedVideos;
import com.delinnovtech.allsxvideodownloader.utils.PermissionRequestCodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import static android.content.ContentValues.TAG;


public class Whatsapp extends VDFragment implements MainActivity.OnBackPressedListener {
    private View view;
    private RecyclerView rvVideo;
    private StatusVideoAdapter statusVideoAdapter;
    private ArrayList<WhatsappStatusItem> videoList = new ArrayList<>();
    private CompletedVideos completedVideos;
    ProgressBar progressBar;

    private static final String WHATSAPP_STATUSES_LOCATION = "/WhatsApp/Media/.Statuses";

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);

        if (view == null) {

            view = inflater.inflate(R.layout.whatsapp, container, false);
            getVDActivity().setOnBackPressedListener(this);
            rvVideo = view.findViewById(R.id.rvWhatsappStatusList);
            progressBar = view.findViewById(R.id.myProg);
            rvVideo.setLayoutManager(new GridLayoutManager(getActivity(), 1));

        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar.setVisibility(View.VISIBLE);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkpermission();
                progressBar.setVisibility(View.GONE);
            }
        }, 0);
    }

    private void checkpermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            new DownloadPermissionHandler(getActivity()) {
                @Override
                public void onPermissionGranted() {
                    getVideo();
                }
            }.checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    PermissionRequestCodes.DOWNLOADS);
        } else {
            getVideo();
        }
    }

    @Override
    public void onBackpressed() {
        getVDActivity().getBrowserManager().unhideCurrentWindow();
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    public void getVideo() {
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + WHATSAPP_STATUSES_LOCATION);
        String pattern = ".mp4";

        //Get the listfile of that folder
        final File listFile[] = dir.listFiles();
        if (listFile != null) {
            Arrays.sort(listFile, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.compare(f2.lastModified(), f1.lastModified());
                }
            });
            Log.e("data founde", "file_count    " + listFile.length);
            for (int i = 0; i < listFile.length; i++) {
                // final int x = i;
                if (listFile[i].isDirectory()) {
                    //directory do nothing
                    Log.d("directory", "directory: directory do nothing ");
                } else {
                    if (listFile[i].getName().endsWith(pattern)) {
                        view.findViewById(R.id.tvNoStatus).setVisibility(View.GONE);
                        WhatsappStatusItem obj_model = new WhatsappStatusItem();
                        obj_model.setBoolean_selected(false);
                        obj_model.setStr_path(listFile[i].getAbsolutePath());
                        obj_model.setFormat(".mp4");
                        videoList.add(obj_model);
                    } else {
                        view.findViewById(R.id.tvNoStatus).setVisibility(View.VISIBLE);
                    }
                }
            }
        } else {
            view.findViewById(R.id.tvNoStatus).setVisibility(View.VISIBLE);
        }
        statusVideoAdapter = new StatusVideoAdapter();
        rvVideo.setAdapter(statusVideoAdapter);
    }

    /* public void getVideo() {
         Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
         String condition = MediaStore.Video.Media.DATA + " like?";
         String[] selectionArguments = new String[]{WHATSAPP_STATUSES_LOCATION};
         String sortOrder = MediaStore.Video.Media.DATE_TAKEN + " DESC";
         String[] projection = {MediaStore.Video.Media._ID, MediaStore.Video.Media.BUCKET_ID,
                 MediaStore.Video.Media.BUCKET_DISPLAY_NAME, MediaStore.Video.Media.DATA};
         Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, sortOrder);


         int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
         int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);

         if (cursor != null) {
             ContentResolver resolver = getContext().getContentResolver();
             cursor.moveToFirst();
             Log.e("data", "count   " + cursor.getCount());
             while (!cursor.isAfterLast()) {
                 if (cursor.getPosition() == 2) {
                     Log.e("data founde", DatabaseUtils.dumpCursorToString(cursor));
                 }
                 WhatsappStatusItem obj_model = new WhatsappStatusItem();
                 String filePath = cursor.getString(pathColumn);
                 Long id = cursor.getLong(idColumn);
                 Bitmap thumbNail = MediaStore.Video.Thumbnails.getThumbnail(resolver, id, MediaStore.Video.Thumbnails.MICRO_KIND, null);
                 obj_model.setBoolean_selected(false);
                 obj_model.setStr_path(filePath);
                 obj_model.setStr_thumb(thumbNail);
                 obj_model.setFormat(".mp4");
                 videoList.add(obj_model);
                 cursor.moveToNext();
             }
         }
     }
 */
    public class StatusVideoAdapter extends RecyclerView.Adapter<StatusVideoAdapter.ViewHolder> {

        @Override
        public void onBindViewHolder(final StatusVideoAdapter.ViewHolder Vholder, final int position) {
            Vholder.setIsRecyclable(false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Glide.with(Vholder.itemView.getContext()).load(videoList.get(position).getStr_path()).into(Vholder.iv_image);
                }
            }, 0);
            Vholder.rl_select.setBackgroundColor(Color.parseColor("#FFFFFF"));
            Vholder.rl_select.setAlpha(0);
            Vholder.rl_select.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent_gallery = new Intent(getActivity(), WhatsappStatusView.class);
                    intent_gallery.putExtra("format", videoList.get(position).getFormat());
                    intent_gallery.putExtra("path", videoList.get(position).getStr_path());
                    getActivity().startActivity(intent_gallery);
                }
            });

            Vholder.share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList.get(position).getStr_path()));
                    sendIntent.setType("file/*");
                    getActivity().startActivity(Intent.createChooser(sendIntent, "Send Status via:"));
                }
            });

            Vholder.wsp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.setPackage("com.whatsapp");
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList.get(position).getStr_path()));
                    sendIntent.setType("file/*");
                    getActivity().startActivity(Intent.createChooser(sendIntent, "Send Status via:"));
                }
            });

            Vholder.download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String sourcePath = videoList.get(position).getStr_path();
                    File source = new File(sourcePath);
                    File directory = Environment.getExternalStoragePublicDirectory(getActivity().getString(R.string.app_name));
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(directory);
                    mediaScanIntent.setData(contentUri);
                    getActivity().sendBroadcast(mediaScanIntent);

                    if (!directory.exists()) {
                        directory.mkdirs();
                    }

                    String pattern = ".mp4";
                    String fileName = getRandomNumberString();
                    String destinationPath = Environment.getExternalStoragePublicDirectory(getActivity().getString(R.string.app_name)) + "/WhatsappStatus" + fileName + pattern;

                    File destination = new File(destinationPath);
                    try {
                        if (sourcePath.endsWith(pattern)) {
                            copyFile(source, destination);
                        } else {
                            Log.d(TAG, "onClick: no data saved");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(v.getContext());
                    alertDialog.setMessage("Status Saved Successfully at location:" + destinationPath);
                    alertDialog.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do the stuff..
                                }

                            }
                    );

                    alertDialog.show();
                    CompletedVideos completedVideos = CompletedVideos.load(VDApp.getInstance().getApplicationContext());
                    completedVideos.addVideo(VDApp.getInstance().getApplicationContext(), "WhatsappStatus" + fileName + ".mp4");
                }


            });
        }

        private void copyFile(File source, File dest) throws IOException {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new FileInputStream(source);
                os = new FileOutputStream(dest);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            } finally {
                is.close();
                os.close();
            }
        }

        @Override
        public StatusVideoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.whatsapp_video_item, parent, false);
            StatusVideoAdapter.ViewHolder viewHolder1 = new StatusVideoAdapter.ViewHolder(view);
            return viewHolder1;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public ImageView iv_image;
            RelativeLayout rl_select;
            ImageView wsp;
            ImageView share;
            ImageView download;
            FloatingActionButton play_btn;

            public ViewHolder(View v) {
                super(v);
                iv_image = v.findViewById(R.id.iv_image);
                rl_select = v.findViewById(R.id.rl_select);
                wsp = v.findViewById(R.id.whatsapp);
                share = v.findViewById(R.id.share);
                download = v.findViewById(R.id.download);
                play_btn = v.findViewById(R.id.play_btn);

            }
        }

        @Override
        public int getItemCount() {
            return videoList.size();
        }

        public String getRandomNumberString() {
            // It will generate 6 digit random Number.
            // from 0 to 999999
            Random rnd = new Random();
            int number = rnd.nextInt(999999);
            // this will convert any number sequence into 6 character.
            return String.format("%06d", number);
        }
    }

}
