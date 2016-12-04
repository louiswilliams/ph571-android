package org.louiswilliams.phcontroller;

import android.databinding.DataBindingUtil;
import android.databinding.tool.reflection.SdkUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.louiswilliams.phcontroller.databinding.FragmentAuxBinding;

public class AuxFragment extends Fragment {

    private DisplayActivity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Require parent activity to be DisplayActivity
        if (getActivity() instanceof DisplayActivity) {
            mActivity = (DisplayActivity) getActivity();
        } else {
            return null;
        }

        CarData carData = mActivity.getCarData();
        FragmentAuxBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_aux, container, false);
        binding.setCarData(carData);

        View root = binding.getRoot();


        return root;
    }
}
