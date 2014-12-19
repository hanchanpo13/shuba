package com.qiwenge.android.act;

import java.util.ArrayList;
import java.util.List;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dev1024.utils.GsonUtils;
import com.loopj.android.http.RequestParams;
import com.qiwenge.android.R;
import com.qiwenge.android.adapters.ChapterAdapter;
import com.qiwenge.android.base.BaseActivity;
import com.qiwenge.android.entity.Book;
import com.qiwenge.android.entity.Chapter;
import com.qiwenge.android.entity.ChapterList;
import com.qiwenge.android.utils.ApiUtils;
import com.qiwenge.android.utils.BookShelfUtils;
import com.qiwenge.android.utils.SkipUtils;
import com.qiwenge.android.utils.ThemeUtils;
import com.qiwenge.android.utils.http.JHttpClient;
import com.qiwenge.android.utils.http.StringResponseHandler;

/**
 * 目录。
 * <p/>
 * Created by John on 2014-5-5
 */
public class ChapterActivity extends BaseActivity {

    public static final String EXTRA_BOOK = "book";

    public static final String EXTRA_BOOK_ID = "book_id";

    public static final String EXTRA_BOOK_TITLE = "book_title";

    private ListView lvChapters;
    private View emptyView;
    private RelativeLayout layoutContainer;
    private ProgressBar ivLoading;

    private ChapterAdapter adapter;

    private List<Chapter> list = new ArrayList<Chapter>();

    private Book book;

    private String bookId;

    private String bookTitle;

    private int pageindex = 1;

    private boolean isInited = false;

    private int lastNumber = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter);
        initViews();
        getIntentData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.setThemeBg(layoutContainer);
        selectedReadNumber();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !isInited) {
            getBookChpaters();
            isInited = true;
        }
    }

    private void getIntentData() {
        Bundle extra = getIntent().getExtras();
        if (extra.containsKey(EXTRA_BOOK)) {
            book = extra.getParcelable(EXTRA_BOOK);
            bookId = book.getId();
            setTitle(book.title);
        } else if (extra.containsKey(EXTRA_BOOK_ID) && extra.containsKey(EXTRA_BOOK_TITLE)) {
            bookId = extra.getString(EXTRA_BOOK_ID);
            bookTitle = extra.getString(EXTRA_BOOK_TITLE);
            setTitle(bookTitle);
        }
    }

    private void initViews() {
        ivLoading = (ProgressBar) this.findViewById(R.id.pb_loading);
        layoutContainer = (RelativeLayout) this.findViewById(R.id.layout_container);

        adapter = new ChapterAdapter(this, list);
        lvChapters = (ListView) this.findViewById(R.id.listview_common);
        lvChapters.setAdapter(adapter);
        lvChapters.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < list.size()) {
                    SkipUtils.skipToReader(getApplicationContext(), book, list.get(position).getId());
                }
            }
        });
    }

    private void showEmptyView() {
        emptyView = LayoutInflater.from(this).inflate(R.layout.layout_empty, null);
        TextView tvEmpty = (TextView) emptyView.findViewById(R.id.tv_empty);
        ImageView ivEmpty = (ImageView) emptyView.findViewById(R.id.iv_empty);
        ivEmpty.setVisibility(View.GONE);
        tvEmpty.setText(R.string.empty_chapter);

        ViewGroup viewGroup = (ViewGroup) lvChapters.getParent();
        viewGroup.addView(emptyView, lvChapters.getLayoutParams());
        lvChapters.setEmptyView(emptyView);
    }

    /**
     * 获取一本书下的，所有章节。
     */
    private void getBookChpaters() {
        if (bookId == null) return;
        String url = ApiUtils.getBookChpaters();
        RequestParams params = new RequestParams();
        params.put("book_id", bookId);
        params.put("limit", "9999");
        params.put("page", "" + pageindex);
        JHttpClient.get(url, params, new StringResponseHandler() {

            @Override
            public void onStart() {
                super.onStart();
                ivLoading.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFinish() {
                if (list.isEmpty()) {
                    showEmptyView();
                }
                ivLoading.setVisibility(View.GONE);
            }

            @Override
            public void onSuccess(String result) {
                ChapterList list = GsonUtils.getModel(result, ChapterList.class);
                adapter.add(list.result);
                selectedReadNumber();
            }
        });
    }

    private ViewTreeObserver viewTreeObserver;

    private void selectedReadNumber() {
        final int number = BookShelfUtils.getReadNumber(getApplicationContext(), bookId) - 1;
        if (number < 0) return;
        if (number > adapter.getCount()) return;
        //改变颜色
        if (lastNumber >= 0 && lastNumber < adapter.getCount() && adapter.get(lastNumber) != null) {
            adapter.get(lastNumber).isSelected = false;
        }
        lastNumber = number;
        if (number >= 0 && number < adapter.getCount() && adapter.get(number) != null) {
            adapter.get(number).isSelected = true;
            adapter.notifyDataSetChanged();
        }

        //定位到阅读到的number，并滚动到中间
        viewTreeObserver = lvChapters.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (viewTreeObserver != null && viewTreeObserver.isAlive()) {

                        try {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                                viewTreeObserver.removeGlobalOnLayoutListener(this);
                            } else {
                                viewTreeObserver.removeOnGlobalLayoutListener(this);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                    int offset = Math.abs(lvChapters.getLastVisiblePosition() - lvChapters.getFirstVisiblePosition());
                    int selectedPostion = number - offset / 2;
                    if (selectedPostion < 0) selectedPostion = 0;

                    lvChapters.setSelection(selectedPostion);
                }
            });
        }

    }
}
