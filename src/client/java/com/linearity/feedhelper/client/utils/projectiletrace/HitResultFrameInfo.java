package com.linearity.feedhelper.client.utils.projectiletrace;

import com.linearity.colorannoation.annoation.ARGBColor;
import org.jetbrains.annotations.Nullable;

public record HitResultFrameInfo(String printText, @ARGBColor int color, int x, int y,String currentHealthLine,String expectHitHealthLine){
}
