package com.example.v;


import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.R;

/**
 * @author DragonJiang
 * @date 2015/10/10
 * @Description:
 */
public class BubbleView extends View {

    /**
     * log tag
     */
    private static final String TAG = "CircleDistributionView";
    /**
     * draw circle
     */
    private Paint mCirclePaint;
    /**
     * draw text
     */
    private Paint mTextPaint;
    /**
     * for circle
     */
    private Xfermode mXfermode;
    /**
     * circle alpha
     */
    private static final int CIRCLE_ALPHA = 150;
    /**
     * text alpha
     */
    private static final int TEXT_ALPHA = 255;
    /**
     * circle color
     */
    private static final int[] CIRCLE_COLOR = {
            R.color.circle_color_A,
            R.color.circle_color_B,
            R.color.circle_color_C,
            R.color.circle_color_D,
            R.color.circle_color_E,
            R.color.circle_color_F,
            R.color.circle_color_G,
            R.color.circle_color_H,
    };
    /**
     * list which hold original circles info
     */
    private List<CircleInfo> mOriginalCircleList;
    /**
     * list which hold circles info
     */
    private List<CircleInfo> mCircleInfoList;
    /**
     * current touched point
     */
    private PointF mTouchedPoint;
    /**
     * current touched circle
     */
    private CircleInfo mTouchedCircle;
    /**
     * distance between touched circle and other circle
     */
    private float[] mDistance;
    /**
     * view 的 width
     */
    private int mWidth;
    /**
     * view's height
     */
    private int mHeight;

    /**
     * circle's max radius
     */
    private int mMaxRadius;
    /**
     * circle's min radius
     */
    private int mMinRadius;
    /**
     * bubble's radius, 不显示文字时的小气泡
     */
    private int mBubbleRadius;
    /**
     * list which hold distribution data: area and people cnt
     */
    private List<PeopleDistributionInfo> mDistributionList;

    /**
     * timer to redraw
     */
    private Timer mTimer;
    /**
     * 重绘间隔
     */
    private static final int REDRAW_TIME = 25;
    /**
     * 判断当前是否touching
     */
    private boolean mIsTouching = false;
    /**
     * 数据是否有更新,当重新bindData时设置为true
     */
    private boolean mIsDataChanged = false;


    /**
     * handle to redraw
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    invalidate();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public BubbleView(Context context) {
        super(context);
        init(context);
    }

    public BubbleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BubbleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        if (Build.VERSION.SDK_INT < 18) {
            // 关闭硬件加速
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        if (mCirclePaint == null) {
            mCirclePaint = new Paint();
            mCirclePaint.setAntiAlias(true);
            mXfermode = new PorterDuffXfermode(PorterDuff.Mode.DARKEN);
        }

        if (mTextPaint == null) {
            mTextPaint = new Paint();
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setAlpha(TEXT_ALPHA);
            mTextPaint.setAntiAlias(true);
        }

        if (mTouchedPoint == null) {
            mTouchedPoint = new PointF();
        }

        if (mTimer == null) {
            mTimer = new Timer();
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                calcRunAwayCircle();
                //                calcFloatCircle();
                mHandler.sendEmptyMessage(0);
            }
        }, 0, REDRAW_TIME);
    }

    /**
     * 计算逃逸型动画的圆
     */
    private void calcRunAwayCircle() {
        if (mCircleInfoList == null) {
            return;
        }

        if (mIsTouching) {
            if (mTouchedCircle == null) {
                return;
            }
            //calc the distance between touched circle and other circle
            if (mDistance == null) {
                mDistance = new float[mCircleInfoList.size()];
            }
            for (int i = 0; i < mCircleInfoList.size(); i++) {
                CircleInfo circle = mCircleInfoList.get(i);
                if (circle.getCx() == mTouchedCircle.getCx() && circle.getCy() == mTouchedCircle.getCy()) {
                    mDistance[i] = 0;
                    continue;
                }
                float dx = Math.abs(mTouchedCircle.getCx() - circle.getCx());
                float dy = Math.abs(mTouchedCircle.getCy() - circle.getCy());
                mDistance[i] = (float) Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
            }
        } else {
            //calc the distance 现在的位置与原始的位置之间的距离
            if (mDistance == null) {
                mDistance = new float[mCircleInfoList.size()];
            }
            for (int i = 0; i < mCircleInfoList.size(); i++) {
                CircleInfo circle = mCircleInfoList.get(i);
                CircleInfo oriCircle = mOriginalCircleList.get(i);
                float dx = Math.abs(oriCircle.getCx() - circle.getCx());
                float dy = Math.abs(oriCircle.getCy() - circle.getCy());
                mDistance[i] = (float) Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
            }
        }

        //reset circle Center
        for (int i = 0; i < mCircleInfoList.size(); i++) {
            CircleInfo circleInfo = mCircleInfoList.get(i);
            float[] next = getNextStep(i);
            circleInfo.setCenter(next[0], next[1]);
        }
    }

    /**
     * 计算下一步
     *
     * @param circleIndex
     * @return
     */
    private float[] getNextStep(int circleIndex) {
        float[] next = new float[2];
        CircleInfo info = mCircleInfoList.get(circleIndex);
        CircleInfo oriCircle = mOriginalCircleList.get(circleIndex);
        float step = mCircleInfoList.get(0).getRadius() / info.getRadius();
        float scale = step / mDistance[circleIndex];

        if (mDistance[circleIndex] == 0) {
            next[0] = info.getCx();
            next[1] = info.getCy();
            return next;
        }

        if (mIsTouching) {
            float dx = scale * (info.getCx() - mTouchedCircle.getCx());
            float dy = scale * (info.getCy() - mTouchedCircle.getCy());
            float nextX = dx + info.getCx();
            float nextY = dy + info.getCy();
            float radius = info.getRadius();

            if (nextX >= mWidth - radius) {
                nextX = mWidth - radius;
            }
            if (nextX <= radius) {
                nextX = radius;
            }
            if (nextY >= mHeight - radius) {
                nextY = mHeight - radius;
            }
            if (nextY <= radius) {
                nextY = radius;
            }

            next[0] = nextX;
            next[1] = nextY;
        } else {
            float dx = scale * (oriCircle.getCx() - info.getCx());
            float dy = scale * (oriCircle.getCy() - info.getCy());
            float nextX = dx + info.getCx();
            float nextY = dy + info.getCy();

            if (Math.abs(nextX - oriCircle.getCx()) < step) {
                nextX = oriCircle.getCx();
            }
            if (Math.abs(nextY - oriCircle.getCy()) < step) {
                nextY = oriCircle.getCy();
            }

            next[0] = nextX;
            next[1] = nextY;
        }

        return next;
    }

    /**
     * get current touching circle
     *
     * @return
     */
    private CircleInfo getTouchedCircle() {
        if (mCircleInfoList == null || mCircleInfoList.size() == 0 || mTouchedPoint == null) {
            return null;
        }

        //calc touch point to circle center distance
        float[] distance = new float[mCircleInfoList.size()];
        for (int i = 0; i < mCircleInfoList.size(); i++) {
            CircleInfo circle = mCircleInfoList.get(i);
            float dx = Math.abs(mTouchedPoint.x - circle.getCx());
            float dy = Math.abs(mTouchedPoint.y - circle.getCy());
            distance[i] = (float) Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        }

        //search touched circle
        float dmin = Float.MAX_VALUE;
        int index = -1;
        for (int i = 0; i < distance.length; i++) {
            if (distance[i] < mCircleInfoList.get(i).getRadius() && distance[i] < dmin) {
                dmin = distance[i];
                index = i;
            }
        }

        if (index >= 0) {
            return mCircleInfoList.get(index);
        } else {
            return null;
        }
    }

    /**
     * 第一次运行时初始化circle,
     * 需要在measure方法里得到mWidth和mHeight后运行
     * 需要在updateCircleInfo之前运行
     */
    private void initCircle() {
        //set radius
        mMaxRadius = mWidth / 6;
        mMinRadius = mWidth / 20;
        mBubbleRadius = mWidth / 30;

        //init circle info first time
        if (mCircleInfoList == null) {
            mCircleInfoList = createCircleInfo(mWidth, mHeight);
            if (mCircleInfoList == null || mCircleInfoList.size() == 0) {
                return;
            }
            if (mOriginalCircleList == null) {
                mOriginalCircleList = new ArrayList<>();
                for (CircleInfo circle : mCircleInfoList) {
                    try {
                        //save original circle info
                        mOriginalCircleList.add(circle.clone());
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                    //第一次设置所有的圆的位置
                    circle.setCx(mWidth / 2);
                    circle.setCy(mHeight / 2);
                }
            }
        }
    }

    /**
     * bind data to view
     *
     * @param list
     */
    public void bindData(List<PeopleDistributionInfo> list) {

        mDistributionList = list;
        if (mDistributionList == null) {
            return;
        }
        mIsDataChanged = true;
        invalidate();
    }

    /**
     * 更新圆的信息，半径等
     */
    private void updateCircleInfo() {

        if (mDistributionList == null || mDistributionList.size() <= 0 || mCircleInfoList == null || mCircleInfoList
                .size() <= 0) {
            return;
        }

        //如果地区少于8，就更据地区的数量显示圆的数量。
        if (mDistributionList != null && mDistributionList.size() < 8) {
            for (int i = 7; i > mDistributionList.size() - 1; i--) {
                mCircleInfoList.remove(mCircleInfoList.size() - 1);
            }
        }

        //set circles to bubble size
        for (CircleInfo circle : mCircleInfoList) {
            circle.setRadius(mBubbleRadius);
        }

        //update radius by distribution info
        int maxPeople = mDistributionList.get(0).getPeople();
        int minPeople = mDistributionList.get(mDistributionList.size() - 1).getPeople();
        if (maxPeople == minPeople) {
            //如果人数相同，圆圈的大小就会都是一样的，根据地区的数量，设置圆圈的大小。
            float count = mDistributionList.size();
            if (count > 8) {
                count = 8;
            }
            float r = mWidth / count / 2;
            if (r > mMaxRadius) {
                r = mMaxRadius;
            }
            if (r < mMinRadius) {
                r = mMinRadius;
            }
            for (CircleInfo circle : mCircleInfoList) {
                circle.setRadius(r);
            }
        } else {
            //根据人数的多少，设置圆圈的大小
            for (int i = 0; i < mCircleInfoList.size(); i++) {
                if (mDistributionList.size() > i) {
                    int people = mDistributionList.get(i).getPeople();
                    CircleInfo circle = mCircleInfoList.get(i);
                    float r = (float) (mMaxRadius * people * 1.0 / maxPeople);
                    if (r < mMinRadius) {
                        r = mMinRadius;
                    }
                    circle.setRadius(r);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchedPoint.set(event.getX(), event.getY());//save touched point
                mTouchedCircle = getTouchedCircle(); //get touched circle
                mIsTouching = true;
                break;
            case MotionEvent.ACTION_UP:
                mIsTouching = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int index = mCircleInfoList.indexOf(mTouchedCircle);
                if (index >= 0) {
                    mCircleInfoList.get(index).setCenter(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsTouching = false;
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        initCircle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //if data has changed,update circleInfo
        if (mIsDataChanged) {
            updateCircleInfo();
            mIsDataChanged = false;
        }

        //draw circles
        for (int i = 0; i < mCircleInfoList.size(); i++) {
            CircleInfo info = mCircleInfoList.get(i);

            mCirclePaint.setColor(getResources().getColor(info.getColor()));
            mCirclePaint.setXfermode(mXfermode);
            mCirclePaint.setAlpha(CIRCLE_ALPHA);
            //draw circle
            canvas.drawCircle(info.getCx(), info.getCy(), info.getRadius(), mCirclePaint);

            //draw text
            if (mDistributionList != null && mDistributionList.size() > i) {
                PeopleDistributionInfo distributionInfo = mDistributionList.get(i);
                drawText(info.getCx(), info.getCy(), info.getRadius(), canvas, mTextPaint,
                        distributionInfo.getArea(),
                        distributionInfo.getPeople() + getResources().getString(R.string.unit_people));
            }

        }
    }

    /**
     * 创建圆的信息
     *
     * @param width
     * @param height
     * @return list<CircleInfo> 返回list存放circle
     */
    private List<CircleInfo> createCircleInfo(int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }

        int mainCircleCenterX = width / 2;
        int mainCircleCenterY = height / 3;
        int mainCircleRadius = mMaxRadius;

        List<CircleInfo> circles = new ArrayList<>();


        CircleInfo circleInfo = new CircleInfo(mainCircleCenterX, mainCircleCenterY, mainCircleRadius,
                CIRCLE_COLOR[0]);
        circles.add(circleInfo);

        circleInfo = new CircleInfo(mainCircleCenterX + mainCircleRadius, mainCircleCenterY + mainCircleRadius,
                mainCircleRadius * (3 / 4f), CIRCLE_COLOR[1 % CIRCLE_COLOR.length]);
        circles.add(circleInfo);

        circleInfo = new CircleInfo(mainCircleCenterX - mainCircleRadius * (5 / 4f), mainCircleCenterY +
                mainCircleRadius * (2 / 4f), mainCircleRadius * (3 / 5f), CIRCLE_COLOR[2 % CIRCLE_COLOR.length]);
        circles.add(circleInfo);

        circleInfo = new CircleInfo(mainCircleCenterX + mainCircleRadius * (6 / 4f), mainCircleCenterY,
                mainCircleRadius * (3 / 5f), CIRCLE_COLOR[3 % CIRCLE_COLOR.length]);
        circles.add(circleInfo);

        circleInfo = new CircleInfo(mainCircleCenterX, mainCircleCenterY + mainCircleRadius * (6 / 4f),
                mainCircleRadius * (3 / 5f), CIRCLE_COLOR[4 % CIRCLE_COLOR.length]);
        circles.add(circleInfo);

        circleInfo = new CircleInfo(mainCircleCenterX - mainCircleRadius * (5 / 4f), mainCircleCenterY +
                mainCircleRadius * (6 / 4f), mainCircleRadius * (1 / 5f), CIRCLE_COLOR[5 % CIRCLE_COLOR.length]);
        circles.add(circleInfo);

        circleInfo = new CircleInfo(mainCircleCenterX - mainCircleRadius * (5 / 4f), mainCircleCenterY -
                mainCircleRadius * (2 / 3f), mainCircleRadius * (1 / 5f), CIRCLE_COLOR[6 % CIRCLE_COLOR.length]);
        circles.add(circleInfo);

        circleInfo = new CircleInfo(mainCircleCenterX + mainCircleRadius * (8 / 4f), mainCircleCenterY +
                mainCircleRadius * (5 / 4f), mainCircleRadius * (1 / 5f), CIRCLE_COLOR[7 % CIRCLE_COLOR.length]);
        circles.add(circleInfo);

        return circles;
    }

    /**
     * 画文字,根据圆的大小调整文字的大小,固定分两行，第一行是地区，第二行是人数
     *
     * @param cx
     * @param cy
     * @param radius
     * @param canvas
     * @param paint
     * @param company
     * @param people
     */
    private void drawText(float cx, float cy, float radius, Canvas canvas, Paint paint, String company, String people) {
        if (cy < 0 || cy < 0 || radius <= 0 || paint == null || canvas == null || TextUtils.isEmpty(company)
                || TextUtils.isEmpty(people)) {
            return;
        }

        //设置textSize为半径的1/4
        int textSize = (int) radius / 3;
        if (textSize < 16) {
            //圆太小，直接返回.
            return;
        }
        paint.setTextSize(textSize);

        //设置text的最大行宽为3/4直径
        float maxWidth = radius * (3 / 2f);

        //取得字体的高度
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float height = fontMetrics.descent - fontMetrics.ascent;

        //取得第一行的字数
        int count = paint.breakText(company, true, maxWidth, null);

        if (count > 0 && count < company.length()) {
            //公司名字过长的情况
            String subStr = company.substring(0, count - 1) + "...";
            int textWidth = measureTextWidth(paint, subStr);
            canvas.drawText(subStr, cx - textWidth / 2, cy, paint);
        } else if (count >= company.length()) {
            int textWidth = measureTextWidth(paint, company);
            canvas.drawText(company, cx - textWidth / 2, cy, paint);
        }

        //取得第二行的字数
        count = paint.breakText(people, true, maxWidth, null);

        if (count > 0 && count < people.length()) {
            //人数字过长的情况
            String subStr = people.substring(0, count - 1) + "...";
            int textWidth = measureTextWidth(paint, subStr);
            canvas.drawText(subStr, cx - textWidth / 2, cy + height, paint);
        } else if (count >= people.length()) {
            int textWidth = measureTextWidth(paint, people);
            canvas.drawText(people, cx - textWidth / 2, cy + height, paint);
        }

    }

    /**
     * 取得文字的行宽度
     *
     * @param paint
     * @param str
     * @return
     */
    private int measureTextWidth(Paint paint, String str) {
        if (paint == null || TextUtils.isEmpty(str)) {
            return 0;
        }

        int iRet = 0;
        int len = str.length();
        float[] widths = new float[len];
        paint.getTextWidths(str, widths);
        for (int j = 0; j < len; j++) {
            iRet += (int) Math.ceil(widths[j]);
        }
        return iRet;
    }

    /**
     * Circle坐标等信息
     */
    private class CircleInfo {
        private float cx;
        private float cy;
        private float radius;
        private int color;
        private String text;

        public CircleInfo(float cx, float cy, float radius, int color) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.color = color;
        }

        public CircleInfo(float cx, float cy, float radius, int color, String text) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.color = color;
            this.text = text;
        }

        public float getCx() {
            return cx;
        }

        public void setCx(float cx) {
            this.cx = cx;
        }

        public float getCy() {
            return cy;
        }

        public void setCy(float cy) {
            this.cy = cy;
        }

        public void setCenter(float cx, float cy) {
            this.cx = cx;
            this.cy = cy;
        }

        public float getRadius() {
            return radius;
        }

        public void setRadius(float radius) {
            this.radius = radius;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        protected CircleInfo clone() throws CloneNotSupportedException {
            CircleInfo circle = new CircleInfo(cx, cy, radius, color, text);
            return circle;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }
}

