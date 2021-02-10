package com.delinnovtech.allsxvideodownloader.browsing_feature;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.delinnovtech.allsxvideodownloader.VideoPlayer;
import com.delinnovtech.allsxvideodownloader.download_feature.DownloadPermissionHandler;
import com.delinnovtech.allsxvideodownloader.download_feature.DownloadVideo;
import com.delinnovtech.allsxvideodownloader.download_feature.lists.DownloadQueues;
import com.delinnovtech.allsxvideodownloader.utils.PermissionRequestCodes;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.delinnovtech.allsxvideodownloader.MainActivity;
import com.delinnovtech.allsxvideodownloader.R;
import com.delinnovtech.allsxvideodownloader.VDApp;
import com.delinnovtech.allsxvideodownloader.VDFragment;
import com.delinnovtech.allsxvideodownloader.custom_video_view.CustomMediaController;
import com.delinnovtech.allsxvideodownloader.custom_video_view.CustomVideoView;
import com.delinnovtech.allsxvideodownloader.history_feature.HistorySQLite;
import com.delinnovtech.allsxvideodownloader.history_feature.VisitedPage;
import com.delinnovtech.allsxvideodownloader.utils.Utils;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class BrowserWindow extends VDFragment implements View.OnClickListener, MainActivity.OnBackPressedListener {
    private String url;
    private View view;
    private TouchableWebView page;
    private SSLSocketFactory defaultSSLSF;

    private FrameLayout videoFoundTV;
    private CustomVideoView videoFoundView;
    private CustomMediaController mediaFoundController;
    private ImageView videosFoundHUD;
    private TextView tvDownloadCount;
    private float prevX, prevY;
    private boolean moved = false;
    private GestureDetector gesture;

    private View foundVideosWindow;
    private VideoList videoList;
    private ImageView foundVideosClose;

    private ProgressBar loadingPageProgress;

    private int orientation;
    private boolean loadedFirsTime;

    private List<String> blockedWebsites;
    private ConstraintLayout download_layout;
    AlertDialog alertDialog = null;

    @Override
    public void onClick(View v) {
        if (v == videosFoundHUD) {
            foundVideosWindow.setVisibility(View.VISIBLE);
        } else if (v == foundVideosClose) {
            foundVideosWindow.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = getArguments();
        url = data.getString("url");
        defaultSSLSF = HttpsURLConnection.getDefaultSSLSocketFactory();
        blockedWebsites = Arrays.asList(getResources().getStringArray(R.array.blocked_sites));
        setRetainInstance(true);
    }

    private void createVideosFoundTV() {
        videoFoundTV = view.findViewById(R.id.videoFoundTV);
        videoFoundView = view.findViewById(R.id.videoFoundView);
        mediaFoundController = view.findViewById(R.id.mediaFoundController);
        mediaFoundController.setFullscreenEnabled(false);
        videoFoundView.setMediaController(mediaFoundController);
        videoFoundTV.setVisibility(View.GONE);
    }

    private void createVideosFoundHUD() {
        videosFoundHUD = view.findViewById(R.id.videosFoundHUD);
        tvDownloadCount = view.findViewById(R.id.tv_download_count);
        videosFoundHUD.setOnClickListener(this);
        gesture = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                videosFoundHUD.performClick();
                return true;
            }
        });
    }

    private void createFoundVideosWindow() {
        View oldFoundVideosWindow = foundVideosWindow;
        foundVideosWindow = view.findViewById(R.id.foundVideosWindow);
        if (videoList != null) {
            videoList.recreateVideoList((RecyclerView) foundVideosWindow.findViewById(R.id.videoList));
        } else {
            videoList = new VideoList(getActivity(), (RecyclerView) foundVideosWindow.findViewById(R.id.videoList)) {
                @Override
                void onItemDeleted() {
                    //getVDActivity().loadInterstitialAd();
                    updateFoundVideosBar();
                }

                @Override
                void onVideoPlayed(String url) {
                    updateVideoPlayer(url);
                }
            };
        }

        if (oldFoundVideosWindow != null) {
            switch (oldFoundVideosWindow.getVisibility()) {
                case View.VISIBLE:
                    foundVideosWindow.setVisibility(View.VISIBLE);
                    break;
                case View.GONE:
                    foundVideosWindow.setVisibility(View.GONE);
                    break;
                case View.INVISIBLE:
                    foundVideosWindow.setVisibility(View.INVISIBLE);
                    break;
            }
        } else {
            foundVideosWindow.setVisibility(View.GONE);
        }

        foundVideosClose = foundVideosWindow.findViewById(R.id.foundVideosClose);
        foundVideosClose.setOnClickListener(this);
    }

    @SuppressLint("JavascriptInterface")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        if (view == null || getResources().getConfiguration().orientation != orientation) {
            int visibility = View.VISIBLE;
            if (view != null) {
                visibility = view.getVisibility();
            }
            view = inflater.inflate(R.layout.browser, container, false);
            view.setVisibility(visibility);

            download_layout = view.findViewById(R.id.download_layout);
            if (page == null) {
                page = view.findViewById(R.id.page);
            } else {
                View page1 = view.findViewById(R.id.page);
                ((ViewGroup) view).removeView(page1);
                ((ViewGroup) page.getParent()).removeView(page);
                ((ViewGroup) view).addView(page);
                ((ViewGroup) view).bringChildToFront(view.findViewById(R.id.videosFoundHUD));
                ((ViewGroup) view).bringChildToFront(view.findViewById(R.id.foundVideosWindow));
            }
            if (url.contains("facebook.com")) {
                download_layout.setVisibility(View.GONE);
                page.addJavascriptInterface(this, "FBDownloader");
            } else {
                download_layout.setVisibility(View.VISIBLE);
                page.addJavascriptInterface(new FBDownloader(getContext()), "FBDownloader");
            }
            loadingPageProgress = view.findViewById(R.id.loadingPageProgress);
            loadingPageProgress.setVisibility(View.GONE);

            createVideosFoundHUD();
            createVideosFoundTV();
            createFoundVideosWindow();
            updateFoundVideosBar();
        }

        return view;
    }

    class FBDownloader {
        private final Context mContext;

        FBDownloader(Context context) {
            mContext = context;
        }

        @SuppressLint("JavascriptInterface")
        @JavascriptInterface
        void processVideo(String s, String s1) {
            final String url = "https://m.facebook.com/video.php?story_fbid=" + s1;


            new VideoContentSearch(mContext, s, s, "facebook.com") {
                @Override
                public void onStartInspectingURL() {

                }

                @Override
                public void onFinishedInspectingURL(boolean finishedAll) {

                }

                @Override
                public void onVideoFound(String size, String type, String link, String name, String page, boolean chunked, String website, boolean audio) {
                    videoList.clear();
                    videoList.addItem(size, type, link, name, page, chunked, website, audio);

                    Log.e("data", "facebook  video founde");
                    updateFoundVideosBar();
                }
            }.start();
            Log.e("vid_id1", url);

        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        if (!loadedFirsTime) {

            if (url.contains("facebook.com")) {
                page.getSettings().setJavaScriptEnabled(true);
//        myWebView.getSettings().setMediaPlaybackRequiresUserGesture(true);
                page.getSettings().setUserAgentString("Android 7.0");
                page.getSettings().setPluginState(WebSettings.PluginState.ON);
                page.getSettings().setBuiltInZoomControls(true);
                page.getSettings().setDisplayZoomControls(true);
                page.getSettings().setUseWideViewPort(true);
                page.getSettings().setLoadWithOverviewMode(true);
                page.setWebViewClient(new WebViewClient() {

                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        if (blockedWebsites.contains(Utils.getBaseDomain(request.getUrl().toString()))) {
                            Log.d("vdd", "URL : " + request.getUrl().toString());
                            new AlertDialog.Builder(getContext())
                                    .setMessage("Youtube is not supported according to google policy.")
                                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .create()
                                    .show();
                            return true;
                        }
                        return super.shouldOverrideUrlLoading(view, request);
                    }

                    @Override
                    public void onPageStarted(WebView webview, final String url, Bitmap favicon) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                EditText urlBox = getVDActivity().findViewById(R.id.et_search_bar);
                                urlBox.setText(url);
                                urlBox.setSelection(urlBox.getText().length());
                                BrowserWindow.this.url = url;
                            }
                        });
                        view.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
                        loadingPageProgress.setVisibility(View.VISIBLE);
                        super.onPageStarted(webview, url, favicon);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        loadingPageProgress.setVisibility(View.GONE);
                        page.loadUrl("javascript:(function() { "
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

                        page.loadUrl("javascript:(function prepareVideo() { "
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
                        page.loadUrl("javascript:( window.onload=prepareVideo;" + ")()");
                    }
                });
            } else {

                WebSettings webSettings = page.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setAllowUniversalAccessFromFileURLs(true);
                webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                page.setWebViewClient(new WebViewClient() {//it seems not setting webclient, launches
                    //default browser instead of opening the page in webview
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        if (blockedWebsites.contains(Utils.getBaseDomain(request.getUrl().toString()))) {
                            Log.d("vdd", "URL : " + request.getUrl().toString());
                            new AlertDialog.Builder(getContext())
                                    .setMessage("Youtube is not supported according to google policy.")
                                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .create()
                                    .show();
                            return true;
                        }
                        return super.shouldOverrideUrlLoading(view, request);
                    }

                    @Override
                    public void onPageStarted(final WebView webview, final String url, Bitmap favicon) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                EditText urlBox = getVDActivity().findViewById(R.id.et_search_bar);
                                urlBox.setText(url);
                                urlBox.setSelection(urlBox.getText().length());
                                BrowserWindow.this.url = url;
                            }
                        });
                        view.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
                        loadingPageProgress.setVisibility(View.VISIBLE);
                        super.onPageStarted(webview, url, favicon);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        loadingPageProgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadResource(final WebView view, final String url) {
                        Log.d("fb :", "URL: " + url);
                        final String page = view.getUrl();
                        final String title = view.getTitle();

                   /* if (view.getUrl().contains("facebook.com")) {
                        Log.e("data","facebook   "+view.getUrl());
                        view.loadUrl("javascript:(function prepareVideo() { var el = document.querySelectorAll('div[data-sigil]');for(var i=0;i<el.length; i++){var sigil = el[i].dataset.sigil;if(sigil.indexOf('inlineVideo') > -1){console.log(i);var jsonData = JSON.parse(el[i].dataset.store);el[i].setAttribute('onClick', 'FBDownloader.processVideo(\"'+jsonData['src']+'\",\"'+jsonData['videoID']+'\");');}}})()");
                    } else {
                   */
                        new VideoContentSearch(getActivity(), url, page, title) {
                            @Override
                            public void onStartInspectingURL() {
                                Utils.disableSSLCertificateChecking();
                            }

                            @Override
                            public void onFinishedInspectingURL(boolean finishedAll) {
                                HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSLSF);
                            }

                            @Override
                            public void onVideoFound(String size, String type, String link, String name, String page, boolean chunked, String website, boolean audio) {
                                videoList.addItem(size, type, link, name, page, chunked, website, audio);
                                updateFoundVideosBar();
                            }
                        }.start();
                    }
//                }

                    @Override
                    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                        if (getActivity() != null) {
                            Log.d("VDDebug", "Url: " + url);
                            if (getActivity().getSharedPreferences("settings", 0).getBoolean(getString(R
                                    .string.adBlockON), true)
                                    && (url.contains("ad") || url.contains("banner") || url.contains("pop"))
                                    && getVDActivity().getBrowserManager().checkUrlIfAds(url)) {
                                Log.d("VDDebug", "Ads detected: " + url);
                                return new WebResourceResponse(null, null, null);
                            }
                        }
                        return super.shouldInterceptRequest(view, url);
                    }

                    @Override
                    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getVDActivity() !=
                                null) {
                            if (VDApp.getInstance().getSharedPreferences("settings", 0).getBoolean(getString
                                    (R.string.adBlockON), false)
                                    && (request.getUrl().toString().contains("ad") ||
                                    request.getUrl().toString().contains("banner") ||
                                    request.getUrl().toString().contains("pop"))
                                    && getVDActivity().getBrowserManager().checkUrlIfAds(request.getUrl()
                                    .toString())) {
                                Log.i("VDInfo", "Ads detected: " + request.getUrl().toString());
                                return new WebResourceResponse(null, null, null);
                            } else return null;
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                return shouldInterceptRequest(view, request.getUrl().toString());
                            }
                        }
                        return null;
                    }
                });
                page.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onProgressChanged(WebView view, int newProgress) {
                        loadingPageProgress.setProgress(newProgress);
                    }

                    @Override
                    public void onReceivedTitle(WebView view, String title) {
                        super.onReceivedTitle(view, title);
                        videoList.deleteAllItems();
                        updateFoundVideosBar();
                        VisitedPage vp = new VisitedPage();
                        vp.title = title;
                        vp.link = view.getUrl();
                        new HistorySQLite(getActivity()).addPageToHistory(vp);
                    }
                });
            }

            page.loadUrl(url);
            loadedFirsTime = true;
        } else {
            EditText urlBox = getVDActivity().findViewById(R.id.et_search_bar);
            urlBox.setText(url);
            urlBox.setSelection(urlBox.getText().length());
        }
    }

    @Override
    public void onDestroy() {
        page.stopLoading();
        page.destroy();
        super.onDestroy();
    }

    private void updateFoundVideosBar() {
        if (getActivity() == null) {
            Log.d("debug", "No activity found");
            return;
        } else {
            Log.d("debug", "Activity found");
            if (videoList.getSize() > 0) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //videosFoundHUD.setImageDrawable(getResources().getDrawable(R.drawable.icon_download));
                        videosFoundHUD.setBackground(getResources().getDrawable(R.drawable.bg_download_enble));
                        videosFoundHUD.setEnabled(true);
                        Animation expandIn = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.expand_in);
                        videosFoundHUD.startAnimation(expandIn);
                        tvDownloadCount.startAnimation(expandIn);
                        tvDownloadCount.setText(String.valueOf(videoList.getSize()));
                        tvDownloadCount.setVisibility(View.VISIBLE);
                    }
                });

            } else {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //videosFoundHUD.setImageDrawable(getResources().getDrawable(R.drawable.icon_download_na));
                        videosFoundHUD.setBackground(getResources().getDrawable(R.drawable.bg_download_disble));
                        tvDownloadCount.setVisibility(View.GONE);
                        videosFoundHUD.setEnabled(false);
                        if (foundVideosWindow.getVisibility() == View.VISIBLE)
                            foundVideosWindow.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private void updateVideoPlayer(String url) {
        videoFoundTV.setVisibility(View.VISIBLE);
        Uri uri = Uri.parse(url);
        Log.d("debug", url);
        videoFoundView.setVideoURI(uri);
        videoFoundView.start();
    }

    @Override
    public void onBackpressed() {
        if (foundVideosWindow.getVisibility() == View.VISIBLE && !videoFoundView.isPlaying() && videoFoundTV.getVisibility() == View.GONE) {
            foundVideosWindow.setVisibility(View.GONE);
        } else if (videoFoundView.isPlaying() || videoFoundTV.getVisibility() == View.VISIBLE) {
            videoFoundView.closePlayer();
            videoFoundTV.setVisibility(View.GONE);
        } else if (page.canGoBack()) {
            page.goBack();
        } else {
            getVDActivity().getBrowserManager().closeWindow(BrowserWindow.this);
        }
    }

    @JavascriptInterface
    public void processVideo(final String vidData, final String vidID) {
//        Toast.makeText(this, "Download Link: " + vidID, Toast.LENGTH_LONG).show();
        showDialog(vidData, vidID);
    }

    private void showDialog(final String vidData, final String vidID) {


        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.download_dialog, null);
        TextView download = view.findViewById(R.id.download);
        TextView watch = view.findViewById(R.id.watch);
        TextView sizeVideo = view.findViewById(R.id.size);
        builder.setView(view);
        URL url= null;
        try {
            url = new URL(vidData);
            long size=getFileSizeFromUrl(url);
            String sizeFormatted = Formatter.formatShortFileSize(getActivity(), size);
            sizeVideo.setText(sizeFormatted);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }


        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    new DownloadPermissionHandler(getActivity()) {
                        @Override
                        public void onPermissionGranted() {
                            startDownload(vidData, vidID);
                        }
                    }.checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            PermissionRequestCodes.DOWNLOADS);
                } else {
                    startDownload(vidData, vidID);
                }


                alertDialog.dismiss();
            }
        });

        watch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), VideoPlayer.class);
                intent.putExtra("video_uri", vidData);
                startActivity(intent);
                alertDialog.dismiss();

            }
        });

        alertDialog=builder.create();
        alertDialog.show();

    }

    public String getUrl() {
        return url;
    }

    @Override
    public void onPause() {
        super.onPause();
        page.onPause();
        Log.d("debug", "onPause: ");
    }

    @Override
    public void onResume() {
        super.onResume();
        page.onResume();
        Log.d("debug", "onResume: ");
    }

    public long getFileSizeFromUrl(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            return conn.getContentLength();
        } catch (IOException e) {
            //Maybe throw the exception, or just stacktrace it. Your call
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return 0;
    }
    private void startDownload(final String vidData, final String vidID) {
        try {

            URL url=new URL(vidData);
            long size=getFileSizeFromUrl(url);
            DownloadQueues queues = DownloadQueues.load(getActivity());
            queues.insertToTop(String.valueOf(size), "mp4", vidData, "facebook" + vidID, "0", false, "Facebook.com");
            queues.save(getActivity());
            DownloadVideo topVideo = queues.getTopVideo();
            Intent downloadService = VDApp.getInstance().getDownloadService();
//                VDApp.getInstance().stopService(downloadService);
//                DownloadManager.stopThread();
//            DownloadManager.stop();
            downloadService.putExtra("link", topVideo.link);
            downloadService.putExtra("name", topVideo.name);
            downloadService.putExtra("type", topVideo.type);
            downloadService.putExtra("size", topVideo.size);
            downloadService.putExtra("page", topVideo.page);
            downloadService.putExtra("chunked", topVideo.chunked);
            downloadService.putExtra("website", topVideo.website);
            VDApp.getInstance().startService(downloadService);
            Toast.makeText(getActivity(), "Downloading video in the background. Check the " +
                    "Downloads panel", Toast.LENGTH_LONG).show();
        } catch (Exception e) {

            Log.i("Download error", e.toString());
        }


//        try {
//
//            Uri downloadUri = Uri.parse(vidData);
//            android.app.DownloadManager.Request req = new android.app.DownloadManager.Request(downloadUri);
////                            req.setDestinationUri(Uri.fromFile(mFilePath));
//            req.addRequestHeader("Accept", "application/mp4");
//           // req.setDestinationInExternalPublicDir(getString(R.string.app_name),  File.separator + "facebook_fb" + vidID + ".mp4");
//            req.setDestinationUri(Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/All SX Video Downloader", "facebook_fb" + vidID + ".mp4")));
//
//            req.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//            android.app.DownloadManager dm = (DownloadManager) getActivity().getSystemService(getActivity().DOWNLOAD_SERVICE);
//            dm.enqueue(req);
//
//
//        } catch (Exception e) {
//            Log.i("CrashTesting", e.toString());
//            Toast.makeText(getActivity(), "Video Can't be downloaded! Try Again", Toast.LENGTH_LONG).show();
//        }
    }


}
