package com.gdcp.refreshlistview.refreshlistview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private RefreshListview refreshListview;
    private ArrayList<String> data = new ArrayList<>();
    private MyAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        refreshListview = findViewById(R.id.refreshlistview);
        initData();
    }

    private void initData() {
        for (int i = 0; i < 30; i++) {
            data.add("这是listview数据" + i);
        }
        myAdapter = new MyAdapter();
        refreshListview.setOnRereshListener(new RefreshListview.OnRereshListener() {
            @Override
            public void onRefresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            data.add(0,"这是下拉刷新的数据");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    myAdapter.notifyDataSetChanged();
                                    refreshListview.onRefreshComplete();
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void onLoadMore() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            for (int i = 0; i < 10; i++) {
                                data.add("这是加载更多的数据"+i);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    myAdapter.notifyDataSetChanged();
                                    refreshListview.onLoadMoreComplete();
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        refreshListview.setAdapter(myAdapter);
    }

    class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TextView textView = new TextView(MainActivity.this);
            textView.setTextSize(18f);
            textView.setPadding(20, 20, 20, 20);
            textView.setText(data.get(i));
            return textView;
        }
    }


}
