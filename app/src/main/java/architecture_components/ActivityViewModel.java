package architecture_components;

import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class ActivityViewModel extends ViewModel {

    private List<MarkerOptions> mMarkers;

    private CameraPosition mCameraPosition;

    @Nullable
    public List<MarkerOptions> getMarkers() {
        return mMarkers;
    }

    public void clearList (){
        mMarkers = new ArrayList<>();
    }

    public void addMarker(MarkerOptions marker) {
        mMarkers.add(marker);
    }

    @Nullable
    public CameraPosition getCameraPosition() {
        return mCameraPosition;
    }

    public void setCameraPosition(CameraPosition cameraPosition) {
        this.mCameraPosition = cameraPosition;
    }
}
