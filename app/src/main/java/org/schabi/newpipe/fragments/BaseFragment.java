package org.schabi.newpipe.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import org.schabi.newpipe.MainActivity;
import org.schabi.newpipe.R;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseFragment extends Fragment {
    protected final String TAG = "BaseFragment@" + Integer.toHexString(hashCode());
    protected static final boolean DEBUG = MainActivity.DEBUG;

    protected AppCompatActivity activity;
    protected OnItemSelectedListener onItemSelectedListener;

    protected AtomicBoolean isLoading = new AtomicBoolean(false);
    protected AtomicBoolean wasLoading = new AtomicBoolean(false);

    protected static final ImageLoader imageLoader = ImageLoader.getInstance();
    protected static final DisplayImageOptions displayImageOptions =
            new DisplayImageOptions.Builder().displayer(new FadeInBitmapDisplayer(400)).cacheInMemory(false).build();

    /*//////////////////////////////////////////////////////////////////////////
    // Views
    //////////////////////////////////////////////////////////////////////////*/

    protected Toolbar toolbar;

    protected View errorPanel;
    protected Button errorButtonRetry;
    protected TextView errorTextView;
    protected ProgressBar loadingProgressBar;
    //protected SwipeRefreshLayout swipeRefreshLayout;

    /*//////////////////////////////////////////////////////////////////////////
    // Fragment's Lifecycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (DEBUG) Log.d(TAG, "onAttach() called with: context = [" + context + "]");

        activity = (AppCompatActivity) context;
        onItemSelectedListener = (OnItemSelectedListener) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");

        isLoading.set(false);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "onViewCreated() called with: rootView = [" + rootView + "], savedInstanceState = [" + savedInstanceState + "]");
        initViews(rootView, savedInstanceState);
        initListeners();
        wasLoading.set(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG) Log.d(TAG, "onDestroyView() called");
        toolbar = null;

        errorPanel = null;
        errorButtonRetry = null;
        errorTextView = null;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init
    //////////////////////////////////////////////////////////////////////////*/

    protected void initViews(View rootView, Bundle savedInstanceState) {
        toolbar = (Toolbar) activity.findViewById(R.id.toolbar);

        loadingProgressBar = (ProgressBar) rootView.findViewById(R.id.loading_progress_bar);
        //swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh);

        errorPanel = rootView.findViewById(R.id.error_panel);
        errorButtonRetry = (Button) rootView.findViewById(R.id.error_button_retry);
        errorTextView = (TextView) rootView.findViewById(R.id.error_message_view);
    }

    protected void initListeners() {
        errorButtonRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRetryButtonClicked();
            }
        });
    }

    protected abstract void reloadContent();

    protected void onRetryButtonClicked() {
        if (DEBUG) Log.d(TAG, "onRetryButtonClicked() called");
        reloadContent();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    public void animateView(final View view, final boolean enterOrExit, long duration) {
        animateView(view, enterOrExit, duration, 0, null);
    }

    public void animateView(final View view, final boolean enterOrExit, long duration, final Runnable execOnEnd) {
        animateView(view, enterOrExit, duration, 0, execOnEnd);
    }

    public void animateView(final View view, final boolean enterOrExit, long duration, long delay) {
        animateView(view, enterOrExit, duration, delay, null);
    }

    /**
     * Animate the view
     *
     * @param view        view that will be animated
     * @param enterOrExit true to enter, false to exit
     * @param duration    how long the animation will take, in milliseconds
     * @param delay       how long the animation will take to start, in milliseconds
     * @param execOnEnd   runnable that will be executed when the animation ends
     */
    public void animateView(final View view, final boolean enterOrExit, long duration, long delay, final Runnable execOnEnd) {
        if (DEBUG) Log.d(TAG, "animateView() called with: view = [" + view + "], enterOrExit = [" + enterOrExit + "], duration = [" + duration + "], execOnEnd = [" + execOnEnd + "]");
        if (view == null) return;

        if (view.getVisibility() == View.VISIBLE && enterOrExit) {
            view.animate().setListener(null).cancel();
            view.setVisibility(View.VISIBLE);
            view.setAlpha(1f);
            if (execOnEnd != null) execOnEnd.run();
            return;
        } else if ((view.getVisibility() == View.GONE || view.getVisibility() == View.INVISIBLE) && !enterOrExit) {
            view.animate().setListener(null).cancel();
            view.setVisibility(View.GONE);
            view.setAlpha(0f);
            if (execOnEnd != null) execOnEnd.run();
            return;
        }

        view.animate().setListener(null).cancel();
        view.setVisibility(View.VISIBLE);

        if (enterOrExit) {
            view.animate().alpha(1f).setDuration(duration).setStartDelay(delay)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (execOnEnd != null) execOnEnd.run();
                        }
                    }).start();
        } else {
            view.animate().alpha(0f)
                    .setDuration(duration).setStartDelay(delay)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            view.setVisibility(View.GONE);
                            if (execOnEnd != null) execOnEnd.run();
                        }
                    })
                    .start();
        }
    }

    protected void setErrorMessage(String message, boolean showRetryButton) {
        if (errorTextView == null || activity == null) return;

        errorTextView.setText(message);
        if (showRetryButton) animateView(errorButtonRetry, true, 300);
        else animateView(errorButtonRetry, false, 0);

        animateView(errorPanel, true, 300);
        isLoading.set(false);

        animateView(loadingProgressBar, false, 200);
    }

    protected int getResourceIdFromAttr(@AttrRes int attr) {
        TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]{attr});
        int attributeResourceId = a.getResourceId(0, 0);
        a.recycle();
        return attributeResourceId;
    }

    public static void showMenuTooltip(View v, String message) {
        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        v.getLocationOnScreen(screenPos);
        v.getWindowVisibleDisplayFrame(displayFrame);

        final Context context = v.getContext();
        final int width = v.getWidth();
        final int height = v.getHeight();
        final int midy = screenPos[1] + height / 2;
        int referenceX = screenPos[0] + width / 2;
        if (ViewCompat.getLayoutDirection(v) == View.LAYOUT_DIRECTION_LTR) {
            final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            referenceX = screenWidth - referenceX; // mirror
        }
        Toast cheatSheet = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        if (midy < displayFrame.height()) {
            // Show along the top; follow action buttons
            cheatSheet.setGravity(Gravity.TOP | Gravity.END, referenceX,
                    screenPos[1] + height - displayFrame.top);
        } else {
            // Show along the bottom center
            cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
        }
        cheatSheet.show();
    }
}
