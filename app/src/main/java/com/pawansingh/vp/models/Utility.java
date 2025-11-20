package com.pawansingh.vp.models;

import java.util.Locale;

public abstract class Utility {
    public static String timeConversion(Long millie){
            long seconds = (millie/1000);
            long sec = seconds % 60;
            long min = (seconds/60)%60;
            long hrs = (seconds/(60 * 60))% 24;

            if (hrs == 0){
                return String.valueOf(min).concat(":".concat(String.format(Locale.UK,"%02d",sec)));
            }else {
                return String.valueOf(hrs).concat(":".concat(String.format(Locale.UK,"%02d",min)
                        .concat(":".concat(String.format(Locale.UK,"%02d",sec)))));
            }

    }
}
