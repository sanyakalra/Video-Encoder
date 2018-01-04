import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

public class VideoEncoderMain {

    public static int epsilon = 1000;
    public static void printMenu()
    {
        System.out.println("To encode, give following arguments: encode [file1] [file2] ... [fileN] --output [outputFile]");
        System.out.println("To view, give following arguments: view [outputFile]");
    }

    public static void encode(String[] filenames, String outputFileName){
        String path1, path2;
        VSegment vSeg = new VSegment();
        ArrayList<TreeMap<Byte,Byte>> arr;

        String loc=System.getProperty("user.dir")+"\\src\\Images"; //Location where the images are stored.
        VFrame [] frames = new VFrame[filenames.length];
        try {
            vSeg.setRefImageFileNames(loc + "\\" + filenames[0], loc + "\\" + filenames[filenames.length - 1]);
            for (int i = 0; i < filenames.length; i++) {
                frames[i] = new VFrame(ImageIO.read(new File(loc + String.format("\\" + filenames[i], i))), i);
            }
            vSeg.addFrames(frames);
            arr = vSeg.compressSegment(outputFileName + ".sar", epsilon);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void decode(String outputFileName){
        VSegment vSeg = new VSegment();
        BufferedImage [] imgOut;
        VideoPlayer v;
        imgOut = vSeg.decompressSegment(outputFileName + ".sar");
        v = new VideoPlayer(imgOut,10);
        v.playVideo();
    }
    public static void main(String [] arg) {//throws IOException {

        if(arg.length < 2)
            printMenu();
        else if (arg[0].equals("encode"))
        {
            String[] filenames = new String[arg.length-3];
            String outputFilename;
            int end = arg.length - 3;
            for (int i = 0; i < end; i++)
            {
                filenames[i] = arg[i + 1];
            }
            outputFilename = arg[arg.length-1];
            encode(filenames, outputFilename);
        }
        else if (arg[0].equals("view"))
        {
            decode(arg[1]);
        }
        else
            printMenu();

    }
}
