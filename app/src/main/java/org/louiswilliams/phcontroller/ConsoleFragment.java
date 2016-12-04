package org.louiswilliams.phcontroller;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import org.louiswilliams.phcontroller.databinding.FragmentConsoleBinding;

import pl.pawelkleczkowski.customgauge.CustomGauge;

import static android.R.attr.value;


public class ConsoleFragment extends Fragment {

    DisplayActivity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Require parent activity to be DisplayActivity
        if (getActivity() instanceof DisplayActivity) {
            mActivity = (DisplayActivity) getActivity();
        } else {
            return null;
        }

        FragmentConsoleBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_console, container, false);
        binding.setCarData(mActivity.getCarData());

        View layout = binding.getRoot();

        final Button logButton = (Button) layout.findViewById(R.id.log_button);
        logButton.setOnClickListener(mActivity.getOnLogButtonListener());

        final Button auxButton = (Button) layout.findViewById(R.id.aux_button);
        auxButton.setOnClickListener(mActivity.getOnAuxButtonListener());

        final ProgressBar currentPositive = (ProgressBar) layout.findViewById(R.id.current_positive);
        final ProgressBar currentNegative = (ProgressBar) layout.findViewById(R.id.current_negative);
        final CustomGauge motorRpm = (CustomGauge) layout.findViewById(R.id.motor_rpm_gauge);
        final CustomGauge engineRpm = (CustomGauge) layout.findViewById(R.id.engine_rpm_gauge);

        CarData carData = mActivity.getCarData();

        // Update progress bar for motor current
        carData.setDataListener(CarData.MOTOR_CURRENT, new CarData.CarDataListener() {
            @Override
            public void onDataChange(double value) {
                if (value > 0) {
                    currentNegative.setProgress(100);
                    currentPositive.setProgress((int) (value/100));
                } else {
                    currentNegative.setProgress(100 + (int)(value/100));
                    currentPositive.setProgress(0);
                }
            }
        });

        carData.setDataListener(CarData.ENGINE_RPM, new CarData.CarDataListener() {
            @Override
            public void onDataChange(final double value) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        engineRpm.setValue((int)value);
                    }
                });
            }
        });

        carData.setDataListener(CarData.MOTOR_RPM, new CarData.CarDataListener() {
            @Override
            public void onDataChange(final double value) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        motorRpm.setValue((int)value);
                    }
                });
            }
        });



        return layout;
    }


}
