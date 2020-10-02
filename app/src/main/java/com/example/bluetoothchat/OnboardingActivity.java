package com.example.bluetoothchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.example.bluetoothchat.onboardingframents.OnboradingFragment1;
import com.example.bluetoothchat.onboardingframents.OnbordingFragment2;
import com.example.bluetoothchat.onboardingframents.WelcomeFragment;
import com.gc.materialdesign.views.ButtonFlat;
import com.ogaclejapan.smarttablayout.SmartTabLayout;

public class OnboardingActivity extends FragmentActivity {
    private ViewPager pager;
    private SmartTabLayout indicator;
    private ButtonFlat skip;
    private ButtonFlat next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        pager = (ViewPager) findViewById(R.id.pager);
        indicator = (SmartTabLayout) findViewById(R.id.indicator);
        skip = (ButtonFlat) findViewById(R.id.skip);
        next = (ButtonFlat) findViewById(R.id.next);

        pager.setAdapter(adapter);
        indicator.setViewPager(pager);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishboarding();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pager.getCurrentItem() == 2){
                    finishboarding();
                }else {
                    pager.setCurrentItem(pager.getCurrentItem()+1,true);
                }
            }
        });

        indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 2){
                    skip.setVisibility(View.GONE);
                    next.setText("Done");
                }else {
                    skip.setVisibility(View.VISIBLE);
                    next.setText("Next");
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
    FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:{
                    return new WelcomeFragment();
                }
                case 1: {
                    return new OnboradingFragment1();
                }
                case 2:{
                    return new OnbordingFragment2();
                }
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };
    private void finishboarding(){
        //get the shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("Bluetooth_messenger",MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean("Onboarding_completed",true).apply();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }



}
