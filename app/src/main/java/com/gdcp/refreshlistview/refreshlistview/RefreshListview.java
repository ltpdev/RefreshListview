package com.gdcp.refreshlistview.refreshlistview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;

/**
 * Created by asus- on 2018/1/2.
 */

public class RefreshListview extends ListView implements AbsListView.OnScrollListener{
    private View headerView;
    private int headerViewHeight;
    private float downY;
    private float moveY;
    public static final int PULL_TO_REFRESH=0;
    public static final int RELEASE_TO_REFRESH=1;
    public static final int REFRESHING=2;
    private int currentState=PULL_TO_REFRESH;
    private RotateAnimation rotateUpAnim;
    private RotateAnimation rotateDownAnim;
    private ImageView mArrowView;
    private ProgressBar pb;
    private TextView mTitleText;
    private TextView mLastRefreshTime;
    private View footView;
    private int footViewHeight;
    private boolean isLoadingMore=false;

    public RefreshListview(Context context) {
        super(context);
        init();
    }

    public RefreshListview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RefreshListview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        //加载头布局
       initHeaderView();
       initFootView();
        initAnimation();
        setOnScrollListener(this);
    }

    private void initFootView() {
       footView=View.inflate(getContext(),R.layout.layout_footer_list,null);
        footView.measure(0,0);//按照设置的规则测量
        footViewHeight=footView.getMeasuredHeight();
        footView.setPadding(0,-footViewHeight,0,0);
        addFooterView(footView);
    }

    private void initAnimation() {
         rotateUpAnim=new RotateAnimation(0f,-180f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
          rotateUpAnim.setDuration(3000);
        rotateUpAnim.setFillAfter(true);
        // 向下转, 围绕着自己的中心, 逆时针旋转 -180 -> -360
        rotateDownAnim = new RotateAnimation(-180f, -360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateDownAnim.setDuration(300);
        rotateDownAnim.setFillAfter(true); // 动画停留在结束位置
    }

    private void initHeaderView() {
        //
         headerView=View.inflate(getContext(),R.layout.layout_header_list,null);
        mArrowView = (ImageView) headerView.findViewById(R.id.iv_arrow);
        pb = (ProgressBar) headerView.findViewById(R.id.pb);
        mTitleText = (TextView) headerView.findViewById(R.id.tv_title);
        mLastRefreshTime = (TextView) headerView.findViewById(R.id.tv_desc_last_refresh);
         headerView.measure(0,0);//按照设置的规则测量
         headerViewHeight=headerView.getMeasuredHeight();
        headerView.setPadding(0,-headerViewHeight,0,0);
        addHeaderView(headerView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                 downY=ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                moveY=ev.getY();
                // 如果是正在刷新中, 就执行父类的处理
                if(currentState == REFRESHING){
                    return super.onTouchEvent(ev);
                }
                float offset=moveY-downY;
                if (offset>0&&getFirstVisiblePosition()==0) {
                    int paddingTop = (int) (-headerViewHeight + offset);
                    headerView.setPadding(0, paddingTop, 0, 0);
                    if (paddingTop>=0&&currentState!=RELEASE_TO_REFRESH){
                        currentState=RELEASE_TO_REFRESH;
                        updateHeader(); //
                    }else if (paddingTop<0&&currentState!=PULL_TO_REFRESH){
                        currentState=PULL_TO_REFRESH;
                        updateHeader(); //
                    }
                    return true; // 当前事件被我们处理并消费
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currentState==PULL_TO_REFRESH){
                    headerView.setPadding(0,-headerViewHeight,0,0);
                }else if(currentState == RELEASE_TO_REFRESH){
                    headerView.setPadding(0, 0, 0, 0);
                    currentState = REFRESHING;
                    updateHeader();
                }

                break;
        }
        return super.onTouchEvent(ev);
    }
    /**
     * 根据状态更新头布局内容
     */
    private void updateHeader() {
          switch (currentState){
              case PULL_TO_REFRESH:
                   mArrowView.startAnimation(rotateDownAnim);
                     mTitleText.setText("下拉刷新");
                   break;
              case RELEASE_TO_REFRESH:
                  mArrowView.startAnimation(rotateUpAnim);
                  mTitleText.setText("释放刷新");
                  break;
              case REFRESHING:
                  mArrowView.clearAnimation();
                  mArrowView.setVisibility(View.INVISIBLE);
                  pb.setVisibility(View.VISIBLE);
                  mTitleText.setText("正在刷新中...");
                  if (onRereshListener!=null){
                      onRereshListener.onRefresh();
                  }
                  break;
          }
    }

    //刷新完毕
    public void onRefreshComplete() {
        currentState=PULL_TO_REFRESH;
        mTitleText.setText("下拉刷新");
        headerView.setPadding(0,-headerViewHeight,0,0);
        pb.setVisibility(View.INVISIBLE);
        mArrowView.setVisibility(View.VISIBLE);
        String time=getTime();
        mLastRefreshTime.setText("最后刷新时间:"+time);
    }
//加载更多完毕
    public void onLoadMoreComplete() {
        footView.setPadding(0, -footViewHeight, 0, 0);
        isLoadingMore = false;
    }
    private String getTime() {
        long currentTime=System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(currentTime);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        if(isLoadingMore){
            return; // 已经在加载更多.返回
        }
        // 最新状态是空闲状态, 并且当前界面显示了所有数据的最后一条. 加载更多
        if (scrollState==SCROLL_STATE_IDLE&&getLastVisiblePosition()>=(getCount()-1)){
            isLoadingMore = true;
            footView.setPadding(0, 0, 0, 0);
            setSelection(getCount());
            // 跳转到最后一条, 使其显示出加载更多.
            if (onRereshListener!=null){
                onRereshListener.onLoadMore();
            }
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i1, int i2) {

    }

    public interface OnRereshListener{
        void onRefresh();
        void onLoadMore();// 加载更多
    }

    private OnRereshListener onRereshListener;

    public void setOnRereshListener(OnRereshListener onRereshListener) {
        this.onRereshListener = onRereshListener;
    }
}
