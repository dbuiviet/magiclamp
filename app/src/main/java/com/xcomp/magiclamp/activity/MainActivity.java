package com.xcomp.magiclamp.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.xcomp.magiclamp.Entity.ImageModel;
import com.xcomp.magiclamp.Entity.SessionModel;
import com.xcomp.magiclamp.R;
import com.xcomp.magiclamp.Utils.Utils;
import com.xcomp.magiclamp.WebserviceGeneralManage.VolleyRequest;
import com.xcomp.magiclamp.WebserviceGeneralManage.WebserviceInfors;
import com.xcomp.magiclamp.adapter.GridViewAdapter_Images;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import helpers.MqttHelper;

public class MainActivity extends BaseActivity {

    static private String TAG = MainActivity.class.getSimpleName();

    public String mqttTopic = "/topic/qos0";

    MqttHelper mqttHelper;

    GridViewAdapter_Images gridViewAdapter;
    RecyclerView gridRecycleView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    ArrayList<ImageModel> imageModelsList;
    ArrayList<SessionModel> sessionModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
//        actionbar.setDisplayHomeAsUpEnabled(true);
//        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayShowCustomEnabled(true);
//        actionbar.setHomeAsUpIndicator(R.drawable.ico_menu);

        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View navbar_custom_view = inflator.inflate(R.layout.navbar_custom_view, null);
        ActionBar.LayoutParams layout = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionbar.setCustomView(navbar_custom_view);
        setupNavbarActions(navbar_custom_view);


        gridRecycleView = (RecyclerView) findViewById(R.id.images_gridview);
        gridViewAdapter = new GridViewAdapter_Images(this);
        RecyclerView.LayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        gridRecycleView.setLayoutManager(gridLayoutManager);
        ((GridLayoutManager) gridLayoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int spanType = (gridViewAdapter.getItemViewType(position) == GridViewAdapter_Images.ITEM_VIEW_TYPE_SESSION) ? 2 : 1;
                Log.e(TAG, "getSpanSize for position: " + position + " span type: " + spanType);
                return spanType;
            }
        });

        gridRecycleView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(2), true));
//        gridRecycleView.addItemDecoration(new MarginDecoration(this));
        gridRecycleView.setItemAnimator(new DefaultItemAnimator());
        gridRecycleView.setAdapter(gridViewAdapter);

        this.rootViewToShowLoadingIndicator = (ViewGroup)findViewById(R.id.content_frame);
//        ActionBar actionbar = getSupportActionBar();
//        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View v = inflator.inflate(R.layout.navbar_custom_view, null);
//        actionbar.setDisplayShowTitleEnabled(true);
//        actionbar.setDisplayShowCustomEnabled(true);
//        ActionBar.LayoutParams layout = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
//        actionbar.setCustomView(v);

        this.mSwipeRefreshLayout = this.findViewById(R.id.swipeRefresh);
        setupRefreshWhenSwipe();

//        getAllImageModel();
        getAllSession();
        mqttHelper = new MqttHelper(this, "haveNewPage", new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.e(TAG, "messageArrived on topic: " + topic + " --- message: " + message);
                MainActivity.this.getAllSession();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void setupNavbarActions(View navbar_custom_view) {
        navbar_custom_view.findViewById(R.id.bt_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.onMenuButtonClicked(v);
            }
        });

        navbar_custom_view.findViewById(R.id.bt_turnon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.onTurnOnButtonClicked(v);
            }
        });

        navbar_custom_view.findViewById(R.id.bt_turnoff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.onTurnOffButtonClicked(v);
            }
        });
    }

    /**
     * RecyclerView item decoration - give equal margin around grid item
     */
    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.setting_menu, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.setting_item) {

            Intent intent = new Intent(this,SettingActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupRefreshWhenSwipe() {

        this.mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                MainActivity.this.mSwipeRefreshLayout.setRefreshing(false);
                if (!Utils.hasInternetConnection(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, MainActivity.this.getResources().getString(R.string.connect_internet_require_message), Toast.LENGTH_SHORT).show();
                    return;
                }
                MainActivity.this.showLoadingIndicator("");
                MainActivity.this.getAllSession();
            }
        });
    }

    public void getAllImageModel() {

        if(!Utils.hasInternetConnection(MainActivity.this)) {
            Toast.makeText(MainActivity.this, MainActivity.this.getResources().getString(R.string.connect_internet_alert_message), Toast.LENGTH_SHORT).show();
            return;
        }

        this.showLoadingIndicator("");
        RequestQueue queue = VolleyRequest.getInstance(MainActivity.this).getRequestQueue();

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, WebserviceInfors.base_host_service + WebserviceInfors.get_all_image_model,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e(TAG, response);
                        MainActivity.this.hideLoadingIndicator();

                        final ArrayList<ImageModel> questionList = new ArrayList<>() ;

                        try {
                            JSONObject jsResponse = new JSONObject(response);
                            if (jsResponse.getInt("code") == 200) {
                                questionList.clear();
                                JSONArray jsonArray = jsResponse.getJSONArray("content");
                                Log.e(TAG, "onErrorResponse: " + jsonArray);

                                if (jsonArray != null) {
                                    if(jsonArray.length() > 0) {
                                        for (int i=0; i < jsonArray.length(); i++ ){
                                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                                            ImageModel question = ImageModel.createImageModelFromJsonObject(jsonObject);
                                            questionList.add(question);
                                        }
                                    }
                                }
                                MainActivity.this.gridViewAdapter.setImageModelList(questionList);
                            }
                            else {
//                                Toast.makeText(MainActivity.this,jsResponse.getString("content"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        MainActivity.this.hideLoadingIndicator();
                        Log.e(TAG, "onErrorResponse: " + error.toString());
//                        Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
//        {
//            @Override
//            protected Map<String, String> getParams() throws AuthFailureError {
//                Map<String, String> params = new HashMap<String, String>();
//
//                params.put("status", Integer.toString(currentSelectedStatus));
//                params.put("key", category);
//                params.put("offer_id", user_id);
//                params.put("order", Integer.toString(order));
//                return params;
//            }

//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                Map<String, String>  params = new HashMap<>();
//                SharedPreferences sharedPreferences = Utils.getCommonSharepreference(MainActivity.this);
//                params.put("x-access-token", sharedPreferences.getString(getResources().getString(R.string.login_token),""));
//                return params;
//            }

//        };
        queue.getCache().clear();
        queue.add(stringRequest);
    }

    public void getAllSession() {

        if(!Utils.hasInternetConnection(MainActivity.this)) {
            Toast.makeText(MainActivity.this, MainActivity.this.getResources().getString(R.string.connect_internet_alert_message), Toast.LENGTH_SHORT).show();
            return;
        }

        this.showLoadingIndicator("");
        RequestQueue queue = VolleyRequest.getInstance(MainActivity.this).getRequestQueue();

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, WebserviceInfors.base_host_service + WebserviceInfors.get_all_session,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e(TAG, response);
                        MainActivity.this.hideLoadingIndicator();

                        final ArrayList<SessionModel> sessionList = new ArrayList<>() ;

                        try {
                            JSONObject jsResponse = new JSONObject(response);
                            if (jsResponse.getInt("code") == 200) {
                                sessionList.clear();
                                JSONArray jsonArray = jsResponse.getJSONArray("content");
                                Log.e(TAG, "onErrorResponse: " + jsonArray);

                                if (jsonArray != null) {
                                    if(jsonArray.length() > 0) {
                                        for (int i=0; i < jsonArray.length(); i++ ){
                                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                                            SessionModel sessionModel = SessionModel.createImageModelFromJsonObject(jsonObject);
                                            sessionList.add(sessionModel);
                                        }
                                    }
                                }
                            }
                            else {
//                                Toast.makeText(MainActivity.this,jsResponse.getString("content"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        MainActivity.this.gridViewAdapter.setSessionModelList(sessionList);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        MainActivity.this.hideLoadingIndicator();
                        Log.e(TAG, "onErrorResponse: " + error.toString());
//                        Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
//        {
//            @Override
//            protected Map<String, String> getParams() throws AuthFailureError {
//                Map<String, String> params = new HashMap<String, String>();
//
//                params.put("status", Integer.toString(currentSelectedStatus));
//                params.put("key", category);
//                params.put("offer_id", user_id);
//                params.put("order", Integer.toString(order));
//                return params;
//            }

//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                Map<String, String>  params = new HashMap<>();
//                SharedPreferences sharedPreferences = Utils.getCommonSharepreference(MainActivity.this);
//                params.put("x-access-token", sharedPreferences.getString(getResources().getString(R.string.login_token),""));
//                return params;
//            }

//        };
        queue.getCache().clear();
        queue.add(stringRequest);

    }

    public void onTurnOnButtonClicked(View v) {
        String msg = "on";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = msg.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            message.setId(5866);
            message.setRetained(true);
            message.setQos(0);
            //mqttHelper.mqttAndroidClient.publish(mqttTopic,message);
            mqttHelper.mqttAndroidClient.publish("/topic/qos0", message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void onTurnOffButtonClicked(View v) {
        String msg = "off";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = msg.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            message.setId(5866);
            message.setRetained(true);
            message.setQos(0);
            //mqttHelper.mqttAndroidClient.publish(mqttTopic,message);
            mqttHelper.mqttAndroidClient.publish("/topic/qos0", message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void onMenuButtonClicked(View v) {
        Intent intent = new Intent(this,SettingActivity.class);
        startActivity(intent);
    }
}
