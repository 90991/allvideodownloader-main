package com.delinnovtech.allsxvideodownloader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.delinnovtech.allsxvideodownloader.R;

import java.util.ArrayList;
import java.util.List;

public class VideoSitesList extends RecyclerView.Adapter<VideoSitesList.VideoStreamingSiteItem> {
    private List<Site> sites;
    private MainActivity activity;


    class Site {
        int drawable;
        String title;
        String url;


        Site(int drawable, String title, String url) {
            this.drawable = drawable;
            this.title = title;
            this.url = url;

        }
    }

    VideoSitesList(MainActivity activity) {
        this.activity = activity;
        sites = new ArrayList<>();
        this.activity = activity;
        sites = new ArrayList<>();
        //sites.add(new Site(R.drawable.favicon_youtube, "youtube", "https://m.youtube.com"));
        sites.add(new Site(R.drawable.favicon_facebook,"Facebook", "https://m.facebook.com"));
        sites.add(new Site(R.drawable.whatsapp,"Whatsapp", "https://web.whatsapp.com/"));
        sites.add(new Site(R.drawable.favicon_instagram,"Instagram", "https://www" +
                ".instagram.com"));
//        sites.add(new Site(R.drawable.favicon_dailymotion,"Dailymotion", "https://www" +
//                ".dailymotion.com"));

        sites.add(new Site(R.drawable.favicon_twitter, "Twitter", "https://mobile.twitter.com"));

        sites.add(new Site(R.drawable.favicon_veoh,  "veoh", "https://www.veoh.com"));
         sites.add(new Site(R.drawable.favicon_vimeo , "Vimeo", "https://vimeo.com"));
  //      sites.add(new Site(R.drawable.favicon_vk,"vk", "https://m.vk.com"));
  //      sites.add(new Site(R.drawable.favicon_fc2, "fc2", "https://video.fc2.com"));
    //    sites.add(new Site(R.drawable.favicon_vlive,"Vlive", "https://m.vlive.tv"));
      //  sites.add(new Site(R.drawable.favicon_naver, "naver", "https://m.tv.naver.com"));
        sites.add(new Site(R.drawable.favicon_metacafe, "metacafe", "https://www.metacafe.com"));
        sites.add(new Site(R.drawable.favicon_tudou , "Tudou", "https://www.tudou.com"));
        sites.add(new Site(R.drawable.favicon_youku, "Youku", "https://m.youku.com"));
        sites.add(new Site(R.drawable.favicon_myspace,"Myspace", "https://myspace.com"));
        sites.add(new Site(R.drawable.favicon_vine,"Vine", "https://vine.co"));
        sites.add(new Site(R.drawable.favicon_tumblr,"Tumblr", "https://www.tumblr.com"));

    }

    @Override
    public VideoStreamingSiteItem onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_sites_item, parent, false);
        return new VideoStreamingSiteItem(view);
    }

    @Override
    public void onBindViewHolder(VideoStreamingSiteItem holder, int position) {
        holder.bind(sites.get(position));
     }

    @Override
    public int getItemCount() {
        return sites.size();
    }

    class VideoStreamingSiteItem extends RecyclerView.ViewHolder {
        private ImageView icon;
        private TextView title;
        private LinearLayout layout;

        VideoStreamingSiteItem(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivVideoSiteIcon);
            title = itemView.findViewById(R.id.ivVideoSiteName);
            layout = itemView.findViewById(R.id.imageBackground);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                      if(sites.get(getAdapterPosition()).title.equals("Whatsapp")){
                         activity.whatsappClicked();

                    }else
                        {
                        //Set url in search bar
                        EditText search = activity.findViewById(R.id.et_search_bar);
                        search.setText(sites.get(getAdapterPosition()).url);
                        // Start searching
                        activity.getBrowserManager().newWindow(sites.get(getAdapterPosition()).url);
                        activity.hideToolbar();
                    }
                }
            });

        }

        void bind(Site site) {
            icon.setImageDrawable(activity.getResources().getDrawable(site.drawable));
            title.setText(site.title);
        }
    }
}
