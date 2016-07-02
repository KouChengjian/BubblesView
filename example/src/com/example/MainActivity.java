package com.example;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.example.v.BubblesView;
import com.example.v.PeopleDistributionInfo;

public class MainActivity extends Activity {

	private BubblesView mCircleView;
    private List<PeopleDistributionInfo> mInfoList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mCircleView = new BubblesView(this);
		RelativeLayout rl = (RelativeLayout)findViewById(R.id.rlLayout);
		rl.addView(mCircleView);
		
		
//		mCircleView = (BubblesView) findViewById(R.id.circle_view);
//		
//		
//        mInfoList = new ArrayList<>();
//        mInfoList.add(new PeopleDistributionInfo("area1", 9));
//        mInfoList.add(new PeopleDistributionInfo("area2", 8));
//        mInfoList.add(new PeopleDistributionInfo("area3", 7));
//        mInfoList.add(new PeopleDistributionInfo("area4", 6));
//        mInfoList.add(new PeopleDistributionInfo("area5", 5));
//        mInfoList.add(new PeopleDistributionInfo("area6", 4));
//        mInfoList.add(new PeopleDistributionInfo("area7", 3));
//        mInfoList.add(new PeopleDistributionInfo("area8", 2));
//
//        mCircleView.bindData(mInfoList);
		
		
	}

}
