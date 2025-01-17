/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.picker.widget;

import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.picker.util.SeslDatePickerFontUtil.getBoldFontTypeface;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.OverScroller;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.util.SeslMisc;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.picker.R;
import androidx.picker.util.SeslAnimationListener;
import androidx.picker.widget.SeslNumberPicker.OnScrollListener;
import androidx.reflect.content.res.SeslCompatibilityInfoReflector;
import androidx.reflect.content.res.SeslConfigurationReflector;
import androidx.reflect.graphics.SeslPaintReflector;
import androidx.reflect.media.SeslAudioManagerReflector;
import androidx.reflect.media.SeslSemSoundAssistantManagerReflector;
import androidx.reflect.view.SeslHapticFeedbackConstantsReflector;
import androidx.reflect.view.SeslViewReflector;
import androidx.reflect.widget.SeslHoverPopupWindowReflector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/*
 * Original code by Samsung, all rights reserved to the original author.
 */
@RestrictTo(LIBRARY)
class SeslNumberPickerSpinnerDelegate extends SeslNumberPicker.AbsNumberPickerDelegate {

    static final char[] DIGIT_CHARACTERS = {
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            1632, 1633, 1634, 1635, 1636, 1637, 1638, 1639, 1640, 1641,
            1776, 1777, 1778, 1779, 1780, 1781, 1782, 1783, 1784, 1785,
            2406, 2407, 2408, 2409, 2410, 2411, 2412, 2413, 2414, 2415,
            2534, 2535, 2536, 2537, 2538, 2539, 2540, 2541, 2542, 2543,
            3302, 3303, 3304, 3305, 3306, 3307, 3308, 3309, 3310, 3311,
            4160, 4161, 4162, 4163, 4164, 4165, 4166, 4167, 4168, 4169
    };

    private static final int DEFAULT_WHEEL_INTERVAL = 1;

    private static final int DECREASE_BUTTON = 1;
    private static final int INCREASE_BUTTON = 3;

    private static final int INPUT = 2;
    private static final String INPUT_TYPE_MONTH = "inputType=month_edittext";
    private static final String INPUT_TYPE_YEAR_DATE_TIME = "inputType=YearDateTime_edittext";

    private static final int INTERNAL_INDEX_OFFSET = 50024;

    private static final int PICKER_VIBRATE_INDEX = 32;
    private static final int SAMSUNG_VIBRATION_START = 50056;

    private static final int SELECTOR_ADJUSTMENT_DURATION_MILLIS = 300;
    private static final int SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 4;
    private static final int SELECTOR_MIDDLE_ITEM_INDEX = 2;
    private static final int SELECTOR_WHEEL_ITEM_COUNT = 5;

    private static final int SIZE_UNSPECIFIED = -1;

    private static final int SNAP_SCROLL_DURATION = 500;
    private static final int START_ANIMATION_SCROLL_DURATION = 857;
    private static final int START_ANIMATION_SCROLL_DURATION_2016B = 557;

    private static final int UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT = 2;

    private final float FAST_SCROLL_VELOCITY_START = 1000.0f;

    AccessibilityManager mAccessibilityManager;
    private AccessibilityNodeProviderImpl mAccessibilityNodeProvider;
    final Scroller mAdjustScroller;
    SeslAnimationListener mAnimationListener;
    private final AudioManager mAudioManager;
    private BeginSoftInputOnLongPressCommand mBeginSoftInputOnLongPressCommand;
    private final ValueAnimator mColorInAnimator;
    private final ValueAnimator mColorOutAnimator;
    final Scroller mCustomScroller;
    private final Typeface mDefaultTypeface;
    String[] mDisplayedValues;
    private final ValueAnimator mFadeInAnimator;
    private final ValueAnimator mFadeOutAnimator;
    Scroller mFlingScroller;
    private SeslNumberPicker.Formatter mFormatter;
    OverScroller mGravityScroller;
    private final HapticPreDrawListener mHapticPreDrawListener;
    private Typeface mHcfFocusedTypefaceBold;
    private final FloatValueHolder mHolder;
    final EditText mInputText;
    private final Typeface mLegacyTypeface;
    final Scroller mLinearScroller;
    private SeslNumberPicker.OnEditTextModeChangedListener mOnEditTextModeChangedListener;
    private SeslNumberPicker.OnScrollListener mOnScrollListener;
    private SeslNumberPicker.OnValueChangeListener mOnValueChangeListener;
    String mPickerContentDescription;
    private Typeface mPickerSubTypeface;
    private Typeface mPickerTypeface;
    private final PressedStateHelper mPressedStateHelper;
    private final Paint mSelectorWheelPaint;
    private final SpringAnimation mSpringAnimation;
    private SwitchIntervalOnLongPressCommand mSwitchIntervalOnLongPressCommand;
    Toast mToast;
    private String mToastText;
    private String mUnitValue;
    private VelocityTracker mVelocityTracker;
    private final Drawable mVirtualButtonFocusedDrawable;
    private final SparseArray<String> mSelectorIndexToStringCache = new SparseArray<>();

    int mBottomSelectionDividerBottom;
    private int mCurrentScrollOffset;
    int mLastFocusedChildVirtualViewId;
    int mLastHoveredChildVirtualViewId;
    private final int mMaxHeight;
    int mMaxValue;
    private int mMaxWidth;
    private final int mMaximumFlingVelocity;
    private final int mMinHeight;
    int mMinValue;
    private final int mMinWidth;
    private final int mMinimumFlingVelocity;
    private int mModifiedTxtHeight;
    private final int mPickerSoundFastIndex;
    private int mPickerSoundIndex;
    private int mPickerSoundSlowIndex;
    private int mPickerVibrateIndex;
    int mPreviousScrollerY;
    final int mSelectionDividerHeight;
    int mSelectorElementHeight;
    private int mSelectorTextGapHeight;
    int mTextColor;
    private final int mTextColorIdle;
    private final int mTextColorScrolling;
    private int mTextSize;
    int mTopSelectionDividerTop;
    private int mTouchSlop;
    int mValue;
    private int mValueChangeOffset;
    int mWheelInterval = DEFAULT_WHEEL_INTERVAL;
    private final int[] mSelectorIndices = new int[SELECTOR_WHEEL_ITEM_COUNT];
    private int mInitialScrollOffset = Integer.MIN_VALUE;
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    private int mShortFlickThreshold = 1700;
    private int selectedPickerColor;

    private float mActivatedAlpha = 0.4f;
    private float mAlpha = 0.1f;
    float mCurVelocity;
    private final float mHeightRatio;
    private float mIdleAlpha = 0.1f;
    private float mInitialAlpha = 1.0f;
    private float mLastDownEventY;
    private float mLastDownOrMoveEventY;
    float mPreviousSpringY;

    private final boolean mComputeMaxWidth;
    boolean mDecrementVirtualButtonPressed;
    boolean mIgnoreMoveEvents;
    boolean mIgnoreUpEvent;
    boolean mIncrementVirtualButtonPressed;
    boolean mIsAmPm;
    private boolean mIsBoldTextEnabled;
    boolean mIsEditTextMode;
    private boolean mIsHcfEnabled;
    private boolean mPerformClickOnTap;
    boolean mSpringFlingRunning;
    boolean mWrapSelectorWheel;
    boolean mCustomWheelIntervalMode = false;
    private boolean mIsPressedBackKey = false;
    private boolean mWrapSelectorWheelPreferred = true;
    boolean mIsEditTextModeEnabled = true;
    private boolean mIsLongClicked = false;
    boolean mIsStartingAnimation = false;
    boolean mReservedStartAnimation = false;
    private boolean mIsLongPressed = false;
    private boolean mCustomTypefaceSet = false;
    private boolean mIsValueChanged = false;

    private long mLastDownEventTime;

    private final PathInterpolator ALPHA_PATH_INTERPOLATOR
            = new PathInterpolator(0.17f, 0.17f, 0.83f, 0.83f);

    private final PathInterpolator SIZE_PATH_INTERPOLATOR
            = new PathInterpolator(0.5f, 0.0f, 0.4f, 1.0f);

    private ValueAnimator.AnimatorUpdateListener mUpdateListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mAlpha = (float) animation.getAnimatedValue();
            mDelegator.invalidate();
        }
    };

    private ValueAnimator.AnimatorUpdateListener mColorUpdateListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mTextColor = (Integer) animation.getAnimatedValue();
            mDelegator.invalidate();
        }
    };

    private DynamicAnimation.OnAnimationUpdateListener mSpringAnimationUpdateListener
            = new DynamicAnimation.OnAnimationUpdateListener() {
        @Override
        public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
            if (!(velocity > 0.0f)) {
                velocity = -velocity;
            }

            mCurVelocity = velocity;

            final float y = value - mPreviousSpringY;
            if (!mSpringFlingRunning && Math.round(y) == 0) {
                animation.cancel();
                if (!ensureScrollWheelAdjusted()) {
                    updateInputTextView();
                }
            } else {
                if (Math.round(y) == 0) {
                    mSpringFlingRunning = false;
                }
                scrollBy(0, Math.round(y));
                mPreviousSpringY = value;
                mDelegator.invalidate();
            }
        }
    };

    private DynamicAnimation.OnAnimationEndListener mSpringAnimationEndListener
            = new DynamicAnimation.OnAnimationEndListener() {
        @Override
        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value,
                                   float velocity) {
            mSpringFlingRunning = false;
            mGravityScroller.forceFinished(true);
            startFadeAnimation(true);
        }
    };


    @SuppressLint("ClickableViewAccessibility")
    public SeslNumberPickerSpinnerDelegate(SeslNumberPicker numberPicker, Context context,
                                           AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(numberPicker, context);

        final Resources resources = mContext.getResources();

        final int defaultHeight = resources
                .getDimensionPixelSize(R.dimen.sesl_number_picker_spinner_height);
        final int defaultWidth = resources
                .getDimensionPixelSize(R.dimen.sesl_number_picker_spinner_width);
        final float defaultEditTextHeight = resources
                .getDimension(R.dimen.sesl_number_picker_spinner_edit_text_height);

        mHeightRatio = defaultEditTextHeight / defaultHeight;

        TypedArray attributesArray = context
                .obtainStyledAttributes(attrs, R.styleable.NumberPicker, defStyleAttr, defStyleRes);
        mMinHeight = attributesArray.getDimensionPixelSize(R.styleable.NumberPicker_internalMinHeight,
                SIZE_UNSPECIFIED);
        mMaxHeight = attributesArray.getDimensionPixelSize(R.styleable.NumberPicker_internalMaxHeight,
                defaultHeight);
        mMinWidth = attributesArray.getDimensionPixelSize(R.styleable.NumberPicker_internalMinWidth,
                defaultWidth);
        mMaxWidth = attributesArray.getDimensionPixelSize(R.styleable.NumberPicker_internalMaxWidth,
                SIZE_UNSPECIFIED);
        attributesArray.recycle();

        if (mMinHeight != SIZE_UNSPECIFIED && mMaxHeight != SIZE_UNSPECIFIED
                && mMinHeight > mMaxHeight) {
            throw new IllegalArgumentException("minHeight > maxHeight");
        }
        if (mMinWidth != SIZE_UNSPECIFIED && mMaxWidth != SIZE_UNSPECIFIED
                && mMinWidth > mMaxWidth) {
            throw new IllegalArgumentException("minWidth > maxWidth");
        }

        mSelectionDividerHeight = (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT, resources.getDisplayMetrics());
        mComputeMaxWidth = mMaxWidth == SIZE_UNSPECIFIED;

        if (!SeslMisc.isLightTheme(mContext)) {
            mIdleAlpha = 0.2f;
            mAlpha = 0.2f;
        }

        mPressedStateHelper = new PressedStateHelper();
        mDelegator.setWillNotDraw(false);

        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sesl_number_picker_spinner, mDelegator, true);

        mInputText = mDelegator.findViewById(R.id.numberpicker_input);
        mInputText.setLongClickable(false);
        mInputText.setIncludeFontPadding(false);
        mInputText.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                    mInputText.selectAll();
                    showSoftInput();
                }
                return super.performAccessibilityAction(host, action, args);
            }
        });

        mDefaultTypeface = Typeface.defaultFromStyle(Typeface.BOLD);
        mLegacyTypeface = Typeface.create("sec-roboto-condensed-light", Typeface.BOLD);
        mPickerTypeface = getBoldFontTypeface();

        if (mDefaultTypeface.equals(mPickerTypeface)) {
            if (!mLegacyTypeface.equals(mPickerTypeface)) {
                mPickerTypeface = mLegacyTypeface;
            } else {
                mPickerTypeface = Typeface.create("sans-serif-thin", Typeface.BOLD);
            }
        }
        mPickerSubTypeface = Typeface.create(mPickerTypeface, Typeface.NORMAL);

        final boolean isDexMode = SeslConfigurationReflector
                .isDexEnabled(resources.getConfiguration());
        if (!isDexMode) {
            final String themeTypeFace = Settings.System.getString(mContext.getContentResolver(),
                    "theme_font_clock");
            if (themeTypeFace != null && !themeTypeFace.isEmpty()) {
                mPickerTypeface = getFontTypeface(themeTypeFace);
                mPickerSubTypeface = Typeface.create(mPickerTypeface, Typeface.NORMAL);
            }
        } else {
            mIdleAlpha = 0.2f;
            mAlpha = 0.2f;
        }

        if (isCharacterNumberLanguage()) {
            mInputText.setIncludeFontPadding(true);
            mPickerTypeface = mDefaultTypeface;
            mPickerSubTypeface = Typeface.create(mDefaultTypeface, Typeface.NORMAL);
        }

        mIsHcfEnabled = isHighContrastFontEnabled();
        mHcfFocusedTypefaceBold = Typeface.create(mPickerTypeface, Typeface.BOLD);
        setInputTextTypeface();

        mTextColorScrolling = ResourcesCompat.getColor(resources,
                R.color.sesl_number_picker_text_color_scroll, context.getTheme());

        final ColorStateList colors = mInputText.getTextColors();
        final int[] enabledStateSet = mDelegator.getEnableStateSet();

        if (Build.VERSION.SDK_INT > 29) {
            mTextColorIdle = colors.getColorForState(enabledStateSet, Color.WHITE);
            selectedPickerColor = ResourcesCompat
                    .getColor(resources, R.color.sesl_number_picker_text_highlight_color, context.getTheme());
        } else {
            mTextColorIdle = ResourcesCompat
                    .getColor(resources, R.color.sesl_number_picker_text_color_scroll, context.getTheme());
            selectedPickerColor = (mTextColorScrolling & 0xffffff) | 0x33000000;
        }
        mTextColor = mTextColorIdle;

        mVirtualButtonFocusedDrawable = new ColorDrawable(selectedPickerColor);

        mInputText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
               if (hasFocus) {
                    setEditTextMode(true);
                    mInputText.selectAll();
                } else {
                    mInputText.setSelection(0, 0);
                    validateInputTextView(v);
                }
            }
        });
        mInputText.setOnTouchListener((v, event) -> {
            if (v instanceof EditText && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                ((EditText) v).selectAll();
                showSoftInput();
                return true;
            } else {
                return false;
            }
        });

        mInputText.setFilters(new InputFilter[]{new InputTextFilter()});
        mInputText.setRawInputType(EditorInfo.TYPE_CLASS_NUMBER);
        mInputText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_DONE);
        mInputText.setCursorVisible(false);
        mInputText.setHighlightColor(selectedPickerColor);
        if (Build.VERSION.SDK_INT <= 29) {
            mInputText.setTextColor(mTextColorScrolling);
        }
        SeslViewReflector.semSetHoverPopupType(mInputText,
                SeslHoverPopupWindowReflector.getField_TYPE_NONE());

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity() * 2;
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity() / 4;
        mTextSize = (int) mInputText.getTextSize();

        mSelectorWheelPaint = new Paint();
        mSelectorWheelPaint.setAntiAlias(true);
        mSelectorWheelPaint.setTextAlign(Paint.Align.CENTER);
        mSelectorWheelPaint.setTextSize((float) mTextSize);
        mSelectorWheelPaint.setTypeface(mPickerTypeface);
        mSelectorWheelPaint.setColor(mTextColor);
        mInitialAlpha = mSelectorWheelPaint.getAlpha() / 255.0f;
        if (updateBoldTextEnabledInSettings()) {
            mSelectorWheelPaint.setFakeBoldText(true);
        }

        mCustomScroller = new Scroller(mContext, SIZE_PATH_INTERPOLATOR, true);
        mLinearScroller = new Scroller(mContext, null, true);
        mFlingScroller = new Scroller(mContext, null, true);
        mAdjustScroller = new Scroller(mContext,
                new PathInterpolator(0.4f, 0.0f, 0.3f, 1.0f));
        mGravityScroller = new OverScroller(mContext, new DecelerateInterpolator());

        mHolder = new FloatValueHolder();
        mSpringAnimation = new SpringAnimation(mHolder);
        mSpringAnimation.setSpring(new SpringForce());
        mSpringAnimation.setMinimumVisibleChange(1.0f);
        mSpringAnimation.addUpdateListener(mSpringAnimationUpdateListener);
        mSpringAnimation.addEndListener(mSpringAnimationEndListener);
        mSpringAnimation.getSpring().setStiffness(7.0f);
        mSpringAnimation.getSpring().setDampingRatio(0.99f);

        setFormatter(SeslNumberPicker.getTwoDigitFormatter());
        updateInputTextView();
        mDelegator.setVerticalScrollBarEnabled(false);
        if (mDelegator.getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            mDelegator.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mHapticPreDrawListener = new HapticPreDrawListener();
        mPickerVibrateIndex = SeslHapticFeedbackConstantsReflector.semGetVibrationIndex(PICKER_VIBRATE_INDEX);
        mPickerSoundIndex = SeslAudioManagerReflector.getField_SOUND_TIME_PICKER_SCROLL();
        mPickerSoundFastIndex = SeslAudioManagerReflector.getField_SOUND_TIME_PICKER_SCROLL_FAST();
        mPickerSoundSlowIndex = SeslAudioManagerReflector.getField_SOUND_TIME_PICKER_SCROLL_SLOW();
        SeslSemSoundAssistantManagerReflector.setFastAudioOpenMode(mContext, true);

        mDelegator.setFocusableInTouchMode(false);
        mDelegator.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        if (Build.VERSION.SDK_INT >= 26) {
            mDelegator.setDefaultFocusHighlightEnabled(false);
        }

        mPickerContentDescription = "";
        mToastText = resources.getString(R.string.sesl_number_picker_invalid_value_entered);
        mUnitValue = "";

        SeslViewReflector.semSetDirectPenInputEnabled(mInputText, false);

        mAccessibilityManager
                = (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);

        mFadeOutAnimator = ValueAnimator.ofFloat(mActivatedAlpha, mIdleAlpha);
        mFadeOutAnimator.setInterpolator(ALPHA_PATH_INTERPOLATOR);
        mFadeOutAnimator.setDuration(200);
        mFadeOutAnimator.setStartDelay(100);
        mFadeOutAnimator.addUpdateListener(mUpdateListener);
        mFadeInAnimator = ValueAnimator.ofFloat(mIdleAlpha, mActivatedAlpha);
        mFadeInAnimator.setInterpolator(ALPHA_PATH_INTERPOLATOR);
        mFadeInAnimator.setDuration(200);
        mFadeInAnimator.addUpdateListener(mUpdateListener);

        mColorInAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                mTextColorIdle, mTextColorScrolling);
        mColorInAnimator.setInterpolator(ALPHA_PATH_INTERPOLATOR);
        mColorInAnimator.setDuration(200);
        mColorInAnimator.addUpdateListener(mColorUpdateListener);
        mColorOutAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                mTextColorScrolling, mTextColorIdle);
        mColorOutAnimator.setInterpolator(ALPHA_PATH_INTERPOLATOR);
        mColorOutAnimator.setDuration(200);
        mColorOutAnimator.setStartDelay(100);
        mColorOutAnimator.addUpdateListener(mColorUpdateListener);
    }

    @Override
    public void setDateUnit(int unit) {
        if (unit != SeslNumberPicker.MODE_UNIT_NONE) {
            switch (unit) {
                case SeslNumberPicker.MODE_UNIT_DAY:
                    mUnitValue = mContext.getResources().getString(R.string.sesl_date_picker_day);
                    break;
                case SeslNumberPicker.MODE_UNIT_MONTH:
                    mUnitValue = mContext.getResources().getString(R.string.sesl_date_picker_month);
                    break;
                case SeslNumberPicker.MODE_UNIT_YEAR:
                    mUnitValue = mContext.getResources().getString(R.string.sesl_date_picker_year);
                    break;
            }
        } else {
            mUnitValue = "";
        }
    }

    @Override
    public void setPickerContentDescription(String name) {
        mPickerContentDescription = name;
        ((SeslNumberPicker.CustomEditText) mInputText).setPickerContentDescription(name);
    }

    @Override
    public void setImeOptions(int imeOptions) {
        mInputText.setImeOptions(imeOptions);
    }

    @Override
    public void setAmPm() {
        mIsAmPm = true;
        mTextSize = mContext.getResources()
                .getDimensionPixelSize(R.dimen.sesl_time_picker_spinner_am_pm_text_size);
        mSelectorWheelPaint.setTextSize((float) mTextSize);
        mInputText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) mTextSize);
        setEditTextModeEnabled(false);
    }

    @Override
    public void setEditTextModeEnabled(boolean enabled) {
        if (mIsEditTextModeEnabled != enabled && !enabled) {
            if (mIsEditTextMode) {
                setEditTextMode(false);
            }
            mInputText.setAccessibilityDelegate(null);
            mIsEditTextModeEnabled = enabled;
        }
    }

    @Override
    public boolean isEditTextModeEnabled() {
        return mIsEditTextModeEnabled;
    }

    @Override
    public void setEditTextMode(boolean isEditTextMode) {
        if (mIsEditTextModeEnabled && mIsEditTextMode != isEditTextMode) {
            mIsEditTextMode = isEditTextMode;

            if (isEditTextMode) {
                tryComputeMaxWidth();
                removeAllCallbacks();

                if (!mIsStartingAnimation) {
                    mCurrentScrollOffset = mInitialScrollOffset;
                    mFlingScroller.abortAnimation();
                    mGravityScroller.abortAnimation();
                    mSpringFlingRunning = false;
                    mSpringAnimation.cancel();
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                }

                mDelegator.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                updateInputTextView();
                mInputText.setVisibility(View.VISIBLE);

                if (mAccessibilityManager.isEnabled()) {
                    final AccessibilityNodeProviderImpl provider
                            = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
                    if (provider != null) {
                        provider.performAction(INPUT, 128, null);
                    }
                }
            } else {
                if (mWheelInterval != DEFAULT_WHEEL_INTERVAL && mCustomWheelIntervalMode
                        && mValue % mWheelInterval != 0) {
                    applyWheelCustomInterval(false);
                }

                if (mFadeOutAnimator.isRunning()) {
                    mFadeOutAnimator.cancel();
                }
                if (mFadeInAnimator.isRunning()) {
                    mFadeInAnimator.cancel();
                }
                if (mColorInAnimator.isRunning()) {
                    mColorInAnimator.cancel();
                }
                if (mColorOutAnimator.isRunning()) {
                    mColorOutAnimator.cancel();
                }

                mTextColor = mTextColorIdle;
                mAlpha = mIdleAlpha;
                mInputText.setVisibility(View.INVISIBLE);
                mDelegator.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            }

            mLastFocusedChildVirtualViewId = View.NO_ID;
            mDelegator.invalidate();

            if (mOnEditTextModeChangedListener != null) {
                mOnEditTextModeChangedListener.onEditTextModeChanged(mDelegator, mIsEditTextMode);
            }
        }
    }

    @Override
    public void setCustomIntervalValue(int interval) {
        mWheelInterval = interval;
    }

    @Override
    public boolean setCustomInterval(int interval) {
        final boolean wrapSelectorWheel = mWrapSelectorWheel;
        if (mMinValue % interval == 0
                && (mMaxValue + (wrapSelectorWheel ? 1 : 0)) % interval == 0) {
            setCustomIntervalValue(interval);
            applyWheelCustomInterval(true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void applyWheelCustomInterval(boolean enabled) {
        if (mWheelInterval != DEFAULT_WHEEL_INTERVAL) {
            mCustomWheelIntervalMode = enabled;
            if (enabled) {
                ensureValueAdjusted(mWheelInterval);
            }
            initializeSelectorWheelIndices();
            mDelegator.invalidate();
        }
    }

    private void ensureValueAdjusted(int interval) {
        final int diff = mValue % interval;
        if (diff != 0) {
            int newValue = mValue - diff;
            if (diff > interval / 2) {
                newValue += interval;
            }
            setValueInternal(newValue, true);
        }
    }

    @Override
    public boolean isChangedDefaultInterval() {
        return mWheelInterval != DEFAULT_WHEEL_INTERVAL && !mCustomWheelIntervalMode;
    }

    @Override
    public boolean isEditTextMode() {
        return mIsEditTextMode;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int msrdWdth = mDelegator.getMeasuredWidth();
        final int msrdHght = mDelegator.getMeasuredHeight();
        final int inptTxtMsrdWdth = mInputText.getMeasuredWidth();
        final int inptTxtMsrdHght = Math.max(mInputText.getMeasuredHeight(),
                (int) Math.floor(msrdHght * mHeightRatio));
        mModifiedTxtHeight = inptTxtMsrdHght;

        final int inptTxtLeft = (msrdWdth - inptTxtMsrdWdth) / 2;
        final int inptTxtTop = (msrdHght - inptTxtMsrdHght) / 2;
        final int inptTxtRight = inptTxtLeft + inptTxtMsrdWdth;
        final int inptTxtBottom = inptTxtTop + inptTxtMsrdHght;
        mInputText.layout(inptTxtLeft, inptTxtTop, inptTxtRight, inptTxtBottom);

        if (changed) {
            initializeSelectorWheel();
            if (mModifiedTxtHeight > mSelectorElementHeight) {
                mTopSelectionDividerTop = mValueChangeOffset;
                mBottomSelectionDividerBottom = mValueChangeOffset * 2;
            } else {
                mTopSelectionDividerTop = inptTxtTop;
                mBottomSelectionDividerBottom = inptTxtBottom;
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, mMaxWidth);
        final int newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, mMaxHeight);
        mDelegator.superOnMeasure(newWidthMeasureSpec, newHeightMeasureSpec);
        final int widthSize = resolveSizeAndStateRespectingMinSize(mMinWidth,
                mDelegator.getMeasuredWidth(), widthMeasureSpec);
        final int heightSize = resolveSizeAndStateRespectingMinSize(mMinHeight,
                mDelegator.getMeasuredHeight(), heightMeasureSpec);
        mDelegator.setMeasuredDimensionWrapper(widthSize, heightSize);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (!mIsEditTextModeEnabled) {
            return false;
        } else if ((mInputText.hasFocus() || !mIsEditTextModeEnabled && mDelegator.hasFocus())
                && event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            mIsPressedBackKey = true;
            hideSoftInput();
            setEditTextMode(false);
            return true;
        } else {
            mIsPressedBackKey = false;
            return false;
        }
    }

    boolean moveToFinalScrollerPosition(Scroller scroller) {
        scroller.forceFinished(true);
        int amountToScroll = scroller.getFinalY() - scroller.getCurrY();
        if (mSelectorElementHeight == 0) {
            return false;
        }
        int futureScrollOffset = mCurrentScrollOffset + amountToScroll;
        int overshootAdjustment = mInitialScrollOffset - futureScrollOffset;
        if (overshootAdjustment != 0) {
            overshootAdjustment %= mSelectorElementHeight;
            if (Math.abs(overshootAdjustment) > mSelectorElementHeight / 2) {
                if (overshootAdjustment > 0) {
                    overshootAdjustment -= mSelectorElementHeight;
                } else {
                    overshootAdjustment += mSelectorElementHeight;
                }
            }
            amountToScroll += overshootAdjustment;
            scrollBy(0, amountToScroll);
            return true;
        }
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (hasWindowFocus && mIsEditTextMode && mInputText.isFocused()) {
            showSoftInputForWindowFocused();
        } else if (hasWindowFocus && mIsEditTextMode && !mInputText.isFocused()) {
            InputMethodManager inputMethodManager
                    = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null && inputMethodManager.isActive(mInputText)) {
                inputMethodManager.hideSoftInputFromWindow(mDelegator.getWindowToken(), 0);
            }
        }

        if (!mIsStartingAnimation) {
            if (!mFlingScroller.isFinished()) {
                mFlingScroller.forceFinished(true);
            }
            if (!mAdjustScroller.isFinished()) {
                mAdjustScroller.forceFinished(true);
            }
            if (!mGravityScroller.isFinished()) {
                mGravityScroller.forceFinished(true);
            }
            if (mSpringAnimation.isRunning()) {
                mSpringAnimation.cancel();
                mSpringFlingRunning = false;
            }
            ensureScrollWheelAdjusted();
        }

        mIsHcfEnabled = isHighContrastFontEnabled();
        mSelectorWheelPaint.setTextSize((float) mTextSize);
        mSelectorWheelPaint.setTypeface(mPickerTypeface);
        setInputTextTypeface();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!mDelegator.isEnabled() || mIsEditTextMode || mIsStartingAnimation) {
            return false;
        }

        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                removeAllCallbacks();
                mInputText.setVisibility(View.INVISIBLE);
                mLastDownOrMoveEventY = mLastDownEventY = event.getY();
                mLastDownEventTime = event.getEventTime();
                mIgnoreMoveEvents = false;
                mIgnoreUpEvent = false;
                mPerformClickOnTap = false;
                mIsValueChanged = false;
                if (mLastDownEventY < mTopSelectionDividerTop) {
                    startFadeAnimation(false);
                    if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                        mPressedStateHelper.buttonPressDelayed(PressedStateHelper.BUTTON_DECREMENT);
                    }
                } else if (mLastDownEventY > mBottomSelectionDividerBottom) {
                    startFadeAnimation(false);
                    if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                        mPressedStateHelper.buttonPressDelayed(PressedStateHelper.BUTTON_INCREMENT);
                    }
                }
                mDelegator.getParent().requestDisallowInterceptTouchEvent(true);
                if (!mFlingScroller.isFinished()) {
                    mFlingScroller.forceFinished(true);
                    mAdjustScroller.forceFinished(true);
                    if (mScrollState == OnScrollListener.SCROLL_STATE_FLING) {
                        mFlingScroller.abortAnimation();
                        mAdjustScroller.abortAnimation();
                    }
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                } else if (mSpringAnimation.isRunning()) {
                    mGravityScroller.forceFinished(true);
                    mAdjustScroller.forceFinished(true);
                    mSpringAnimation.cancel();
                    mSpringFlingRunning = false;
                    if (mScrollState == OnScrollListener.SCROLL_STATE_FLING) {
                        mGravityScroller.abortAnimation();
                        mAdjustScroller.abortAnimation();
                    }
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                } else if (!mAdjustScroller.isFinished()) {
                    mFlingScroller.forceFinished(true);
                    mAdjustScroller.forceFinished(true);
                } else if (mLastDownEventY < mTopSelectionDividerTop) {
                    if (mWheelInterval != DEFAULT_WHEEL_INTERVAL) {
                        postSwitchIntervalOnLongPress();
                    }
                } else if (mLastDownEventY > mBottomSelectionDividerBottom) {
                    if (mWheelInterval != DEFAULT_WHEEL_INTERVAL) {
                        postSwitchIntervalOnLongPress();
                    }
                } else {
                    mPerformClickOnTap = true;
                    if (mWheelInterval != DEFAULT_WHEEL_INTERVAL) {
                        postSwitchIntervalOnLongPress();
                    } else {
                        postBeginSoftInputOnLongPressCommand();
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mDelegator.isEnabled() || mIsEditTextMode || mIsStartingAnimation) {
            return false;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_UP: {
                removeBeginSoftInputCommand();
                removeSwitchIntervalOnLongPress();
                if (!mIgnoreUpEvent) {
                    mPressedStateHelper.cancel();
                    VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity();
                    int eventY = (int) event.getY();
                    int deltaMoveY = (int) Math.abs(eventY - mLastDownEventY);
                    if (!mIsEditTextModeEnabled && mIgnoreMoveEvents) {
                        ensureScrollWheelAdjusted();
                        startFadeAnimation(true);
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    } else if (Math.abs(initialVelocity) > mMinimumFlingVelocity
                            && Math.abs(initialVelocity) > mShortFlickThreshold) {
                        if (deltaMoveY > mTouchSlop || !mPerformClickOnTap) {
                            fling(initialVelocity);
                            onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                        } else {
                            mPerformClickOnTap = false;
                            performClick();
                            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                        }
                    } else {
                        final long eventTime = event.getEventTime();
                        if (deltaMoveY <= mTouchSlop) {
                            if (mPerformClickOnTap) {
                                mPerformClickOnTap = false;
                                performClick();
                            } else {
                                if (eventY > mBottomSelectionDividerBottom) {
                                    changeValueByOne(true);
                                    mPressedStateHelper.buttonTapped(PressedStateHelper.BUTTON_INCREMENT);
                                } else if (eventY < mTopSelectionDividerTop) {
                                    changeValueByOne(false);
                                    mPressedStateHelper.buttonTapped(PressedStateHelper.BUTTON_DECREMENT);
                                } else {
                                    ensureScrollWheelAdjusted(deltaMoveY);
                                }
                                startFadeAnimation(true);
                            }
                        } else {
                            if (mIsLongClicked) {
                                showSoftInput();
                                mIsLongClicked = false;
                            }
                            ensureScrollWheelAdjusted(deltaMoveY);
                            startFadeAnimation(true);
                        }
                        mIsValueChanged = false;
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    }
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
            } break;
            case MotionEvent.ACTION_MOVE: {
                if (mIgnoreMoveEvents) {
                    break;
                }
                float currentMoveY = event.getY();
                if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    int deltaDownY = (int) Math.abs(currentMoveY - mLastDownEventY);
                    if (deltaDownY > mTouchSlop) {
                        removeAllCallbacks();
                        startFadeAnimation(false);
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                    }
                } else {
                    int deltaMoveY = (int) ((currentMoveY - mLastDownOrMoveEventY));
                    scrollBy(0, deltaMoveY);
                    mDelegator.invalidate();
                }
                mLastDownOrMoveEventY = currentMoveY;
            } break;
            case MotionEvent.ACTION_CANCEL: {
                ensureScrollWheelAdjusted();
                startFadeAnimation(true);
                onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            } break;
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeAllCallbacks();
                break;
        }
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (!mDelegator.isEnabled() || mIsEditTextMode || mIsStartingAnimation) {
            return false;
        }

        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0
                && event.getAction() == MotionEvent.ACTION_SCROLL) {
            float axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (axisValue != 0.0f) {
                startFadeAnimation(false);
                changeValueByOne(axisValue < 0.0f);
                startFadeAnimation(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        boolean previousBoldText = mIsBoldTextEnabled;
        updateBoldTextEnabledInSettings();
        if (previousBoldText != mIsBoldTextEnabled) {
            mSelectorWheelPaint.setFakeBoldText(mIsBoldTextEnabled);
        }

        if (!mCustomTypefaceSet) {
            if (isCharacterNumberLanguage()) {
                mInputText.setIncludeFontPadding(true);
                mPickerTypeface = mDefaultTypeface;
                mPickerSubTypeface = Typeface.create(mDefaultTypeface, Typeface.NORMAL);
                mHcfFocusedTypefaceBold = Typeface.create(mPickerTypeface, Typeface.BOLD);
                setInputTextTypeface();
            } else {
                mInputText.setIncludeFontPadding(false);
                setInputTextTypeface();
                tryComputeMaxWidth();
            }
        }
    }

    @Override
    public void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (gainFocus) {
            if (mIsEditTextMode) {
                mLastFocusedChildVirtualViewId = View.NO_ID;
                if (mInputText.getVisibility() == View.VISIBLE) {
                    mInputText.requestFocus();
                }
            } else {
                mLastFocusedChildVirtualViewId = 1;
                if (!mWrapSelectorWheel && getValue() == getMinValue()) {
                    mLastFocusedChildVirtualViewId = 2;
                }
            }


            if (mAccessibilityManager.isEnabled()) {
                AccessibilityNodeProviderImpl provider
                        = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
                if (provider != null) {
                    if (mIsEditTextMode) {
                        mLastFocusedChildVirtualViewId = 2;
                    }
                    provider.performAction(mLastFocusedChildVirtualViewId, 64, null);
                }
            }
        } else {
            if (mAccessibilityManager.isEnabled()) {
                AccessibilityNodeProviderImpl provider
                        = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
                if (provider != null) {
                    if (mIsEditTextMode) {
                        mLastFocusedChildVirtualViewId = 2;
                    }
                    provider.performAction(mLastFocusedChildVirtualViewId, 128, null);
                }
            }
            mLastFocusedChildVirtualViewId = View.NO_ID;
            mLastHoveredChildVirtualViewId = Integer.MIN_VALUE;
        }

        mDelegator.invalidate();
    }

    @Override
    public void onWindowVisibilityChanged(int visibility) {
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int action = event.getAction();
        final int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_ENTER && keyCode != KeyEvent.KEYCODE_NUMPAD_ENTER) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (mIsEditTextMode) {
                        return false;
                    }
                    switch (action) {
                        case KeyEvent.ACTION_DOWN:
                            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                if (mLastFocusedChildVirtualViewId == 1) {
                                    mLastFocusedChildVirtualViewId = 2;
                                    mDelegator.invalidate();
                                    return true;
                                } else if (mLastFocusedChildVirtualViewId == 2) {
                                    if (!mWrapSelectorWheel && getValue() == getMaxValue()) {
                                        return false;
                                    }
                                    mLastFocusedChildVirtualViewId = 3;
                                    mDelegator.invalidate();
                                    return true;
                                }
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                if (mLastFocusedChildVirtualViewId != 2) {
                                    if (mLastFocusedChildVirtualViewId == 3) {
                                        mLastFocusedChildVirtualViewId = 2;
                                        mDelegator.invalidate();
                                        return true;
                                    }
                                } else if (!mWrapSelectorWheel && getValue() == getMinValue()) {
                                    return false;
                                } else {
                                    mLastFocusedChildVirtualViewId = 1;
                                    mDelegator.invalidate();
                                    return true;
                                }
                            }
                            return false;
                        case KeyEvent.ACTION_UP:
                            if (mAccessibilityManager.isEnabled()) {
                                AccessibilityNodeProviderImpl provider
                                        = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
                                if (provider != null) {
                                    provider.performAction(mLastFocusedChildVirtualViewId, 64, null);
                                }
                                return true;
                            }
                            return false;
                    }
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            View leftView = mDelegator.focusSearch(View.FOCUS_LEFT);
                            if (leftView != null) {
                                leftView.requestFocus(View.FOCUS_LEFT);
                            }
                            return true;
                        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            View rightView = mDelegator.focusSearch(View.FOCUS_RIGHT);
                            if (rightView != null) {
                                rightView.requestFocus(View.FOCUS_RIGHT);
                            }
                            return true;
                        }
                    }
                    return false;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    break;
                default:
                    return false;
            }
        }

        if (!mIsEditTextMode && action == KeyEvent.ACTION_UP) {
            if (mLastFocusedChildVirtualViewId == 2) {
                if (!mIsEditTextModeEnabled) {
                    return false;
                }
                mInputText.setVisibility(View.VISIBLE);
                mInputText.requestFocus();
                showSoftInput();
                removeAllCallbacks();
                return true;
            } else if (mFlingScroller.isFinished()) {
                if (mLastFocusedChildVirtualViewId == 1) {
                    startFadeAnimation(false);
                    changeValueByOne(false);
                    if (!mWrapSelectorWheel && getValue() == getMinValue() + 1) {
                        mLastFocusedChildVirtualViewId = 2;
                    }
                    startFadeAnimation(true);
                } else if (mLastFocusedChildVirtualViewId == 3) {
                    startFadeAnimation(false);
                    changeValueByOne(true);
                    if (!mWrapSelectorWheel && getValue() == getMaxValue() - 1) {
                        mLastFocusedChildVirtualViewId = 2;
                    }
                    startFadeAnimation(true);
                }
            }
        }
        return false;
    }

    @Override
    public void dispatchTrackballEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeAllCallbacks();
                break;
        }
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        if (mAccessibilityManager.isEnabled()) {
            final int eventY = (int) event.getY();

            int index = 2;
            if (!mIsEditTextMode) {
                if (eventY <= mTopSelectionDividerTop) {
                    index = 1;
                } else if (mBottomSelectionDividerBottom <= eventY) {
                    index = 3;
                }
            }

            final int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_HOVER_MOVE:
                case MotionEvent.ACTION_HOVER_ENTER: {
                    updateHoveredVirtualView(index);
                    return index != Integer.MIN_VALUE;
                }
                case MotionEvent.ACTION_HOVER_EXIT: {
                    if (mLastHoveredChildVirtualViewId != Integer.MIN_VALUE) {
                        updateHoveredVirtualView(Integer.MIN_VALUE);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void updateHoveredVirtualView(int virtualViewId) {
        if (mLastHoveredChildVirtualViewId != virtualViewId) {
            final int previousVirtualViewId = mLastHoveredChildVirtualViewId;
            mLastHoveredChildVirtualViewId = virtualViewId;
            AccessibilityNodeProviderImpl provider
                    = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
            provider.sendAccessibilityEventForVirtualView(virtualViewId, 128);
            provider.sendAccessibilityEventForVirtualView(previousVirtualViewId, 256);
        }
    }

    private void playSoundAndHapticFeedback() {
        mAudioManager.playSoundEffect(mCurVelocity > FAST_SCROLL_VELOCITY_START
                ? mPickerSoundFastIndex : mPickerSoundSlowIndex);
        if (!mHapticPreDrawListener.mSkipHapticCalls) {
            mDelegator.performHapticFeedback(SAMSUNG_VIBRATION_START);
            mHapticPreDrawListener.mSkipHapticCalls = true;
        }
    }

    private static class HapticPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
        public boolean mSkipHapticCalls;

        HapticPreDrawListener() {
            mSkipHapticCalls = false;
        }

        @Override
        public boolean onPreDraw() {
            mSkipHapticCalls = false;
            return true;
        }
    }

    @Override
    public void computeScroll() {
        if (!mSpringFlingRunning) {
            Scroller scroller = mFlingScroller;
            if (scroller.isFinished()) {
                scroller = mAdjustScroller;
                if (scroller.isFinished()) {
                    return;
                }
            }
            scroller.computeScrollOffset();
            int currentScrollerY = scroller.getCurrY();
            if (mPreviousScrollerY == 0) {
                mPreviousScrollerY = scroller.getStartY();
            }
            scrollBy(0, currentScrollerY - mPreviousScrollerY);
            mPreviousScrollerY = currentScrollerY;
            if (scroller.isFinished()) {
                onScrollerFinished(scroller);
            } else {
                mDelegator.invalidate();
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        mInputText.setEnabled(enabled);
        if (!enabled && mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
            stopScrollAnimation();
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        }
    }

    // TODO rework this method
    // kang
    @Override
    public void scrollBy(int var1, int var2) {
        /* var1 = x; var2 = y; */
        int[] var3 = this.mSelectorIndices;
        if (var2 != 0 && this.mSelectorElementHeight > 0) {
            var1 = var2;
            int var4;
            int var5;
            if (!this.mWrapSelectorWheel) {
                var4 = this.mCurrentScrollOffset;
                var5 = this.mInitialScrollOffset;
                var1 = var2;
                if (var4 + var2 > var5) {
                    var1 = var2;
                    if (var3[2] <= this.mMinValue) {
                        var2 = var5 - var4;
                        this.stopFlingAnimation();
                        var1 = var2;
                        if (this.mIsAmPm) {
                            var1 = var2;
                            if (this.mLastDownOrMoveEventY > (float)this.mDelegator.getBottom()) {
                                this.mIgnoreMoveEvents = true;
                                return;
                            }
                        }
                    }
                }
            }

            var2 = var1;
            if (!this.mWrapSelectorWheel) {
                var5 = this.mCurrentScrollOffset;
                var4 = this.mInitialScrollOffset;
                var2 = var1;
                if (var5 + var1 < var4) {
                    var2 = var1;
                    if (var3[2] >= this.mMaxValue) {
                        var1 = var4 - var5;
                        this.stopFlingAnimation();
                        var2 = var1;
                        if (this.mIsAmPm) {
                            var2 = var1;
                            if (this.mLastDownOrMoveEventY < (float)this.mDelegator.getTop()) {
                                this.mIgnoreMoveEvents = true;
                                return;
                            }
                        }
                    }
                }
            }

            this.mCurrentScrollOffset += var2;

            while(true) {
                var1 = this.mCurrentScrollOffset;
                if (var1 - this.mInitialScrollOffset < this.mValueChangeOffset) {
                    while(true) {
                        var1 = this.mCurrentScrollOffset;
                        if (var1 - this.mInitialScrollOffset > -this.mValueChangeOffset) {
                            return;
                        }

                        this.mCurrentScrollOffset = var1 + this.mSelectorElementHeight;
                        this.incrementSelectorIndices(var3);
                        this.playSoundAndHapticFeedback();
                        if (!this.mIsStartingAnimation) {
                            this.setValueInternal(var3[2], true);
                            this.mIsValueChanged = true;
                        } else if (this.mWheelInterval != 1 && this.mCustomWheelIntervalMode) {
                            this.initializeSelectorWheelIndices();
                        }

                        if (!this.mWrapSelectorWheel && var3[2] >= this.mMaxValue) {
                            this.mCurrentScrollOffset = this.mInitialScrollOffset;
                        }
                    }
                }

                this.mCurrentScrollOffset = var1 - this.mSelectorElementHeight;
                this.decrementSelectorIndices(var3);
                this.playSoundAndHapticFeedback();
                if (!this.mIsStartingAnimation) {
                    this.setValueInternal(var3[2], true);
                    this.mIsValueChanged = true;
                } else if (this.mWheelInterval != 1 && this.mCustomWheelIntervalMode) {
                    this.initializeSelectorWheelIndices();
                }

                if (!this.mWrapSelectorWheel && var3[2] <= this.mMinValue) {
                    this.mCurrentScrollOffset = this.mInitialScrollOffset;
                }
            }
        }
    }
    // kang

    @Override
    public int computeVerticalScrollOffset() {
        return mCurrentScrollOffset;
    }

    @Override
    public int computeVerticalScrollRange() {
        return (mMaxValue - mMinValue + 1) * mSelectorElementHeight;
    }

    @Override
    public int computeVerticalScrollExtent() {
        return mDelegator.getHeight();
    }

    @Override
    public void setOnValueChangedListener(SeslNumberPicker.OnValueChangeListener onValueChangeListener) {
        mOnValueChangeListener = onValueChangeListener;
    }

    @Override
    public void setOnScrollListener(SeslNumberPicker.OnScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    @Override
    public void setOnEditTextModeChangedListener(
            SeslNumberPicker.OnEditTextModeChangedListener onEditTextModeChangedListener) {
        mOnEditTextModeChangedListener = onEditTextModeChangedListener;
    }

    @Override
    public void setFormatter(SeslNumberPicker.Formatter formatter) {
        if (formatter == mFormatter) {
            return;
        }
        mFormatter = formatter;
        initializeSelectorWheelIndices();
        updateInputTextView();
    }

    @Override
    public void setValue(int value) {
        if (!mFlingScroller.isFinished() || mSpringAnimation.isRunning()) {
            stopScrollAnimation();
        }
        setValueInternal(value, false);
    }

    @Override
    public boolean isEditTextModeNotAmPm() {
        return mIsEditTextMode && !mIsAmPm;
    }

    @Override
    public void performClick() {
        if (mIsEditTextModeEnabled) {
            showSoftInput();
        }
    }

    @Override
    public void performClick(boolean toIncrement) {
        if (mIsAmPm) {
            toIncrement = mValue != mMaxValue;
        }
        changeValueByOne(toIncrement);
    }

    @Override
    public void performLongClick() {
        mIgnoreMoveEvents = true;
        if (mIsEditTextModeEnabled) {
            mIsLongClicked = true;
        }
    }

    private void showSoftInputForWindowFocused() {
        mDelegator.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager inputMethodManager
                        = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null) {
                    if (mIsEditTextMode && mInputText.isFocused() && !inputMethodManager.showSoftInput(mInputText, 0)) {
                        mDelegator.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                InputMethodManager inputMethodManager
                                        = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                                if (inputMethodManager != null && mIsEditTextMode && mInputText.isFocused()) {
                                    inputMethodManager.showSoftInput(mInputText, 0);
                                }
                            }
                        }, 20);
                    }
                }
            }
        }, 20);
    }

    void showSoftInput() {
        InputMethodManager inputMethodManager
                = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            mInputText.setVisibility(View.VISIBLE);
            mInputText.requestFocus();
            inputMethodManager.viewClicked(mInputText);
            inputMethodManager.showSoftInput(mInputText, 0);
        }
    }

    private void hideSoftInput() {
        InputMethodManager inputMethodManager
                = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && inputMethodManager.isActive(mInputText)) {
            inputMethodManager.hideSoftInputFromWindow(mDelegator.getWindowToken(), 0);
            mInputText.setVisibility(View.INVISIBLE);
        }
    }

    private void tryComputeMaxWidth() {
        if (!mComputeMaxWidth) {
            return;
        }
        int maxTextWidth = 0;
        int i;
        if (mDisplayedValues == null) {
            float maxDigitWidth = 0;
            for (i = 0; i <= 9; i++) {
                final float digitWidth
                        = mSelectorWheelPaint.measureText(formatNumberWithLocale(i));
                if (digitWidth > maxDigitWidth) {
                    maxDigitWidth = digitWidth;
                }
            }
            int numberOfDigits = 0;
            int current = mMaxValue;
            while (current > 0) {
                numberOfDigits++;
                current = current / 10;
            }
            maxTextWidth = (int) (numberOfDigits * maxDigitWidth);
        } else {
            final int valueCount = mDisplayedValues.length;
            for (i = 0; i < valueCount; i++) {
                final float textWidth = mSelectorWheelPaint.measureText(mDisplayedValues[i]);
                if (textWidth > maxTextWidth) {
                    maxTextWidth = (int) textWidth;
                }
            }
        }
        maxTextWidth += mInputText.getPaddingLeft() + mInputText.getPaddingRight();
        if (isHighContrastFontEnabled()) {
            maxTextWidth += ((int) Math.ceil(
                    (double) (SeslPaintReflector.getHCTStrokeWidth(mSelectorWheelPaint) / 2.0f)))
                        * (i + 2);
        }
        if (mMaxWidth != maxTextWidth) {
            if (maxTextWidth > mMinWidth) {
                mMaxWidth = maxTextWidth;
            } else {
                mMaxWidth = mMinWidth;
            }
            mDelegator.invalidate();
        }
    }

    @Override
    public boolean getWrapSelectorWheel() {
        return mWrapSelectorWheel;
    }

    @Override
    public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
        mWrapSelectorWheelPreferred = wrapSelectorWheel;
        updateWrapSelectorWheel();
    }

    private void updateWrapSelectorWheel() {
        boolean wrapSelectorWheel = true;
        final boolean wrappingAllowed = mMaxValue - mMinValue >= mSelectorIndices.length;
        if (!wrappingAllowed || !mWrapSelectorWheelPreferred) {
            wrapSelectorWheel = false;
        }
        if (mWrapSelectorWheel != wrapSelectorWheel) {
            mWrapSelectorWheel = wrapSelectorWheel;
            initializeSelectorWheelIndices();
            mDelegator.invalidate();
        }
    }

    @Override
    public int getValue() {
        return mValue;
    }

    @Override
    public int getMinValue() {
        return mMinValue;
    }

    @Override
    public void setMinValue(int minValue) {
        if (mMinValue == minValue) {
            return;
        }
        if (minValue < 0) {
            throw new IllegalArgumentException("minValue must be >= 0");
        }
        if (mWheelInterval == DEFAULT_WHEEL_INTERVAL || minValue % mWheelInterval == 0) {
            mMinValue = minValue;
            if (mMinValue > mValue) {
                mValue = mMinValue;
            }
            updateWrapSelectorWheel();
            initializeSelectorWheelIndices();
            updateInputTextView();
            tryComputeMaxWidth();
            mDelegator.invalidate();
        }
    }

    @Override
    public int getMaxValue() {
        return mMaxValue;
    }

    @Override
    public void setMaxValue(int maxValue) {
        if (mMaxValue == maxValue) {
            return;
        }
        if (maxValue < 0) {
            throw new IllegalArgumentException("maxValue must be >= 0");
        }
        final boolean wrapSelectorWheel = mWrapSelectorWheel;
        if (mWheelInterval == DEFAULT_WHEEL_INTERVAL
                || ((wrapSelectorWheel ? 1 : 0) + maxValue) % mWheelInterval == 0) {
            mMaxValue = maxValue;
            if (mMaxValue < mValue) {
                mValue = mMaxValue;
            }
            updateWrapSelectorWheel();
            initializeSelectorWheelIndices();
            updateInputTextView();
            tryComputeMaxWidth();
            mDelegator.invalidate();
        }
    }

    @Override
    public int getMaxHeight() {
        return 0;
    }

    @Override
    public int getMaxWidth() {
        return 0;
    }

    @Override
    public int getMinHeight() {
        return 0;
    }

    @Override
    public int getMinWidth() {
        return 0;
    }

    @Override
    public String[] getDisplayedValues() {
        return mDisplayedValues;
    }

    @Override
    public void setDisplayedValues(String[] displayedValues) {
        if (mDisplayedValues == displayedValues) {
            return;
        }
        mDisplayedValues = displayedValues;
        if (mDisplayedValues != null) {
            mInputText.setRawInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        } else {
            mInputText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        }
        updateInputTextView();
        initializeSelectorWheelIndices();
        tryComputeMaxWidth();
    }

    @Override
    public void setErrorToastMessage(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            mToastText = msg;
        }
    }

    @Override
    public void setTextSize(float size) {
        final int scaledSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, size, mContext.getResources().getDisplayMetrics());
        mTextSize = scaledSize;
        mSelectorWheelPaint.setTextSize((float) scaledSize);
        mInputText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) mTextSize);
        tryComputeMaxWidth();
    }

    @Override
    public void setSubTextSize(float size) {
    }

    @Override
    public void setTextTypeface(Typeface typeface) {
        mCustomTypefaceSet = true;
        mPickerTypeface = typeface;
        mPickerSubTypeface = Typeface.create(typeface, Typeface.NORMAL);
        mSelectorWheelPaint.setTypeface(mPickerTypeface);
        mHcfFocusedTypefaceBold = Typeface.create(mPickerTypeface, Typeface.BOLD);
        setInputTextTypeface();
        tryComputeMaxWidth();
    }

    private void setInputTextTypeface() {
        if (mIsHcfEnabled) {
            mInputText.setTypeface(mHcfFocusedTypefaceBold);
        } else {
            mInputText.setTypeface(mPickerTypeface);
        }
    }

    private void setHcfTextAppearance(boolean bold) {
        if (mIsHcfEnabled) {
            if (bold) {
                mSelectorWheelPaint.setTypeface(mHcfFocusedTypefaceBold);
            } else {
                mSelectorWheelPaint.setTypeface(mPickerSubTypeface);
            }
            mSelectorWheelPaint.setTextSize((float) mTextSize);
        }
    }

    private static Typeface getFontTypeface(String fontFile) {
        if (!new File(fontFile).exists()) {
            return null;
        }
        try {
            return Typeface.createFromFile(fontFile);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getPaintFlags() {
        return mSelectorWheelPaint.getFlags();
    }

    @Override
    public void setPaintFlags(int flags) {
        if (mSelectorWheelPaint.getFlags() != flags) {
            mSelectorWheelPaint.setFlags(flags);
            mInputText.setPaintFlags(flags);
            tryComputeMaxWidth();
        }
    }

    // TODO rework this method
    @Override
    public void startAnimation(int delayMillis, SeslAnimationListener listener) {
        mAnimationListener = listener;
        if (!mIsEditTextMode) {
            if (!mIsAmPm && !mWrapSelectorWheel && getValue() - getMinValue() == 0) {
                if (mAnimationListener != null) {
                    mAnimationListener.onAnimationEnd();
                }
            } else {
                if (mFadeOutAnimator.isStarted()) {
                    mFadeOutAnimator.cancel();
                }
                if (mFadeInAnimator.isStarted()) {
                    mFadeInAnimator.cancel();
                }
                if (mColorInAnimator.isStarted()) {
                    mColorInAnimator.cancel();
                }
                if (mColorOutAnimator.isStarted()) {
                    mColorOutAnimator.cancel();
                }

                mDelegator.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mSelectorElementHeight == 0) {
                            mReservedStartAnimation = true;
                            return;
                        }

                        mIsStartingAnimation = true;
                        mFlingScroller = mCustomScroller;

                        // kang
                        int i2;
                        if (SeslNumberPickerSpinnerDelegate.this.getValue() != SeslNumberPickerSpinnerDelegate.this.getMinValue()) {
                            i2 = mSelectorElementHeight;
                        } else {
                            i2 = -mSelectorElementHeight;
                        }
                        int value = getValue() - getMinValue();
                        int i3 = (mWrapSelectorWheel || value >= 5) ? 5 : value;
                        float f = (mWrapSelectorWheel || value >= 5) ? 5.4f : ((float) value) + 0.4f;
                        int i4 = mIsAmPm ? i2 : mSelectorElementHeight * i3;
                        if (!mIsAmPm) {
                            i2 = (int) (((float) mSelectorElementHeight) * f);
                        }
                        scrollBy(0, i4);
                        mDelegator.invalidate();
                        int finalI = i2;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!moveToFinalScrollerPosition(mFlingScroller)) {
                                            moveToFinalScrollerPosition(mAdjustScroller);
                                        }
                                        startFadeAnimation(false);
                                        mPreviousScrollerY = 0;
                                        mFlingScroller.startScroll(0, 0, 0, -finalI,
                                                SeslNumberPickerSpinnerDelegate.this.mIsAmPm ? SeslNumberPickerSpinnerDelegate.START_ANIMATION_SCROLL_DURATION : SeslNumberPickerSpinnerDelegate.START_ANIMATION_SCROLL_DURATION_2016B);
                                        mDelegator.invalidate();
                                        new Handler().postDelayed(new Runnable() {
                                            @Override // java.lang.Runnable
                                            public void run() {
                                                moveToFinalScrollerPosition(mFlingScroller);
                                                mFlingScroller.abortAnimation();
                                                mAdjustScroller.abortAnimation();
                                                ensureScrollWheelAdjusted();
                                                mFlingScroller = mLinearScroller;
                                                mIsStartingAnimation = false;
                                                mDelegator.invalidate();
                                                startFadeAnimation(true);
                                                if (mAnimationListener != null) {
                                                    mAnimationListener.onAnimationEnd();
                                                }
                                            }
                                        }, START_ANIMATION_SCROLL_DURATION);
                                    }
                                }, 100);
                            }
                        }, (long) delayMillis);
                        // kang
                    }
                });
            }
        }
    }

    private void stopScrollAnimation() {
        mFlingScroller.abortAnimation();
        mAdjustScroller.abortAnimation();
        mGravityScroller.abortAnimation();
        mSpringAnimation.cancel();
        mSpringFlingRunning = false;
        mIsStartingAnimation = false;
        if (!moveToFinalScrollerPosition(mFlingScroller)) {
            moveToFinalScrollerPosition(mAdjustScroller);
        }
        ensureScrollWheelAdjusted();
    }

    private void stopFlingAnimation() {
        mFlingScroller.abortAnimation();
        mAdjustScroller.abortAnimation();
        mGravityScroller.abortAnimation();
        mSpringAnimation.cancel();
        mSpringFlingRunning = false;
    }

    void startFadeAnimation(boolean fadeOut) {
        if (fadeOut) {
            mFadeOutAnimator.setStartDelay((mFlingScroller.isFinished() ?
                    0 : mFlingScroller.getDuration()) + 100);
            mColorOutAnimator.setStartDelay(mFlingScroller.isFinished() ?
                    0 : mFlingScroller.getDuration() + 100);
            mColorOutAnimator.start();
            mFadeOutAnimator.start();
        } else  {
            mFadeInAnimator.setFloatValues(mAlpha, mActivatedAlpha);
            mColorInAnimator.setIntValues(mTextColor, mTextColorScrolling);
            mColorOutAnimator.cancel();
            mFadeOutAnimator.cancel();
            mColorInAnimator.start();
            mFadeInAnimator.start();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        mGravityScroller.abortAnimation();
        mSpringAnimation.cancel();
        mSpringFlingRunning = false;
        removeAllCallbacks();
        mDelegator.getViewTreeObserver().removeOnPreDrawListener(mHapticPreDrawListener);
    }

    @Override
    public void onAttachedToWindow() {
        mDelegator.getViewTreeObserver().addOnPreDrawListener(mHapticPreDrawListener);
    }

    // TODO rework this method
    // kang
    @Override
    public void onDraw(Canvas var1) {
        int var2 = this.mDelegator.getRight();
        int var3 = this.mDelegator.getLeft();
        int var4 = this.mDelegator.getBottom();
        float var5 = (float)(var2 - var3) / 2.0F;
        float var6 = (float)(this.mCurrentScrollOffset - this.mSelectorElementHeight);
        Drawable var7 = this.mVirtualButtonFocusedDrawable;
        if (var7 != null && this.mScrollState == 0) {
            var3 = this.mLastFocusedChildVirtualViewId;
            if (var3 != 1) {
                if (var3 != 2) {
                    if (var3 == 3) {
                        var7.setState(this.mDelegator.getDrawableState());
                        this.mVirtualButtonFocusedDrawable.setBounds(0, this.mBottomSelectionDividerBottom, var2, var4);
                        this.mVirtualButtonFocusedDrawable.draw(var1);
                    }
                } else {
                    var7.setState(this.mDelegator.getDrawableState());
                    this.mVirtualButtonFocusedDrawable.setBounds(0, this.mTopSelectionDividerTop, var2, this.mBottomSelectionDividerBottom);
                    this.mVirtualButtonFocusedDrawable.draw(var1);
                }
            } else {
                var7.setState(this.mDelegator.getDrawableState());
                this.mVirtualButtonFocusedDrawable.setBounds(0, 0, var2, this.mTopSelectionDividerTop);
                this.mVirtualButtonFocusedDrawable.draw(var1);
            }
        }

        int[] var8 = this.mSelectorIndices;

        for(var3 = 0; var3 < var8.length; ++var3) {
            int var9 = var8[var3];
            String var10 = (String)this.mSelectorIndexToStringCache.get(var9);
            String var17 = var10;
            if (!var10.isEmpty()) {
                var17 = var10;
                if (!this.mUnitValue.isEmpty()) {
                    var17 = var10 + this.mUnitValue;
                }
            }

            float var11 = this.mAlpha;
            float var12 = this.mIdleAlpha;
            float var13 = var11;
            if (var11 < var12) {
                var13 = var12;
            }

            label44: {
                var9 = (int)((this.mSelectorWheelPaint.descent() - this.mSelectorWheelPaint.ascent()) / 2.0F + var6 - this.mSelectorWheelPaint.descent());
                int var14 = this.mTopSelectionDividerTop;
                int var15 = this.mInitialScrollOffset;
                if (var6 >= (float)(var14 - var15)) {
                    int var16 = this.mBottomSelectionDividerBottom;
                    if (var6 <= (float)(var15 + var16)) {
                        if (var6 <= (float)(var14 + var16) / 2.0F) {
                            var1.save();
                            var1.clipRect(0, this.mTopSelectionDividerTop, var2, this.mBottomSelectionDividerBottom);
                            this.mSelectorWheelPaint.setColor(this.mTextColor);
                            this.mSelectorWheelPaint.setTypeface(this.mPickerTypeface);
                            var11 = (float)var9;
                            var1.drawText(var17, var5, var11, this.mSelectorWheelPaint);
                            var1.restore();
                            var1.save();
                            var1.clipRect(0, 0, var2, this.mTopSelectionDividerTop);
                            this.mSelectorWheelPaint.setTypeface(this.mPickerSubTypeface);
                            this.mSelectorWheelPaint.setAlpha((int)(var13 * 255.0F * this.mInitialAlpha));
                            var1.drawText(var17, var5, var11, this.mSelectorWheelPaint);
                            var1.restore();
                        } else {
                            var1.save();
                            var1.clipRect(0, this.mTopSelectionDividerTop, var2, this.mBottomSelectionDividerBottom);
                            this.mSelectorWheelPaint.setTypeface(this.mPickerTypeface);
                            this.mSelectorWheelPaint.setColor(this.mTextColor);
                            var11 = (float)var9;
                            var1.drawText(var17, var5, var11, this.mSelectorWheelPaint);
                            var1.restore();
                            var1.save();
                            var1.clipRect(0, this.mBottomSelectionDividerBottom, var2, var4);
                            this.mSelectorWheelPaint.setAlpha((int)(var13 * 255.0F * this.mInitialAlpha));
                            this.mSelectorWheelPaint.setTypeface(this.mPickerSubTypeface);
                            var1.drawText(var17, var5, var11, this.mSelectorWheelPaint);
                            var1.restore();
                        }
                        break label44;
                    }
                }

                var1.save();
                this.mSelectorWheelPaint.setAlpha((int)(var13 * 255.0F * this.mInitialAlpha));
                this.mSelectorWheelPaint.setTypeface(this.mPickerSubTypeface);
                var1.drawText(var17, var5, (float)var9, this.mSelectorWheelPaint);
                var1.restore();
            }

            var6 += (float)this.mSelectorElementHeight;
        }

    }
    // kang

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(android.widget.NumberPicker.class.getName());
        event.setScrollable(true);
        event.setScrollY((mMinValue + mValue) * mSelectorElementHeight);
        event.setMaxScrollY((mMaxValue - mMinValue) * mSelectorElementHeight);
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeProviderImpl provider
                = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
        event.getText().add(provider.getVirtualCurrentButtonText(true));
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (mAccessibilityNodeProvider == null) {
            mAccessibilityNodeProvider = new AccessibilityNodeProviderImpl();
        }
        return mAccessibilityNodeProvider;
    }

    private int makeMeasureSpec(int measureSpec, int maxSize) {
        if (maxSize == SIZE_UNSPECIFIED) {
            return measureSpec;
        }
        final int size = View.MeasureSpec.getSize(measureSpec);
        final int mode = View.MeasureSpec.getMode(measureSpec);
        switch (mode) {
            case View.MeasureSpec.EXACTLY:
                return measureSpec;
            case View.MeasureSpec.AT_MOST:
                return View.MeasureSpec.makeMeasureSpec(Math.min(size, maxSize), View.MeasureSpec.EXACTLY);
            case View.MeasureSpec.UNSPECIFIED:
                return View.MeasureSpec.makeMeasureSpec(maxSize, View.MeasureSpec.EXACTLY);
            default:
                throw new IllegalArgumentException("Unknown measure mode: " + mode);
        }
    }

    private int resolveSizeAndStateRespectingMinSize(int minSize, int measuredSize, int measureSpec) {
        if (minSize != SIZE_UNSPECIFIED) {
            final int desiredWidth = Math.max(minSize, measuredSize);
            return View.resolveSizeAndState(desiredWidth, measureSpec, 0);
        } else {
            return measuredSize;
        }
    }

    private void initializeSelectorWheelIndices() {
        mSelectorIndexToStringCache.clear();
        int[] selectorIndices = mSelectorIndices;
        int current = mIsStartingAnimation ? selectorIndices[2] : getValue();
        for (int i = 0; i < mSelectorIndices.length; i++) {
            int selectorIndex = current + ((i - SELECTOR_MIDDLE_ITEM_INDEX)
                    * (mCustomWheelIntervalMode ? mWheelInterval : DEFAULT_WHEEL_INTERVAL));
            if (mWrapSelectorWheel) {
                selectorIndex = getWrappedSelectorIndex(selectorIndex);
            }
            selectorIndices[i] = selectorIndex;
            ensureCachedScrollSelectorValue(selectorIndices[i]);
        }
    }

    private void setValueInternal(int current, boolean notifyChange) {
        if (mValue != current) {
            if (mWrapSelectorWheel) {
                current = getWrappedSelectorIndex(current);
            } else {
                current = Math.max(current, mMinValue);
                current = Math.min(current, mMaxValue);
            }
            int previous = mValue;
            mValue = current;
            updateInputTextView();
            if (notifyChange) {
                notifyChange(previous, current);
            }
            initializeSelectorWheelIndices();
            mDelegator.invalidate();
            if (mAccessibilityManager.isEnabled() && mDelegator.getParent() != null) {
                mDelegator.getParent().notifySubtreeAccessibilityStateChanged(mDelegator, mDelegator, 1);
            }
        } else {
            if (isCharacterNumberLanguage()) {
                updateInputTextView();
                mDelegator.invalidate();
            }
        }
    }

    void changeValueByOne(boolean increment) {
        mInputText.setVisibility(View.INVISIBLE);
        if (!moveToFinalScrollerPosition(mFlingScroller)) {
            moveToFinalScrollerPosition(mAdjustScroller);
        }
        mPreviousScrollerY = 0;
        if (increment) {
            mFlingScroller.startScroll(0, 0,
                    0, -mSelectorElementHeight, SNAP_SCROLL_DURATION);
        } else {
            mFlingScroller.startScroll(0, 0,
                    0, mSelectorElementHeight, SNAP_SCROLL_DURATION);
        }
        mDelegator.invalidate();
    }

    private void initializeSelectorWheel() {
        if (mIsStartingAnimation) {
            if (!moveToFinalScrollerPosition(mFlingScroller)) {
                moveToFinalScrollerPosition(mAdjustScroller);
            }
            stopScrollAnimation();
        } else {
            initializeSelectorWheelIndices();
        }
        int totalTextHeight = mTextSize * 3;
        float totalTextGapHeight = (mDelegator.getBottom() - mDelegator.getTop()) - totalTextHeight;
        mSelectorTextGapHeight = (int) ((totalTextGapHeight / 3) + 0.5f);
        mSelectorElementHeight = mTextSize + mSelectorTextGapHeight;
        mValueChangeOffset = (mModifiedTxtHeight > mSelectorElementHeight || mIsAmPm) ?
                mDelegator.getHeight() / 3 : mModifiedTxtHeight;
        mInitialScrollOffset = (mInputText.getTop() + (mModifiedTxtHeight / 2)) - mSelectorElementHeight;
        mCurrentScrollOffset = mInitialScrollOffset;
        ((SeslNumberPicker.CustomEditText) mInputText).setEditTextPosition(
                ((int) (((mSelectorWheelPaint.descent() - mSelectorWheelPaint.ascent()) / 2.0f) - mSelectorWheelPaint.descent()))
                        - (mInputText.getBaseline() - (mModifiedTxtHeight / 2)));
        if (mReservedStartAnimation) {
            startAnimation(0, mAnimationListener);
            mReservedStartAnimation = false;
        }
    }

    private void onScrollerFinished(Scroller scroller) {
        if (scroller == mFlingScroller) {
            if (!ensureScrollWheelAdjusted()) {
                updateInputTextView();
            }
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        } else {
            if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                updateInputTextView();
            }
        }
    }

    private void onScrollStateChange(int scrollState) {
        if (mScrollState == scrollState) {
            return;
        }
        mScrollState = scrollState;
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChange(mDelegator, scrollState);
        }
    }

    // TODO rework this method
    // kang
    private void fling(int velocityY) {
        if (!this.mWrapSelectorWheel && velocityY > 0 && getValue() == getMinValue()) {
            startFadeAnimation(true);
        } else if (this.mWrapSelectorWheel || velocityY >= 0 || getValue() != getMaxValue()) {
            this.mPreviousScrollerY = 0;
            float f = (float) velocityY;
            Math.round((((float) Math.abs(velocityY)) / ((float) this.mMaximumFlingVelocity)) * f);
            this.mPreviousSpringY = (float) this.mCurrentScrollOffset;
            this.mSpringAnimation.setStartVelocity(f);
            this.mGravityScroller.forceFinished(true);
            this.mGravityScroller.fling(0, this.mCurrentScrollOffset, 0, velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            int round = Math.round(((float) (this.mGravityScroller.getFinalY() + this.mCurrentScrollOffset)) / ((float) this.mSelectorElementHeight));
            int i2 = this.mSelectorElementHeight;
            int i3 = this.mInitialScrollOffset;
            int i4 = (round * i2) + i3;
            int i;
            if (velocityY > 0) {
                i = Math.max(i4, i2 + i3);
            } else {
                i = Math.min(i4, (-i2) + i3);
            }
            this.mSpringAnimation.setStartValue((float) this.mCurrentScrollOffset);
            this.mSpringFlingRunning = true;
            this.mSpringAnimation.animateToFinalPosition((float) i);
            this.mDelegator.invalidate();
        } else {
            startFadeAnimation(true);
        }
    }
    // kang

    int getWrappedSelectorIndex(int selectorIndex) {
        if (selectorIndex > mMaxValue) {
            return mMinValue + (selectorIndex - mMaxValue) % (mMaxValue - mMinValue) - 1;
        } else if (selectorIndex < mMinValue) {
            return mMaxValue - (mMinValue - selectorIndex) % (mMaxValue - mMinValue) + 1;
        }
        return selectorIndex;
    }

    private void incrementSelectorIndices(int[] selectorIndices) {
        System.arraycopy(selectorIndices, 1, selectorIndices,
                0, selectorIndices.length - 1);
        int nextScrollSelectorIndex = selectorIndices[selectorIndices.length - 2] + 1;
        if (mWrapSelectorWheel && nextScrollSelectorIndex > mMaxValue) {
            nextScrollSelectorIndex = mMinValue;
        }
        selectorIndices[selectorIndices.length - 1] = nextScrollSelectorIndex;
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex);
    }

    private void decrementSelectorIndices(int[] selectorIndices) {
        System.arraycopy(selectorIndices, 0, selectorIndices,
                1, selectorIndices.length - 1);
        int nextScrollSelectorIndex = selectorIndices[1] - 1;
        if (mWrapSelectorWheel && nextScrollSelectorIndex < mMinValue) {
            nextScrollSelectorIndex = mMaxValue;
        }
        selectorIndices[0] = nextScrollSelectorIndex;
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex);
    }

    private void ensureCachedScrollSelectorValue(int selectorIndex) {
        SparseArray<String> cache = mSelectorIndexToStringCache;
        String scrollSelectorValue = cache.get(selectorIndex);
        if (scrollSelectorValue != null) {
            return;
        }
        if (selectorIndex < mMinValue || selectorIndex > mMaxValue) {
            scrollSelectorValue = "";
        } else {
            if (mDisplayedValues != null) {
                int displayedValueIndex = selectorIndex - mMinValue;
                scrollSelectorValue = mDisplayedValues[displayedValueIndex];
            } else {
                scrollSelectorValue = formatNumber(selectorIndex);
            }
        }
        cache.put(selectorIndex, scrollSelectorValue);
    }

    String formatNumber(int value) {
        return (mFormatter != null) ?
                mFormatter.format(value) : formatNumberWithLocale(value);
    }

    void validateInputTextView(View v) {
        String str = String.valueOf(((TextView) v).getText());
        int current = getSelectedPos(str);
        if (TextUtils.isEmpty(str) || mValue == current) {
            if (mWheelInterval != DEFAULT_WHEEL_INTERVAL
                    && mCustomWheelIntervalMode && mIsPressedBackKey) {
                applyWheelCustomInterval(current % mWheelInterval == 0);
            } else {
                updateInputTextView();
            }
        } else {
            if (mWheelInterval != DEFAULT_WHEEL_INTERVAL && mCustomWheelIntervalMode) {
                applyWheelCustomInterval(current % mWheelInterval == 0);
            }
            setValueInternal(current, true);
        }
    }

    boolean updateInputTextView() {
        String text = (mDisplayedValues == null) ?
                formatNumber(mValue) : mDisplayedValues[mValue - mMinValue];
        if (!TextUtils.isEmpty(text)) {
            CharSequence beforeText = mInputText.getText();
            if (!text.equals(beforeText.toString())) {
                mInputText.setText(text);
                Selection.setSelection(mInputText.getText(), mInputText.getText().length());
                return true;
            }
        }

        return false;
    }

    private void notifyChange(int previous, int current) {
        if (mAccessibilityManager.isEnabled() && !mIsStartingAnimation) {
            final int value = getWrappedSelectorIndex(mValue);
            String text = null;
            if (value <= mMaxValue) {
                text = mDisplayedValues == null ?
                        formatNumber(value) : mDisplayedValues[value - mMinValue];
            }
            mDelegator.sendAccessibilityEvent(4);
            AccessibilityNodeProviderImpl provider
                    = (AccessibilityNodeProviderImpl) getAccessibilityNodeProvider();
            if (!mIsEditTextModeEnabled && provider != null) {
                provider.performAction(2, 64, null);
            }
            if (provider != null && !mWrapSelectorWheel
                    && (getValue() == getMaxValue() || getValue() == getMinValue())) {
                provider.sendAccessibilityEventForVirtualView(2, 32768);
            }
        }

        if (mOnValueChangeListener != null) {
            mOnValueChangeListener.onValueChange(mDelegator, previous, mValue);
        }
    }

    private void postSwitchIntervalOnLongPress() {
        if (mSwitchIntervalOnLongPressCommand == null) {
            mSwitchIntervalOnLongPressCommand = new SwitchIntervalOnLongPressCommand();
        } else {
            mDelegator.removeCallbacks(mSwitchIntervalOnLongPressCommand);
        }
        mDelegator.postDelayed(mSwitchIntervalOnLongPressCommand,
                ViewConfiguration.getLongPressTimeout());
    }

    private void removeSwitchIntervalOnLongPress() {
        if (mSwitchIntervalOnLongPressCommand != null) {
            mDelegator.removeCallbacks(mSwitchIntervalOnLongPressCommand);
        }
    }

    private void postBeginSoftInputOnLongPressCommand() {
        if (mBeginSoftInputOnLongPressCommand == null) {
            mBeginSoftInputOnLongPressCommand = new BeginSoftInputOnLongPressCommand();
        } else {
            mDelegator.removeCallbacks(mBeginSoftInputOnLongPressCommand);
        }
        mDelegator.postDelayed(mBeginSoftInputOnLongPressCommand,
                ViewConfiguration.getLongPressTimeout());
    }

    private void removeBeginSoftInputCommand() {
        if (mBeginSoftInputOnLongPressCommand != null) {
            mDelegator.removeCallbacks(mBeginSoftInputOnLongPressCommand);
        }
    }

    private void removeAllCallbacks() {
        if (mSwitchIntervalOnLongPressCommand != null) {
            mDelegator.removeCallbacks(mSwitchIntervalOnLongPressCommand);
        }
        if (mBeginSoftInputOnLongPressCommand != null) {
            mDelegator.removeCallbacks(mBeginSoftInputOnLongPressCommand);
        }
        mPressedStateHelper.cancel();
    }

    int getSelectedPos(String value) {
        try {
            if (mDisplayedValues == null) {
                return Integer.parseInt(value);
            } else {
                for (int i = 0; i < mDisplayedValues.length; i++) {
                    value = value.toLowerCase();
                    if (mDisplayedValues[i].toLowerCase().startsWith(value)) {
                        return mMinValue + i;
                    }
                }
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException ignored) {
            return mMinValue;
        }
    }

    class InputTextFilter extends NumberKeyListener {
        public int getInputType() {
            return InputType.TYPE_CLASS_TEXT;
        }

        @NonNull
        @Override
        protected char[] getAcceptedChars() {
            return DIGIT_CHARACTERS;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (mDisplayedValues == null) {
                CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
                if (filtered == null) {
                    filtered = source.subSequence(start, end);
                }

                String result = String.valueOf(
                        dest.subSequence(0, dstart)) + filtered + dest.subSequence(dend, dest.length());

                if (result.isEmpty()) {
                    return result;
                }

                if (getSelectedPos(result) > mMaxValue) {
                    if (mIsEditTextMode) {
                        if (mToast == null) {
                            initToastObject();
                        }
                        mToast.show();
                    }
                    return "";
                } else {
                    if (result.length() <= formatNumber(mMaxValue).length()) {
                        return filtered;
                    }
                }
            } else {
                CharSequence filtered = String.valueOf(source.subSequence(start, end));
                String result = String.valueOf(
                        dest.subSequence(0, dstart)) + filtered + dest.subSequence(dend, dest.length());
                String str = String.valueOf(result).toLowerCase();
                for (String val : mDisplayedValues) {
                    String valLowerCase = val.toLowerCase();
                    if ((needCompareEqualMonthLanguage()
                            && valLowerCase.equals(str)) || valLowerCase.startsWith(str)) {
                        return filtered;
                    }
                }
                if (mIsEditTextMode && !TextUtils.isEmpty(str)) {
                    if (mToast == null) {
                        initToastObject();
                    }
                    mToast.show();
                }
            }
            return "";
        }
    }

    void initToastObject() {
        mToast = Toast.makeText(mContext, mToastText, Toast.LENGTH_SHORT);
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.sesl_custom_toast_layout, null);
        ((TextView) view.findViewById(R.id.message)).setText(mToastText);
        mToast.setView(view);
    }

    boolean ensureScrollWheelAdjusted() {
        return ensureScrollWheelAdjusted(0);
    }

    private boolean ensureScrollWheelAdjusted(int distance) {
        if (mInitialScrollOffset == Integer.MIN_VALUE) {
            return false;
        }

        int deltaY = mInitialScrollOffset - mCurrentScrollOffset;
        if (deltaY != 0) {
            mPreviousScrollerY = 0;

            if (!mIsValueChanged && distance != 0) {
                if (Math.abs(distance) < mSelectorElementHeight) {
                    deltaY += (deltaY > 0) ? -mSelectorElementHeight : mSelectorElementHeight;

                    mAdjustScroller.startScroll(0, 0, 0, deltaY, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
                    mDelegator.invalidate();
                    mIsValueChanged = false;
                    return true;
                }
            }

            if (Math.abs(deltaY) > mSelectorElementHeight / 2) {
                deltaY += (deltaY > 0) ? -mSelectorElementHeight : mSelectorElementHeight;
            }

            mAdjustScroller.startScroll(0, 0, 0, deltaY, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
            mDelegator.invalidate();
            mIsValueChanged = false;
            return true;
        }
        mIsValueChanged = false;
        return false;
    }

    class PressedStateHelper implements Runnable {
        public static final int BUTTON_DECREMENT = 2;
        public static final int BUTTON_INCREMENT = 1;
        private final int MODE_PRESS = 1;
        private final int MODE_TAPPED = 2;
        private int mManagedButton;
        private int mMode;

        public void cancel() {
            final int mRight = mDelegator.getRight();
            final int mBottom = mDelegator.getBottom();
            mMode = 0;
            mManagedButton = 0;
            mDelegator.removeCallbacks(this);
            if (mIncrementVirtualButtonPressed) {
                mIncrementVirtualButtonPressed = false;
                mDelegator.invalidate(0, mBottomSelectionDividerBottom,
                        mRight, mBottom);
            }
            if (mDecrementVirtualButtonPressed) {
                mDecrementVirtualButtonPressed = false;
                mDelegator.invalidate(0, 0,
                        mRight, mTopSelectionDividerTop);
            }
        }

        public void buttonPressDelayed(int button) {
            cancel();
            mMode = MODE_PRESS;
            mManagedButton = button;
            mDelegator.postDelayed(this, ViewConfiguration.getTapTimeout());
        }

        public void buttonTapped(int button) {
            cancel();
            mMode = MODE_TAPPED;
            mManagedButton = button;
            mDelegator.post(this);
        }

        @Override
        public void run() {
            final int mRight = mDelegator.getRight();
            final int mBottom = mDelegator.getBottom();
            switch (mMode) {
                case MODE_PRESS: {
                    switch (mManagedButton) {
                        case BUTTON_INCREMENT: {
                            mIncrementVirtualButtonPressed = true;
                            mDelegator.invalidate(0, mBottomSelectionDividerBottom,
                                    mRight, mBottom);
                        } break;
                        case BUTTON_DECREMENT: {
                            mDecrementVirtualButtonPressed = true;
                            mDelegator.invalidate(0, 0,
                                    mRight, mTopSelectionDividerTop);
                        }
                    }
                } break;
                case MODE_TAPPED: {
                    switch (mManagedButton) {
                        case BUTTON_INCREMENT: {
                            if (!mIncrementVirtualButtonPressed) {
                                mDelegator.postDelayed(this, ViewConfiguration.getPressedStateDuration());
                            }
                            mIncrementVirtualButtonPressed ^= true;
                            mDelegator.invalidate(0, mBottomSelectionDividerBottom,
                                    mRight, mBottom);
                        } break;
                        case BUTTON_DECREMENT: {
                            if (!mDecrementVirtualButtonPressed) {
                                mDelegator.postDelayed(this,
                                        ViewConfiguration.getPressedStateDuration());
                            }
                            mDecrementVirtualButtonPressed ^= true;
                            mDelegator.invalidate(0, 0,
                                    mRight, mTopSelectionDividerTop);
                        }
                    }
                } break;
            }
        }
    }

    class SwitchIntervalOnLongPressCommand implements Runnable {
        @Override
        public void run() {
            mIgnoreMoveEvents = true;
            mIgnoreUpEvent = true;
            applyWheelCustomInterval(!mCustomWheelIntervalMode);
        }
    }

    class BeginSoftInputOnLongPressCommand implements Runnable {
        @Override
        public void run() {
            performLongClick();
        }
    }

    class AccessibilityNodeProviderImpl extends AccessibilityNodeProvider {
        private static final int UNDEFINED = Integer.MIN_VALUE;
        private static final int VIRTUAL_VIEW_ID_DECREMENT = 1;
        private static final int VIRTUAL_VIEW_ID_INCREMENT = 3;
        private static final int VIRTUAL_VIEW_ID_INPUT = 2;
        private final Rect mTempRect = new Rect();
        private final int[] mTempArray = new int[2];
        private int mAccessibilityFocusedView = Integer.MIN_VALUE;

        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            final int mLeft = mDelegator.getLeft();
            final int mRight = mDelegator.getRight();
            final int mTop = mDelegator.getTop();
            final int mBottom = mDelegator.getBottom();
            final int mScrollX = mDelegator.getScrollX();
            final int mScrollY = mDelegator.getScrollY();

            if (mLastFocusedChildVirtualViewId != View.NO_ID
                    || mLastHoveredChildVirtualViewId != Integer.MIN_VALUE) {
                switch (virtualViewId) {
                    case View.NO_ID:
                        return createAccessibilityNodeInfoForNumberPicker(
                                mScrollX,
                                mScrollY,
                                mScrollX + (mRight - mLeft),
                                mScrollY + (mBottom - mTop));
                    case VIRTUAL_VIEW_ID_DECREMENT:
                        return createAccessibilityNodeInfoForVirtualButton(
                                VIRTUAL_VIEW_ID_DECREMENT,
                                getVirtualDecrementButtonText(),
                                mScrollX,
                                mScrollY,
                                mScrollX + (mRight - mLeft),
                                mTopSelectionDividerTop + mSelectionDividerHeight);
                    case VIRTUAL_VIEW_ID_INPUT:
                        return createAccessibiltyNodeInfoForInputText(
                                mScrollX,
                                mTopSelectionDividerTop + mSelectionDividerHeight,
                                mScrollX + (mRight - mLeft),
                                mBottomSelectionDividerBottom - mSelectionDividerHeight);
                    case VIRTUAL_VIEW_ID_INCREMENT:
                        return createAccessibilityNodeInfoForVirtualButton(
                                VIRTUAL_VIEW_ID_INCREMENT,
                                getVirtualIncrementButtonText(),
                                mScrollX,
                                mBottomSelectionDividerBottom - mSelectionDividerHeight,
                                mScrollX + (mRight - mLeft),
                                mScrollY + (mBottom - mTop));
                }
            }

            AccessibilityNodeInfo info = super.createAccessibilityNodeInfo(virtualViewId);
            if (info == null) {
                return AccessibilityNodeInfo.obtain();
            }
            return info;
        }

        @Override
        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String searched, int virtualViewId) {
            if (TextUtils.isEmpty(searched)) {
                return Collections.emptyList();
            }
            String searchedLowerCase = searched.toLowerCase();
            List<AccessibilityNodeInfo> result = new ArrayList<AccessibilityNodeInfo>();
            switch (virtualViewId) {
                case View.NO_ID: {
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_DECREMENT, result);
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_INPUT, result);
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, VIRTUAL_VIEW_ID_INCREMENT, result);
                    return result;
                }
                case VIRTUAL_VIEW_ID_DECREMENT:
                case VIRTUAL_VIEW_ID_INCREMENT:
                case VIRTUAL_VIEW_ID_INPUT: {
                    findAccessibilityNodeInfosByTextInChild(searchedLowerCase, virtualViewId, result);
                    return result;
                }
            }
            return super.findAccessibilityNodeInfosByText(searched, virtualViewId);
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            if (mIsStartingAnimation) {
                return false;
            }

            final int mRight = mDelegator.getRight();
            final int mBottom = mDelegator.getBottom();

            switch (virtualViewId) {
                case View.NO_ID: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                SeslViewReflector.requestAccessibilityFocus(mDelegator);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                SeslViewReflector.clearAccessibilityFocus(mDelegator);
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD: {
                            if (mDelegator.isEnabled()
                                    && (getWrapSelectorWheel() || getValue() < getMaxValue())) {
                                startFadeAnimation(false);
                                changeValueByOne(true);
                                startFadeAnimation(true);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD: {
                            if (mDelegator.isEnabled()
                                    && (getWrapSelectorWheel() || getValue() > getMinValue())) {
                                startFadeAnimation(false);
                                changeValueByOne(false);
                                startFadeAnimation(true);
                                return true;
                            }
                        } return false;
                    }
                } break;
                case VIRTUAL_VIEW_ID_INPUT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_FOCUS: {
                            if (mDelegator.isEnabled() && !mInputText.isFocused()) {
                                return mInputText.requestFocus();
                            }
                        } break;
                        case AccessibilityNodeInfo.ACTION_CLEAR_FOCUS: {
                            if (mDelegator.isEnabled() && mInputText.isFocused()) {
                                mInputText.clearFocus();
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (mDelegator.isEnabled()) {
                                performClick();
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_LONG_CLICK: {
                            if (mDelegator.isEnabled()) {
                                performLongClick();
                                return true;
                            }
                            return false;
                        }
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                mDelegator.invalidate(0, mTopSelectionDividerTop, mRight, mBottomSelectionDividerBottom);
                                return true;
                            }
                        } return false;
                        case  AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                mDelegator.invalidate(0, mTopSelectionDividerTop, mRight, mBottomSelectionDividerBottom);
                                return true;
                            }
                        } return false;
                        default: {
                            return mInputText.performAccessibilityAction(action, arguments);
                        }
                    }
                } return false;
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (mDelegator.isEnabled()) {
                                startFadeAnimation(false);
                                changeValueByOne(true);
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                                startFadeAnimation(true);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                mDelegator.invalidate(0, mBottomSelectionDividerBottom, mRight, mBottom);
                                return true;
                            }
                        } return false;
                        case  AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                mDelegator.invalidate(0, mBottomSelectionDividerBottom, mRight, mBottom);
                                return true;
                            }
                        } return false;
                    }
                } return false;
                case VIRTUAL_VIEW_ID_DECREMENT: {
                    switch (action) {
                        case AccessibilityNodeInfo.ACTION_CLICK: {
                            if (mDelegator.isEnabled()) {
                                startFadeAnimation(false);
                                changeValueByOne(false);
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                                startFadeAnimation(true);
                                return true;
                            }
                        } return false;
                        case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView != virtualViewId) {
                                mAccessibilityFocusedView = virtualViewId;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                                mDelegator.invalidate(0, 0, mRight, mTopSelectionDividerTop);
                                return true;
                            }
                        } return false;
                        case  AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
                            if (mAccessibilityFocusedView == virtualViewId) {
                                mAccessibilityFocusedView = UNDEFINED;
                                sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                                mDelegator.invalidate(0, 0, mRight, mTopSelectionDividerTop);
                                return true;
                            }
                        } return false;
                    }
                } return false;
            }
            return super.performAction(virtualViewId, action, arguments);
        }

        public void sendAccessibilityEventForVirtualView(int virtualViewId, int eventType) {
            switch (virtualViewId) {
                case VIRTUAL_VIEW_ID_DECREMENT: {
                    if (hasVirtualDecrementButton()) {
                        sendAccessibilityEventForVirtualButton(
                                virtualViewId, eventType, getVirtualDecrementButtonText());
                    }
                } break;
                case VIRTUAL_VIEW_ID_INPUT: {
                    sendAccessibilityEventForVirtualText(eventType);
                } break;
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    if (hasVirtualIncrementButton()) {
                        sendAccessibilityEventForVirtualButton(
                                virtualViewId, eventType, getVirtualIncrementButtonText());
                    }
                } break;
            }
        }

        private void sendAccessibilityEventForVirtualText(int eventType) {
            if (mAccessibilityManager.isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
                mInputText.onInitializeAccessibilityEvent(event);
                mInputText.onPopulateAccessibilityEvent(event);
                event.setSource(mDelegator, VIRTUAL_VIEW_ID_INPUT);
                mDelegator.requestSendAccessibilityEvent(mDelegator, event);
            }
        }

        private void sendAccessibilityEventForVirtualButton(int virtualViewId, int eventType, String text) {
            if (mAccessibilityManager.isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
                event.setClassName(Button.class.getName());
                event.setPackageName(mContext.getPackageName());
                event.getText().add(text);
                event.setEnabled(mDelegator.isEnabled());
                event.setSource(mDelegator, virtualViewId);
                mDelegator.requestSendAccessibilityEvent(mDelegator, event);
            }
        }

        private void findAccessibilityNodeInfosByTextInChild(String searchedLowerCase, int virtualViewId, List<AccessibilityNodeInfo> outResult) {
            switch (virtualViewId) {
                case VIRTUAL_VIEW_ID_DECREMENT: {
                    String text = getVirtualDecrementButtonText();
                    if (!TextUtils.isEmpty(text) && text.toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_DECREMENT));
                    }
                } return;
                case VIRTUAL_VIEW_ID_INPUT: {
                    CharSequence text = mInputText.getText();
                    if (!TextUtils.isEmpty(text) && text.toString().toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_INPUT));
                    }
                } return;
                case VIRTUAL_VIEW_ID_INCREMENT: {
                    String text = getVirtualIncrementButtonText();
                    if (!TextUtils.isEmpty(text) && text.toLowerCase().contains(searchedLowerCase)) {
                        outResult.add(createAccessibilityNodeInfo(VIRTUAL_VIEW_ID_INCREMENT));
                    }
                } return;
            }
        }

        private AccessibilityNodeInfo createAccessibiltyNodeInfoForInputText(int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = mInputText.createAccessibilityNodeInfo();
            info.setSource(mDelegator, VIRTUAL_VIEW_ID_INPUT);
            if (mAccessibilityFocusedView != VIRTUAL_VIEW_ID_INPUT) {
                info.setAccessibilityFocused(false);
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            } else {
                info.setAccessibilityFocused(true);
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            if (!mIsEditTextModeEnabled) {
                info.setClassName(TextView.class.getName());
                info.setText(getVirtualCurrentButtonText(false));
                AccessibilityNodeInfoCompat.wrap(info).setTooltipText(mPickerContentDescription);
                info.setSelected(true);
                info.setAccessibilityFocused(false);
            }
            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            info.setVisibleToUser(mDelegator.isVisibleToUserWrapper(boundsInParent));
            info.setBoundsInParent(boundsInParent);
            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = mTempArray;
            mDelegator.getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            info.setBoundsInScreen(boundsInScreen);
            return info;
        }

        private AccessibilityNodeInfo createAccessibilityNodeInfoForVirtualButton(int virtualViewId,String text, int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setClassName(Button.class.getName());
            info.setPackageName(mContext.getPackageName());
            info.setSource(mDelegator, virtualViewId);
            info.setParent(mDelegator);
            info.setText(text);
            AccessibilityNodeInfoCompat.wrap(info).setTooltipText(mPickerContentDescription);
            info.setClickable(true);
            info.setLongClickable(true);
            info.setEnabled(mDelegator.isEnabled());
            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            info.setVisibleToUser(mDelegator.isVisibleToUserWrapper(boundsInParent));
            info.setBoundsInParent(boundsInParent);
            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = mTempArray;
            mDelegator.getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            info.setBoundsInScreen(boundsInScreen);

            if (mAccessibilityFocusedView != virtualViewId) {
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            } else {
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            if (mDelegator.isEnabled()) {
                info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            }

            return info;
        }

        private AccessibilityNodeInfo createAccessibilityNodeInfoForNumberPicker(
                int left, int top, int right, int bottom) {
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setClassName(android.widget.NumberPicker.class.getName());
            info.setPackageName(mContext.getPackageName());
            info.setSource(mDelegator);

            if (hasVirtualDecrementButton()) {
                info.addChild(mDelegator, VIRTUAL_VIEW_ID_DECREMENT);
            }
            info.addChild(mDelegator, VIRTUAL_VIEW_ID_INPUT);
            if (hasVirtualIncrementButton()) {
                info.addChild(mDelegator, VIRTUAL_VIEW_ID_INCREMENT);
            }

            info.setParent((View) mDelegator.getParentForAccessibility());
            info.setEnabled(mDelegator.isEnabled());
            info.setScrollable(true);

            final float applicationScale
                    = SeslCompatibilityInfoReflector.getField_applicationScale(mContext.getResources());

            Rect boundsInParent = mTempRect;
            boundsInParent.set(left, top, right, bottom);
            scaleRect(boundsInParent, applicationScale);
            info.setBoundsInParent(boundsInParent);

            info.setVisibleToUser(mDelegator.isVisibleToUserWrapper());

            Rect boundsInScreen = boundsInParent;
            int[] locationOnScreen = mTempArray;
            mDelegator.getLocationOnScreen(locationOnScreen);
            boundsInScreen.offset(locationOnScreen[0], locationOnScreen[1]);
            scaleRect(boundsInParent, applicationScale);
            info.setBoundsInScreen(boundsInScreen);

            if (mAccessibilityFocusedView != View.NO_ID) {
                info.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            } else {
                info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            }
            if (mDelegator.isEnabled()) {
                if (getWrapSelectorWheel() || getValue() < getMaxValue()) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                }
                if (getWrapSelectorWheel() || getValue() > getMinValue()) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                }
            }

            return info;
        }

        private void scaleRect(@NonNull Rect rect, float scale) {
            if (scale != 1.0f) {
                rect.left = (int) ((((float) rect.left) * scale) + 0.5f);
                rect.top = (int) ((((float) rect.top) * scale) + 0.5f);
                rect.right = (int) ((((float) rect.right) * scale) + 0.5f);
                rect.bottom = (int) ((((float) rect.bottom) * scale) + 0.5f);
            }
        }

        private boolean hasVirtualDecrementButton() {
            return getWrapSelectorWheel() || getValue() > getMinValue();
        }

        private boolean hasVirtualIncrementButton() {
            return getWrapSelectorWheel() || getValue() < getMaxValue();
        }

        private String getVirtualDecrementButtonText() {
            final int interval = mWheelInterval != DEFAULT_WHEEL_INTERVAL
                    && mCustomWheelIntervalMode
                        ? mWheelInterval : 1;
            int value = mValue - interval;
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            if (value >= mMinValue) {
                return (mDisplayedValues == null)
                        ? formatNumber(value) : mDisplayedValues[value - mMinValue];
            }
            return null;
        }

        private String getVirtualIncrementButtonText() {
            final int interval = mWheelInterval != DEFAULT_WHEEL_INTERVAL
                    && mCustomWheelIntervalMode
                        ? mWheelInterval : 1;
            int value = mValue + interval;
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            if (value <= mMaxValue) {
                return (mDisplayedValues == null)
                        ? formatNumber(value) : mDisplayedValues[value - mMinValue];
            }
            return null;
        }

        String getVirtualCurrentButtonText(boolean showContentDescription) {
            int value = mValue;
            if (mWrapSelectorWheel) {
                value = getWrappedSelectorIndex(value);
            }
            String text = null;
            if (value <= mMaxValue) {
                text = (mDisplayedValues == null)
                        ? formatNumber(value) : mDisplayedValues[value - mMinValue];
            }
            return text != null && showContentDescription
                    ? text + ", " + mPickerContentDescription : text;
        }
    }

    static private String formatNumberWithLocale(int value) {
        return String.format(Locale.getDefault(), "%d", value);
    }

    @Override
    public void setMaxInputLength(int limit) {
        InputFilter[] filterArr = mInputText.getFilters();
        InputFilter backupFilter = filterArr[0];
        InputFilter lengthFilter = new InputFilter.LengthFilter(limit);
        mInputText.setFilters(new InputFilter[]{backupFilter, lengthFilter});
    }

    @Override
    public EditText getEditText() {
        return mInputText;
    }

    @Override
    public void setMonthInputMode() {
        mInputText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
        mInputText.setPrivateImeOptions(INPUT_TYPE_MONTH);
        mInputText.setText("");
    }

    @Override
    public void setYearDateTimeInputMode() {
        mInputText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
        mInputText.setPrivateImeOptions(INPUT_TYPE_YEAR_DATE_TIME);
        mInputText.setText("");
    }

    private boolean isCharacterNumberLanguage() {
        final String language = Locale.getDefault().getLanguage();
        return "ar".equals(language) || "fa".equals(language) || "my".equals(language);
    }

    boolean needCompareEqualMonthLanguage() {
        return "vi".equals(Locale.getDefault().getLanguage())
                && INPUT_TYPE_MONTH.equals(mInputText.getPrivateImeOptions());
    }

    private boolean isHighContrastFontEnabled() {
        return SeslViewReflector.isHighContrastTextEnabled(mInputText);
    }

    private boolean updateBoldTextEnabledInSettings() {
        if (Build.VERSION.SDK_INT < 28) {
            return false;
        }
        mIsBoldTextEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                "bold_text", 0) != 0;
        return mIsBoldTextEnabled;
    }
}
