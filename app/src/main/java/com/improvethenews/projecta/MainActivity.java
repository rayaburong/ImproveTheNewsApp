package com.improvethenews.projecta;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayoutManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ArticleAdapter articleAdapter;
    private SliderAdapter topicSliderAdapter, biasSliderAdapter;
    private TopicAdapter topicAdapter;
    private ArrayList<Slider> topicSliderList, biasSliderList;
    private ArrayList<Article> articleList = new ArrayList<>();
    private ArrayList<Topic> topicList;
    private String topic, title, settings, path, depth;
    private RecyclerView rv, bsrv, tsrv;
    private SharedPreferences sp;
    private MenuItem searchItem;
    private SearchView searchView;
    private LinearLayoutManager bllm, sllm;
    private FlexboxLayoutManager flm;
    private DisplayMetrics displayMetrics;
    private BottomSheetBehavior biasBottomSheetBehavior, topicBottomSheetBehavior;
    private ConstraintLayout biasSliderBottomSheet, topicSliderBottomSheet;
    private FloatingActionButton fab;

    public void updateArticles(String superSliderChange) {
        applySliderListChanges();
        getArticleList(topic, superSliderChange, false);
        Toast toast = Toast.makeText(getApplicationContext(), "Updating news...", Toast.LENGTH_SHORT);
        toast.show();
//        articleAdapter = new ArticleAdapter(articleList, topic + " " + settings, path, depth, displayMetrics.heightPixels, displayMetrics.widthPixels, showTopicSliders);
//        rv.setAdapter(articleAdapter);
    }

    private void showTopicsScreen() {
        searchItem.setVisible(true);
        searchView.setVisibility(View.VISIBLE);
        fab.hide();
        applySliderListChanges();
        topicAdapter = new TopicAdapter(topicList);
        rv.setAdapter(topicAdapter);
    }

    private ImageView.OnClickListener showTopicSliders = new ImageView.OnClickListener() {
        @Override
        public void onClick(View v){
            applySliderListChanges();
            getTopicAndSliderList(topic, depth);
            if (topicBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN)
                topicBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            else
                topicBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    };

    private String getRequestURL(String topic, String superSliderChange) {
        String base = getResources().getString(R.string.article_source) + topic;
        settings = "";
        base += "&sliders=";
        Map<String,?> allSliders = sp.getAll();
        //Throw all settings into the URL (is it slow?)
        for (Map.Entry<String,?> slider : allSliders.entrySet()) {
            if (slider.getKey().length() != 2) continue;
            settings += slider.getKey();
            String n = String.valueOf(slider.getValue());
            settings += ("00" + n).substring(n.length());
        }
        if (!superSliderChange.equals(""))
            settings += "_" + superSliderChange;
        Log.d("TAG", "getRequestURL: " + base + settings);
        return base + settings;
    }

    private String getArticleList(String topic, String superSliderChange, boolean initial) {
        try {
            ArticleExtractor extractor = new ArticleExtractor(this, new URL(getRequestURL(topic, superSliderChange)), initial, articleList, articleAdapter);
            return extractor.pull();
//            articleList = extractor.getArticleList();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    String[][] defaultSliders = {
            {"Political Stance", "LR", "50", "Left", "Right"},
            {"Establishment Stance", "PE", "50", "Critical", "Pro"},
            {"Writing Style", "NU", "70", "Provocative", "Nuanced"},
            {"Depth", "DE", "70", "Breezy", "Detailed"},
            {"Shelf-Life", "SL", "70", "Short", "Long"},
            {"Recency", "RE", "70", "Evergreen", "Latest"}
    };
    public void updateSliderList(String topic) {
        //mnemonic, to display, official name, "lowercase" name, depth, popularity, code
        topic = topic.split("\\.")[0];
        for (int i = 0; i < biasSliderList.size(); i++) {
            if (biasSliderList.get(i).getCode().length() == 2)
                biasSliderList.get(i).setValue(sp.getInt(biasSliderList.get(i).getCode(), biasSliderList.get(i).getUsualvalue()));
        }
        biasSliderAdapter.notifyDataSetChanged();
        for (int i = 0; i < topicSliderList.size(); i++) {
            if (topicSliderList.get(i).getCode().length() == 2)
                topicSliderList.get(i).setValue(sp.getInt(topicSliderList.get(i).getCode(), topicSliderList.get(i).getUsualvalue()));
        }
        topicSliderAdapter.notifyDataSetChanged();
    }
    private void getTopicAndSliderList(String topic, String depthString) {
        //mnemonic, to display, official name, "lowercase" name, depth, popularity, code
        topic = topic.split("\\.")[0];
        Log.d("TAG", "getTopicAndSliderList: " + depthString);
        topicList = new ArrayList<Topic>();
        topicSliderList = new ArrayList<Slider>();
        biasSliderList = new ArrayList<Slider>();
        biasSliderList.add(new Slider("Bias Sliders", "", 0, 0, -2));
        for (int i = 0; i < defaultSliders.length; i++) {
            biasSliderList.add(new Slider(defaultSliders[i][0], defaultSliders[i][1], sp.getInt(defaultSliders[i][1], Integer.parseInt(defaultSliders[i][2])), Integer.parseInt(defaultSliders[i][2]), 0, defaultSliders[i][3], defaultSliders[i][4]));
        }

        topicSliderList.add(new Slider("Your " + title + " Feed", "", 0, 0, -1));
        topicSliderList.add(new Slider("", "", 0, 0, 2));
        boolean inRange = false;
        int depth = Integer.valueOf(depthString);
        double base = 1.f;
        InputStream is = this.getResources().openRawResource(R.raw.topics);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String readLine;
        try {
            int cnt = 0;
            float[] hsv = new float[3];
            while ((readLine = br.readLine()) != null) {
                String[] row = readLine.split("\t");
                cnt++;
                if (!row[1].equals("0")) {
                    topicList.add(new Topic(row[2], row[0], Integer.parseInt(row[4])));
                    if (row[0].equals(topic)) {
                        inRange = true;
                        base = Double.parseDouble(row[5]);
                    }
                    else if (inRange && Integer.parseInt(row[4]) == depth + 1) {
                        int pop = Double.valueOf(Double.parseDouble(row[5])/base*99).intValue();
                        hsv[0] = ((4*cnt)%29)*12; //engineered to look "random" and aesthetically pleasing
                        hsv[1] = 0.5f;
                        hsv[2] = 0.7f + (cnt%3)/10f;
                        topicSliderList.add(new Slider(row[2], row[6], sp.getInt(row[6], pop), pop, 1, Color.HSVToColor(hsv)));
                        Log.d("TAG", "getTopicList: " + row[2] + row[6] + pop);
                    }
                    else if (inRange && Integer.parseInt(row[4]) == depth)
                        inRange = false;
                }
            }
            is.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Comparator<Topic> bydepth = new Comparator<Topic>() {
            @Override
            public int compare(Topic topic, Topic t1) {
                return Integer.compare(topic.getDepth(), t1.getDepth());
            }
        };
        Collections.sort(topicList, bydepth);
    }

    private void applySliderListChanges() {
        SharedPreferences.Editor editor = sp.edit();
        for (int i = 0; i < biasSliderList.size(); i++) {
            biasSliderList.set(i, biasSliderAdapter.getItem(i));
            if (biasSliderList.get(i).getCode().length() == 2) {
                editor.putInt(biasSliderList.get(i).getCode(), biasSliderList.get(i).getValue());
                Log.d("TAG", "applySliderListChanges: " + biasSliderList.get(i).getCode() + biasSliderList.get(i).getValue());
            }
        }
        for (int i = 0; i < topicSliderList.size(); i++) {
            topicSliderList.set(i, topicSliderAdapter.getItem(i));
            if (topicSliderList.get(i).getCode().length() == 2) {
                editor.putInt(topicSliderList.get(i).getCode(), topicSliderList.get(i).getValue());
                Log.d("TAG", "applySliderListChanges: " + topicSliderList.get(i).getCode() + topicSliderList.get(i).getValue());
            }
        }
        editor.commit();
        Log.d("TAG", "applySliderListChanges: editor commit");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        setContentView(R.layout.activity_main);

        //user id system
        sp = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        if (!sp.contains("userID"))
        {
            Random random = new Random();
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong("userID", random.nextLong());
            editor.commit();
            Log.d("TAG", "onCreate: " + sp.getLong("userID", 0));
        }

        //screen width and height
        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        topic = getIntent().getStringExtra("Mnemonic");
        if (topic == null)
            topic = "news";
//            topic = "test";

        path = getIntent().getStringExtra("Path");
        if (path == null)
            path = "Headlines";

        depth = getIntent().getStringExtra("Depth");
        if (depth == null)
            depth = "0";

        final SwipeRefreshLayout srl = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (ArticleExtractor.isUpdating()) {
                    srl.setRefreshing(false);
                    return;
                }
                updateArticles("");
                srl.setRefreshing(false);
            }
        });

        rv = (RecyclerView) findViewById(R.id.recycler);
        rv.setHasFixedSize(true);
        flm = new FlexboxLayoutManager(this);
        rv.setLayoutManager(flm);

        bsrv = (RecyclerView) findViewById(R.id.bias_slider_recycler);
        bsrv.setHasFixedSize(true);
        bllm = new LinearLayoutManager(this);
        bsrv.setLayoutManager(bllm);

        tsrv = (RecyclerView) findViewById(R.id.topic_slider_recycler);
        tsrv.setHasFixedSize(true);
        sllm = new LinearLayoutManager(this);
        tsrv.setLayoutManager(sllm);

        getTopicAndSliderList(topic, depth);
        topicAdapter = new TopicAdapter(topicList);
        articleAdapter = new ArticleAdapter(articleList, topic + " " + settings, path, depth, displayMetrics.heightPixels, displayMetrics.widthPixels, showTopicSliders);
        String titleAndNewvals = getArticleList(topic, "",true);
        Log.d("TAG", "onCreate: " + titleAndNewvals);
        title = titleAndNewvals.split("\\$")[0];
//        title = articleList.get(0).getTitle();
        topicSliderList.get(0).setTitle("Your " + title + " Feed");
//        path = path + title;
        topicSliderAdapter = new SliderAdapter(topicSliderList);
        biasSliderAdapter = new SliderAdapter(biasSliderList);

        final View shades = findViewById(R.id.shades);
        biasSliderBottomSheet = findViewById(R.id.bias_slider_bottom_sheet);
        biasBottomSheetBehavior = BottomSheetBehavior.from(biasSliderBottomSheet);
        biasBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        biasBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    shades.setAlpha(0);
                else
                    shades.setAlpha(0.5f);
            }

            @Override
            public void onSlide(@NonNull View view, float v) {
                //TODO: mess with this later when free
            }
        });
        topicSliderBottomSheet = findViewById(R.id.topic_slider_bottom_sheet);
        topicBottomSheetBehavior = BottomSheetBehavior.from(topicSliderBottomSheet);
        topicBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        topicBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    shades.setAlpha(0);
                else
                    shades.setAlpha(0.5f);
            }

            @Override
            public void onSlide(@NonNull View view, float v) {
                //TODO: mess with this later when free
            }
        });
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (biasBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN)
                    biasBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                else
                    biasBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.custom_actionbar);
        if (topic != "news")
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);   //show back button

        //default: show articles
        rv.setAdapter(articleAdapter);
        bsrv.setAdapter(biasSliderAdapter);
        tsrv.setAdapter(topicSliderAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search, menu);

        searchItem = menu.findItem(R.id.app_bar_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                fab.show();
                rv.setAdapter(articleAdapter);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                topicAdapter.getFilter().filter(s);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTopicsScreen();
            }
        });

        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
