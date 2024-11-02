# We are accessing this using reflection in TipPopup
-keep class androidx.appcompat.animation.SeslElasticInterpolator {
    <init>(float, float);
    public *;
}