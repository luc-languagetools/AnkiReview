package com.luc.ankireview;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.luc.ankireview.style.CardField;
import com.luc.ankireview.style.CardStyle;
import com.luc.ankireview.style.CardTemplate;
import com.luc.ankireview.style.CardTemplateKey;
import com.luc.ankireview.style.FieldListAdapter;
import com.luc.ankireview.style.ItemTouchCallback;
import com.luc.ankireview.style.ValueSlider;
import com.luc.ankireview.style.ValueSliderUpdate;
import com.thebluealliance.spectrum.SpectrumPalette;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

import java.util.Vector;

public class CardStyleActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener, OnSeekChangeListener {
    private static final String TAG = "CardStyleActivity";
    public static final double TEXT_RELATIVE_SIZE_FACTOR = 10.0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardstyle);


        m_cardStyle = new CardStyle(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.review_toolbar);
        toolbar.setTitle(R.string.card_style);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        long noteId = intent.getLongExtra("noteId", 0l);
        int cardOrd = intent.getIntExtra("cardOrd", 0);

        Log.v(TAG, "starting CardStyleActivity with noteId: " + noteId + " cardOrd: " + cardOrd);

        // retrieve the appropriate card
        m_card = AnkiUtils.retrieveCard(getContentResolver(), noteId, cardOrd);

        // retrieve the card template
        m_cardTemplateKey = new CardTemplateKey(m_card.getModelId(), m_card.getCardOrd());
        m_cardTemplate = m_cardStyle.getCardTemplate(m_cardTemplateKey);

        Log.v(TAG, "num question card fields: " + m_cardTemplate.getQuestionCardFields().size());

        m_cardstyleTabs = findViewById(R.id.cardstyle_tabs);
        m_cardstyleTabs.addOnTabSelectedListener(this);


        m_cardstyleEditorCards = findViewById(R.id.cardstyle_editor_cards);
        m_cardStyle.renderCard(m_card, m_cardstyleEditorCards);

        // get font view and margins view
        m_fieldSettingsView = findViewById(R.id.cardstyle_editor_fieldsettings);
        m_fontView = findViewById(R.id.cardstyle_editor_font);
        m_marginsView = findViewById(R.id.cardstyle_editor_margins);

        // setup the full Field list ListView
        m_fullFieldListView = findViewById(R.id.cardstyle_editor_all_fields);
        m_fullFieldListView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        m_fullFieldListView.setLayoutManager(linearLayoutManager);

        // get field list
        Vector<String> fullFieldList = new Vector<String>();
        for (String field : m_card.getFieldMap().keySet())
        {
            fullFieldList.add(field);
        }

        m_fieldListAdapter = new FieldListAdapter(this, m_cardTemplate, fullFieldList);
        m_fullFieldListView.setAdapter(m_fieldListAdapter);

        ItemTouchCallback itemTouchCallback = new ItemTouchCallback(m_fieldListAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(itemTouchCallback);
        m_fieldListAdapter.setTouchHelper(touchHelper);
        touchHelper.attachToRecyclerView(m_fullFieldListView);

        // set visibility of tabs
        m_fullFieldListView.setVisibility(View.VISIBLE);
        m_fieldSettingsView.setVisibility(View.INVISIBLE);
        m_fontView.setVisibility(View.INVISIBLE);
        m_marginsView.setVisibility(View.INVISIBLE);

        // field settings controls
        // -----------------------
        m_field_fieldName = findViewById(R.id.cardstyle_field_fieldname);
        m_field_back_fieldlist = findViewById(R.id.cardstyle_field_back_to_fieldlist);
        m_field_back_fieldlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backToFieldList();
            }
        });

        m_field_relativesize_isb = findViewById(R.id.cardstyle_text_relativesize_isb);

        m_field_colorPalette = findViewById(R.id.cardstyle_text_color);
        m_field_colorPalette.setOnColorSelectedListener(new SpectrumPalette.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                Log.v(TAG, "color selected: " + color);
                m_currentCardField.setColor(color);
                updateCardPreview();
            }
        });

        // text controls
        // -------------
        m_text_basesize_isb = findViewById(R.id.cardstyle_text_basesize_isb);
        m_text_basesize_isb.setOnSeekChangeListener(this);
        m_text_basesize_isb.setProgress(m_cardTemplate.getBaseTextSize());
        m_text_font_family = findViewById(R.id.cardstyle_editor_font_family);
        m_text_font_family.setText(m_cardTemplate.getFont());
        m_text_font_lookup = findViewById(R.id.cardstyle_editor_font_lookup);
        m_text_font_lookup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fontFamily = m_text_font_family.getText().toString();
                m_cardTemplate.setFont(fontFamily);
                updateCardPreview();
            }
        });

        // margin controls
        // ---------------

        ValueSlider valueSliderMarginLeftRight = findViewById(R.id.cardstyle_margin_leftright_valueslider);
        valueSliderMarginLeftRight.setCurrentValue(m_cardTemplate.getLeftRightMargin());
        valueSliderMarginLeftRight.setListener(new ValueSliderUpdate() {
            @Override
            public void valueUpdate(int currentValue) {
                m_cardTemplate.setLeftRightMargin(currentValue);
                updateCardPreview();
            }
        });

        ValueSlider valueSliderBetween = findViewById(R.id.cardstyle_margin_between_valueslider);
        valueSliderBetween.setCurrentValue(m_cardTemplate.getLeftRightMargin());
        valueSliderBetween.setListener(new ValueSliderUpdate() {
            @Override
            public void valueUpdate(int currentValue) {
                m_cardTemplate.setCenterMargin(currentValue);
                updateCardPreview();
            }
        });


        ValueSlider valueSliderPaddingTop = findViewById(R.id.cardstyle_padding_top_valueslider);
        valueSliderPaddingTop.setCurrentValue(m_cardTemplate.getPaddingTop());
        valueSliderPaddingTop.setListener(new ValueSliderUpdate() {
            @Override
            public void valueUpdate(int currentValue) {
                m_cardTemplate.setPaddingTop(currentValue);
                updateCardPreview();
            }
        });


        ValueSlider valueSliderPaddingBottom = findViewById(R.id.cardstyle_padding_bottom_valueslider);
        valueSliderPaddingBottom.setCurrentValue(m_cardTemplate.getPaddingBottom());
        valueSliderPaddingBottom.setListener(new ValueSliderUpdate() {
            @Override
            public void valueUpdate(int currentValue) {
                m_cardTemplate.setPaddingBottom(currentValue);
                updateCardPreview();
            }
        });

        ValueSlider valueSliderPaddingLeftRight = findViewById(R.id.cardstyle_padding_leftright_valueslider);
        valueSliderPaddingLeftRight.setCurrentValue(m_cardTemplate.getPaddingLeftRight());
        valueSliderPaddingLeftRight.setListener(new ValueSliderUpdate() {
            @Override
            public void valueUpdate(int currentValue) {
                m_cardTemplate.setPaddingLeftRight(currentValue);
                updateCardPreview();
            }
        });


        m_field_relativesize_isb.setOnSeekChangeListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cardstyle_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cardstyle_save:
                Log.v(TAG, "save card style");
                saveCardStyle();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab)
    {

    }

    @Override
    public void onTabSelected(TabLayout.Tab tab)
    {
        Log.v(TAG, "onTabSelected " + tab.getPosition());

        if( tab.getPosition() == 0 ) {
            m_fullFieldListView.setVisibility(View.VISIBLE);
            m_fieldSettingsView.setVisibility(View.INVISIBLE);
            m_fontView.setVisibility(View.INVISIBLE);
            m_marginsView.setVisibility(View.INVISIBLE);
        } else if( tab.getPosition() == 1 ) {
            m_fullFieldListView.setVisibility(View.INVISIBLE);
            m_fieldSettingsView.setVisibility(View.INVISIBLE);
            m_fontView.setVisibility(View.VISIBLE);
            m_marginsView.setVisibility(View.INVISIBLE);
        } else if( tab.getPosition() == 2 ) {
            m_fullFieldListView.setVisibility(View.INVISIBLE);
            m_fieldSettingsView.setVisibility(View.INVISIBLE);
            m_fontView.setVisibility(View.INVISIBLE);
            m_marginsView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab)
    {

    }

    @Override
    public void onSeeking(SeekParams seekParams) {
        /*
        if( seekParams.seekBar == m_margin_leftright_isb) {
            Log.v(TAG, "left/right margin: " + seekParams.progress);
            m_cardTemplate.setLeftRightMargin(seekParams.progress);
        } else if( seekParams.seekBar == m_margin_center_isb ) {
            Log.v(TAG, "left/right margin: " + seekParams.progress);
            m_cardTemplate.setCenterMargin(seekParams.progress);
        } else if ( seekParams.seekBar == m_text_basesize_isb) {
            m_cardTemplate.setBaseTextSize(seekParams.progress);
        } else if ( seekParams.seekBar == m_padding_bottom_isb) {
            m_cardTemplate.setPaddingBottom(seekParams.progress);
        } else if ( seekParams.seekBar == m_padding_top_isb) {
            m_cardTemplate.setPaddingTop(seekParams.progress);
        } else if ( seekParams.seekBar == m_padding_leftright_isb) {
            m_cardTemplate.setPaddingLeftRight(seekParams.progress);
        } else if ( seekParams.seekBar == m_field_relativesize_isb) {
            float progress = seekParams.progressFloat;
            m_currentCardField.setRelativeSize((float) (progress / TEXT_RELATIVE_SIZE_FACTOR));
        }
        updateCardPreview();
        */
    }

    @Override
    public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
    }

    public void updateCardPreview() {
        m_cardStyle.renderCard(m_card, m_cardstyleEditorCards);
    }

    public void saveCardStyle() {
        m_cardStyle.saveCardStyleData();
    }

    public void openFieldSettings(CardField cardField) {
        Log.v(TAG, "openFieldSettings " + cardField.getFieldName());

        m_currentCardField = cardField;

        m_fullFieldListView.setVisibility(View.INVISIBLE);
        m_fieldSettingsView.setVisibility(View.VISIBLE);
        m_fontView.setVisibility(View.INVISIBLE);
        m_marginsView.setVisibility(View.INVISIBLE);

        m_field_fieldName.setText(cardField.getFieldName());

        m_field_relativesize_isb.setProgress((float) (cardField.getRelativeSize() * TEXT_RELATIVE_SIZE_FACTOR));
    }

    public void backToFieldList() {
        m_fullFieldListView.setVisibility(View.VISIBLE);
        m_fieldSettingsView.setVisibility(View.INVISIBLE);
        m_fontView.setVisibility(View.INVISIBLE);
        m_marginsView.setVisibility(View.INVISIBLE);
    }

    public CardStyle getCardStyle() {
        return m_cardStyle;
    }

    private CardStyle m_cardStyle;

    private FrameLayout m_fieldSettingsView;
    private FrameLayout m_fontView;
    private FrameLayout m_marginsView;

    // views
    private RecyclerView m_fullFieldListView;
    private FieldListAdapter m_fieldListAdapter;

    private TabLayout m_cardstyleTabs;

    private LinearLayout m_cardstyleEditorCards;


    // field setting controls
    private TextView m_field_fieldName;
    private Button m_field_back_fieldlist;
    private IndicatorSeekBar m_field_relativesize_isb;
    private SpectrumPalette m_field_colorPalette;

    // text controls
    private TextView m_text_font_family;
    private Button m_text_font_lookup;
    private IndicatorSeekBar m_text_basesize_isb;

    // margin controls

    private CardTemplateKey m_cardTemplateKey;
    private CardTemplate m_cardTemplate;
    private Card m_card;

    private CardField m_currentCardField = null;

}
