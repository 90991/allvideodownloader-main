package com.delinnovtech.allsxvideodownloader.browsing_feature;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.delinnovtech.allsxvideodownloader.MainActivity;
import com.delinnovtech.allsxvideodownloader.R;
import com.delinnovtech.allsxvideodownloader.VideoPlayer;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class FacebookFragment extends Fragment implements MainActivity.OnBackPressedListener {


    WebView myWebView;

    MainActivity mainActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.setOnBackPressedListener(this);
        }
    }

    public FacebookFragment() {
    }


    @SuppressLint("JavascriptInterface")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.facebook_fragment, container, false);
        myWebView = view.findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
//        myWebView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        myWebView.getSettings().setUserAgentString("Android 7.0");
        myWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
        myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.getSettings().setDisplayZoomControls(true);
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.addJavascriptInterface(this, "FBDownloader");

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {

                myWebView.loadUrl("javascript:(function() { "
                        + "var el = document.querySelectorAll('div[data-sigil]');"
                        + "for(var i=0;i<el.length; i++)"
                        + "{"
                        + "var sigil = el[i].dataset.sigil;"
                        + "if(sigil.indexOf('inlineVideo') > -1){"
                        + "delete el[i].dataset.sigil;"
                        + "var jsonData = JSON.parse(el[i].dataset.store);"
                        + "el[i].setAttribute('autoplay','false');"
                        + "el[i].setAttribute('onClick', 'FBDownloader.processVideo(\"'+jsonData['src']+'\");');"
                        + "}" + "}" + "})()");
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.i("information", url);

                myWebView.loadUrl("javascript:(function prepareVideo() { "
                        + "var el = document.querySelectorAll('div[data-sigil]');"
                        + "for(var i=0;i<el.length; i++)"
                        + "{"
                        + "var sigil = el[i].dataset.sigil;"
                        + "if(sigil.indexOf('inlineVideo') > -1){"
                        + "delete el[i].dataset.sigil;"
                        + "console.log(i);"
                        + "var jsonData = JSON.parse(el[i].dataset.store);"
                        + "el[i].setAttribute('autoplay','false');"
                        + "el[i].setAttribute('onClick', 'FBDownloader.processVideo(\"'+jsonData['src']+'\",\"'+jsonData['videoID']+'\");');"
                        + "}" + "}" + "})()");
                myWebView.loadUrl("javascript:( window.onload=prepareVideo;" + ")()");
            }
        });

        myWebView.loadUrl("https://m.facebook.com");
        return view;
    }

    @Override
    public void onBackpressed() {

        if (myWebView.canGoBack()) {
            myWebView.goBack();
        }
    }

    @JavascriptInterface
    public void processVideo(final String vidData, final String vidID) {
//        Toast.makeText(this, "Download Link: " + vidID, Toast.LENGTH_LONG).show();
        showDialog(vidData, vidID);
    }


    private void showDialog(final String vidData, final String vidID) {


                Intent intent = new Intent(getActivity(), VideoPlayer.class);
                intent.putExtra("video_uri", vidData);
                startActivity(intent);




//
//                        try {
//
//                            Uri downloadUri = Uri.parse(vidData);
//                            DownloadManager.Request req = new DownloadManager.Request(downloadUri);
////                            req.setDestinationUri(Uri.fromFile(mFilePath));
//                            req.addRequestHeader("Accept", "application/mp4");
//                            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, File.separator
//                                    + "PupiDownloader" + File.separator + "facebook_fb" + vidID + ".mp4");
//
//                            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//                            DownloadManager dm = (DownloadManager) getSystemService(getApplicationContext().DOWNLOAD_SERVICE);
//                            dm.enqueue(req);
//                            showDownloadDialog();
//
//                        } catch (Exception e) {
//                            Log.i("CrashTesting", e.toString());
//                            Toast.makeText(FacebookActivity.this, "Video Can't be downloaded! Try Again", Toast.LENGTH_LONG).show();
//                        }




    }
}