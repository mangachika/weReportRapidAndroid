package org.rapidandroid.activity;

import org.rapidandroid.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import org.rapidandroid.data.SurveyCreationConstants;

public class PhaseChooser extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.phase_chooser);
	}
	
	public void scopingPhase(View view) {
		Bundle extras = getIntent().getExtras();
		Intent intent = new Intent(this, QuestionChooser.class);
		intent.putExtras(extras);
		intent.putExtra("phase", SurveyCreationConstants.SCOPING);
	}
	
	public void projectPhase(View view) {
		Bundle extras = getIntent().getExtras();
		Intent intent = new Intent(this, QuestionChooser.class);
		intent.putExtras(extras);
		intent.putExtra("phase", SurveyCreationConstants.PROJECT);
		
	}
	
	public void analysisPhase(View view) {
		Bundle extras = getIntent().getExtras();
		Intent intent = new Intent(this, SurveyCreator.class);
		intent.putExtras(extras);
		intent.putExtra("phase", SurveyCreationConstants.ANALYSIS);
		startActivity(intent);
	}
}
