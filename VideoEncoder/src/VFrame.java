
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

public class VFrame {
    VFrame() {
        pixelData = null;
        frameNum = 0;
    }

    VFrame(int numSize, int fnum, int width, int height) {
        pixelData = new byte[numSize];
        frameNum = fnum;
        this.width = width;
        this.height = height;
        numPixels = width * height;
    }

    VFrame(BufferedImage img, int fnum) {
        DataBufferByte b = (DataBufferByte) img.getRaster().getDataBuffer();
        pixelData = b.getData();
        width = img.getWidth();
        height = img.getHeight();
        numPixels = height * width;
        frameNum = fnum;
    }

    BufferedImage toBufferedImage(int width, int height) {
        int [] pInt = new int[numPixels];
        BufferedImage img = new BufferedImage(width, height, 10);
        WritableRaster raster = (WritableRaster) img.getData();
        for(int i = 0;i < numPixels;i++) {
            pInt[i] = (byte) pixelData[i];
        }
        raster.setPixels(0,0,width,height,pInt);
        img.setData(raster);
        return img;
    }

    byte [] pixelData;
    int width, height;
    int numPixels;
    int frameNum;
}
