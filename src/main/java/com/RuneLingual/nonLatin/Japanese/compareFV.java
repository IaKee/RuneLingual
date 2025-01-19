package com.RuneLingual.nonLatin.Japanese;

import java.util.Comparator;
import com.RuneLingual.nonLatin.Japanese.Rom2hira.FourValues;

public class compareFV implements Comparator<FourValues> {
    public int compare(FourValues fv1, FourValues fv2) {
        return Integer.compare(fv1.getRank(), fv2.getRank());
    }
}
