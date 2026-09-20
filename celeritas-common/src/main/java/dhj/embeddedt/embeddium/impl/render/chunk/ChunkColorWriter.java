package dhj.embeddedt.embeddium.impl.render.chunk;

import dhj.embeddedt.embeddium.api.util.ColorABGR;
import dhj.embeddedt.embeddium.api.util.ColorMixer;

public enum ChunkColorWriter {
    SEPARATE_AO {
        @Override
        public int writeColor(int colorWithAlpha, float aoValue) {
            return ColorABGR.withAlpha(colorWithAlpha, aoValue);
        }
    },
    EMBEDDIUM {
        @Override
        public int writeColor(int colorWithAlpha, float aoValue) {
            return ColorMixer.mulSingleWithoutAlpha(colorWithAlpha, (int)(aoValue * 255));
        }
    };

    public abstract int writeColor(int colorWithAlpha, float aoValue);
}
