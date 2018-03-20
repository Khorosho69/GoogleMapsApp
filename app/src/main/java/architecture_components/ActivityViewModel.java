package architecture_components;

import android.arch.lifecycle.ViewModel;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class ActivityViewModel extends ViewModel{

    private List<MarkerOptions> mMarkers;

    private CameraPosition mCameraPosition;

    public List<MarkerOptions> getMarkers() {
        return mMarkers;
    }

    public void setMarkers(List<MarkerOptions> markers) {
        mMarkers = markers;
    }

    public CameraPosition getCameraPosition() {
        return mCameraPosition;
    }

    public void setCameraPosition(CameraPosition cameraPosition) {
        this.mCameraPosition = cameraPosition;
    }
}
