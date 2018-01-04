import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class VSegment{
    VSegment() {
        vSegIn = null;
    }

    void addFrames(VFrame [] frameList) {
        vSegIn = frameList;
        numFrames = frameList.length;
    }

    ArrayList<TreeMap<Byte,Byte>> compressSegment(String fileName, int epsilon) throws IOException {
        ArrayList<TreeMap<Byte,Byte>> data = new ArrayList<>();
        TreeMap<Byte,Byte> sigPixels;
        vSegInNumPixels = vSegIn[0].width * vSegIn[0].height;
        FileOutputStream fos = new FileOutputStream(".dpcomp.temp");
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bos.write(ByteBuffer.allocate(1).put((byte)numFrames).array());
        bos.write(ByteBuffer.allocate(4).putInt(vSegIn[0].width).array());
        bos.write(ByteBuffer.allocate(4).putInt(vSegIn[0].height).array());
        for(Integer i = 0;i < vSegInNumPixels;i++) {
            sigPixels = runDPAlgorithm((byte) 0, (byte) (vSegIn.length - 1), i, epsilon);
			if(sigPixels.size() != 2) {
				data.add(sigPixels);
				bos.write(ByteBuffer.allocate(4).putInt(i).array());
				bos.write(2 * (sigPixels.size() - 2));
				for(byte j : sigPixels.keySet()) {
					if(j != 0 && j != numFrames - 1) {
						bos.write(j);
						bos.write(sigPixels.get(j));
					}
				}
			}
        }
        bos.flush();
        bos.close();
        FileChannel src1 = new FileInputStream(image1).getChannel();
        FileChannel src2 = new FileInputStream(image2).getChannel();
        FileChannel cmp1 = new FileInputStream(".dpcomp.temp").getChannel();
        FileChannel dest = new FileOutputStream(fileName).getChannel();
        dest.position(4);
        long size1 = src1.transferTo(0, src1.size(),dest);
        dest.position(size1 + 8);
        long size2 = src2.transferTo(0, src2.size(),dest);
        dest.position(size1 + size2 + 12);
        long size3 = cmp1.transferTo(0, cmp1.size(),dest);
        dest.write(ByteBuffer.wrap(ByteBuffer.allocate(4).putInt((int) src1.size()).array()),0);
        dest.write(ByteBuffer.wrap(ByteBuffer.allocate(4).putInt((int) size2).array()),size1 + 4);
        dest.write(ByteBuffer.wrap(ByteBuffer.allocate(4).putInt((int) size3).array()),size1 + size2 + 8);
        src1.close();
        src2.close();
        cmp1.close();
        dest.close();
        Files.deleteIfExists(Paths.get(".dpcomp.temp"));
        return data;
    }

    private BufferedImage [] toBufferedImage(int width, int height) {
        BufferedImage [] img = new BufferedImage[numFrames];
        for(int i = 0;i < numFrames;i++)
            img[i] = vSegOut[i].toBufferedImage(width,height);
        return img;
    }

    BufferedImage [] decompressSegment(String fname) {
        int index = 0, ind = 0;
        Byte keyStart;
        Byte keyEnd;
        byte num;
        ArrayList<TreeMap<Byte,Byte>> arr = new ArrayList<>();
        TreeMap<Byte,Byte> temp = new TreeMap<>();
        int width, height;
        ByteBuffer b = null;
        Path file = Paths.get(fname);
        try {
            b = ByteBuffer.wrap(Files.readAllBytes(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        int size1 = b.getInt();
        byte [] imageByte1 = new byte[size1];
        b.get(imageByte1, 0, size1);
        int size2 = b.getInt();
        byte [] imageByte2 = new byte[size2];
        b.get(imageByte2, 0, size2);
        byte [] imgSpatialData1 = null, imgSpatialData2 = null;
        try {
            imgSpatialData1 = ((DataBufferByte)ImageIO.read(new ByteArrayInputStream(imageByte1)).getRaster().getDataBuffer()).getData();
            imgSpatialData2  = ((DataBufferByte)ImageIO.read(new ByteArrayInputStream(imageByte2)).getRaster().getDataBuffer()).getData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int encodedFileSize = b.getInt();
        numFrames = b.get();
        width = b.getInt();
        height = b.getInt();
        encodedFileSize = encodedFileSize - 9;
        System.out.println(width+" "+height);
        for(int i = 0;i < width * height;i++) {
            temp.put((byte) 0,imgSpatialData1[i]);
            temp.put((byte) (numFrames - 1), imgSpatialData2[i]);
            arr.add(new TreeMap<>(temp));
            temp.clear();
        }
        while(ind < encodedFileSize) {
            index = b.getInt();
            num = b.get();
            for(int i = 0;i < num ;i+=2) {
                byte key = b.get();
                byte val = b.get();
                arr.get(index).put(key, val);
            }
            //temp.clear();
            ind += (5 + num);
        }
        //System.out.println(arr);
        int numPixels = width * height;
        BufferedImage [] img = null;
        vSegOut = new VFrame[numFrames];
        for(int i = 0;i < numFrames;i++)
            vSegOut[i] = new VFrame(numPixels, i, width, height);
        for(int i = 0;i < numPixels;i++) {
            keyEnd = 0;
            for(int j = 0;j < arr.get(i).size() - 1;j++) {
                keyStart = keyEnd;
                keyEnd = arr.get(i).ceilingKey((byte) (keyStart + 1));
                vSegOut[keyStart].pixelData[i] = arr.get(i).get(keyStart);
                vSegOut[keyEnd].pixelData[i] = arr.get(i).get(keyEnd);
                for(int k = keyStart + 1;k < keyEnd;k++)
                    vSegOut[k].pixelData[i] = (byte) ((vSegOut[keyStart].pixelData[i] * (keyEnd - k) + (k - keyStart) * vSegOut[keyEnd].pixelData[i]) / (keyEnd - keyStart));
            }
        }
        img = toBufferedImage(width, height);
        return img;
    }

    private TreeMap<Byte,Byte> runDPAlgorithm(byte startTempIndex, byte endTempIndex, int spatialIndex, int epsilon) {
        byte index = 0;
        int maxDist = 0;
        int dist = 0;
        TreeMap<Byte,Byte> sigPixels = new TreeMap<>();
        byte startIndexPixelValue = vSegIn[startTempIndex].pixelData[spatialIndex];
        byte endIndexPixelValue = vSegIn[endTempIndex].pixelData[spatialIndex];
        for(byte i = (byte) (startTempIndex + 1); i < endTempIndex; i++) {
            dist = Math.abs((endIndexPixelValue - startIndexPixelValue) * i - (endTempIndex - startTempIndex) * vSegIn[i].pixelData[spatialIndex]
                            + endTempIndex * startIndexPixelValue - endIndexPixelValue * startTempIndex);
            if(dist > maxDist) {
                index = i;
                maxDist = dist;
            }
        }
        if(maxDist > epsilon) {
            sigPixels.putAll(runDPAlgorithm(startTempIndex, index, spatialIndex, epsilon));
            sigPixels.putAll(runDPAlgorithm(index, endTempIndex, spatialIndex, epsilon));
        }
        else {
            sigPixels.put(startTempIndex, startIndexPixelValue);
            sigPixels.put(endTempIndex, endIndexPixelValue);
        }
        return sigPixels;
    }

    void setRefImageFileNames(String img1, String img2) {
        image1 = img1;
        image2 = img2;
    }

    private VFrame [] vSegIn;
    private int vSegInNumPixels;
    private String image1, image2;
    private int numFrames;
    private VFrame [] vSegOut;
}
