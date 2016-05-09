package blassoft.hitappme.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {

    private OnBottomReachedListener mListener;

    public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScrollView(Context context) {
        super(context);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        View view = getChildAt(getChildCount()-1);
        int diff = (view.getBottom()-(getHeight()+getScrollY()));

        if (diff == 0 && mListener != null) {
            mListener.onBottomReached();
        }

        super.onScrollChanged(l, t, oldl, oldt);
    }

    /*@Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mPrevY = MotionEvent.obtain(ev).getY();
            return true;
        }

        if ((ev.getAction() == MotionEvent.ACTION_UP) || (ev.getAction() == MotionEvent.ACTION_CANCEL) ) {
            float currentY = MotionEvent.obtain(ev).getY();
            if (((Math.abs(mPrevY - currentY)) > 10) && mListener != null) {
                mListener.onBottomReached();
            }
        }

        if (ev.getAction() == MotionEvent.ACTION_SCROLL) {
            return super.onTouchEvent(ev);
        }

        return false;
    }*/

    // Getters & Setters

    public OnBottomReachedListener getOnBottomReachedListener() {
        return mListener;
    }

    public void setOnBottomReachedListener(OnBottomReachedListener onBottomReachedListener) {
        mListener = onBottomReachedListener;
    }


    /**
     * Event listener.
     */
    public interface OnBottomReachedListener{
        void onBottomReached();
    }

}

