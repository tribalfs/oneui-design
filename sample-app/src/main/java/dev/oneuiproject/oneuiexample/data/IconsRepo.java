package dev.oneuiproject.oneuiexample.data;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class IconsRepo {

    public IconsRepo() {
    }

    // Method to retrieve icons
    public List<Integer> getIcons() {
        final List<Integer> mIconsId = new ArrayList<>();
        Class<dev.oneuiproject.oneui.R.drawable> rClass = dev.oneuiproject.oneui.R.drawable.class;
        for (Field field : rClass.getDeclaredFields()) {
            try {
                mIconsId.add(field.getInt(null));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return mIconsId;
    }
}
